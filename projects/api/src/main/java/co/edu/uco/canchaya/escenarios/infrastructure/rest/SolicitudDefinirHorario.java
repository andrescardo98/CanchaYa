package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.time.LocalTime;

import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * HU7, y HU11 al modificar (P2). Los tres campos llegan como texto para que un valor mal escrito responda ERR-GEN-02 con el
 * campo, y no ERR-GEN-07 como pasaría si Jackson no pudiera leerlo. El patrón de la hora deja
 * pasar minutos distintos de 00: eso es la política 13 (ERR-HOR-13) y lo revisa el dominio.
 */
record SolicitudDefinirHorario(

		@NotBlank(message = "ERR-GEN-01")
		@Pattern(regexp = "LUNES|MARTES|MIERCOLES|JUEVES|VIERNES|SABADO|DOMINGO", message = "ERR-GEN-02")
		String diaSemana,

		@NotBlank(message = "ERR-GEN-01")
		@Pattern(regexp = HORA, message = "ERR-GEN-02")
		String horaInicio,

		@NotBlank(message = "ERR-GEN-01")
		@Pattern(regexp = HORA, message = "ERR-GEN-02")
		String horaFin) {

	// hh:mm de 00:00 a 23:59
	static final String HORA = "([01]\\d|2[0-3]):[0-5]\\d";

	DiaSemana dia() {
		return DiaSemana.valueOf(diaSemana);
	}

	LocalTime inicio() {
		return LocalTime.parse(horaInicio);
	}

	LocalTime fin() {
		return LocalTime.parse(horaFin);
	}

}
