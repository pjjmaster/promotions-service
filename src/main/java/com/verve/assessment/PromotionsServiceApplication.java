package com.verve.assessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PromotionsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PromotionsServiceApplication.class, args);
	}

}
