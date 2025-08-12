package com.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {
    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    private MonitoringThreadPoolTaskExecutor executor;

    @Override
    public Executor getAsyncExecutor() {
        executor = new MonitoringThreadPoolTaskExecutor();
        executor.setCorePoolSize(32);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("MyAsyncThread-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.initialize();
        return executor;
    }

    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void initializeExecutor() {
        if (executor != null) {
            executor.initialize();
        }
    }

    public boolean isThreadPoolExhausted() {
        if (executor == null) {
            throw new IllegalStateException("Thread pool executor is not initialized.");
        }
        return executor.isThreadPoolExhausted();
    }

    // inner class to monitor thread pool status
    public static class MonitoringThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

        public boolean isThreadPoolExhausted() {
            try {
                ThreadPoolExecutor threadPoolExecutor = getThreadPoolExecutor();
                int activeCount = threadPoolExecutor.getActiveCount();
                int queueSize = threadPoolExecutor.getQueue().size();

                return (activeCount >= getMaxPoolSize() && queueSize >= getQueueCapacity());
            } catch (IllegalStateException e) {
                log.warn("ThreadPoolExecutor is not initialized or has been shut down.", e);
            }

            return false;
        }
    }
}
