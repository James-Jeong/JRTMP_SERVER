package service.resource;

import config.ConfigManager;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.RtmpManager;
import rtmp.flazr.rtmp.message.Command;
import rtmp.flazr.rtmp.server.ServerApplication;
import rtmp.flazr.rtmp.server.ServerStream;
import service.AppInstance;

public class ResourceReleaseManager {
    private static final Logger logger = LoggerFactory.getLogger(ResourceReleaseManager.class);

    private static final ResourceReleaseManager INSTANCE = new ResourceReleaseManager();
    private final StreamIdManager streamIdManager = StreamIdManager.getInstance();
    private final ResourceManager streamManager = ResourceManager.getInstance();
    private final ConfigManager config = AppInstance.getInstance().getConfigManager();

    private ResourceReleaseManager() {
        // Nothing
    }

    public static ResourceReleaseManager getInstance() {
        return INSTANCE;
    }

    /*
     * uRtmp 에서 관리 하는 자원
     * - KafkaInfo           - kafkaKey (pubType : streamName)   - Kafka 연동 정보
     * - ServerApplication   - connect.app Name                  - ServerStream appName 별 관리
     * - ServerStream        - streamName                        - publish, play 개별 stream 관리
     * - StreamId                                                - RTMP stream ID
     * */

    /**
     * @fn unPublishIfLive
     * @brief publish 했던 stream 정리
     * @param app publishStream 관리 하는 ServerApplication
     * @param publishStream 정리 하려는 publishStream
     * @param streamId stream ID
     * */
    public void unPublishIfLive(ServerApplication app, ServerStream publishStream, int streamId) {
        if(publishStream != null && publishStream.getPublishChannel() != null) {
            final Channel publishChannel = publishStream.getPublishChannel();
            String channelId = publishChannel.getId() + "";
            String streamName = publishStream.getStreamName();

            logger.warn("({}) [ResourceRelease] Cleaning PublishStream...(streamId:{}, streamName:{})", channelId, streamId, streamName);

            // Publish Channel 에 unPublish 응답 전송
            if(publishChannel.isWritable()) {
                publishChannel.write(Command.unpublishSuccess(streamName, channelId, streamId));
            }

            // Subscribers 에게 NetStream.Play.UnPublishNotify 알림 전송, Header 에 streamId 전달
            ChannelGroup channelGroup = publishStream.getSubscribers();
            if (channelGroup != null) {
                channelGroup.write(Command.unpublishNotify(streamId));
            }
            publishStream.setPublishChannel(null);

            // Resource 정리
            logger.info("({}) [ResourceRelease] Delete Publish ServerStream [{}]", channelId, streamName);
            app.deleteStream(streamName);
        }
    }

    /**
     * @fn clearPlayStream
     * @brief play 요청에 의해 생성된 ServerStream 정리
     * @param app playStream 관리 하는 ServerApplication
     * @param playStreamName 정리 하려는 playStream 이름
     * @param channelId 함수 호출한 channel ID (로그 출력용)
     * */
    public void clearPlayStream(ServerApplication app, String playStreamName, String channelId) {
        ServerStream serverStream = app.getStream(playStreamName);
        if (serverStream != null && serverStream.isPlayStream()) {
            logger.info("({}) [ResourceRelease] Delete Play ServerStream [{}]", channelId, playStreamName);
            app.deleteStream(playStreamName);
        }
    }

    /**
     * @fn releaseStreamId
     * @brief stream ID 자원 반환
     * @param streamId 반환 할 ID
     * @param channelId 함수 호출한 channel ID (로그 출력용)
     * */
    public void releaseStreamId(int streamId, String channelId) {
        if (streamIdManager.restoreStreamId(streamId)) {
            logger.info("({}) [ResourceRelease] Restore StreamId [{}] (Id Queue Size:{})", channelId, streamId, streamIdManager.getStreamIdSize());
        }
    }

    /**
     * @fn sendRtmpFail
     * @brief RTMP 실패 메시지 전송 (ServerStream 생성 전 정리)
     * @param channel 인증 대상 channel
     * @param streamName 인증 하려는 streamName
     * @param streamId stream ID
     * @param isPlay Play Stream 여부
     * */
    public void sendRtmpFail(Channel channel, String streamName, int streamId, boolean isPlay) {
        String channelId = channel.getId() + "";

        // Play streamName 인증 실패
        if (isPlay) {
            // NetStream.Play.Failed
            logger.warn("({}) [ResourceRelease] <FAIL TO PLAY> [{}]", channelId, streamName);
            channel.write(Command.playFailed(streamName, channelId));
        }
        // Publish streamName 인증 실패
        else {
            logger.warn("({}) [ResourceRelease] <FAIL TO PUBLISH> [{}]", channelId, streamName);
            // NetStream.Unpublish.Success 전송 -> Client 에서 closeStream 수신 받은 후 정리
            //channel.write(Command.unPublishSuccess(streamName, channelId, streamId));

            // NetStream.Publish.Failed (onStatus _error) 전송 -> Client 에서 바로 channel close
            channel.write(Command.publishFailed(streamName, channelId, streamId));
        }
    }

    /**
     * @fn checkStreamName
     * @brief Check StreamName
     *        AUTH_LIST_CHECK_MODE = true -> auth list 파일 검증 실패 시 kafkaInfo 도 조회
     *                              false -> M-A2S 에서 등록한 streamName 만 조회(stream_create_req 통해 등록)
     * @param streamName 검증 하려는 stream name
     * @param publishTypeStr publish Type
     * @return 검증 결과 (whiteList 에 없거나, blackList 에 있으면 / M-A2S 에서 kafkaKey 미등록 이면 실패)
     * */
    public boolean checkStreamName(String streamName, String publishTypeStr) {
        if (streamName == null || publishTypeStr == null) { return false; }

        return RtmpManager.getInstance().getWhitelist().contains(streamName)
                && !RtmpManager.getInstance().getBlacklist().contains(streamName);
    }
}
