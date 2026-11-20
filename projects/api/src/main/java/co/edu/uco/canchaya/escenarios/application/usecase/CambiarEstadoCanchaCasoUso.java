package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uco.canchaya.escenarios.application.port.in.CambiarEstadoCancha;
import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaReservasFuturas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * Una sola transacción: el turno de la cancha (P2 de la especificación) dura hasta que termina,
 * así que ninguna reserva entra entre la consulta de reservas futuras y el cambio de estado. No
 * envía correo (D19), por eso no hay nada que dejar por fuera de la transacción.
 */
@Service
public class CambiarEstadoCanchaCasoUso implements CambiarEstadoCancha {

	private final RepositorioEscenarios repositorioEscenarios;
	private final RepositorioCanchas repositorioCanchas;
	private final ConsultaReservasFuturas consultaReservasFuturas;

	public CambiarEstadoCanchaCasoUso(RepositorioEscenarios repositorioEscenarios,
			RepositorioCanchas repositorioCanchas, ConsultaReservasFuturas consultaReservasFuturas) {
		this.repositorioEscenarios = repositorioEscenarios;
		this.repositorioCanchas = repositorioCanchas;
		this.consultaReservasFuturas = consultaReservasFuturas;
	}

	@Override
	@Transactional
	public Cancha ejecutar(UUID canchaId, EstadoCancha nuevoEstado, Solicitante solicitante) {
		// Política 7, antes que la 2 como en HU9
		Cancha cancha = repositorioCanchas.buscarPorId(canchaId)
				.orElseThrow(() -> new ReglaNegocioException("ERR-CAN-07"));
		// La cancha no existe sin su escenario (llave foránea), así que no encontrarlo es un error de datos
		Escenario escenario = repositorioEscenarios.buscarPorId(cancha.getEscenarioId())
				.orElseThrow(() -> new IllegalStateException("La cancha " + cancha.getId() + " no tiene escenario"));

		// Política 2 antes del turno: quien no es el propietario no detiene las reservas de la cancha.
		// cambiarEstado la vuelve a revisar, así el dominio no depende de este orden
		Cancha.validarPropietario(escenario, solicitante);

		// Política 12: solo al inactivar se toma el turno (P2, D29 punto 2, D36) y se pregunta a Reservas (D08)
		boolean reservasFuturas = false;
		if (nuevoEstado == EstadoCancha.INACTIVA) {
			repositorioCanchas.tomarTurno(canchaId);
			reservasFuturas = consultaReservasFuturas.existenConfirmadasFuturas(List.of(canchaId));
		}

		// Políticas 2, 10 y 12
		cancha.cambiarEstado(escenario, nuevoEstado, solicitante, reservasFuturas);
		repositorioCanchas.actualizarEstado(cancha);
		return cancha;
	}

}
