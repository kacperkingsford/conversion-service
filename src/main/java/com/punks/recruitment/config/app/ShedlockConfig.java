package com.punks.recruitment.config.app;

import io.r2dbc.spi.ConnectionFactory;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.r2dbc.R2dbcLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S") // TODO can be moved to config
public class ShedlockConfig {

	@Bean
	public LockProvider lockProvider(ConnectionFactory connectionFactory) {
		return new R2dbcLockProvider(connectionFactory);
	}
}
