package com.vaxly.historicalservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class HistoricalServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(HistoricalServiceApplication.class, args);
	}

}