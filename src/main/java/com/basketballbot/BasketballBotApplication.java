package com.basketballbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BasketballBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(BasketballBotApplication.class, args);
	}

}
