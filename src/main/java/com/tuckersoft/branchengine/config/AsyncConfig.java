package com.tuckersoft.branchengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * El pool donde corre el listener del Informe de Realidad.
 *
 * El prefijo del hilo es lo que se ve en el [BRANCH-LOG]: si ahi sale
 * http-nio-8080-exec-N en vez de branch-worker-N, el @Async no esta funcionando.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "branchExecutor")
    public Executor branchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("branch-worker-");
        executor.initialize();
        return executor;
    }
}
