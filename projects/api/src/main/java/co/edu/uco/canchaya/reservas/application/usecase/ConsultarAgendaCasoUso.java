package co.edu.uco.canchaya.reservas.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios.BloqueoAgenda;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios.CanchaAgenda;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios.DatosAgenda;
import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios.ContactoUsuario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarAgenda;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.EstadoFranja;
import co.edu.uco.canchaya.reservas.domain.ParametrosReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.reservas.domain.VistaAgenda;

/**
 * HU29. Solo lee: no toma el turno de la cancha ni escribe, así que D29 y D36 no aplican, y no envía
 * correos (D15 no aplica).
 */
@Service
public class ConsultarAgendaCasoUso implements ConsultarAgenda {

	private final ConsultaEscenarios consultaEscenarios;
	private final RepositorioReservas repositorioReservas;
	private final ConsultaUsuarios consultaUsuarios;
	private final ParametrosReserva parametros;
	private final Clock reloj;

	public ConsultarAgendaCasoUso(ConsultaEscenarios consultaEscenarios, RepositorioReservas repositorioReservas,
			ConsultaUsuarios consultaUsuarios, ParametrosReserva parametros, Clock reloj) {
		this.consultaEscenarios = consultaEscenarios;
		this.repositorioReservas = repositorioReservas;
		this.consultaUsuarios = consultaUsuarios;
		this.parametros = parametros;
		this.reloj = reloj;
	}

	@Override
	public Agenda ejecutar(UUID escenarioId, LocalDate fecha, VistaAgenda vista, Solicitante solicitante) {
		// ESC-USA-04: sin fecha, la agenda abre en el día de hoy del servidor (D27)
		LocalDate dia = fecha != null ? fecha : LocalDate.now(reloj);
		VistaAgenda vistaElegida = vista != null ? vista : VistaAgenda.DIA;
		LocalDate desde = vistaElegida.desde(dia);
		LocalDate hasta = vistaElegida.hasta(dia);

		// CA6: Escenarios revisa el permiso antes de que aquí se lea cualquier reserva
		DatosAgenda datos = consultaEscenarios.datosAgenda(escenarioId, solicitante, desde, hasta);

		// P4: todas las reservas del rango, también las canceladas, en una sola consulta
		List<UUID> canchaIds = datos.canchas().stream().map(CanchaAgenda::id).toList();
		List<Reserva> reservas = repositorioReservas.deCanchasEntre(canchaIds, desde, hasta);

		// CA3: nombre y celular de todos los jugadores en una sola llamada
		Map<UUID, ContactoUsuario> jugadores = contactos(reservas);
		LocalDateTime ahora = LocalDateTime.now(reloj);

		List<CanchaDeAgenda> canchas = new ArrayList<>();
		for (CanchaAgenda cancha : datos.canchas()) {
			List<DiaDeAgenda> dias = new ArrayList<>();
			for (Map.Entry<LocalDate, List<Franja>> delDia : datos.franjasPorFecha().entrySet()) {
				dias.add(diaDeAgenda(cancha.id(), delDia.getKey(), delDia.getValue(), reservas, datos.bloqueos(),
						jugadores, datos.propietario(), ahora));
			}
			canchas.add(new CanchaDeAgenda(cancha.id(), cancha.nombre(), cancha.estado(), List.copyOf(dias)));
		}
		return new Agenda(datos.escenarioId(), datos.escenario(), datos.propietario(), desde, hasta,
				List.copyOf(canchas));
	}

	private Map<UUID, ContactoUsuario> contactos(List<Reserva> reservas) {
		if (reservas.isEmpty()) {
			return Map.of();
		}
		Set<UUID> jugadorIds = reservas.stream().map(Reserva::getJugadorId).collect(Collectors.toSet());
		return consultaUsuarios.buscarContactos(jugadorIds).stream()
				.collect(Collectors.toMap(ContactoUsuario::id, Function.identity()));
	}

	private DiaDeAgenda diaDeAgenda(UUID canchaId, LocalDate fecha, List<Franja> franjas, List<Reserva> todas,
			List<BloqueoAgenda> todosLosBloqueos, Map<UUID, ContactoUsuario> jugadores, boolean propietario,
			LocalDateTime ahora) {
		// El repositorio ya las trae por fecha y hora de inicio; el filtro conserva ese orden
		List<Reserva> reservas = todas.stream()
				.filter(r -> r.getCanchaId().equals(canchaId) && r.getFecha().equals(fecha)).toList();
		List<BloqueoAgenda> bloqueos = todosLosBloqueos.stream()
				.filter(b -> b.canchaId().equals(canchaId) && b.fecha().equals(fecha)).toList();

		List<FranjaDeAgenda> franjasDeAgenda = franjas.stream()
				.map(franja -> franjaDeAgenda(franja, reservas, bloqueos)).toList();

		List<ReservaDeAgenda> reservasDeAgenda = reservas.stream().map(reserva -> {
			Optional<ContactoUsuario> jugador = Optional.ofNullable(jugadores.get(reserva.getJugadorId()));
			return new ReservaDeAgenda(reserva, jugador.map(ContactoUsuario::nombre).orElse(null),
					jugador.map(ContactoUsuario::numeroCelular).orElse(null),
					// CA3: las opciones de quien consulta, con la hora del servidor (D02, PAR-RES-09)
					reserva.sePuedeCancelarPorFuerzaMayor(ahora, propietario),
					reserva.sePuedeMarcarInasistencia(ahora, parametros));
		}).toList();

		return new DiaDeAgenda(fecha, franjasDeAgenda, reservasDeAgenda, bloqueos);
	}

	private static FranjaDeAgenda franjaDeAgenda(Franja franja, List<Reserva> reservas, List<BloqueoAgenda> bloqueos) {
		Optional<BloqueoAgenda> bloqueo = bloqueos.stream()
				.filter(b -> franja.seCruzaCon(b.fecha(), b.horaInicio(), b.horaFin())).findFirst();
		// CA1: libre, reservada o bloqueada; la cancelada no ocupa (P4)
		EstadoFranja estado = EstadoFranja.de(franja, reservas, bloqueo.isPresent());
		UUID reservaId = estado == EstadoFranja.RESERVADA
				? reservas.stream().filter(r -> r.ocupa(franja)).findFirst().map(Reserva::getId).orElseThrow()
				: null;
		UUID bloqueoId = estado == EstadoFranja.BLOQUEADA ? bloqueo.map(BloqueoAgenda::id).orElseThrow() : null;
		return new FranjaDeAgenda(franja, estado, reservaId, bloqueoId);
	}

}
