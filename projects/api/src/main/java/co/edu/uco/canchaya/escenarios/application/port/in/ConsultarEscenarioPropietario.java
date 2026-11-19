package co.edu.uco.canchaya.escenarios.application.port.in;

import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/**
 * HU8: los datos actuales del escenario para llenar el formulario de modificación (P2 de la
 * especificación). A diferencia del perfil público (HU19), sirve también un escenario INACTIVO,
 * pero solo a su propietario.
 */
public interface ConsultarEscenarioPropietario {

	/**
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-ESC-06 o ERR-ESC-07
	 */
	Escenario ejecutar(UUID escenarioId, Solicitante solicitante);

}
