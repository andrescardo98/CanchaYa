package co.edu.uco.canchaya.escenarios.application.port.in;

import java.util.UUID;

import co.edu.uco.canchaya.identidad.domain.Solicitante;

// HU6, CA8: el formulario pregunta si el nombre está libre al salir del campo
public interface ConsultarNombreCanchaDisponible {

	/**
	 * @return falso si el escenario ya tiene una cancha con ese nombre, sin importar mayúsculas
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-CAN-01 o ERR-CAN-02
	 */
	boolean ejecutar(UUID escenarioId, String nombre, Solicitante solicitante);

}
