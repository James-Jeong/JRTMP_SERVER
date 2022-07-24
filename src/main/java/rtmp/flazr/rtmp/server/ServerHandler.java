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

import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.base.PublishType;
import rtmp.flazr.amf.Amf0Object;
import rtmp.flazr.rtmp.RtmpMessage;
import rtmp.flazr.rtmp.message.*;
import rtmp.flazr.util.ChannelUtils;
import rtmp.metadata.AudioCodecId;
import rtmp.metadata.MetaDataChecker;
import rtmp.metadata.VideoCodecId;
import service.resource.ResourceManager;
import service.resource.ResourceReleaseManager;
import service.resource.StreamIdManager;
import service.scheduler.schedule.ScheduleManager;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ServerHandler extends SimpleChannelHandler {

    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    private static final int BYTES_READ_WINDOW = 250000;
    private static final int BYTES_WRITTEN_WINDOW = 250000;
    private long bytesRead;
    private long bytesReadLastSent;
    private long bytesWritten;
    private ServerApplication application;
    private String clientId;
    private String playName;
    private int streamId;
    private int bufferDuration;
    private ServerStream publishStream;
    private boolean aggregateModeEnabled = true;
    private String remoteHost;
    private int remotePort;
    private Date createTime;
    private String tcURL;
    private Channel channel;
    private String appName;

    private static final ResourceManager resourceManager = ResourceManager.getInstance();
    private static final StreamIdManager streamIdManager = StreamIdManager.getInstance();
    private static final ResourceReleaseManager resourceReleaseManager = ResourceReleaseManager.getInstance();

    private final ScheduleManager scheduleManager;
    private static final String RTMP_SCHEDULE_JOB = "RTMP";

    public ServerHandler() {
        scheduleManager = new ScheduleManager();
        scheduleManager.initJob(RTMP_SCHEDULE_JOB, 10, 10 * 2);
    }

    public void setAggregateModeEnabled(boolean aggregateModeEnabled) {
        this.aggregateModeEnabled = aggregateModeEnabled;
    }
    @Override
    public void channelOpen(final ChannelHandlerContext ctx, final ChannelStateEvent e) {
        InetSocketAddress sa=(InetSocketAddress) e.getChannel().getRemoteAddress();
        remoteHost = sa.getAddress().getHostAddress();
        remotePort = sa.getPort();

        this.createTime = new Date();
        this.channel = ctx.getChannel();
        logger.debug("({}) [CHANNEL OPEN] Channel: {}", channel.getId(), channel);
        //logger.info("({}) [CHANNEL OPEN] : {}", channel.getId(), e);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) {
        ChannelUtils.exceptionCaught(e);
    }

    @Override
    public void channelClosed(final ChannelHandlerContext ctx, final ChannelStateEvent e) {
        logger.debug("({}) [CHANNEL CLOSED] Channel: {}", channel.getId(), channel);
        //logger.info("({}) [CHANNEL CLOSED] : {}", channel.getId(), e);

        if (publishStream != null) {
            publishStream.removeSubscriber(channel);
        }

        releaseResource();
    }

    public void close(){
        channel.close();
    }

    @Override
    public void writeComplete(final ChannelHandlerContext ctx, final WriteCompletionEvent e) throws Exception {
        bytesWritten += e.getWrittenAmount();
        super.writeComplete(ctx, e);
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent me) throws Exception {
        try {
            final Channel channel = me.getChannel();
            final RtmpMessage message = (RtmpMessage) me.getMessage();

            bytesRead += message.getHeader().getSize();
            if ((bytesRead - bytesReadLastSent) > BYTES_READ_WINDOW) {
                BytesRead ack = new BytesRead(bytesRead);
                channel.write(ack);
                bytesReadLastSent = bytesRead;
            }

            MessageType msgType = message.getHeader().getMessageType();
            switch (msgType) {
                case CHUNK_SIZE: // handled by decoder
                    break;
                case CONTROL:
                    onControl(channel, message);
                    break;
                case COMMAND_AMF0:
                case COMMAND_AMF3:
                    onCommand(channel, message);
                    return; // NOT break
                case METADATA_AMF0:
                case METADATA_AMF3:
                    onMetadata(channel, message);
                    break;
                case AUDIO:
                case VIDEO:
                    if (((DataMessage) message).isConfig()) {
                        logger.debug("({}) [<{}>] Recv the rtmp config message: {}", message.getHeader().getMessageType().name(), channel.getId(), message);
                        publishStream.addConfigMessage(message);
                    }
                case AGGREGATE:
                    broadcast(message);
                    break;
                case BYTES_READ:
                    break;
                case WINDOW_ACK_SIZE:
                    WindowAckSize was = (WindowAckSize) message;
                    if (was.getValue() != BYTES_READ_WINDOW) {
                        channel.write(SetPeerBw.dynamic(BYTES_READ_WINDOW));
                    }
                    break;
                case SET_PEER_BW:
                    SetPeerBw spb = (SetPeerBw) message;
                    if (spb.getValue() != BYTES_WRITTEN_WINDOW) {
                        channel.write(new WindowAckSize(BYTES_WRITTEN_WINDOW));
                    }
                    break;
                default:
                    logger.warn("ignoring message: {}", message);
            }
        } catch (Exception e) {
            logger.warn("ServerHandler.messageReceived.Exception", e);
            throw e;
        }
    }

    // ChunkSize, StreamIsRecorded, StreamBegin, PlayStart, Metadata
    private RtmpMessage[] getStartMessages(final RtmpMessage variation) {
        final List<RtmpMessage> list = new ArrayList<>();
        list.add(new ChunkSize(4096));
        list.add(Control.streamIsRecorded(streamId));
        list.add(Control.streamBegin(streamId));
        if(variation != null) {
            list.add(variation);
        }
        list.add(Command.playStart(playName, clientId));
        list.add(Metadata.rtmpSampleAccess());
        list.add(Audio.empty());
        list.add(Metadata.dataStart());
        return list.toArray(new RtmpMessage[list.size()]);
    }

    private void broadcast(final RtmpMessage message) {
        ChannelGroup subscribers = publishStream.getSubscribers();
        if (subscribers == null) { return; }

        subscribers.write(message);
    }

    private void writeToStream(final Channel channel, final RtmpMessage message) {
        if(message.getHeader().getChannelId() > 2) {
            message.getHeader().setStreamId(streamId);
        }
        channel.write(message);
    }

    private void writeToStream(final ChannelGroup channelGroup, final RtmpMessage message) {
        if(message.getHeader().getChannelId() > 2) {
            message.getHeader().setStreamId(streamId);
        }
        channelGroup.write(message);
    }

    // MessageType.CONTROL
    private void onControl(Channel channel, RtmpMessage message) {
        Control control = (Control) message;
        if (control.getType() == Control.Type.SET_BUFFER) {
            logger.debug("received set buffer: {}", control);
            bufferDuration = control.getBufferLength();
        } else {
            logger.info("({}) [Control] ignored control: {}", channel.getId(), control);
        }
    }

    // MessageType.METADATA
    public void onMetadata(Channel channel, RtmpMessage message) {
        Metadata meta = (Metadata) message;
        logger.info("onMetaData message: {}", meta);

        // 1) Check video codec id
        VideoCodecId videoCodecId = MetaDataChecker.checkVideoCodecId(meta.getInnerValue("videocodecid"));
        if (videoCodecId != null) {
            logger.debug("({}) Video codec is matched. (id={}, allowedIds={})", clientId, videoCodecId, VideoCodecId.getCodecIdListString());
        } else {
            logger.warn("({}) Video codec is unmatched. (allowedIds={})", clientId, VideoCodecId.getCodecIdListString());
            resourceReleaseManager.sendRtmpFail(channel, publishStream.getStreamName(), streamId, false);
            releaseResource();
            return;
        }

        // 2) Check audio codec id
        AudioCodecId audioCodecId = MetaDataChecker.checkAudioCodecId(meta.getInnerValue("audiocodecid"));
        if (audioCodecId != null) {
            logger.debug("({}) Audio codec is matched. (id={}, allowedIds={})", clientId, audioCodecId, AudioCodecId.getCodecIdListString());
        } else {
            logger.warn("({}) Audio codec is unmatched. (allowedIds={})", clientId, AudioCodecId.getCodecIdListString());
            resourceReleaseManager.sendRtmpFail(channel, publishStream.getStreamName(), streamId, false);
            releaseResource();
            return;
        }

        publishStream.setMetadata(meta);

        if(meta.getName().equals("onMetaData")) {
            meta.setDuration(-1);
            publishStream.addConfigMessage(meta);
        }

        broadcast(message);
    }

    // MessageType.COMMAND
    private void onCommand(Channel channel, RtmpMessage message) {
        Command command = (Command) message;
        String cmdName = command.getName();

        logger.info("({}) [RTMP COMMAND] Recv [{}] <-- [{}:{}]", channel.getId(), cmdName, remoteHost, remotePort);
        logger.debug("({}) [RTMP COMMAND] Recv: {}", channel.getId(), message);

        switch (cmdName) {
            case "connect":
                connectResponse(channel, command);
                break;
            case "createStream":
                streamId = streamIdManager.takeStreamId();
                if (streamId < 0) {
                    return;
                }
                logger.info("({}) [CreateStream] Take StreamId [{}]", clientId, streamId);
                channel.write(Command.createStreamSuccess(command.getTransactionId(), streamId));
                break;
            case "play":
                playResponse(channel, command);
                break;
            case "deleteStream":
                int deleteStreamId = ((Double) command.getArg(0)).intValue();
                closeDeleteResponse("DeleteStream", deleteStreamId);
                break;
            case "closeStream":
                final int clientStreamId = command.getHeader().getStreamId();
                closeDeleteResponse("CloseStream", clientStreamId);
                break;
            case "pause":
                pauseResponse(channel, command);
                break;
            case "seek":
                seekResponse(channel, command);
                break;
            case "publish":
                publishResponse(channel, command);
                break;
            default:
                logger.warn("({}) [Command] UnRegistered CmdMsg, Ignoring [{}]", channel.getId(), cmdName);
                break;
        }
    }

    // MessageType.COMMAND.connect
    private void connectResponse(final Channel channel, final Command connect) {
        Amf0Object obj = connect.getObject();
        this.appName = (String) obj.get("app");
        this.tcURL = (String) obj.get("tcUrl");
        this.channel = channel;
        this.clientId = channel.getId() + "";
        this.application = resourceManager.getServerApp(appName); // auth, validation
        logger.info("({}) [Connect] appName: {}, tcUrl: {}, app: {}", clientId, appName, tcURL, application);

        //only support amf0
        final Double objectEncoding = (Double) obj.get("objectEncoding");
        if(objectEncoding != null && objectEncoding != 0){
            throw new RuntimeException("not support object encoding:" + objectEncoding);
        }

        // Window Ack, Set Peer BandWidth, Stream Begin
        channel.write(new WindowAckSize(BYTES_WRITTEN_WINDOW));
        channel.write(SetPeerBw.dynamic(BYTES_READ_WINDOW));
        channel.write(Control.streamBegin(streamId));
        // _result (connect response)
        final Command result = Command.connectSuccess(connect.getTransactionId());
        channel.write(result);
        // OnBWDone
        channel.write(Command.onBWDone());
    }

    // MessageType.COMMAND.deleteStream & closeStream
    private void closeDeleteResponse(String cmdName, int targetStreamId) {
        logger.info("({}) [{}] target StreamId [{}]", clientId, cmdName, targetStreamId);

        if (streamId == targetStreamId) {
            releaseResource();
        } else {
            logger.error("({}) [{}] Check Server_StreamId ({}) & Target_StreamId ({})",
                    clientId, cmdName, streamId, targetStreamId);
        }
    }

    // MessageType.COMMAND.play
    private void playResponse(final Channel channel, final Command play) {
        final String playStreamName = (String) play.getArg(0);
        this.playName = playStreamName;

        int playStart = -2;
        if (play.getArgCount() > 1) {
            playStart = ((Double) play.getArg(1)).intValue();
        }
        int playDuration = -1;
        if (play.getArgCount() > 2) {
            playDuration = ((Double) play.getArg(2)).intValue();
        }

        final boolean playReset;
        if (play.getArgCount() > 3) {
            playReset = ((Boolean) play.getArg(3));
        } else {
            playReset = true;
        }

        final Command playResetCommand = playReset ? Command.playReset(playName, clientId) : null;

        /////////////////////////////
        // CHECK STREAM NAME
        if (!resourceReleaseManager.checkStreamName(playStreamName, PublishType.LIVE.asString())) {
            denyStream(channel, playStreamName, true);
            return;
        }
        /////////////////////////////

        // Published ServerStream 조회
        publishStream = application.getStream(playStreamName);
        if (publishStream == null) {
            // create play ServerStream, PlayStream 정리 위해 playStream flag 사용
            logger.warn("({}) [Play] Not Exist [{}] PublishStream.", clientId, playStreamName);
            denyStream(channel, playStreamName, true);
            return;
        }

        logger.debug("({}) [Play] streamName: {}, streamId: {}, start: {}, duration: {}, reset: {}",
                clientId, playStreamName, streamId, playStart, playDuration, playReset
        );

        // ---------- LIVE STREAMING ---------- //
        // live 타입으로 publish 했던 stream
        if(publishStream.isLive()) {
            // ChunkSize, StreamIsRecorded, StreamBegin, PlayStart, Metadata
            for (final RtmpMessage message : getStartMessages(playResetCommand)) {
                writeToStream(channel, message);
            }

            boolean videoConfigPresent = false;

            // PublishStream 에서 받은 ConfigMessage -> playStream 에 전달
            for (RtmpMessage message : publishStream.getConfigMessages()) {
                logger.info("({}) [Play] writing start meta / config: {}", clientId, message);

                if (message.getHeader().isVideo()) {
                    videoConfigPresent = true;
                }

                writeToStream(channel, message);
            }

            if (!videoConfigPresent) {
                logger.info("({}) [Play] videoConfigPresent: false", clientId);
                writeToStream(channel, Video.empty());
            }

            // PublishStream subscribers 에 playStream channel 추가
            publishStream.addSubscriber(channel);
            logger.info("({}) [Play] client requested live stream: {}, added to stream: {}", clientId, playStreamName, publishStream);
        } else {
            denyStream(channel, playStreamName, true);
        }
    }

    // MessageType.COMMAND.pause
    private void pauseResponse(final Channel channel, final Command command) {
        logger.warn("({}) cannot pause when live", channel.getId());
    }

    // MessageType.COMMAND.seek
    private void seekResponse(final Channel channel, final Command command) {
        logger.warn("({}) cannot seek when live", channel.getId());
    }

    // // MessageType.COMMAND.publish
    private void publishResponse(final Channel channel, final Command command) {
        if(command.getArgCount() > 1) { // publish
            // streamName, publishType 검증?
            final String streamName = (String) command.getArg(0);
            final String publishTypeStr = (String) command.getArg(1);

            /////////////////////////////
            // CHECK STREAM NAME
            if (!resourceReleaseManager.checkStreamName(streamName, publishTypeStr)) {
                denyStream(channel, streamName, false);
                return;
            }

            logger.info("({}) [Publish] streamName: {}, type: {}, streamId: {}", clientId, streamName, publishTypeStr, this.streamId);
            /////////////////////////////

            // Create Publish ServerStream
            publishStream = application.getStream(streamName);
            if (publishStream == null) {
                publishStream = application.addStream(streamId, streamName, publishTypeStr);
            } else {
                logger.info("({}) [Publish] ServerStream ALREADY EXIST", clientId);
            }

            if (publishStream == null) {
                logger.warn("({}) [Publish] Fail to Create Publish ServerStream [streamId:{}]", clientId, this.streamId);
                denyStream(channel, streamName, false);
                return;
            }

            if(publishStream.getPublishChannel() != null) {
                logger.info("disconnecting publisher client, stream already in use");
                ChannelFuture future = channel.write(Command.publishBadName(streamId));
                future.addListener(ChannelFutureListener.CLOSE);
                return;
            }
            publishStream.setPublishChannel(channel);
            logger.info("({}) [Publish] created publish stream {}", clientId, publishStream);

            // onStatus
            channel.write(Command.publishStart(streamName, clientId, streamId));
            // Set ChunkSize, Stream Begin
            channel.write(new ChunkSize(4096));
            channel.write(Control.streamBegin(streamId));

            if (publishStream.getPublishType() != null) {
                switch (publishStream.getPublishType()) {
                    case LIVE:
                        // Subscribers 에게 NetStream.Play.PublishNotify 알림 전송, Header 에 streamId 전달
                        final ChannelGroup subscribers = publishStream.getSubscribers();
                        if (subscribers != null) {
                            subscribers.write(Command.publishNotify(streamId));
                            writeToStream(subscribers, Video.empty());
                            writeToStream(subscribers, Metadata.rtmpSampleAccess());
                            writeToStream(subscribers, Audio.empty());
                            writeToStream(subscribers, Metadata.dataStart());
                        }
                        break;
                    case RECORD: // DENY : 파일 스트리밍 지원하지 않음
                        logger.warn("[ServerHandler] Record is not implemented yet, un-publishing...");
                        denyStream(channel, streamName, false);
                        releaseResource();
                        break;
                    case APPEND:
                        logger.warn("[ServerHandler] Append is not implemented yet, un-publishing...");
                        denyStream(channel, streamName, false);
                        releaseResource();
                        break;
                }
            }
        } else { // un-publish
            final boolean publish = (Boolean) command.getArg(0);
            if (!publish) {
                releaseResource();
            }
        }
    }

    // kafkaInfo, publishStream, streamId 정리
    private void releaseResource() {
        if (playName == null) {
            resourceReleaseManager.unPublishIfLive(application, publishStream, streamId);
        }

        resourceReleaseManager.releaseStreamId(streamId, clientId);
        scheduleManager.stopAll(RTMP_SCHEDULE_JOB);
    }

    // RTMP 실패 메시지 전송, streamId 정리
    private void denyStream(Channel channel, String streamName, boolean isPlay) {
        resourceReleaseManager.sendRtmpFail(channel, streamName, streamId, isPlay);
        resourceReleaseManager.releaseStreamId(streamId, clientId);
    }

}
