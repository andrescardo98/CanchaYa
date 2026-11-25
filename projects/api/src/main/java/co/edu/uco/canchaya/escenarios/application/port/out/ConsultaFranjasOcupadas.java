package co.edu.uco.canchaya.escenarios.application.port.out;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Escenarios la define y Reservas la implementa (P1 de HU18), igual que
 * {@link ConsultaReservasFuturas} (D08): así Reservas depende de Escenarios y no al revés.
 * Escenarios entrega los identificadores de sus canchas para que Reservas no lea la tabla cancha.
 */
public interface ConsultaFranjasOcupadas {

	/**
	 * Horas de las reservas CONFIRMADAS de esas canchas en la fecha, por cancha. Sin el jugador ni
	 * nada más de la reserva. Una cancha sin reservas no viene en el mapa (HU18, CA5 y P2).
	 */
	Map<UUID, List<RangoOcupado>> confirmadasDelDia(Collection<UUID> canchaIds, LocalDate fecha);

	record RangoOcupado(LocalTime horaInicio, LocalTime horaFin) {
	}

}
