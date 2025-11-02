package com.baketballbot.baskeballbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BasketballbotApplication {

	public static void main(String[] args) {
		SpringApplication.run(BasketballbotApplication.class, args);
	}

}
