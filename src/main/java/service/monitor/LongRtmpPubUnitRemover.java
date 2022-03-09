package service.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.RtmpManager;
import rtmp.base.RtmpPubUnit;
import service.AppInstance;
import service.scheduler.job.Job;
import service.scheduler.schedule.ScheduleManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LongRtmpPubUnitRemover extends Job {

    private static final Logger logger = LoggerFactory.getLogger(LongRtmpPubUnitRemover.class);

    private final long limitTime;

    public LongRtmpPubUnitRemover(ScheduleManager scheduleManager, String name, int initialDelay, int interval, TimeUnit timeUnit, int priority, int totalRunCount, boolean isLasted) {
        super(scheduleManager, name, initialDelay, interval, timeUnit, priority, totalRunCount, isLasted);

        limitTime = AppInstance.getInstance().getConfigManager().getLocalSessionLimitTime();
    }

    @Override
    public void run() {
        RtmpManager rtmpManager = RtmpManager.getInstance();

        HashMap<Integer, RtmpPubUnit> rtmpUnitMap = rtmpManager.getCloneRtmpPubUnitMap();
        if (!rtmpUnitMap.isEmpty()) {
            for (Map.Entry<Integer, RtmpPubUnit> entry : rtmpUnitMap.entrySet()) {
                if (entry == null) {
                    continue;
                }

                RtmpPubUnit rtmpPubUnit = entry.getValue();
                if (rtmpPubUnit == null) {
                    continue;
                }

                long curTime = System.currentTimeMillis();
                if ((curTime - rtmpPubUnit.getInitiationTime()) >= limitTime) {
                    rtmpManager.deleteRtmpPubUnit(rtmpPubUnit.getId());
                    logger.warn("({}) REMOVED LONG RTMP [PUB] UNIT(RtmpPubUnit=\n{})", getName(), rtmpPubUnit);
                }
            }
        }
    }
    
}
