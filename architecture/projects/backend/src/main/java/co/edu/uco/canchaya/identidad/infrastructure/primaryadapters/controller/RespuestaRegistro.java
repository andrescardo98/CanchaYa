package co.edu.uco.canchaya.identidad.infrastructure.primaryadapters.controller;

import java.time.OffsetDateTime;
import java.util.UUID;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Usuario;

record RespuestaRegistro(String codigo, String mensaje, UsuarioRegistrado usuario) {

	// Sin contraseña, hash, documento ni celular (AG-13, AG-15)
	record UsuarioRegistrado(UUID id, String nombre, String correoElectronico, Rol rol,
			OffsetDateTime fechaRegistro) {
	}

	static RespuestaRegistro de(Usuario usuario, String codigo, String mensaje) {
		return new RespuestaRegistro(codigo, mensaje, new UsuarioRegistrado(usuario.getId(), usuario.getNombre(),
				usuario.getCorreoElectronico(), usuario.getRol(), usuario.getFechaRegistro()));
	}

}
