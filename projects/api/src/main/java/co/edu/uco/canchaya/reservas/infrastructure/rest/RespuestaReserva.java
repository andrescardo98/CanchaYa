package co.edu.uco.canchaya.reservas.infrastructure.rest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.reservas.application.port.in.ReservarCancha.ReservaConfirmada;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/**
 * HU21, CA1: la reserva confirmada con EXI-RES-01. No lleva estado, confirmacionAsistencia,
 * origenCancelacion ni las fechas de creación y cancelación (AG-14). El id sí: lo usan HU23 y HU24.
 */
record RespuestaReserva(UUID id, UUID canchaId, String cancha, String escenario, LocalDate fecha, String horaInicio,
		String horaFin, BigDecimal precio, String codigo, String mensaje) {

	private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
	// AG-18: así va la fecha dentro del mensaje
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	static RespuestaReserva de(ReservaConfirmada confirmada, CatalogoMensajes catalogo) {
		Reserva reserva = confirmada.reserva();
		String horaInicio = reserva.getHoraInicio().format(HORA);
		String horaFin = reserva.getHoraFin().format(HORA);
		String mensaje = catalogo.texto("EXI-RES-01", List.of(confirmada.escenario(), confirmada.cancha(),
				reserva.getFecha().format(FECHA), horaInicio, horaFin, precioConFormato(reserva.getPrecio())));
		return new RespuestaReserva(reserva.getId(), reserva.getCanchaId(), confirmada.cancha(),
				confirmada.escenario(), reserva.getFecha(), horaInicio, horaFin, reserva.getPrecio(), "EXI-RES-01",
				mensaje);
	}

	// AG-19: pesos con signo y punto de miles, sin decimales. Por ejemplo, $ 160.000
	static String precioConFormato(BigDecimal precio) {
		DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
		simbolos.setGroupingSeparator('.');
		DecimalFormat formato = new DecimalFormat("#,##0", simbolos);
		formato.setRoundingMode(RoundingMode.HALF_UP);
		return "$ " + formato.format(precio);
	}

}
