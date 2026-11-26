package co.edu.uco.canchaya.reservas.application.port.in;

import java.util.UUID;

import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.domain.Reserva;

/** HU24: el jugador cancela su reserva confirmada con más de 3 horas de anticipación (D06). Exige sesión (AG-11). */
public interface CancelarReserva {

	/**
	 * @return la reserva CANCELADA, con origen JUGADOR y su fecha de cancelación
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-RES-14, ERR-RES-15 o
	 *         ERR-RES-16
	 */
	Reserva ejecutar(UUID reservaId, Solicitante solicitante);

}
