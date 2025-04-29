package com.takuro_tamura.autofx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableRetry
@EnableScheduling
public class AutofxApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutofxApplication.class, args);
	}

}
