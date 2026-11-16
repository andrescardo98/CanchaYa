package co.edu.uco.canchaya.escenarios.application.port.in;

import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU13, CA1: la lista de escenarios que revisa el administrador del sistema. */
public interface ListarEscenariosAdministracion {

	/**
	 * @param estado si es vacío (null), trae todos
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-GEN-06
	 */
	List<EscenarioAdministracion> ejecutar(EstadoEscenario estado, Solicitante solicitante);

	/** Lo que muestra la lista: nombre, ciudad, administrador propietario y estado. */
	record EscenarioAdministracion(UUID id, String nombre, String ciudad, String propietario,
			EstadoEscenario estado) {
	}

}
