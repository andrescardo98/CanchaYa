package co.edu.uco.canchaya.reservas.application.port.out;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.uco.canchaya.reservas.domain.Reserva;

public interface RepositorioReservas {

	/**
	 * Si alguna de las canchas tiene una reserva CONFIRMADA que empieza después de {@code momento}
	 * (fecha y hora de America/Bogota, D27). HU13, D08.
	 */
	boolean existenConfirmadasQueEmpiezanDespuesDe(Collection<UUID> canchaIds, LocalDateTime momento);

	/**
	 * Como la anterior, pero solo las de ese día de la semana que se cruzan con
	 * [horaInicio, horaFin), aunque sea en parte. HU12, política 15 de HorarioRecurrente (D08, P2).
	 */
	boolean existenConfirmadasEnHorarioQueEmpiezanDespuesDe(Collection<UUID> canchaIds, DayOfWeek dia,
			LocalTime horaInicio, LocalTime horaFin, LocalDateTime momento);

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

	/**
	 * HU23 (CA5): todas las reservas del jugador, en cualquier estado y fecha, y solo las suyas. El
	 * orden lo pone el caso de uso, porque depende de la hora actual.
	 */
	List<Reserva> deJugador(UUID jugadorId);

	/** Política 14 (HU24): la reserva con ese id, en cualquier estado. */
	Optional<Reserva> buscarPorId(UUID id);

	/**
	 * HU24, P3 (D29 punto 3): guarda el estado, el origen y la fecha de cancelación solo si la
	 * reserva sigue CONFIRMADA en la base.
	 *
	 * @return falso si otra operación ya la había cambiado, y entonces no se toca nada
	 */
	boolean cancelarSiSigueConfirmada(Reserva reserva);

	/**
	 * HU29 (P4): todas las reservas de esas canchas de desde a hasta (las dos incluidas), en cualquier
	 * estado, por fecha y hora de inicio. Vacía si no se pide ninguna cancha.
	 */
	List<Reserva> deCanchasEntre(Collection<UUID> canchaIds, LocalDate desde, LocalDate hasta);

}
