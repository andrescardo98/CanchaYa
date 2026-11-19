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

}
