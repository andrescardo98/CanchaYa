package co.edu.uco.canchaya.escenarios.application.port.in;

import java.time.LocalTime;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU11: cambiar el día o las horas de un horario de atención. */
public interface ModificarHorario {

	/**
	 * @return el horario con sus datos actualizados
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-HOR-02, 03, 06, 07 o 13
	 */
	HorarioRecurrente ejecutar(UUID horarioId, DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin,
			Solicitante solicitante);

}
