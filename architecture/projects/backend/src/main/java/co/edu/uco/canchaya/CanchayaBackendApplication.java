package co.edu.uco.canchaya;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CanchayaBackendApplication {

	/** Zona horaria de la plataforma (D27, PAR-GEN-01). */
	public static final String ZONA_HORARIA = "America/Bogota";

	public static void main(String[] args) {
		// Fija la zona antes de arrancar, para que en local y en Render los plazos
		// (franjas, cancelación, confirmación) se calculen con la hora de Colombia.
		TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
		SpringApplication.run(CanchayaBackendApplication.class, args);
	}

}
