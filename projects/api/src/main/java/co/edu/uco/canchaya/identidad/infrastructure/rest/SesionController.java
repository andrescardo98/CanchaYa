package co.edu.uco.canchaya.identidad.infrastructure.rest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.canchaya.identidad.application.port.in.IniciarSesion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sesiones")
class SesionController {

	private final IniciarSesion iniciarSesion;

	SesionController(IniciarSesion iniciarSesion) {
		this.iniciarSesion = iniciarSesion;
	}

	// HU2
	@PostMapping
	RespuestaInicioSesion iniciar(@Valid @RequestBody SolicitudInicioSesion solicitud, HttpServletRequest peticion) {
		// El origen es la IP del cliente. Detrás del proxy de Render la toma de X-Forwarded-For
		// (server.forward-headers-strategy) para el límite de intentos por origen (D28).
		return RespuestaInicioSesion.de(iniciarSesion.ejecutar(solicitud.aCredenciales(), peticion.getRemoteAddr()));
	}

}
