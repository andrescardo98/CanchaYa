package co.edu.uco.canchaya.escenarios.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario.FotoAdjunta;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU8: modificar los datos de un escenario. */
public interface ModificarEscenario {

	/**
	 * @return el escenario con sus datos actualizados
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-ESC-02, 03, 06, 07 o 13, o
	 *         ERR-GEN-07 si alguna de las fotos conservadas no es del escenario
	 */
	Escenario ejecutar(UUID escenarioId, DatosModificacionEscenario datos, Solicitante solicitante);

	/**
	 * @param fotosConservadas direcciones (URL) de las fotos actuales que se quedan, en el orden en
	 *        que se muestran (P1 de la especificación)
	 * @param fotosNuevas archivos que se agregan; van después de las conservadas
	 */
	record DatosModificacionEscenario(String nombre, String descripcion, String direccion, UUID ciudadId,
			BigDecimal latitud, BigDecimal longitud, String telefonoContacto, String correoContacto,
			List<String> fotosConservadas, List<FotoAdjunta> fotosNuevas) {
	}

}
