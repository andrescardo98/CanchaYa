package co.edu.uco.canchaya.reservas.application.usecase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.CanchaReservable;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.RangoBloqueado;
import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.application.port.in.ReservarCancha;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.ParametrosReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * HU21. Todo va en una sola transacción porque el turno de la cancha dura hasta que la transacción
 * termina (D36): mientras tanto, otra reserva o un bloqueo de la misma cancha esperan (D29 punto 2).
 * Los correos no van aquí: los agrega HU22 después de confirmar la transacción (D15, P6).
 */
@Service
public class ReservarCanchaCasoUso implements ReservarCancha {

	private static final Logger LOG = LoggerFactory.getLogger(ReservarCanchaCasoUso.class);

	private final ConsultaCanchas consultaCanchas;
	private final RepositorioReservas repositorioReservas;
	private final ParametrosReserva parametros;
	private final Clock reloj;
	// Es la TransactionTemplate de Spring; la interfaz deja probar el caso de uso sin base
	private final TransactionOperations transaccion;

	public ReservarCanchaCasoUso(ConsultaCanchas consultaCanchas, RepositorioReservas repositorioReservas,
			ParametrosReserva parametros, Clock reloj, TransactionOperations transaccion) {
		this.consultaCanchas = consultaCanchas;
		this.repositorioReservas = repositorioReservas;
		this.parametros = parametros;
		this.reloj = reloj;
		this.transaccion = transaccion;
	}

	@Override
	public ReservaConfirmada ejecutar(UUID canchaId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
			Solicitante solicitante) {
		return transaccion.execute(estado -> reservar(canchaId, fecha, horaInicio, horaFin, solicitante));
	}

	private ReservaConfirmada reservar(UUID canchaId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
			Solicitante solicitante) {
		// 1. D29 punto 2: el turno se toma antes de leer franjas y reservas, si no se leería algo viejo
		consultaCanchas.tomarTurno(canchaId);
		LocalDateTime ahora = LocalDateTime.now(reloj);

		// 2. Política 1. Sin cancha no se piden franjas; confirmar decide entre ERR-RES-05 y ERR-RES-01
		Optional<CanchaReservable> cancha = consultaCanchas.buscarReservable(canchaId);
		List<Franja> franjasLibres = List.of();
		boolean cubiertaPorBloqueo = false;
		if (cancha.isPresent()) {
			// 3. Ya vienen sin las franjas que empezaron
			List<Franja> franjas = franjasDelDia(canchaId, fecha);
			// 4. Reservas y bloqueos por separado, para distinguir ERR-RES-06 de ERR-RES-07
			List<Reserva> reservas = repositorioReservas.confirmadasDeCanchaEnFecha(canchaId, fecha);
			franjasLibres = franjas.stream()
					.filter(f -> reservas.stream().noneMatch(r -> f.seCruzaCon(r.getFecha(), r.getHoraInicio(), r.getHoraFin())))
					.toList();
			List<RangoBloqueado> bloqueos = consultaCanchas.bloqueosDelDia(canchaId, fecha);
			cubiertaPorBloqueo = bloqueos.stream()
					.anyMatch(b -> b.horaInicio().isBefore(horaFin) && horaInicio.isBefore(b.horaFin()));
		}

		// 5. Políticas 8 y 9: los límites del jugador
		UUID jugadorId = solicitante.usuarioId();
		int activas = repositorioReservas.contarActivasDeJugador(jugadorId, ahora);
		int horasDelDia = repositorioReservas.horasConfirmadasDeJugadorEnFecha(jugadorId, fecha);

		// 6
		BigDecimal precioHora = cancha.map(CanchaReservable::precioHora).orElse(null);
		Reserva reserva = Reserva.confirmar(canchaId, solicitante, fecha, horaInicio, horaFin, precioHora,
				cancha.isPresent(), franjasLibres, cubiertaPorBloqueo, activas, horasDelDia, ahora, parametros);

		// 7. Política 10. ERR-RES-10 solo va al log; el usuario no lo ve
		while (repositorioReservas.existePorId(reserva.getId())) {
			LOG.warn("ERR-RES-10 objeto=Reserva id={}", reserva.getId());
			reserva = reserva.conNuevoId();
		}

		// 8. Si la restricción de exclusión rechaza el cruce, el repositorio lanza ERR-RES-06
		repositorioReservas.guardar(reserva);
		return new ReservaConfirmada(reserva, cancha.get().nombre(), cancha.get().nombreEscenario());
	}

	// P1: fuera de la ventana, Escenarios responde ERR-HOR-10, pero al jugador que reserva se le
	// dice ERR-RES-02. Se sigue sin franjas y confirmar lanza el error con su propia regla
	private List<Franja> franjasDelDia(UUID canchaId, LocalDate fecha) {
		try {
			return consultaCanchas.franjasDelDia(canchaId, fecha);
		} catch (ReglaNegocioException e) {
			if ("ERR-HOR-10".equals(e.getCodigoMensaje())) {
				return List.of();
			}
			throw e;
		}
	}

}
