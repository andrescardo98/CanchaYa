package co.edu.uco.canchaya.escenarios.application.port.in;

import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU10: poner una cancha como DISPONIBLE o INACTIVA. */
public interface CambiarEstadoCancha {

	/**
	 * @return la cancha con su nuevo estado
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-CAN-02, 07, 10 o 12
	 */
	Cancha ejecutar(UUID canchaId, EstadoCancha nuevoEstado, Solicitante solicitante);

}
