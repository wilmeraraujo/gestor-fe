package com.gestor_fe.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync(proxyTargetClass = true)
public class AsyncConfig {
	
	@Value("${setCorePoolSize}")
	private int setCorePoolSize;
	
	@Value("${setMaxPoolSize}")
	private int setMaxPoolSize;	
	
	@Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(setCorePoolSize);
        executor.setMaxPoolSize(setMaxPoolSize);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("JobLoteCargue-");
        executor.initialize();
        return executor;
    }

}
