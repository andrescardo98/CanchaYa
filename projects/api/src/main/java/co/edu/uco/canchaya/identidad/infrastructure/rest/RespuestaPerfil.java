package co.edu.uco.canchaya.identidad.infrastructure.rest;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;

/**
 * HU3: datos del formulario «Mi perfil». El documento y el rol van para mostrarlos como solo
 * lectura. Nunca lleva la contraseña ni su hash (AG-13) ni la fecha de registro (AG-14). Solo la
 * recibe el dueño del perfil, porque el documento y el celular son datos sensibles (AG-15).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record RespuestaPerfil(String codigo, String mensaje, Perfil perfil) {

	record Perfil(UUID id, String nombre, String correoElectronico, String numeroCelular,
			TipoDocumento tipoDocumento, String numeroDocumento, Rol rol) {
	}

	/** GET: solo los datos. */
	static RespuestaPerfil de(Usuario usuario) {
		return new RespuestaPerfil(null, null, perfilDe(usuario));
	}

	/** PUT: los datos actualizados y el mensaje de éxito (CA1). */
	static RespuestaPerfil de(Usuario usuario, String codigo, String mensaje) {
		return new RespuestaPerfil(codigo, mensaje, perfilDe(usuario));
	}

	private static Perfil perfilDe(Usuario usuario) {
		return new Perfil(usuario.getId(), usuario.getNombre(), usuario.getCorreoElectronico(),
				usuario.getNumeroCelular(), usuario.getTipoDocumento(), usuario.getNumeroDocumento(), usuario.getRol());
	}

}
