package com.demo.scheduler;

import com.demo.config.AsyncConfig;
import com.demo.service.SocketService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTask {
    private static final Logger log = LogManager.getLogger(ScheduledTask.class);

    public final static long ONE_Minute = 60 * 1000;
    public final static long FIVE_Minute = 5 * 60 * 1000;

    @Autowired
    private SocketService socketService;

    @Autowired
    private AsyncConfig asyncConfig;

    // check TC connection every minute
    @Scheduled(fixedRate = ONE_Minute, initialDelay = ONE_Minute)
    public void checkTcConnection() {
        socketService.checkAllConnections();
    }

    @Scheduled(fixedRate = ONE_Minute, initialDelay = ONE_Minute)
    public void checkAndRefreshThreadPool() {
        try {
            if (asyncConfig.isThreadPoolExhausted()) {
                asyncConfig.shutdownExecutor();
                asyncConfig.initializeExecutor();

                log.info("Thread pool exhausted, shutting down and reinitializing executor.");
            }
        } catch (Exception e) {
            log.error("Error checking and refreshing thread pool: {}", e.getMessage(), e);
        }
    }
}
