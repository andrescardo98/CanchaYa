package co.edu.uco.canchaya.escenarios.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Bloque de una hora en punto de una fecha, calculado a partir de los horarios recurrentes del
 * escenario. No se guarda en la base de datos (RN-02): se arma en cada consulta.
 *
 * <p>Lo usa también el dominio de Reservas (Reserva.verificarDisponibilidad y confirmar). Es la
 * excepción a la regla de puertos que se aprobó en P1 de HU20, igual que {@code Solicitante}: un
 * valor inmutable, sin comportamiento que dependa de Escenarios.
 */
public record Franja(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {

	/**
	 * PAR-HOR-01: la franja dura una hora. El catálogo la marca como regla fija («cambiarla exige
	 * rediseñar el cálculo de franjas»), por eso es una constante y no un valor de la configuración.
	 */
	static final Duration DURACION = Duration.ofHours(1);

	/** @throws IllegalArgumentException si la franja no empieza en punto o no dura exactamente una hora */
	public Franja {
		Objects.requireNonNull(fecha, "fecha");
		Objects.requireNonNull(horaInicio, "horaInicio");
		Objects.requireNonNull(horaFin, "horaFin");
		// PAR-HOR-02 (en punto) y PAR-HOR-01 (una hora). 23:00 + 1 h da 00:00, que no es mayor: también se rechaza
		if (!enPunto(horaInicio) || !horaFin.isAfter(horaInicio) || !horaFin.equals(horaInicio.plus(DURACION))) {
			throw new IllegalArgumentException(
					"Una franja es de una hora en punto: " + fecha + " " + horaInicio + "–" + horaFin);
		}
	}

	/** Fecha y hora de inicio de la franja. */
	public LocalDateTime inicio() {
		return fecha.atTime(horaInicio);
	}

	/**
	 * Si la franja se cruza con un rango de horas de esa fecha (una reserva o un bloqueo). La hora
	 * de fin es abierta, igual que el tsrange '[)' de la base: una reserva de 17 a 18 no ocupa la
	 * franja de 18 a 19.
	 */
	public boolean seCruzaCon(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
		return this.fecha.equals(fecha)
				&& this.horaInicio.isBefore(horaFin)
				&& horaInicio.isBefore(this.horaFin);
	}

	private static boolean enPunto(LocalTime hora) {
		return hora.getMinute() == 0 && hora.getSecond() == 0 && hora.getNano() == 0;
	}

}
