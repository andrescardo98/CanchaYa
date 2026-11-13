package co.edu.uco.canchaya;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CanchayaBackendApplication {

	/** D27 */
	public static final String ZONA_HORARIA = "America/Bogota";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
		SpringApplication.run(CanchayaBackendApplication.class, args);
	}

	/** Se inyecta en lugar de llamar a now() para que las pruebas puedan fijar la hora. */
	@Bean
	Clock reloj() {
		return Clock.system(ZoneId.of(ZONA_HORARIA));
	}

}
