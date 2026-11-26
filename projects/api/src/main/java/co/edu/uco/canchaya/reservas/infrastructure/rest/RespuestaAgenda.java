package co.edu.uco.canchaya.reservas.infrastructure.rest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios.BloqueoAgenda;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarAgenda.Agenda;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarAgenda.CanchaDeAgenda;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarAgenda.DiaDeAgenda;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarAgenda.FranjaDeAgenda;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarAgenda.ReservaDeAgenda;
import co.edu.uco.canchaya.reservas.domain.ConfirmacionAsistencia;
import co.edu.uco.canchaya.reservas.domain.EstadoFranja;
import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.OrigenCancelacion;
import co.edu.uco.canchaya.reservas.domain.Reserva;

/**
 * HU29: la agenda del escenario (contrato de la especificación). Los estados van como valor del enum
 * y la pantalla pone la etiqueta. Los id van para que HU31, HU32 y HU33 puedan actuar; no van
 * jugadorId, precio ni las fechas de registro (AG-14).
 */
record RespuestaAgenda(UUID escenarioId, String escenario, boolean propietario, LocalDate desde, LocalDate hasta,
		List<RespuestaCancha> canchas) {

	// Las horas como texto hh:mm, igual que en HU21 y HU23
	private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

	static RespuestaAgenda de(Agenda agenda) {
		return new RespuestaAgenda(agenda.escenarioId(), agenda.escenario(), agenda.propietario(), agenda.desde(),
				agenda.hasta(), agenda.canchas().stream().map(RespuestaCancha::de).toList());
	}

	private static String hora(LocalTime hora) {
		return hora.format(HORA);
	}

	record RespuestaCancha(UUID id, String nombre, String estado, List<RespuestaDia> dias) {

		static RespuestaCancha de(CanchaDeAgenda cancha) {
			return new RespuestaCancha(cancha.id(), cancha.nombre(), cancha.estado(),
					cancha.dias().stream().map(RespuestaDia::de).toList());
		}

	}

	record RespuestaDia(LocalDate fecha, List<RespuestaFranja> franjas, List<RespuestaReservaAgenda> reservas,
			List<RespuestaBloqueo> bloqueos) {

		static RespuestaDia de(DiaDeAgenda dia) {
			return new RespuestaDia(dia.fecha(), dia.franjas().stream().map(RespuestaFranja::de).toList(),
					dia.reservas().stream().map(RespuestaReservaAgenda::de).toList(),
					dia.bloqueos().stream().map(RespuestaBloqueo::de).toList());
		}

	}

	/** CA1. */
	record RespuestaFranja(String horaInicio, String horaFin, EstadoFranja estado, UUID reservaId, UUID bloqueoId) {

		static RespuestaFranja de(FranjaDeAgenda franja) {
			return new RespuestaFranja(hora(franja.franja().horaInicio()), hora(franja.franja().horaFin()),
					franja.estado(), franja.reservaId(), franja.bloqueoId());
		}

	}

	/** CA3. celularJugador es dato sensible: solo sale aquí, para el propietario y los encargados (AG-15). */
	record RespuestaReservaAgenda(UUID id, String horaInicio, String horaFin, EstadoReserva estado, String jugador,
			String celularJugador, ConfirmacionAsistencia confirmacionAsistencia, OrigenCancelacion origenCancelacion,
			String motivoCancelacion, OffsetDateTime fechaCancelacion, boolean puedeCancelarPorFuerzaMayor,
			boolean puedeMarcarInasistencia) {

		static RespuestaReservaAgenda de(ReservaDeAgenda deAgenda) {
			Reserva reserva = deAgenda.reserva();
			// La justificación solo se muestra si canceló el administrador (regla del atributo, como en HU23)
			String motivo = reserva.getOrigenCancelacion() == OrigenCancelacion.ADMINISTRADOR
					? reserva.getMotivoCancelacion()
					: null;
			return new RespuestaReservaAgenda(reserva.getId(), hora(reserva.getHoraInicio()),
					hora(reserva.getHoraFin()), reserva.getEstado(), deAgenda.jugador(), deAgenda.celularJugador(),
					reserva.getConfirmacionAsistencia(), reserva.getOrigenCancelacion(), motivo,
					reserva.getFechaCancelacion(), deAgenda.puedeCancelarPorFuerzaMayor(),
					deAgenda.puedeMarcarInasistencia());
		}

	}

	record RespuestaBloqueo(UUID id, String horaInicio, String horaFin, String tipoBloqueo, String motivo) {

		static RespuestaBloqueo de(BloqueoAgenda bloqueo) {
			return new RespuestaBloqueo(bloqueo.id(), hora(bloqueo.horaInicio()), hora(bloqueo.horaFin()),
					bloqueo.tipoBloqueo(), bloqueo.motivo());
		}

	}

}
