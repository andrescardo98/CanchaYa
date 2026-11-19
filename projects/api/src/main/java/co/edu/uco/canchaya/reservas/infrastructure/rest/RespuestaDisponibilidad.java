package co.edu.uco.canchaya.reservas.infrastructure.rest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.reservas.application.port.in.ConsultarDisponibilidad.Disponibilidad;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarDisponibilidad.FranjaDisponible;

/**
 * HU20: franjas de la cancha en la fecha. No trae el jugador, el tipo ni el motivo del bloqueo
 * (restricción de la HU). codigo y mensaje solo vienen con INF-HOR-01, cuando no hay franjas (CA4).
 */
record RespuestaDisponibilidad(UUID canchaId, LocalDate fecha, LocalDate fechaMinima, LocalDate fechaMaxima,
		int franjasMaximasPorReserva, List<RespuestaFranja> franjas, String codigo, String mensaje) {

	static RespuestaDisponibilidad de(Disponibilidad disponibilidad, String codigo, String mensaje) {
		return new RespuestaDisponibilidad(disponibilidad.canchaId(), disponibilidad.fecha(),
				disponibilidad.fechaMinima(), disponibilidad.fechaMaxima(), disponibilidad.franjasMaximasPorReserva(),
				disponibilidad.franjas().stream().map(RespuestaFranja::de).toList(), codigo, mensaje);
	}

	// Las horas van como texto hh:mm, igual que en RespuestaHorario (HU7)
	record RespuestaFranja(String horaInicio, String horaFin, boolean libre) {

		private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

		static RespuestaFranja de(FranjaDisponible franja) {
			return new RespuestaFranja(franja.horaInicio().format(HORA), franja.horaFin().format(HORA), franja.libre());
		}

	}

}
