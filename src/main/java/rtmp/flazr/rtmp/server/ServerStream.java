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
package rtmp.flazr.rtmp.server;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.base.PublishType;
import rtmp.flazr.rtmp.RtmpMessage;
import rtmp.flazr.rtmp.message.Metadata;
import rtmp.flazr.util.DateFormatUtil;
import rtmp.flazr.util.Utils;
import rtmp.metadata.AudioAttr;
import rtmp.metadata.VideoAttr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerStream {

    ///////////////////////////////////////////////////////
    private static final Logger logger = LoggerFactory.getLogger(ServerStream.class);
    private final long initiationTime = System.currentTimeMillis();

    private final String streamName;
    private final PublishType publishType;
    private ChannelGroup subscribers;
    private final List<String> subscriberChIds;
    private Channel publishChannel;
    private String publishChId;
    private final List<RtmpMessage> configMessages;
    private final Map<String, String> metadata;
    private boolean isPlayStream;
    ///////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////
    public ServerStream(final String rawName, final String typeString) {
        this.streamName = Utils.trimSlashes(rawName).toLowerCase();
        this.configMessages = new ArrayList<>();

        if (typeString != null) {
            // record, append
            this.publishType = PublishType.getType(typeString);
            initSubscribers();
        } else {
            // publish 없이 play 요청 온 Stream
            this.publishType = null;
            this.subscribers = null;
        }

        metadata = new HashMap<>();
        subscriberChIds = new ArrayList<>();
    }
    ///////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////
    public long getInitiationTime() {
        return initiationTime;
    }

    public String getInitiationTimeFormat() {
        return DateFormatUtil.formatYmdHmsS(initiationTime);
    }

    public String getStreamName() {
        return streamName;
    }

    public boolean isLive() {
        return PublishType.LIVE.equals(publishType);
    }

    public PublishType getPublishType() {
        return publishType;
    }

    public ChannelGroup getSubscribers() {
        return subscribers;
    }

    public void initSubscribers() {
        if (streamName != null && subscribers == null) {
            this.subscribers = new DefaultChannelGroup(streamName);
        }
    }

    public void addSubscriber(Channel channel) {
        if (subscribers != null) {
            subscribers.add(channel);
            subscriberChIds.add(channel.getId() + "");
        } else {
            logger.warn("({}) [ServerStream] [{}:{}] subscribers is Null, Fail to addSubscriber (channelId:{})",
                    publishChId, publishType, streamName, channel.getId());
        }
    }

    public List<RtmpMessage> getConfigMessages() {
        return configMessages;
    }

    public void addConfigMessage(final RtmpMessage message) {
        configMessages.add(message);
    }

    public void setPublishChannel(Channel channel) {
        if (this.publishChannel != null) {
            logger.warn("({}) [ServerStream] [{}:{}] PublishChannel Changed {} -> {}",
                    publishChId, publishType, streamName, this.publishChannel, channel);
        }
        this.publishChannel = channel;
        if (channel != null) this.publishChId = channel.getId() + "";
        configMessages.clear();
    }

    public Channel getPublishChannel() {
        return publishChannel;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public String getPublishChId() {
        return publishChId;
    }

    public List<String> getSubscriberChIds() {
        return subscriberChIds;
    }

    public boolean isPlayStream() {
        return isPlayStream;
    }

    public void setPlayStream(boolean playStream) {
        isPlayStream = playStream;
    }

    public void setMetadata(Metadata metadata) {
        // [onMetaData,
        // {duration=0.0, fileSize=0.0, width=1920.0, height=1080.0, videocodecid=7.0, videodatarate=2000.0, framerate=30.0,
        // audiocodecid=10.0, audiodatarate=160.0, audiosamplerate=44100.0, audiosamplesize=16.0, audiochannels=2.0,
        // stereo=true, 2.1=false, 3.1=false, 4.0=false, 4.1=false, 5.1=false, 7.1=false,
        // encoder=obs-output module (libobs version 27.2.1)}]

        // VIDEO META DATA
        this.metadata.put("duration", metadata.getInnerValue("duration"));
        this.metadata.put("width", metadata.getInnerValue("width"));
        this.metadata.put("height", metadata.getInnerValue("height"));
        this.metadata.put("videodatarate", metadata.getInnerValue("videodatarate"));
        this.metadata.put("framerate", metadata.getInnerValue("framerate"));
        this.metadata.put("videocodecid", metadata.getInnerValue("videocodecid"));

        // AUDIO META DATA
        this.metadata.put("audiocodecid", metadata.getInnerValue("audiocodecid"));
        this.metadata.put("audiodatarate", metadata.getInnerValue("audiodatarate"));
        this.metadata.put("audiosamplerate", metadata.getInnerValue("audiosamplerate"));
        this.metadata.put("audiosamplesize", metadata.getInnerValue("audiosamplesize"));
        this.metadata.put("audiochannels", metadata.getInnerValue("audiochannels"));

        // ETC
        this.metadata.put("encoder", metadata.getInnerValue("encoder"));
        this.metadata.put("filesize", metadata.getInnerValue("filesize"));
    }

    private String makeIntegerString(String sourceStr) {
        return sourceStr.substring(0, sourceStr.lastIndexOf(".")).trim();
    }

    public VideoAttr makeVideoAttr() {
        VideoAttr videoAttr = new VideoAttr();

        String videoWidth = metadata.get("width");
        if (videoWidth != null && !videoWidth.isEmpty()) {
            videoAttr.setVideoWidth(makeIntegerString(videoWidth));
        }

        String videoHeight = metadata.get("height");
        if (videoHeight != null && !videoHeight.isEmpty()) {
            videoAttr.setVideoHeight(makeIntegerString(videoHeight));
        }

        String videoDataRate = metadata.get("videodatarate");
        if (videoDataRate != null && !videoDataRate.isEmpty()) {
            videoAttr.setVideoDataRate(makeIntegerString(videoDataRate));
        }

        String videoCodecId = metadata.get("videocodecid");
        if (videoCodecId != null && !videoCodecId.isEmpty()) {
            videoAttr.setVideoCodecId(makeIntegerString(videoCodecId));
        }

        String framerate = metadata.get("framerate");
        if (framerate != null && !framerate.isEmpty()) {
            videoAttr.setVideoFrameRate(makeIntegerString(framerate));
        }

        return videoAttr;
    }

    public AudioAttr makeAudioAttr() {
        AudioAttr audioAttr = new AudioAttr();

        String audioCodecId = metadata.get("audiocodecid");
        if (audioCodecId != null && !audioCodecId.isEmpty()) {
            audioAttr.setAudioCodecId(makeIntegerString(audioCodecId));
        }

        String audioDataRate = metadata.get("audiodatarate");
        if (audioDataRate != null && !audioDataRate.isEmpty()) {
            audioAttr.setAudioDataRate(makeIntegerString(audioDataRate));
        }

        String audioSampleRate = metadata.get("audiosamplerate");
        if (audioSampleRate != null && !audioSampleRate.isEmpty()) {
            audioAttr.setAudioSampleRate(makeIntegerString(audioSampleRate));
        }

        String audioSampleSize = metadata.get("audiosamplesize");
        if (audioSampleSize != null && !audioSampleSize.isEmpty()) {
            audioAttr.setAudioSampleSize(makeIntegerString(audioSampleSize));
        }

        String audioChannels = metadata.get("audiochannels");
        if (audioChannels != null && !audioChannels.isEmpty()) {
            audioAttr.setAudioChannels(makeIntegerString(audioChannels));
        }

        return audioAttr;
    }

    @Override
    public String toString() {
        return  "ServerStream{\r\n" +
                "\tName=" + streamName + "\r\n" +
                "\tCreatedTime=" + getInitiationTimeFormat() + "\r\n" +
                "\tType=" + publishType + "\r\n" +
                "\tpublishChannelId=" + publishChId + "\r\n" +
                "\tPublisher=" + publishChannel + "\r\n" +
                "\tSubscribersChannelId=" + subscriberChIds + "\r\n" +
                "\tSubscribers=" + subscribers + "\r\n" +
                "\tAudioAttr=" + makeAudioAttr() + "\r\n" +
                "\tVideoAttr=" + makeVideoAttr() + "\r\n" +
                '}';
    }
    ///////////////////////////////////////////////////////

}
