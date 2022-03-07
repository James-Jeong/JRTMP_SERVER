package rtmp.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RtmpUnit {

    ////////////////////////////////////////////////////////////
    private static final Logger logger = LoggerFactory.getLogger(RtmpUnit.class);

    private final long initiationTime = System.currentTimeMillis();

    private final int id;
    private String streamName = null;
    private String publishTypeString = null;
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public RtmpUnit(int id) {
        this.id = id;
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public long getInitiationTime() {
        return initiationTime;
    }

    public int getId() {
        return id;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public String getPublishTypeString() {
        return publishTypeString;
    }

    public void setPublishTypeString(String publishTypeString) {
        this.publishTypeString = publishTypeString;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
