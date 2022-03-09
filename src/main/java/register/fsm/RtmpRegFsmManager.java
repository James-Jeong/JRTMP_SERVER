package register.fsm;

import fsm.StateManager;
import fsm.module.StateHandler;
import rtmp.base.RtmpRegUnit;

/**
 * @class public class RtspFsmManager
 * @brief RtspFsmManager class
 */
public class RtmpRegFsmManager {

    private final StateManager stateManager = new StateManager(10);

    ////////////////////////////////////////////////////////////////////////////////

    public RtmpRegFsmManager() {
        // Nothing
    }

    ////////////////////////////////////////////////////////////////////////////////

    public StateManager getStateManager() {
        return stateManager;
    }

    public void init(RtmpRegUnit rtmpRegUnit) {
        if (rtmpRegUnit == null) {
            return;
        }

        //
        stateManager.addStateHandler(RtmpRegState.NAME);
        StateHandler rtspStateHandler = stateManager.getStateHandler(RtmpRegState.NAME);
        //

        // REGISTER
        rtspStateHandler.addState(
                RtmpRegEvent.REGISTER,
                RtmpRegState.IDLE, RtmpRegState.REGISTER,
                null,
                null,
                null, 0, 0
        );

        // IDLE > REGISTER 상태에서만 동작
        rtspStateHandler.addState(
                RtmpRegEvent.IDLE,
                RtmpRegState.REGISTER, RtmpRegState.IDLE,
                null,
                null,
                null, 0, 0
        );
        //
    }

}
