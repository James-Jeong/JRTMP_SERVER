package service.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.base.PublishType;
import rtmp.flazr.rtmp.server.ServerApplication;
import rtmp.flazr.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceManager {
    private static final Logger log = LoggerFactory.getLogger(ResourceManager.class);
    private static ResourceManager resourceManager;

    private final Map<String, ServerApplication> serverAppMap = new ConcurrentHashMap<>();

    private ResourceManager() {
        // Nothing
    }

    public static ResourceManager getInstance() {
        if (resourceManager == null) {
            resourceManager = new ResourceManager();
        }
        return resourceManager;
    }

    public void releaseAllResources() {
        deleteAllServerApp();
    }

    // ServerApplication (KEY: RTMP.Connect app)
    // - ServerApp 에서 ServerStream 관리 (KEY: stream name)
    ////////////////////////////////////////////////////////////
    public ServerApplication getServerApp(final String rawName) {
        final String appName = Utils.trimSlashes(rawName).toLowerCase();

        ServerApplication app = serverAppMap.get(appName);
        if (app == null) {
            app = new ServerApplication(appName);
            serverAppMap.put(appName, app);
            log.debug("[Resource] ServerApplication [{}] (+)CREATED", appName);
        } else {
            log.trace("[Resource] Get [{}] ServerApplication", appName);
        }
        return app;
    }

    public void deleteServerApp(final String rawName) {
        final String appName = Utils.trimSlashes(rawName).toLowerCase();

        ServerApplication serverApp = serverAppMap.remove(appName);
        if (serverApp != null) {
            log.warn("[Resource] ServerApplication [{}] (-)DELETED", appName);
            log.debug("[Resource] [(-)DELETED] \n{}", serverApp);
        }
    }

    public void deleteAllServerApp() {
        log.info("[Resource] Delete ServerApps...({})", getAppNames());
        for(ServerApplication app : serverAppMap.values()) {
            app.deleteAllServerStreams();
        }
    }

    public List<String> getAppNames() {
        synchronized (serverAppMap) {
            return new ArrayList<>(serverAppMap.keySet());
        }
    }

    public int getStreamSize() {
        int size = 0;
        for(ServerApplication app : serverAppMap.values()) {
            size += app.getStreamSize();
        }
        return size;
    }
    ////////////////////////////////////////////////////////////


}
