package com.takuro_tamura.autofx;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AutofxApplicationTests {

	@Test
	void contextLoads() {
	}

}
