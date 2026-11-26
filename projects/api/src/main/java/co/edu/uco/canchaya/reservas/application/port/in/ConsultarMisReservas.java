package co.edu.uco.canchaya.reservas.application.port.in;

import java.util.List;

import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.domain.Reserva;

/** HU23: las reservas del jugador del token, vigentes y pasadas. Exige sesión (AG-11). */
public interface ConsultarMisReservas {

	/**
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-GEN-06 si el
	 *         solicitante no tiene rol JUGADOR (P2)
	 */
	MisReservas ejecutar(Solicitante solicitante);

	/**
	 * @param vigentes CONFIRMADAS que no han terminado, de la más próxima a la más lejana (P1)
	 * @param pasadas las demás, de la más reciente a la más antigua
	 */
	record MisReservas(List<ReservaDelJugador> vigentes, List<ReservaDelJugador> pasadas) {

		/** CA2: el jugador aún no tiene reservas. */
		public boolean vacia() {
			return vigentes.isEmpty() && pasadas.isEmpty();
		}

	}

	/**
	 * @param cancha y escenario: los nombres que muestra la lista (CA1)
	 * @param cancelable si hoy se puede mostrar la opción de cancelar (CA3, política 16)
	 */
	record ReservaDelJugador(Reserva reserva, String cancha, String escenario, boolean cancelable) {
	}

}
