package com.gestor_fe.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class GestorFeCoreServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestorFeCoreServiceApplication.class, args);
	}

}
