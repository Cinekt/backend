package com.jacobo.cinekt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CinektApplication {

	public static void main(String[] args) {
		SpringApplication.run(CinektApplication.class, args);
	}

}
