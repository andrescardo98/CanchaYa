package co.edu.uco.canchaya.escenarios.application.usecase;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ModificarHorario;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioHorarios;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * El horario modificado solo vive en memoria hasta {@code actualizar}, así que un error no deja
 * nada guardado. La política 14 (RN-17, D09) no tiene código: no se consulta ni se cambia ninguna
 * reserva, por eso las ya hechas se conservan aunque queden por fuera del nuevo horario.
 */
@Service
public class ModificarHorarioCasoUso implements ModificarHorario {

	private final RepositorioEscenarios escenarios;
	private final RepositorioHorarios horarios;

	public ModificarHorarioCasoUso(RepositorioEscenarios escenarios, RepositorioHorarios horarios) {
		this.escenarios = escenarios;
		this.horarios = horarios;
	}

	@Override
	public HorarioRecurrente ejecutar(UUID horarioId, DiaSemana diaSemana, LocalTime horaInicio,
			LocalTime horaFin, Solicitante solicitante) {
		// Política 6
		HorarioRecurrente horario = horarios.buscarPorId(horarioId)
				.orElseThrow(() -> new ReglaNegocioException("ERR-HOR-06"));
		// El horario no existe sin su escenario (llave foránea), así que no encontrarlo es un error de datos
		Escenario escenario = escenarios.buscarPorId(horario.getEscenarioId())
				.orElseThrow(() -> new IllegalStateException("El horario " + horarioId + " no tiene escenario"));

		// Política 7: con los horarios del día nuevo; el dominio no cuenta al propio horario
		List<HorarioRecurrente> delDiaNuevo = horarios.buscarPorEscenarioYDia(escenario.getId(), diaSemana);
		// Políticas 2, 13, 3 y 7
		horario.modificar(escenario, diaSemana, horaInicio, horaFin, delDiaNuevo, solicitante);

		horarios.actualizar(horario);
		return horario;
	}

}
