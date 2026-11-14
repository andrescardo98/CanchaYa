package co.edu.uco.canchaya.escenarios.application.port.in;

import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU13: activar o inactivar un escenario. */
public interface CambiarEstadoEscenario {

	/**
	 * @return el escenario con su nuevo estado
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-ESC-06, 08, 09 o 14
	 */
	Escenario ejecutar(UUID escenarioId, EstadoEscenario nuevoEstado, Solicitante solicitante);

}
