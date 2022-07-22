package rtmp;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.flazr.rtmp.RtmpConfig;
import rtmp.flazr.rtmp.server.ServerPipelineFactory;
import service.AppInstance;
import service.resource.ResourceManager;
import service.resource.StreamIdManager;
import util.FileManager;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class RtmpManager {

    static {
        RtmpConfig.configureServer(AppInstance.getInstance().getConfigManager().getFlazrConfPath());
    }

    ////////////////////////////////////////////////////////////
    private static final Logger logger = LoggerFactory.getLogger(RtmpManager.class);
    private static RtmpManager rtmpManager = null;
    private final ChannelGroup channels;
    private ChannelFactory factory = null;

    private final List<String> whitelist = new ArrayList<>();
    private final List<String> blacklist = new ArrayList<>();
    private static final String BLACK_LIST = "black list";
    private static final String WHITE_LIST = "white list";
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public RtmpManager() {
        this.channels = new DefaultChannelGroup("rtmp-server-channels");

        StreamIdManager.getInstance().initResource();
        initRtmpServer();
        loadAuthList();
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

    public void loadAuthList() {
        try {
            loadWhitelist();
            loadBlacklist();
        } catch (Exception e) {
            logger.error("[RtmpManager] Fail to get the authentication list file(s).", e);
        }
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public void loadWhitelist() {
        String whitelistPath = AppInstance.getInstance().getConfigManager().getAuthWhitelistPath();
        loadList(WHITE_LIST, whitelistPath);
    }

    public void loadBlacklist() {
        String blacklistPath = AppInstance.getInstance().getConfigManager().getAuthBlacklistPath();
        loadList(BLACK_LIST, blacklistPath);
    }

    private void loadList(String fileType, String filePath) {
        File file = new File(filePath);
        if (!file.exists() || file.isDirectory()) {
            logger.error("[RtmpManager] Fail to get the {} file. (Path={})", fileType, filePath);
        } else {
            switch (fileType) {
                case WHITE_LIST:
                    whitelist.clear();
                    whitelist.addAll(FileManager.readAllLines(filePath));
                    logger.debug("[RtmpManager] STREAM WHITE LIST: [{}]", whitelist);
                    break;
                case BLACK_LIST:
                    blacklist.clear();
                    blacklist.addAll(FileManager.readAllLines(filePath));
                    logger.debug("[RtmpManager] STREAM BLACK LIST: [{}]", blacklist);
                    break;
                default:
                    logger.error("");
                    break;
            }
        }
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public void stop() {
        StreamIdManager.getInstance().releaseResource();
        ResourceManager.getInstance().releaseAllResources();

        final ChannelGroupFuture future = channels.close();
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
    public ChannelGroup getChannels() {
        return channels;
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public List<String> getBlacklist() {
        return blacklist;
    }

    ////////////////////////////////////////////////////////////

}
