/*
 * Flazr <http://flazr.com> Copyright (C) 2009  Peter Thomas.
 *
 * This file is part of Flazr.
 *
 * Flazr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Flazr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Flazr.  If not, see <http://www.gnu.org/licenses/>.
 */

package rtmp.flazr.rtmp;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public abstract class RtmpPublisher {

    ///////////////////////////////////////////////////////
    private static final Logger logger = LoggerFactory.getLogger(RtmpPublisher.class);

    private final Timer timer;
    private final int timerTickSize;
    private final boolean usingSharedTimer;
    private final boolean aggregateModeEnabled;

    private final RtmpReader reader;
    private final int streamId;
    private long startTime;
    private long seekTime;
    private long timePosition;
    private int currentConversationId;
    private int playLength = -1;
    private boolean paused;
    private int bufferDuration;
    ///////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////
    protected RtmpPublisher(final RtmpReader reader, final int streamId, final int bufferDuration,
                         boolean useSharedTimer, boolean aggregateModeEnabled) {
        this.aggregateModeEnabled = aggregateModeEnabled;
        this.usingSharedTimer = useSharedTimer;

        timer = new HashedWheelTimer(RtmpConfig.TIMER_TICK_SIZE, TimeUnit.MILLISECONDS);
        timerTickSize = RtmpConfig.TIMER_TICK_SIZE;

        this.reader = reader;
        this.streamId = streamId;
        this.bufferDuration = bufferDuration;

        logger.debug("[RtmpPublisher] [+] NEW RtmpPublisher is created. (streamId: [{}])", streamId);
    }
    ///////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////
    public void start(final Channel channel, final int seekTime, final int playLength, final RtmpMessage ... messages) {
        this.playLength = playLength;
        start(channel, seekTime, messages);
    }

    public void start(final Channel channel, final int seekTimeRequested, final RtmpMessage ... messages) {
        paused = false;
        currentConversationId++;
        startTime = System.currentTimeMillis();

        if (seekTimeRequested >= 0) {
            seekTime = reader.seek(seekTimeRequested);
        } else {
            seekTime = 0;
        }

        timePosition = seekTime;
        logger.debug("publish start, seek requested: {} actual seek: {}, play length: {}, conversation: {}",
                seekTimeRequested, seekTime, playLength, currentConversationId
        );

        for (final RtmpMessage message : messages) {
            writeToStream(channel, message);
        }

        for (final RtmpMessage message : reader.getStartMessages()) {
            writeToStream(channel, message);
        }

        write(channel);
    }

    public void pause() {
        paused = true;
        currentConversationId++;
    }

    private void stop(final Channel channel) {
        currentConversationId++;

        final long elapsedTime = System.currentTimeMillis() - startTime;
        logger.info("finished, start: {}, elapsed {}, streamed: {}",
                seekTime / 1000, elapsedTime / 1000, (timePosition - seekTime) / 1000
        );

        for (RtmpMessage message : getStopMessages(timePosition)) {
            writeToStream(channel, message);
        }
    }

    public void close() {
        if (!usingSharedTimer) {
            timer.stop();
        }

        reader.close();
    }
    ///////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////

    public boolean isStarted() {
        return currentConversationId > 0;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setBufferDuration(int bufferDuration) {
        this.bufferDuration = bufferDuration;
    }

    public boolean handle(final MessageEvent me) {
        if (me.getMessage() instanceof Event) {
            final Event pe = (Event) me.getMessage();
            if (pe.conversationId != currentConversationId) {
                logger.debug("stopping obsolete conversation id: {}, current: {}",
                        pe.getConversationId(), currentConversationId);
                return true;
            }

            write(me.getChannel());
            return true;
        }

        return false;
    }
    ///////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////
    private void writeToStream(final Channel channel, final RtmpMessage message) {
        if (message.getHeader().getChannelId() > 2) {
            message.getHeader().setStreamId(streamId);
            message.getHeader().setTime((int) timePosition);
        }

        channel.write(message);
    }

    private void write(final Channel channel) {
        if (!channel.isWritable()) {
            return;
        }

        final long writeTime = System.currentTimeMillis();
        final RtmpMessage message;

        synchronized (reader) {
            if (reader.hasNext()) {
                message = reader.next();
            } else {
                message = null;
            }
        }

        if (message == null || playLength >= 0
                && (timePosition > (seekTime + playLength))) {
            stop(channel);
            return;
        }

        final long elapsedTime = System.currentTimeMillis() - startTime;
        final long elapsedTimePlusSeek = elapsedTime + seekTime;
        final double clientBuffer = timePosition - elapsedTimePlusSeek;

        if (aggregateModeEnabled && (clientBuffer > timerTickSize)) {
            reader.setAggregateDuration((int) clientBuffer);
        } else {
            reader.setAggregateDuration(0);
        }

        final RtmpHeader header = message.getHeader();
        final double compensationFactor = clientBuffer / (bufferDuration + timerTickSize);
        final long delay = (long) ((header.getTime() - timePosition) * compensationFactor);

        if (logger.isDebugEnabled()) {
            logger.debug("[RtmpPublisher] elapsed: {}, streamed: {}, buffer: {}, factor: {}, delay: {}",
                    elapsedTimePlusSeek, timePosition, clientBuffer, compensationFactor, delay
            );
        }

        timePosition = header.getTime();
        header.setStreamId(streamId);

        final ChannelFuture future = channel.write(message);
        future.addListener(cf -> {
            final long completedIn = System.currentTimeMillis() - writeTime;
            if (completedIn > 2000) {
                logger.warn("[RtmpPublisher] channel busy? time taken to write last message: {}", completedIn);
            }

            final long delayToUse = clientBuffer > 0 ? delay - completedIn : 0;
            fireNext(channel, delayToUse);
        });
    }

    public void fireNext(final Channel channel, final long delay) {
        final Event readyForNext = new Event(currentConversationId);
        if (delay > timerTickSize) {
            timer.newTimeout(
                    timeout -> {
                        if (logger.isDebugEnabled()) {
                            logger.debug("[RtmpPublisher] running after delay: {}", delay);
                        }
                        if (readyForNext.conversationId != currentConversationId) {
                            logger.debug("[RtmpPublisher] pending 'next' event found obsolete, aborting");
                            return;
                        }

                        Channels.fireMessageReceived(channel, readyForNext);
                    }, delay, TimeUnit.MILLISECONDS
            );
        } else {
            Channels.fireMessageReceived(channel, readyForNext);
        }
    }

    protected abstract RtmpMessage[] getStopMessages(long timePosition);
    ///////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////
    /**
     * @author yama
     */
    public static class Event {

        private final int conversationId;

        public Event(final int conversationId) {
            this.conversationId = conversationId;
        }

        public int getConversationId() {
            return conversationId;
        }

    }
    ///////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////
    public Timer getTimer() {
        return timer;
    }

    public int getTimerTickSize() {
        return timerTickSize;
    }

    public boolean isUsingSharedTimer() {
        return usingSharedTimer;
    }

    public boolean isAggregateModeEnabled() {
        return aggregateModeEnabled;
    }

    public RtmpReader getReader() {
        return reader;
    }

    public int getStreamId() {
        return streamId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getSeekTime() {
        return seekTime;
    }

    public long getTimePosition() {
        return timePosition;
    }

    public int getCurrentConversationId() {
        return currentConversationId;
    }

    public int getPlayLength() {
        return playLength;
    }

    public int getBufferDuration() {
        return bufferDuration;
    }
    ///////////////////////////////////////////////////////

}
