package co.edu.uco.canchaya.reservas.infrastructure.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.reservas.application.port.in.ReservarCancha.ReservaConfirmada;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.domain.FormatoTexto;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/**
 * HU21, CA1: la reserva confirmada con EXI-RES-01. No lleva estado, confirmacionAsistencia,
 * origenCancelacion ni las fechas de creación y cancelación (AG-14). El id sí: lo usan HU23 y HU24.
 */
record RespuestaReserva(UUID id, UUID canchaId, String cancha, String escenario, LocalDate fecha, String horaInicio,
		String horaFin, BigDecimal precio, String codigo, String mensaje) {

	// AG-18 y AG-19 con FormatoTexto: el correo de HU22 muestra la fecha, las horas y el precio igual
	static RespuestaReserva de(ReservaConfirmada confirmada, CatalogoMensajes catalogo) {
		Reserva reserva = confirmada.reserva();
		String horaInicio = FormatoTexto.hora(reserva.getHoraInicio());
		String horaFin = FormatoTexto.hora(reserva.getHoraFin());
		String mensaje = catalogo.texto("EXI-RES-01", List.of(confirmada.escenario(), confirmada.cancha(),
				FormatoTexto.fecha(reserva.getFecha()), horaInicio, horaFin, FormatoTexto.precio(reserva.getPrecio())));
		return new RespuestaReserva(reserva.getId(), reserva.getCanchaId(), confirmada.cancha(),
				confirmada.escenario(), reserva.getFecha(), horaInicio, horaFin, reserva.getPrecio(), "EXI-RES-01",
				mensaje);
	}

}
