package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import co.edu.uco.canchaya.escenarios.application.port.in.EliminarHorario;
import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaReservasFuturas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioHorarios;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * Todo va dentro de {@code transaccion.execute}, como inactivar un escenario (HU13): los turnos de
 * las canchas se sueltan cuando el borrado ya quedó guardado. Así una reserva que llegue al mismo
 * tiempo (HU21) espera, recalcula las franjas y ya no encuentra el horario (D29 punto 2, D36).
 */
@Service
public class EliminarHorarioCasoUso implements EliminarHorario {

	private final RepositorioEscenarios escenarios;
	private final RepositorioHorarios horarios;
	private final RepositorioCanchas canchas;
	private final ConsultaReservasFuturas consultaReservasFuturas;
	private final TransactionOperations transaccion;

	public EliminarHorarioCasoUso(RepositorioEscenarios escenarios, RepositorioHorarios horarios,
			RepositorioCanchas canchas, ConsultaReservasFuturas consultaReservasFuturas,
			TransactionOperations transaccion) {
		this.escenarios = escenarios;
		this.horarios = horarios;
		this.canchas = canchas;
		this.consultaReservasFuturas = consultaReservasFuturas;
		this.transaccion = transaccion;
	}

	@Override
	public void ejecutar(UUID horarioId, Solicitante solicitante) {
		transaccion.executeWithoutResult(estado -> eliminar(horarioId, solicitante));
	}

	private void eliminar(UUID horarioId, Solicitante solicitante) {
		// Política 6
		HorarioRecurrente horario = horarios.buscarPorId(horarioId)
				.orElseThrow(() -> new ReglaNegocioException("ERR-HOR-06"));
		// El horario no existe sin su escenario (llave foránea), así que no encontrarlo es un error de datos
		Escenario escenario = escenarios.buscarPorId(horario.getEscenarioId())
				.orElseThrow(() -> new IllegalStateException("El horario " + horarioId + " no tiene escenario"));
		// Política 2 antes de los turnos (P5): quien no es el dueño no detiene las reservas de nadie
		HorarioRecurrente.validarPropietario(escenario, solicitante);

		// Políticas 2 y 15
		horario.validarEliminacion(escenario, solicitante, tieneReservasFuturas(horario));
		horarios.eliminar(horarioId);
	}

	/**
	 * Toma el turno de cada cancha antes de preguntar. Siempre en el mismo orden, por id, para que
	 * dos operaciones que tomen varios turnos no se queden esperando la una a la otra (P2 de HU13).
	 */
	private boolean tieneReservasFuturas(HorarioRecurrente horario) {
		List<UUID> ids = escenarios.idsCanchas(horario.getEscenarioId()).stream().sorted().toList();
		if (ids.isEmpty()) {
			return false;
		}
		ids.forEach(canchas::tomarTurno);
		return consultaReservasFuturas.existenConfirmadasFuturasEnHorario(ids, horario.getDiaSemana().aDayOfWeek(),
				horario.getHoraInicio(), horario.getHoraFin());
	}

}
