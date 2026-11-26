package co.edu.uco.canchaya.reservas.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios.BloqueoAgenda;
import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.domain.EstadoFranja;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.reservas.domain.VistaAgenda;

/** HU29: la agenda de un escenario por día o por semana, para su propietario o un encargado. */
public interface ConsultarAgenda {

	/**
	 * @param fecha si es vacía (null), hoy en America/Bogota (D27, ESC-USA-04)
	 * @param vista si es vacía (null), DIA
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-GEN-06 si el escenario
	 *         no existe o el solicitante no es su propietario ni un encargado asignado (CA6, P7)
	 */
	Agenda ejecutar(UUID escenarioId, LocalDate fecha, VistaAgenda vista, Solicitante solicitante);

	/** propietario es falso si quien consulta es un encargado asignado. */
	record Agenda(UUID escenarioId, String escenario, boolean propietario, LocalDate desde, LocalDate hasta,
			List<CanchaDeAgenda> canchas) {
	}

	/** CA1: todas las canchas del escenario; estado es el nombre de EstadoCancha. dias va de desde a hasta. */
	record CanchaDeAgenda(UUID id, String nombre, String estado, List<DiaDeAgenda> dias) {
	}

	/** Las franjas, las reservas (también las canceladas, P4) y los bloqueos de una cancha en un día. */
	record DiaDeAgenda(LocalDate fecha, List<FranjaDeAgenda> franjas, List<ReservaDeAgenda> reservas,
			List<BloqueoAgenda> bloqueos) {
	}

	/** reservaId solo si es RESERVADA y bloqueoId solo si es BLOQUEADA. */
	record FranjaDeAgenda(Franja franja, EstadoFranja estado, UUID reservaId, UUID bloqueoId) {
	}

	/**
	 * CA3: los datos del jugador (dato sensible, AG-15) y las opciones de quien consulta, calculadas
	 * con la hora del servidor.
	 */
	record ReservaDeAgenda(Reserva reserva, String jugador, String celularJugador,
			boolean puedeCancelarPorFuerzaMayor, boolean puedeMarcarInasistencia) {
	}

}
