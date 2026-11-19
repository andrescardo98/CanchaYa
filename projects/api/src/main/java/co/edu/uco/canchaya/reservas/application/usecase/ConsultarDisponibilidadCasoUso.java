package co.edu.uco.canchaya.reservas.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.RangoBloqueado;
import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarDisponibilidad;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.ParametrosReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * La disponibilidad la arma Reservas porque es el único módulo que ve a la vez sus reservas y las
 * franjas y bloqueos de Escenarios (por ConsultaCanchas). Solo lee: no abre transacción de escritura.
 */
@Service
public class ConsultarDisponibilidadCasoUso implements ConsultarDisponibilidad {

	private final ConsultaCanchas consultaCanchas;
	private final RepositorioReservas repositorioReservas;
	private final ParametrosReserva parametros;
	private final Clock reloj;

	public ConsultarDisponibilidadCasoUso(ConsultaCanchas consultaCanchas, RepositorioReservas repositorioReservas,
			ParametrosReserva parametros, Clock reloj) {
		this.consultaCanchas = consultaCanchas;
		this.repositorioReservas = repositorioReservas;
		this.parametros = parametros;
		this.reloj = reloj;
	}

	@Override
	public Disponibilidad ejecutar(UUID canchaId, LocalDate fecha) {
		LocalDateTime ahora = LocalDateTime.now(reloj);
		LocalDate hoy = ahora.toLocalDate();
		// CA1: por defecto se muestra la fecha de hoy, la del servidor (AG-17)
		LocalDate consultada = fecha == null ? hoy : fecha;

		// CA6 y P2: primero la cancha, porque sin ella no se pueden calcular las franjas
		consultaCanchas.buscarReservable(canchaId).orElseThrow(() -> new ReglaNegocioException("ERR-RES-01"));
		// CA5 (ERR-HOR-10) y CA3: ya vienen sin las franjas que empezaron
		List<Franja> franjas = consultaCanchas.franjasDelDia(canchaId, consultada);

		// CA2: ocupan las reservas CONFIRMADAS y los bloqueos
		List<Reserva> reservas = repositorioReservas.confirmadasDeCanchaEnFecha(canchaId, consultada);
		List<RangoBloqueado> bloqueos = consultaCanchas.bloqueosDelDia(canchaId, consultada);
		List<Franja> franjasLibres = franjas.stream()
				.filter(f -> reservas.stream().noneMatch(r -> f.seCruzaCon(r.getFecha(), r.getHoraInicio(), r.getHoraFin())))
				.filter(f -> bloqueos.stream().noneMatch(b -> f.seCruzaCon(consultada, b.horaInicio(), b.horaFin())))
				.toList();

		List<FranjaDisponible> disponibles = franjas.stream()
				.map(f -> new FranjaDisponible(f.horaInicio(), f.horaFin(), Reserva.verificarDisponibilidad(consultada,
						f.horaInicio(), f.horaFin(), true, franjasLibres, ahora, parametros)))
				.toList();
		return new Disponibilidad(canchaId, consultada, hoy, hoy.plusDays(parametros.ventanaReservaDias()),
				parametros.franjasMaximasPorReserva(), disponibles);
	}

}
