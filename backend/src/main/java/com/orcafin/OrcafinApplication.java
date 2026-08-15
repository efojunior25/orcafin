package com.orcafin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrcafinApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrcafinApplication.class, args);
	}

}
