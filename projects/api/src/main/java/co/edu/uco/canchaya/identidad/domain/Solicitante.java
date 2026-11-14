package co.edu.uco.canchaya.identidad.domain;

import java.util.UUID;

/**
 * Quién ejecuta una operación, tomado del token JWT. Es lo que Identidad ofrece a los demás
 * módulos para revisar permisos sin que conozcan la clase Usuario.
 */
public record Solicitante(UUID usuarioId, Rol rol) {

	public boolean tieneRol(Rol rol) {
		return this.rol == rol;
	}

}
