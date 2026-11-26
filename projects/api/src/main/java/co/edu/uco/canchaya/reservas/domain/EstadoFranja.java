package co.edu.uco.canchaya.reservas.domain;

import java.util.List;
import java.util.Objects;

import co.edu.uco.canchaya.escenarios.domain.Franja;

/** HU29, CA1: cómo se ve una franja en la agenda del escenario. */
public enum EstadoFranja {

	// Sin reserva que la ocupe ni bloqueo
	LIBRE,

	// La ocupa una reserva CONFIRMADA o NO_ASISTIO
	RESERVADA,

	// La cubre un bloqueo (HU30)
	BLOQUEADA;

	/**
	 * La reserva manda sobre el bloqueo: HU30 no deja bloquear encima de una reserva confirmada
	 * (ERR-BLO-06), así que las dos a la vez solo pasarían con datos viejos.
	 *
	 * @param reservasDelDia reservas de la cancha en la fecha de la franja, en cualquier estado
	 * @param bloqueada      si algún bloqueo de la cancha cubre la franja
	 */
	public static EstadoFranja de(Franja franja, List<Reserva> reservasDelDia, boolean bloqueada) {
		Objects.requireNonNull(franja, "franja");
		Objects.requireNonNull(reservasDelDia, "reservasDelDia");
		if (reservasDelDia.stream().anyMatch(reserva -> reserva.ocupa(franja))) {
			return RESERVADA;
		}
		return bloqueada ? BLOQUEADA : LIBRE;
	}

}
