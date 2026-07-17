package com.smartrecycle.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartRecycleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartRecycleApplication.class, args);
		System.out.println("http://localhost:8080/swagger-ui/index.html");
	}
}