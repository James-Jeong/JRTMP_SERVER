package rtmp.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fsm.StateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import register.fsm.RtmpRegFsmManager;

import java.util.UUID;

public class RtmpRegUnit {

    ////////////////////////////////////////////////////////////
    private static final Logger logger = LoggerFactory.getLogger(RtmpRegUnit.class);

    private final long initiationTime = System.currentTimeMillis();

    private final String id;

    private boolean isRegistered = false;

    transient private final RtmpRegFsmManager rtmpRegFsmManager = new RtmpRegFsmManager();
    private final String rtmpRegStateUnitId;
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public RtmpRegUnit(String id) {
        this.id = id;
        this.rtmpRegStateUnitId = String.valueOf(UUID.randomUUID());

        rtmpRegFsmManager.init(this);
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    public long getInitiationTime() {
        return initiationTime;
    }

    public String getId() {
        return id;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(boolean registered) {
        isRegistered = registered;
    }

    public RtmpRegFsmManager getRtmpRegFsmManager() {
        return rtmpRegFsmManager;
    }

    public StateManager getRtmpRegStateManager() {
        return rtmpRegFsmManager.getStateManager();
    }

    public String getRtmpRegStateUnitId() {
        return rtmpRegStateUnitId;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
