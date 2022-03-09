package register.handler;

import config.ConfigManager;
import fsm.module.StateHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import register.RtmpRegisterManager;
import register.channel.RtmpRegisterNettyChannel;
import register.fsm.RtmpRegEvent;
import register.fsm.RtmpRegState;
import register.message.RtmpRegisterReq;
import register.message.RtmpRegisterRes;
import register.message.RtmpUnRegisterReq;
import register.message.RtmpUnRegisterRes;
import register.base.URtmpHeader;
import register.base.URtmpMessageType;
import rtmp.RtmpManager;
import rtmp.base.RtmpRegUnit;
import service.AppInstance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class RtmpRegisterServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(RtmpRegisterServerHandler.class);

    private final String ip;
    private final int port;

    ////////////////////////////////////////////////////////////////////////////////

    public RtmpRegisterServerHandler(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) {
        try {
            RtmpRegisterManager rtmpRegisterManager = RtmpManager.getInstance().getRtmpRegisterManager();
            RtmpRegisterNettyChannel rtmpRegisterNettyChannel = rtmpRegisterManager.getRegisterChannel();
            if (rtmpRegisterNettyChannel == null) {
                logger.warn("[RtmpRegisterServerHandler] RtmpRegister Channel is not defined.");
                return;
            }

            ByteBuf buf = datagramPacket.content();
            if (buf == null) {
                logger.warn("[RtmpRegisterServerHandler] DatagramPacket's content is null.");
                return;
            }

            int readBytes = buf.readableBytes();
            if (buf.readableBytes() <= 0) {
                logger.warn("[RtmpRegisterServerHandler] Message is null.");
                return;
            }

            byte[] data = new byte[readBytes];
            buf.getBytes(0, data);

            ConfigManager configManager = AppInstance.getInstance().getConfigManager();

            URtmpHeader uRtmpHeader = new URtmpHeader(data);
            if (uRtmpHeader.getMessageType() == URtmpMessageType.REGISTER) {
                RtmpRegisterReq rtmpRegisterReq = new RtmpRegisterReq(data);
                logger.debug("[RtmpRegisterServerHandler] [>] {} ({})", rtmpRegisterReq, readBytes);

                String regId = rtmpRegisterReq.getId();
                String nonce = rtmpRegisterReq.getNonce();

                RtmpRegUnit rtmpRegUnit = rtmpRegisterManager.getRtmpRegUnit(regId);
                if (rtmpRegUnit == null) { // NOT AUTHORIZED
                    RtmpRegisterRes rtmpRegisterRes = new RtmpRegisterRes(
                            configManager.getRegisterMagicCookie(),
                            rtmpRegisterReq.getURtspHeader().getMessageType(),
                            rtmpRegisterReq.getURtspHeader().getSeqNumber(),
                            rtmpRegisterReq.getURtspHeader().getTimeStamp(),
                            configManager.getServiceName(),
                            RtmpRegisterRes.NOT_AUTHORIZED
                    );
                    rtmpRegisterRes.setReason("NOT_AUTHORIZED");

                    // RTMP REG ID 등록
                    rtmpRegisterManager.addRtmpRegUnit(regId);

                    rtmpRegisterNettyChannel.sendResponse(
                            datagramPacket.sender().getAddress().getHostAddress(),
                            rtmpRegisterReq.getListenPort(),
                            rtmpRegisterRes
                    );
                } else {
                    RtmpRegisterRes rtmpRegisterRes;
                    StateHandler rtmpRegUnitStateHandler = rtmpRegUnit.getRtmpRegStateManager().getStateHandler(RtmpRegState.NAME);
                    String curState = rtmpRegUnit.getRtmpRegStateManager().getStateUnit(rtmpRegUnit.getRtmpRegStateUnitId()).getCurState();
                    if (!curState.equals(RtmpRegState.IDLE)) {
                        rtmpRegisterRes = new RtmpRegisterRes(
                                configManager.getRegisterMagicCookie(),
                                rtmpRegisterReq.getURtspHeader().getMessageType(),
                                rtmpRegisterReq.getURtspHeader().getSeqNumber(),
                                rtmpRegisterReq.getURtspHeader().getTimeStamp(),
                                configManager.getServiceName(),
                                RtmpRegisterRes.STATE_ERROR
                        );
                    } else {

                        if (!rtmpRegUnit.isRegistered()) {
                            // 1) Check nonce
                            // 2) If ok, register
                            // 3) If not, reject
                            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                            messageDigest.update(configManager.getServiceName().getBytes(StandardCharsets.UTF_8));
                            messageDigest.update(configManager.getRegisterHashKey().getBytes(StandardCharsets.UTF_8));
                            byte[] a1 = messageDigest.digest();
                            messageDigest.reset();
                            messageDigest.update(a1);

                            String curNonce = new String(messageDigest.digest());
                            if (curNonce.equals(nonce)) {
                                rtmpRegisterRes = new RtmpRegisterRes(
                                        configManager.getRegisterMagicCookie(),
                                        rtmpRegisterReq.getURtspHeader().getMessageType(),
                                        rtmpRegisterReq.getURtspHeader().getSeqNumber(),
                                        rtmpRegisterReq.getURtspHeader().getTimeStamp(),
                                        configManager.getServiceName(),
                                        RtmpRegisterRes.SUCCESS
                                );
                                rtmpRegUnit.setRegistered(true);

                                rtmpRegUnitStateHandler.fire(
                                        RtmpRegEvent.REGISTER,
                                        rtmpRegUnit.getRtmpRegStateManager().getStateUnit(rtmpRegUnit.getRtmpRegStateUnitId())
                                );
                            } else {
                                rtmpRegisterRes = new RtmpRegisterRes(
                                        configManager.getRegisterMagicCookie(),
                                        rtmpRegisterReq.getURtspHeader().getMessageType(),
                                        rtmpRegisterReq.getURtspHeader().getSeqNumber(),
                                        rtmpRegisterReq.getURtspHeader().getTimeStamp(),
                                        configManager.getServiceName(),
                                        RtmpRegisterRes.NOT_AUTHORIZED
                                );
                                rtmpRegisterRes.setReason("WRONG_NONCE");

                                // RTMP REG ID 등록 해제
                                rtmpRegisterManager.deleteRtmpRegUnit(regId);
                                rtmpRegUnit.setRegistered(false);
                            }
                        } else {
                            rtmpRegisterRes = new RtmpRegisterRes(
                                    configManager.getRegisterMagicCookie(),
                                    rtmpRegisterReq.getURtspHeader().getMessageType(),
                                    rtmpRegisterReq.getURtspHeader().getSeqNumber(),
                                    rtmpRegisterReq.getURtspHeader().getTimeStamp(),
                                    configManager.getServiceName(),
                                    RtmpRegisterRes.SUCCESS
                            );

                            rtmpRegUnitStateHandler.fire(
                                    RtmpRegEvent.REGISTER,
                                    rtmpRegUnit.getRtmpRegStateManager().getStateUnit(rtmpRegUnit.getRtmpRegStateUnitId())
                            );
                        }
                    }

                    rtmpRegisterNettyChannel.sendResponse(
                            datagramPacket.sender().getAddress().getHostAddress(),
                            rtmpRegisterReq.getListenPort(),
                            rtmpRegisterRes
                    );
                }
            } else if (uRtmpHeader.getMessageType() == URtmpMessageType.UNREGISTER) {
                RtmpUnRegisterReq rtmpUnRegisterReq = new RtmpUnRegisterReq(data);
                logger.debug("[RtmpRegisterServerHandler] [>] {} ({})", rtmpUnRegisterReq, readBytes);

                String regId = rtmpUnRegisterReq.getId();
                RtmpRegUnit rtmpRegUnit = rtmpRegisterManager.getRtmpRegUnit(regId);
                RtmpUnRegisterRes rtmpUnRegisterRes;
                if (rtmpRegUnit == null) {
                    rtmpUnRegisterRes = new RtmpUnRegisterRes(
                            configManager.getRegisterMagicCookie(),
                            rtmpUnRegisterReq.getURtspHeader().getMessageType(),
                            rtmpUnRegisterReq.getURtspHeader().getSeqNumber(),
                            rtmpUnRegisterReq.getURtspHeader().getTimeStamp(),
                            RtmpUnRegisterRes.NOT_ACCEPTED
                    );
                } else {
                    String curState = rtmpRegUnit.getRtmpRegStateManager().getStateUnit(rtmpRegUnit.getRtmpRegStateUnitId()).getCurState();
                    if (!curState.equals(RtmpRegState.REGISTER)) {
                        rtmpUnRegisterRes = new RtmpUnRegisterRes(
                                configManager.getRegisterMagicCookie(),
                                rtmpUnRegisterReq.getURtspHeader().getMessageType(),
                                rtmpUnRegisterReq.getURtspHeader().getSeqNumber(),
                                rtmpUnRegisterReq.getURtspHeader().getTimeStamp(),
                                RtmpUnRegisterRes.STATE_ERROR
                        );
                    } else {
                        rtmpUnRegisterRes = new RtmpUnRegisterRes(
                                configManager.getRegisterMagicCookie(),
                                rtmpUnRegisterReq.getURtspHeader().getMessageType(),
                                rtmpUnRegisterReq.getURtspHeader().getSeqNumber(),
                                rtmpUnRegisterReq.getURtspHeader().getTimeStamp(),
                                RtmpUnRegisterRes.SUCCESS
                        );

                        // RTMP REG ID 등록 해제
                        rtmpRegisterManager.deleteRtmpRegUnit(regId);
                        rtmpRegUnit.setRegistered(false);

                        StateHandler rtmpRegStateHandler = rtmpRegUnit.getRtmpRegStateManager().getStateHandler(RtmpRegState.NAME);
                        rtmpRegStateHandler.fire(
                                RtmpRegEvent.IDLE,
                                rtmpRegUnit.getRtmpRegStateManager().getStateUnit(rtmpRegUnit.getRtmpRegStateUnitId())
                        );
                    }
                }

                rtmpRegisterNettyChannel.sendResponse(
                        datagramPacket.sender().getAddress().getHostAddress(),
                        rtmpUnRegisterReq.getListenPort(),
                        rtmpUnRegisterRes
                );
            }
        } catch (Exception e) {
            logger.warn("[RtmpRegisterServerHandler] RtmpRegisterChannelHandler.channelRead0.Exception", e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.warn("[RtmpRegisterServerHandler] RtmpRegisterChannelHandler is inactive.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("[RtmpRegisterServerHandler] RtmpRegisterChannelHandler.Exception", cause);
    }

    ////////////////////////////////////////////////////////////////////////////////

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

}
