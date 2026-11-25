package co.edu.uco.canchaya.reservas.infrastructure.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.reservas.application.port.in.ConsultarMisReservas.MisReservas;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarMisReservas.ReservaDelJugador;
import co.edu.uco.canchaya.reservas.domain.ConfirmacionAsistencia;
import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.OrigenCancelacion;
import co.edu.uco.canchaya.reservas.domain.Reserva;

/**
 * HU23: las reservas del jugador, ya separadas y ordenadas por el servidor (P4). codigo y mensaje
 * solo vienen con INF-RES-01, cuando no tiene ninguna (CA2).
 */
record RespuestaMisReservas(List<RespuestaReservaDelJugador> vigentes, List<RespuestaReservaDelJugador> pasadas,
		String codigo, String mensaje) {

	static RespuestaMisReservas de(MisReservas mis, String codigo, String mensaje) {
		return new RespuestaMisReservas(mis.vigentes().stream().map(RespuestaReservaDelJugador::de).toList(),
				mis.pasadas().stream().map(RespuestaReservaDelJugador::de).toList(), codigo, mensaje);
	}

	/**
	 * CA1: lo que muestra cada reserva. Los estados van como valor del enum y la pantalla pone la
	 * etiqueta (P5). El id lo usará HU24 para cancelar; no van canchaId, jugadorId ni las fechas de
	 * registro (AG-14).
	 */
	record RespuestaReservaDelJugador(UUID id, String escenario, String cancha, LocalDate fecha, String horaInicio,
			String horaFin, BigDecimal precio, EstadoReserva estado, ConfirmacionAsistencia confirmacionAsistencia,
			OrigenCancelacion origenCancelacion, String motivoCancelacion, boolean cancelable) {

		// Las horas como texto hh:mm, igual que RespuestaReserva (HU21)
		private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

		static RespuestaReservaDelJugador de(ReservaDelJugador delJugador) {
			Reserva reserva = delJugador.reserva();
			// CA4: la justificación solo se muestra si canceló el administrador
			String motivo = reserva.getOrigenCancelacion() == OrigenCancelacion.ADMINISTRADOR
					? reserva.getMotivoCancelacion()
					: null;
			return new RespuestaReservaDelJugador(reserva.getId(), delJugador.escenario(), delJugador.cancha(),
					reserva.getFecha(), reserva.getHoraInicio().format(HORA), reserva.getHoraFin().format(HORA),
					reserva.getPrecio(), reserva.getEstado(), reserva.getConfirmacionAsistencia(),
					reserva.getOrigenCancelacion(), motivo, delJugador.cancelable());
		}

	}

}
