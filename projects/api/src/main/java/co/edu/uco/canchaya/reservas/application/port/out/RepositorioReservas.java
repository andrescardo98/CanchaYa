package co.edu.uco.canchaya.reservas.application.port.out;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

public interface RepositorioReservas {

	/**
	 * Si alguna de las canchas tiene una reserva CONFIRMADA que empieza después de {@code momento}
	 * (fecha y hora de America/Bogota, D27). HU13, D08.
	 */
	boolean existenConfirmadasQueEmpiezanDespuesDe(Collection<UUID> canchaIds, LocalDateTime momento);

}
