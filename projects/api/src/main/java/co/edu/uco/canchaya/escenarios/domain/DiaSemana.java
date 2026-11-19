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

}
