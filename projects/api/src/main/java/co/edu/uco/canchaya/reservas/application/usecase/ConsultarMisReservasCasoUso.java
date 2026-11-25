package co.edu.uco.canchaya.reservas.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.NombresCancha;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarMisReservas;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.ParametrosReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * HU23. Solo lee: no toma el turno de la cancha ni escribe, así que D29 y D36 no aplican, y no envía
 * correos (D15 no aplica).
 */
@Service
public class ConsultarMisReservasCasoUso implements ConsultarMisReservas {

	private static final NombresCancha SIN_NOMBRES = new NombresCancha(null, null);

	private final RepositorioReservas repositorioReservas;
	private final ConsultaCanchas consultaCanchas;
	private final ParametrosReserva parametros;
	private final Clock reloj;

	public ConsultarMisReservasCasoUso(RepositorioReservas repositorioReservas, ConsultaCanchas consultaCanchas,
			ParametrosReserva parametros, Clock reloj) {
		this.repositorioReservas = repositorioReservas;
		this.consultaCanchas = consultaCanchas;
		this.parametros = parametros;
		this.reloj = reloj;
	}

	@Override
	public MisReservas ejecutar(Solicitante solicitante) {
		// P2: solo el jugador tiene «Mis reservas»; como en HU13, sin un código propio va ERR-GEN-06
		if (solicitante == null || !solicitante.tieneRol(Rol.JUGADOR)) {
			throw new ReglaNegocioException("ERR-GEN-06");
		}

		// CA5: el id sale del token, nunca de la petición
		List<Reserva> reservas = repositorioReservas.deJugador(solicitante.usuarioId());
		if (reservas.isEmpty()) {
			return new MisReservas(List.of(), List.of());
		}

		// CA1: los nombres de todas las canchas en una sola llamada
		Set<UUID> canchaIds = reservas.stream().map(Reserva::getCanchaId).collect(Collectors.toSet());
		Map<UUID, NombresCancha> nombres = consultaCanchas.nombres(canchaIds);
		LocalDateTime ahora = LocalDateTime.now(reloj);

		// P1 y restricción de diseño de la HU: vigentes de la más próxima a la más lejana, pasadas al revés
		List<ReservaDelJugador> vigentes = reservas.stream()
				.filter(r -> r.esVigente(ahora))
				.sorted(Comparator.comparing(Reserva::inicio))
				.map(r -> completar(r, nombres, ahora))
				.toList();
		List<ReservaDelJugador> pasadas = reservas.stream()
				.filter(r -> !r.esVigente(ahora))
				.sorted(Comparator.comparing(Reserva::inicio).reversed())
				.map(r -> completar(r, nombres, ahora))
				.toList();
		return new MisReservas(vigentes, pasadas);
	}

	// CA3: cancelable con la misma regla que usará la cancelación de HU24 (política 16)
	private ReservaDelJugador completar(Reserva reserva, Map<UUID, NombresCancha> nombres, LocalDateTime ahora) {
		NombresCancha nombre = nombres.getOrDefault(reserva.getCanchaId(), SIN_NOMBRES);
		return new ReservaDelJugador(reserva, nombre.cancha(), nombre.escenario(),
				reserva.sePuedeCancelarPorJugador(ahora, parametros));
	}

}
