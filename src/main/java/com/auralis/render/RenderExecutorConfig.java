package com.auralis.render;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RenderExecutorConfig {

    /**
     * Bounded pool that actually runs ffmpeg. Queue capacity 0 + caller-runs is
     * avoided on purpose: the worker checks {@link ThreadPoolTaskExecutor#getActiveCount()}
     * against the pool size before submitting, so jobs stay QUEUED in the DB
     * rather than piling up in memory.
     */
    @Bean("renderExecutor")
    public ThreadPoolTaskExecutor renderExecutor(RenderProperties props) {
        int threads = Math.max(1, props.workerThreads());
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(threads);
        executor.setThreadNamePrefix("render-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}
