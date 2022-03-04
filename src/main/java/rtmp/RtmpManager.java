package rtmp;

import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.stream.IBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RtmpManager {

    private static final Logger logger = LoggerFactory.getLogger(RtmpManager.class);

    public void streamBroadcastStart(IBroadcastStream stream) {
        IConnection connection = Red5.getConnectionLocal();
        if (connection != null &&  stream != null) {
            logger.debug("Broadcast started for: [{}]", stream.getPublishedName());
            connection.setAttribute("streamStart", System.currentTimeMillis());
            connection.setAttribute("streamName", stream.getPublishedName());
        }
    }



}
