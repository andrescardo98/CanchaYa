package co.edu.uco.canchaya.escenarios.application.port.in;

import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU7, CA1: la lista de horarios que ve el propietario al definir uno nuevo. */
public interface ListarHorarios {

	/**
	 * @return los horarios del escenario, de lunes a domingo y por hora de inicio
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-HOR-01 o 02
	 */
	List<HorarioRecurrente> ejecutar(UUID escenarioId, Solicitante solicitante);

}
