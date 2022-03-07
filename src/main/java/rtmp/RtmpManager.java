package rtmp;

import com.flazr.rtmp.RtmpConfig;
import com.flazr.rtmp.server.ServerApplication;
import com.flazr.rtmp.server.ServerPipelineFactory;
import config.ConfigManager;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.base.RtmpUnit;
import service.AppInstance;

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

    private final HashMap<Integer, RtmpUnit> rtmpUnitMap = new HashMap<>();
    private final ReentrantLock rtmpUnitMapLock = new ReentrantLock();
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public RtmpManager() {
        this.CHANNELS = new DefaultChannelGroup("rtmp-server-channels");
        this.APPLICATIONS = new ConcurrentHashMap<>();

        initRtmpServer();
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
        deleteAllRtmpUnits();

        final ChannelGroupFuture future = CHANNELS.close();
        logger.info("[RtmpManager] Closing rtmp channels...");
        future.awaitUninterruptibly();
        //logger.info("[RtmpManager] Releasing rtmp resources...");
        //factory.releaseExternalResources();
        logger.info("[RtmpManager] RTMP Server stopped.");
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public RtmpUnit addRtmpUnit(int streamId) {
        if (getRtmpUnit(streamId) != null) { return null; }

        try {
            rtmpUnitMapLock.lock();

            RtmpUnit rtmpUnit = new RtmpUnit(streamId);
            rtmpUnitMap.putIfAbsent(streamId, rtmpUnit);
            logger.debug("[RtmpManager] [(+)CREATED] \n{}", rtmpUnit);
            return rtmpUnit;
        } catch (Exception e) {
            logger.warn("Fail to open the rtmp unit. (id={})", streamId, e);
            return null;
        } finally {
            rtmpUnitMapLock.unlock();
        }
    }

    public void deleteRtmpUnit(int streamId) {
        RtmpUnit rtmpUnit = getRtmpUnit(streamId);
        if (rtmpUnit == null) { return; }

        try {
            rtmpUnitMapLock.lock();

            logger.debug("[RtmpManager] [(-)DELETED] \n{}", rtmpUnit);

            rtmpUnitMap.remove(streamId);
        } catch (Exception e) {
            logger.warn("Fail to close the rtmp unit. (id={})", streamId, e);
        } finally {
            rtmpUnitMapLock.unlock();
        }
    }

    public HashMap<Integer, RtmpUnit> getCloneDashMap( ) {
        HashMap<Integer, RtmpUnit> cloneMap;

        try {
            rtmpUnitMapLock.lock();

            cloneMap = (HashMap<Integer, RtmpUnit>) rtmpUnitMap.clone();
        } catch (Exception e) {
            logger.warn("Fail to clone the rtmp unit map.", e);
            cloneMap = rtmpUnitMap;
        } finally {
            rtmpUnitMapLock.unlock();
        }

        return cloneMap;
    }

    public void deleteAllRtmpUnits() {
        try {
            rtmpUnitMapLock.lock();
            rtmpUnitMap.entrySet().removeIf(Objects::nonNull);
        } catch (Exception e) {
            logger.warn("Fail to close all rtmp units.", e);
        } finally {
            rtmpUnitMapLock.unlock();
        }
    }

    public RtmpUnit getRtmpUnit(int streamId) {
        return rtmpUnitMap.get(streamId);
    }

    public int getRtmpUnitMapSize() {
        return rtmpUnitMap.size();
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public ChannelGroup getCHANNELS() {
        return CHANNELS;
    }

    public Map<String, ServerApplication> getAPPLICATIONS() {
        return APPLICATIONS;
    }
    ////////////////////////////////////////////////////////////

}
