package com.jacobo.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SecurityJwtTemplateApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecurityJwtTemplateApplication.class, args);
	}

}
