package co.edu.uco.canchaya.reservas.infrastructure.configuracion;

import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.edu.uco.canchaya.reservas.domain.ParametrosReserva;

/**
 * Arma ParametrosReserva con los valores del catálogo que están en application.properties (P8 de
 * HU20, regla 7 de AGENTS.md). Así el dominio recibe los parámetros sin depender de Spring.
 */
@Configuration
class ConfiguracionReservas {

	@Bean
	ParametrosReserva parametrosReserva(
			@Value("${canchaya.reserva.ventana-dias}") int ventanaReservaDias,
			@Value("${canchaya.reserva.franjas-maximas}") int franjasMaximasPorReserva,
			@Value("${canchaya.reserva.horas-maximas-dia}") int horasMaximasPorDia,
			@Value("${canchaya.reserva.activas-maximas}") int reservasActivasMaximas,
			@Value("${canchaya.reserva.horas-minimas-cancelacion}") int horasMinimasCancelacion,
			@Value("${canchaya.reserva.horas-anticipacion-confirmacion}") int horasAnticipacionConfirmacion,
			@Value("${canchaya.reserva.hora-minima-confirmacion}") String horaMinimaConfirmacion,
			@Value("${canchaya.reserva.horas-plazo-confirmacion}") int horasPlazoConfirmacion,
			@Value("${canchaya.reserva.horas-plazo-inasistencia}") int horasPlazoInasistencia,
			@Value("${canchaya.reserva.longitud-maxima-justificacion}") int longitudMaximaJustificacion,
			@Value("${canchaya.reserva.confirmacion-asistencia-activa}") boolean confirmacionAsistenciaActiva) {
		return new ParametrosReserva(ventanaReservaDias, franjasMaximasPorReserva, horasMaximasPorDia,
				reservasActivasMaximas, horasMinimasCancelacion, horasAnticipacionConfirmacion,
				LocalTime.parse(horaMinimaConfirmacion), horasPlazoConfirmacion, horasPlazoInasistencia,
				longitudMaximaJustificacion, confirmacionAsistenciaActiva);
	}

}
