package co.edu.uco.canchaya;

import org.springframework.boot.SpringApplication;

public class TestCanchayaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(CanchayaBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
