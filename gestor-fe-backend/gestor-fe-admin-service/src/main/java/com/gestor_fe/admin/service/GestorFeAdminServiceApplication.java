package com.gestor_fe.admin.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.gestor_fe.admin.service", "com.service.common"})
@EnableJpaRepositories(basePackages = {"com.gestor_fe.admin.service.repository", "com.service.common.repository"})
public class GestorFeAdminServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestorFeAdminServiceApplication.class, args);
	}

}
