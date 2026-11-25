package co.edu.uco.canchaya.reservas.application.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.reservas.domain.Reserva;

public interface RepositorioReservas {

	/**
	 * Si alguna de las canchas tiene una reserva CONFIRMADA que empieza después de {@code momento}
	 * (fecha y hora de America/Bogota, D27). HU13, D08.
	 */
	boolean existenConfirmadasQueEmpiezanDespuesDe(Collection<UUID> canchaIds, LocalDateTime momento);

	/**
	 * Reservas CONFIRMADAS de la cancha en esa fecha, por hora de inicio: son las que ocupan franjas
	 * (HU20, CA2). HU21 la usa dentro del turno de la cancha (D36).
	 */
	List<Reserva> confirmadasDeCanchaEnFecha(UUID canchaId, LocalDate fecha);

	/**
	 * HU18 (CA5): reservas CONFIRMADAS de varias canchas en esa fecha, en una sola consulta, por hora
	 * de inicio. Vacía si no se pide ninguna cancha.
	 */
	List<Reserva> confirmadasDeCanchasEnFecha(Collection<UUID> canchaIds, LocalDate fecha);

	/** Política 10 de Reserva (HU21). */
	boolean existePorId(UUID id);

	/**
	 * HU21. Si la restricción ex_reserva_sin_cruce rechaza la reserva, lanza ERR-RES-06: es el
	 * respaldo del turno de la cancha (D29 punto 1).
	 */
	void guardar(Reserva reserva);

	/**
	 * Política 8 (HU21, P4): reservas CONFIRMADAS del jugador que no han terminado
	 * ({@code fecha + horaFin > ahora}, en America/Bogota).
	 */
	int contarActivasDeJugador(UUID jugadorId, LocalDateTime ahora);

	/** Política 9 (HU21): horas de las reservas CONFIRMADAS del jugador en esa fecha, en cualquier cancha. */
	int horasConfirmadasDeJugadorEnFecha(UUID jugadorId, LocalDate fecha);

}
