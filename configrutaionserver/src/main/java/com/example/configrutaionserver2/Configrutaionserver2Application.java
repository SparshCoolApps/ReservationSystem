package com.example.configrutaionserver2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer
@SpringBootApplication
public class Configrutaionserver2Application {

	public static void main(String[] args) {
		SpringApplication.run(Configrutaionserver2Application.class, args);
	}
}
