package co.edu.uco.canchaya.escenarios.application.port.in;

import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU29, CA4 y CA5: los escenarios de los que el usuario puede consultar la agenda. */
public interface ListarEscenariosAgenda {

	/**
	 * Los del propietario y los que tiene asignados como encargado, por nombre, en cualquier estado
	 * (P8). Vacía si no tiene ninguno.
	 *
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-GEN-06 si no tiene rol
	 *         ADMINISTRADOR_CANCHA (P7)
	 */
	List<EscenarioAgenda> ejecutar(Solicitante solicitante);

	/** propietario es falso si el usuario es encargado asignado del escenario. */
	record EscenarioAgenda(UUID id, String nombre, EstadoEscenario estado, boolean propietario) {
	}

}
