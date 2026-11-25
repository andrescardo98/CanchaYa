package co.edu.uco.canchaya.escenarios.application.port.out;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collection;
import java.util.UUID;

/**
 * Escenarios la define y Reservas la implementa (D08). Así Reservas depende de Escenarios y no
 * al revés, sin dependencia en círculo. Escenarios entrega los identificadores de sus canchas
 * para que Reservas no tenga que leer la tabla cancha.
 */
public interface ConsultaReservasFuturas {

	/**
	 * Si alguna de las canchas tiene una reserva CONFIRMADA que todavía no empieza, con la hora
	 * de America/Bogota (D27). RN-17.
	 */
	boolean existenConfirmadasFuturas(Collection<UUID> canchaIds);

	/**
	 * HU12, política 15: si alguna de las canchas tiene una reserva CONFIRMADA que todavía no
	 * empieza, en ese día de la semana, y que se cruza aunque sea en parte con
	 * [horaInicio, horaFin) (P2). El día va como DayOfWeek para que Reservas no dependa de
	 * DiaSemana.
	 */
	boolean existenConfirmadasFuturasEnHorario(Collection<UUID> canchaIds, DayOfWeek dia, LocalTime horaInicio,
			LocalTime horaFin);

}
