package rtmp;

import register.RtmpRegisterManager;
import rtmp.flazr.rtmp.RtmpConfig;
import rtmp.flazr.rtmp.server.ServerApplication;
import rtmp.flazr.rtmp.server.ServerPipelineFactory;
import config.ConfigManager;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.base.RtmpPubUnit;
import service.AppInstance;
import service.StreamIdResourceManager;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class RtmpManager {

    static {
        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        RtmpConfig.configureServer(configManager.getFlazrConfPath());
    }

    ////////////////////////////////////////////////////////////
    private static final Logger logger = LoggerFactory.getLogger(RtmpManager.class);

    private static RtmpManager rtmpManager = null;

    private final ChannelGroup CHANNELS;
    private final Map<String, ServerApplication> APPLICATIONS;
    private ChannelFactory factory = null;

    private final HashMap<Integer, RtmpPubUnit> rtmpPubUnitMap = new HashMap<>();
    private final ReentrantLock rtmpPubUnitMapLock = new ReentrantLock();

    private final RtmpRegisterManager rtmpRegisterManager = new RtmpRegisterManager();
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public RtmpManager() {
        this.CHANNELS = new DefaultChannelGroup("rtmp-server-channels");
        this.APPLICATIONS = new ConcurrentHashMap<>();

        StreamIdResourceManager.getInstance().initResource();
        initRtmpServer();

        rtmpRegisterManager.addRegisterChannel();
    }

    public static RtmpManager getInstance() {
        if (rtmpManager == null) {
            rtmpManager = new RtmpManager();
        }

        return rtmpManager;
    }

    private void initRtmpServer() {
        factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        ServerBootstrap bootstrap = new ServerBootstrap(factory);
        bootstrap.setPipelineFactory(new ServerPipelineFactory());
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        final InetSocketAddress socketAddress = new InetSocketAddress(RtmpConfig.SERVER_PORT);
        bootstrap.bind(socketAddress);
        logger.info("[RtmpManager] RTMP Server started, listening on: [{}]", socketAddress);
    }

    public void stop() {
        rtmpRegisterManager.removeRegisterChannel();

        deleteAllRtmpPubUnits();

        final ChannelGroupFuture future = CHANNELS.close();
        logger.info("[RtmpManager] Closing rtmp channels...");
        future.awaitUninterruptibly();
        /*if (factory != null) {
            logger.info("[RtmpManager] Releasing rtmp resources...");
            factory.releaseExternalResources();
        }*/
        logger.info("[RtmpManager] RTMP Server stopped.");
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public RtmpPubUnit addRtmpPubUnit(int streamId) {
        if (getRtmpPubUnit(streamId) != null) { return null; }

        try {
            rtmpPubUnitMapLock.lock();

            RtmpPubUnit rtmpUnit = new RtmpPubUnit(streamId);
            rtmpPubUnitMap.putIfAbsent(streamId, rtmpUnit);
            logger.debug("[RtmpManager] [(+)CREATED] \n{}", rtmpUnit);
            return rtmpUnit;
        } catch (Exception e) {
            logger.warn("Fail to open the rtmp publish unit. (id={})", streamId, e);
            return null;
        } finally {
            rtmpPubUnitMapLock.unlock();
        }
    }

    public void deleteRtmpPubUnit(int streamId) {
        RtmpPubUnit rtmpUnit = getRtmpPubUnit(streamId);
        if (rtmpUnit == null) { return; }

        try {
            rtmpPubUnitMapLock.lock();

            logger.debug("[RtmpManager] [(-)DELETED] \n{}", rtmpUnit);

            rtmpPubUnitMap.remove(streamId);
        } catch (Exception e) {
            logger.warn("Fail to close the rtmp publish unit. (id={})", streamId, e);
        } finally {
            rtmpPubUnitMapLock.unlock();
        }
    }

    public HashMap<Integer, RtmpPubUnit> getCloneRtmpPubUnitMap( ) {
        HashMap<Integer, RtmpPubUnit> cloneMap;

        try {
            rtmpPubUnitMapLock.lock();

            cloneMap = (HashMap<Integer, RtmpPubUnit>) rtmpPubUnitMap.clone();
        } catch (Exception e) {
            logger.warn("Fail to clone the rtmp publish unit map.", e);
            cloneMap = rtmpPubUnitMap;
        } finally {
            rtmpPubUnitMapLock.unlock();
        }

        return cloneMap;
    }

    public void deleteAllRtmpPubUnits() {
        try {
            rtmpPubUnitMapLock.lock();
            rtmpPubUnitMap.entrySet().removeIf(Objects::nonNull);
        } catch (Exception e) {
            logger.warn("Fail to close all rtmp publish units.", e);
        } finally {
            rtmpPubUnitMapLock.unlock();
        }
    }

    public RtmpPubUnit getRtmpPubUnit(int streamId) {
        return rtmpPubUnitMap.get(streamId);
    }

    public int getRtmpPubUnitMapSize() {
        return rtmpPubUnitMap.size();
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public ChannelGroup getCHANNELS() {
        return CHANNELS;
    }

    public Map<String, ServerApplication> getAPPLICATIONS() {
        return APPLICATIONS;
    }

    public RtmpRegisterManager getRtmpRegisterManager() {
        return rtmpRegisterManager;
    }
    ////////////////////////////////////////////////////////////

}
