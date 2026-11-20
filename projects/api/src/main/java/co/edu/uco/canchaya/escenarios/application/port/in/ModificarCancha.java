package co.edu.uco.canchaya.escenarios.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU9: cambiar el nombre, el tipo o el precio por hora de una cancha. */
public interface ModificarCancha {

	/**
	 * @return la cancha con sus datos actualizados
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-CAN-02, 04, 05, 07 u 08
	 */
	Cancha ejecutar(DatosModificacionCancha datos, Solicitante solicitante);

	record DatosModificacionCancha(UUID canchaId, UUID tipoCanchaId, String nombre, BigDecimal precioHora) {
	}

}
