package com.nammakuzhu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NammakuzhuApplication {

	public static void main(String[] args) {
		SpringApplication.run(NammakuzhuApplication.class, args);
	}

}
