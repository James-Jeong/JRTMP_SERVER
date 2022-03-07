package service.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.RtmpManager;
import rtmp.base.RtmpUnit;
import service.AppInstance;
import service.scheduler.job.Job;
import service.scheduler.schedule.ScheduleManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LongSessionRemover extends Job {

    private static final Logger logger = LoggerFactory.getLogger(LongSessionRemover.class);

    private final long limitTime;

    public LongSessionRemover(ScheduleManager scheduleManager, String name, int initialDelay, int interval, TimeUnit timeUnit, int priority, int totalRunCount, boolean isLasted) {
        super(scheduleManager, name, initialDelay, interval, timeUnit, priority, totalRunCount, isLasted);

        limitTime = AppInstance.getInstance().getConfigManager().getLocalSessionLimitTime();
    }

    @Override
    public void run() {
        RtmpManager rtmpManager = RtmpManager.getInstance();

        HashMap<Integer, RtmpUnit> rtmpUnitMap = rtmpManager.getCloneDashMap();
        if (!rtmpUnitMap.isEmpty()) {
            for (Map.Entry<Integer, RtmpUnit> entry : rtmpUnitMap.entrySet()) {
                if (entry == null) {
                    continue;
                }

                RtmpUnit rtmpUnit = entry.getValue();
                if (rtmpUnit == null) {
                    continue;
                }

                long curTime = System.currentTimeMillis();
                if ((curTime - rtmpUnit.getInitiationTime()) >= limitTime) {
                    rtmpManager.deleteRtmpUnit(rtmpUnit.getId());
                    logger.warn("({}) REMOVED LONG RTMP UNIT(RtmpUnit=\n{})", getName(), rtmpUnit);
                }
            }
        }
    }
    
}
