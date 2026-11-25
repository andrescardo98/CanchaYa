package co.edu.uco.canchaya.escenarios.domain;

import java.time.DayOfWeek;

/** Día de un horario recurrente. Son los mismos valores que acepta la columna dia_semana. */
public enum DiaSemana {

	LUNES,

	MARTES,

	MIERCOLES,

	JUEVES,

	VIERNES,

	SABADO,

	DOMINGO;

	/**
	 * El día de una fecha (HU20, calcularFranjas). Se compara por nombre de día y no por la
	 * posición en el enum, para no depender del orden en que están escritos los valores.
	 */
	public static DiaSemana de(DayOfWeek dia) {
		return switch (dia) {
			case MONDAY -> LUNES;
			case TUESDAY -> MARTES;
			case WEDNESDAY -> MIERCOLES;
			case THURSDAY -> JUEVES;
			case FRIDAY -> VIERNES;
			case SATURDAY -> SABADO;
			case SUNDAY -> DOMINGO;
		};
	}

	/**
	 * El inverso de {@link #de(DayOfWeek)}. Con él Escenarios le pasa el día a Reservas
	 * (ConsultaReservasFuturas, HU12) sin que Reservas dependa de este enum.
	 */
	public DayOfWeek aDayOfWeek() {
		return switch (this) {
			case LUNES -> DayOfWeek.MONDAY;
			case MARTES -> DayOfWeek.TUESDAY;
			case MIERCOLES -> DayOfWeek.WEDNESDAY;
			case JUEVES -> DayOfWeek.THURSDAY;
			case VIERNES -> DayOfWeek.FRIDAY;
			case SABADO -> DayOfWeek.SATURDAY;
			case DOMINGO -> DayOfWeek.SUNDAY;
		};
	}

}
