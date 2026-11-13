package co.edu.uco.canchaya.identidad.infrastructure.rest;

import java.time.Instant;

import co.edu.uco.canchaya.identidad.application.port.in.IniciarSesion.Sesion;
import co.edu.uco.canchaya.identidad.domain.Rol;

/** El frontend guarda el token y, según el rol, lleva al usuario a su página de inicio (CA1). */
record RespuestaInicioSesion(String token, Rol rol, Instant venceEn) {

	static RespuestaInicioSesion de(Sesion sesion) {
		return new RespuestaInicioSesion(sesion.token(), sesion.rol(), sesion.venceEn());
	}

}
