package co.edu.uco.canchaya.identidad.application.secondaryports;

import java.util.UUID;

import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;

public interface RepositorioUsuarios {

	/** El correo debe llegar normalizado (en minúscula). */
	boolean existePorCorreo(String correoElectronico);

	boolean existePorDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

	boolean existePorId(UUID id);

	/**
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-USU-01 o
	 *         ERR-USU-02 si otro registro con el mismo correo o documento se guardó primero
	 */
	void guardar(Usuario usuario);

}
