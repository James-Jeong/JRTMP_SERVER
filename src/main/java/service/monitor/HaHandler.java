package service.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.resource.ResourceManager;
import service.resource.StreamIdManager;
import service.scheduler.job.Job;
import service.scheduler.job.JobContainer;
import service.system.SystemManager;

/**
 * @author jamesj
 * @class public class ServiceHaHandler extends TaskUnit
 * @brief ServiceHaHandler
 */
public class HaHandler extends JobContainer {

    private static final Logger logger = LoggerFactory.getLogger(HaHandler.class);

    ////////////////////////////////////////////////////////////////////////////////

    public HaHandler(Job haHandleJob) {
        setJob(haHandleJob);
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void init () {
        getJob().setRunnable(() -> {
            SystemManager systemManager = SystemManager.getInstance();

            String cpuUsageStr = systemManager.getCpuUsage();
            String memoryUsageStr = systemManager.getHeapMemoryUsage();

            logger.debug("| cpu=[{}], mem=[{}], thread=[{}] | stream=[{}], idle_id_count=[{}]",
                    cpuUsageStr, memoryUsageStr, Thread.activeCount(),
                    ResourceManager.getInstance().getStreamSize(),
                    StreamIdManager.getInstance().getStreamIdSize()
            );
        });
    }

}
