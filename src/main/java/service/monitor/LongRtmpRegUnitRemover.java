package service.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.RtmpManager;
import rtmp.base.RtmpRegUnit;
import service.AppInstance;
import service.scheduler.job.Job;
import service.scheduler.schedule.ScheduleManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LongRtmpRegUnitRemover extends Job {

    private static final Logger logger = LoggerFactory.getLogger(LongRtmpRegUnitRemover.class);

    private final long limitTime;

    public LongRtmpRegUnitRemover(ScheduleManager scheduleManager, String name, int initialDelay, int interval, TimeUnit timeUnit, int priority, int totalRunCount, boolean isLasted) {
        super(scheduleManager, name, initialDelay, interval, timeUnit, priority, totalRunCount, isLasted);

        limitTime = AppInstance.getInstance().getConfigManager().getLocalSessionLimitTime();
    }

    @Override
    public void run() {
        RtmpManager rtmpManager = RtmpManager.getInstance();

        HashMap<String, RtmpRegUnit> rtmpUnitMap = rtmpManager.getRtmpRegisterManager().getCloneRtmpRegUnitMap();
        if (!rtmpUnitMap.isEmpty()) {
            for (Map.Entry<String, RtmpRegUnit> entry : rtmpUnitMap.entrySet()) {
                if (entry == null) {
                    continue;
                }

                RtmpRegUnit rtmpUnit = entry.getValue();
                if (rtmpUnit == null) {
                    continue;
                }

                long curTime = System.currentTimeMillis();
                if ((curTime - rtmpUnit.getInitiationTime()) >= limitTime) {
                    rtmpManager.getRtmpRegisterManager().deleteRtmpRegUnit(rtmpUnit.getId());
                    logger.warn("({}) REMOVED LONG RTMP [REG] UNIT(RtmpRegUnit=\n{})", getName(), rtmpUnit);
                }
            }
        }
    }
    
}
