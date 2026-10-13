package com.gestor_fe.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableFeignClients
@ComponentScan(basePackages = {"com.gestor_fe.core", "com.service.common"})
@EnableJpaRepositories(basePackages = {"com.gestor_fe.core.repository", "com.service.common.repository"})
public class GestorFeCoreServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestorFeCoreServiceApplication.class, args);
	}

}
