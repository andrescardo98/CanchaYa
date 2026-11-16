package co.edu.uco.canchaya.escenarios.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU6: agregar una cancha a un escenario. */
public interface RegistrarCancha {

	/**
	 * @return la cancha guardada, con su identificador y en estado DISPONIBLE
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-CAN-01 a ERR-CAN-05
	 */
	Cancha ejecutar(DatosRegistroCancha datos, Solicitante solicitante);

	record DatosRegistroCancha(UUID escenarioId, UUID tipoCanchaId, String nombre, BigDecimal precioHora) {
	}

}
