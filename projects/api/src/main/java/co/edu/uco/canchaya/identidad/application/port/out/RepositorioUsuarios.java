package co.edu.uco.canchaya.identidad.application.port.out;

import java.util.Optional;
import java.util.UUID;

import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;

public interface RepositorioUsuarios {

	/** El correo debe llegar normalizado (en minúscula). */
	boolean existePorCorreo(String correoElectronico);

	boolean existePorDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

	boolean existePorId(UUID id);

	/** El correo debe llegar normalizado (en minúscula). */
	Optional<Usuario> buscarPorCorreo(String correoElectronico);

	/**
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-USU-01 o
	 *         ERR-USU-02 si otro registro con el mismo correo o documento se guardó primero
	 */
	void guardar(Usuario usuario);

	Optional<Usuario> buscarPorId(UUID id);

	/** Política 10. El correo debe llegar normalizado (en minúscula). */
	boolean existeOtroConCorreo(String correoElectronico, UUID idExcluido);

	/**
	 * Guarda los cambios del perfil: nombre, correo, celular y contraseña. Nada más.
	 *
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-USU-10 si otro
	 *         usuario guardó el mismo correo primero
	 */
	void actualizar(Usuario usuario);

}
