package co.edu.uco.canchaya.identidad.application.port.in;

import java.util.UUID;

import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.domain.Usuario;

/** HU3: datos guardados que llenan el formulario «Mi perfil». */
public interface ConsultarPerfil {

	/**
	 * @param solicitante usuario autenticado, tomado del token de acceso
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-USU-08 o ERR-USU-09
	 */
	Usuario ejecutar(Solicitante solicitante, UUID id);

}
