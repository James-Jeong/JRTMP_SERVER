package service.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.AppInstance;
import service.resource.ResourceManager;
import service.scheduler.job.Job;
import service.scheduler.job.JobContainer;

import java.util.Objects;

/**
 * @author dajin kim
 */
public class LongServerStreamRemover extends JobContainer {

    private static final Logger logger = LoggerFactory.getLogger(LongServerStreamRemover.class);
    private static final ResourceManager resourceManager = ResourceManager.getInstance();
    private final long limitTime;

    public LongServerStreamRemover(Job longServerStreamRemoveJob) {
        setJob(longServerStreamRemoveJob);
        limitTime = AppInstance.getInstance().getConfigManager().getLocalSessionLimitTime();
    }

    public void init() {
        getJob().setRunnable(() -> resourceManager.getAppNames().stream()
                .map(resourceManager::getServerApp).filter(Objects::nonNull)
                .forEach(serverApp -> serverApp.getStreamNames().stream()
                        .map(serverApp::getStream).filter(Objects::nonNull)
                        .filter(stream -> ((stream.getInitiationTime() + limitTime) < System.currentTimeMillis()))
                        .forEach(stream -> {
                                    logger.warn("[LongServerStreamRemover] REMOVED LONG SERVER STREAM\r\n({})", stream);
                                    serverApp.deleteStream(stream.getStreamName());
                                }
                        )
                ));
    }
}
