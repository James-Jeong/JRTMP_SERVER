package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtmp.RtmpManager;
import service.monitor.FileKeeper;
import service.monitor.HaHandler;
import service.monitor.LongServerStreamRemover;
import service.scheduler.job.Job;
import service.scheduler.job.JobBuilder;
import service.scheduler.schedule.ScheduleManager;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardOpenOption.*;

public class ServiceManager {

    ////////////////////////////////////////////////////////////////////////////////
    private static final Logger logger = LoggerFactory.getLogger(ServiceManager.class);

    private final static ServiceManager serviceManager = new ServiceManager(); // lazy initialization

    private final ScheduleManager scheduleManager = new ScheduleManager();

    public static final String MAIN_SCHEDULE_JOB = "MAIN";
    public static final int DELAY = 1000;

    private final String tmpdir = System.getProperty("java.io.tmpdir");
    private final File lockFile = new File(tmpdir, System.getProperty("lock_file", "jrtmp_server.lock"));
    private FileChannel fileChannel;
    private FileLock lock;
    private boolean isQuit = false;

    private FileKeeper fileKeeper = null;

    private final RtmpManager rtmpManager = RtmpManager.getInstance();
    ////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////
    private ServiceManager() {
        Runtime.getRuntime().addShutdownHook(new ShutDownHookHandler("ShutDownHookHandler", Thread.currentThread()));
    }
    
    public static ServiceManager getInstance ( ) {
        return serviceManager;
    }
    ////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////
    private boolean start () {
        ////////////////////////////////////////
        // System Lock
        systemLock();
        ////////////////////////////////////////

        ////////////////////////////////////////
        // SCHEDULE MAIN JOBS
        if (scheduleManager.initJob(MAIN_SCHEDULE_JOB, 10, 10 * 2)) {
            Job haHandleJob = new JobBuilder()
                    .setScheduleManager(scheduleManager)
                    .setInitialDelay(0)
                    .setInterval(DELAY)
                    .setTimeUnit(TimeUnit.MILLISECONDS)
                    .setPriority(5)
                    .setTotalRunCount(1)
                    .setIsLasted(true)
                    .build();

            HaHandler haHandler = new HaHandler(haHandleJob);
            haHandler.init();
            if (scheduleManager.startJob(MAIN_SCHEDULE_JOB, haHandler.getJob())) {
                logger.debug("[ServerManager] [+RUN] HaHandler");
            } else {
                logger.warn("[ServerManager] [-RUN FAIL] HaHandler");
            }

            if (AppInstance.getInstance().getConfigManager().getLocalSessionLimitTime() > 0) {
                Job longServerStreamRemoveJob = new JobBuilder()
                        .setScheduleManager(scheduleManager)
                        .setName(LongServerStreamRemover.class.getSimpleName())
                        .setInitialDelay(0)
                        .setInterval(DELAY)
                        .setTimeUnit(TimeUnit.MILLISECONDS)
                        .setPriority(3)
                        .setTotalRunCount(1)
                        .setIsLasted(true)
                        .build();
                LongServerStreamRemover longServerStreamRemover = new LongServerStreamRemover(longServerStreamRemoveJob);
                longServerStreamRemover.init();
                if (scheduleManager.startJob(MAIN_SCHEDULE_JOB, longServerStreamRemover.getJob())) {
                    logger.debug("[ServerManager] [+RUN] LongServerStreamRemover");
                } else {
                    logger.warn("[ServerManager] [-RUN FAIL] LongServerStreamRemover");
                }
            }

            Job fileKeepJob = new JobBuilder()
                    .setScheduleManager(scheduleManager)
                    .setName(FileKeeper.class.getSimpleName())
                    .setInitialDelay(0)
                    .setInterval(DELAY)
                    .setTimeUnit(TimeUnit.MILLISECONDS)
                    .setPriority(1)
                    .setTotalRunCount(1)
                    .setIsLasted(true)
                    .build();
            fileKeeper = new FileKeeper(fileKeepJob);
            if (fileKeeper.init()) {
                fileKeeper.start();
                if (scheduleManager.startJob(MAIN_SCHEDULE_JOB, fileKeeper.getJob())) {
                    logger.debug("[ServiceManager] [+RUN] FileKeeper");
                } else {
                    logger.warn("[ServiceManager] [-RUN FAIL] FileKeeper");
                }
            }
        }
        ////////////////////////////////////////

        logger.debug("| All services are opened.");
        return true;
    }

    public void stop () {
        rtmpManager.stop();

        ////////////////////////////////////////
        // FINISH ALL MAIN JOBS
        scheduleManager.stopAll(MAIN_SCHEDULE_JOB);
        ////////////////////////////////////////

        ////////////////////////////////////////
        // System Unlock
        systemUnLock();
        ////////////////////////////////////////

        isQuit = true;
        logger.debug("| All services are closed.");
    }

    /**
     * @fn public void loop ()
     * @brief Main Service Loop
     */
    public void loop () {
        if (!start()) {
            logger.error("Fail to start the program.");
            return;
        }

        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        while (!isQuit) {
            try {
                timeUnit.sleep(DELAY);
            } catch (InterruptedException e) {
                logger.warn("| ServiceManager.loop.InterruptedException", e);
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////
    private void systemLock () {
        try {
            fileChannel = FileChannel.open(lockFile.toPath(), CREATE, READ, WRITE);
            lock = fileChannel.tryLock();
            if (lock == null) {
                logger.error("DASH process is already running.");
                Thread.sleep(500L);
                System.exit(1);
            }
        } catch (Exception e) {
            logger.error("ServiceManager.systemLock.Exception.", e);
        }
    }

    private void systemUnLock () {
        try {
            if (lock != null) {
                lock.release();
            }

            if (fileChannel != null) {
                fileChannel.close();
            }

            Files.delete(lockFile.toPath());
        } catch (IOException e) {
            //ignore
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////
    /**
     * @class private static class ShutDownHookHandler extends Thread
     * @brief Graceful Shutdown 을 처리하는 클래스
     * Runtime.getRuntime().addShutdownHook(*) 에서 사용됨
     */
    private static class ShutDownHookHandler extends Thread {

        // shutdown 로직 후에 join 할 thread
        private final Thread target;

        public ShutDownHookHandler (String name, Thread target) {
            super(name);

            this.target = target;
            logger.debug("| ShutDownHookHandler is initiated. (target={})", target.getName());
        }

        /**
         * @fn public void run ()
         * @brief 정의된 Shutdown 로직을 수행하는 함수
         */
        @Override
        public void run ( ) {
            try {
                shutDown();
                target.join();
                logger.debug("| ShutDownHookHandler's target is finished successfully. (target={})", target.getName());
            } catch (Exception e) {
                logger.warn("| ShutDownHookHandler.run.Exception", e);
            }
        }

        /**
         * @fn private void shutDown ()
         * @brief Runtime 에서 선언된 Handler 에서 사용할 서비스 중지 함수
         */
        private void shutDown ( ) {
            logger.warn("| Process is about to quit. (Ctrl+C)");
            ServiceManager.getInstance().stop();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////

}
