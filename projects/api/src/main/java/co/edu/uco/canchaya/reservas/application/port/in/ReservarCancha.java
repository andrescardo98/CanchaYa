package co.edu.uco.canchaya.reservas.application.port.in;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.domain.Reserva;

/** HU21: un jugador reserva 1 o 2 franjas seguidas de una cancha (D10). Exige sesión (AG-11). */
public interface ReservarCancha {

	/**
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-RES-05, ERR-RES-01,
	 *         ERR-RES-02, ERR-RES-31, ERR-RES-06, ERR-RES-07, ERR-RES-08 o ERR-RES-09
	 */
	ReservaConfirmada ejecutar(UUID canchaId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
			Solicitante solicitante);

	/** La reserva guardada y los nombres que pide EXI-RES-01. */
	record ReservaConfirmada(Reserva reserva, String cancha, String escenario) {
	}

}
