package rtmp.flazr.rtmp.client.dash;

import org.scijava.nativelib.NativeLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class DashManager {

    private static final Logger logger = LoggerFactory.getLogger(DashManager.class);

    private static DashManager dashManager = null;

    public DashManager() {
        // Nothing
    }

    public static DashManager getInstance() {
        if (dashManager == null) {
            dashManager = new DashManager();
        }

        return dashManager;
    }

    public boolean start() {
        try {
            final String arch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
            logger.debug("ARCH: {}", arch);

            //System.load("/Users/jamesj/GIT_PROJECTS/JRTMP_SERVER/src/main/resources/natives/libavcodec.dylib");

            NativeLoader.loadLibrary("avcodec");
            NativeLoader.loadLibrary("avfilter");
            NativeLoader.loadLibrary("avformat");
            NativeLoader.loadLibrary("avutil");
            //NativeLoader.loadLibrary("gpac");

            logger.debug("[DashManager] Success to start.");
        } catch (Exception e) {
            logger.warn("[DashManager] Fail to start.", e);
            return false;
        }

        return true;
    }

}
