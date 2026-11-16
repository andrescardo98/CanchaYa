package co.edu.uco.canchaya.escenarios.application.port.in;

import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

// HU6, CA1: la lista de canchas del escenario que se actualiza después de registrar una
public interface ListarCanchasEscenario {

	/**
	 * @return las canchas del escenario ordenadas por nombre
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-CAN-01 o ERR-CAN-02
	 */
	List<Cancha> ejecutar(UUID escenarioId, Solicitante solicitante);

}
