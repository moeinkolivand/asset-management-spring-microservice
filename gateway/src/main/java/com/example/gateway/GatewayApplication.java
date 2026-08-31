package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		System.out.println("The Gateway Is Runned");
		SpringApplication.run(GatewayApplication.class, args);
	}

}
