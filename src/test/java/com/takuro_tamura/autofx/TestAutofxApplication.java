package com.takuro_tamura.autofx;

import org.springframework.boot.SpringApplication;

public class TestAutofxApplication {

	public static void main(String[] args) {
		SpringApplication.from(AutofxApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
