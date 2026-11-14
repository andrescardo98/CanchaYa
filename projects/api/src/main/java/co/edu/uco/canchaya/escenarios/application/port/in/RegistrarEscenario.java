package co.edu.uco.canchaya.escenarios.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU5: registrar un escenario. */
public interface RegistrarEscenario {

	/**
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-ESC-01, 02, 03 o 13
	 */
	Escenario ejecutar(DatosRegistroEscenario datos, Solicitante solicitante);

	record DatosRegistroEscenario(String nombre, String descripcion, String direccion, UUID ciudadId,
			BigDecimal latitud, BigDecimal longitud, String telefonoContacto, String correoContacto,
			List<FotoAdjunta> fotos) {
	}

	/** Archivo tal como llega del formulario, antes de subirlo. */
	record FotoAdjunta(String nombreArchivo, byte[] contenido) {
	}

}
