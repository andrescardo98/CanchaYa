package co.edu.uco.canchaya.escenarios.application.port.in;

import java.time.LocalTime;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU7: definir un horario de atención del escenario. */
public interface DefinirHorario {

	/**
	 * @return el horario guardado, con su identificador
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-HOR-01, 02, 03, 04 o 13
	 */
	HorarioRecurrente ejecutar(UUID escenarioId, DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin,
			Solicitante solicitante);

}
