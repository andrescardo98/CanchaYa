package co.edu.uco.canchaya.identidad.domain;

import java.util.UUID;

/**
 * Quién ejecuta una operación: el identificador y el rol del usuario autenticado, tomados del
 * token de acceso. Los demás módulos lo usan para revisar permisos sin conocer la clase Usuario.
 */
public record Solicitante(UUID usuarioId, Rol rol) {

	public boolean tieneRol(Rol rol) {
		return this.rol == rol;
	}

}
