package co.edu.uco.canchaya.escenarios.application.port.in;

import java.util.UUID;

import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU12: eliminar un horario de atención. */
public interface EliminarHorario {

	/**
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-HOR-02, 06 o 15
	 */
	void ejecutar(UUID horarioId, Solicitante solicitante);

}
