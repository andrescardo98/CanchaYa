package co.edu.uco.canchaya.identidad.infrastructure.primaryadapters.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.canchaya.identidad.application.primaryports.ConsultarCorreoDisponible;
import co.edu.uco.canchaya.identidad.application.primaryports.RegistrarUsuario;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.transversal.infrastructure.primaryadapters.controller.CatalogoMensajes;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
class UsuarioController {

	private final RegistrarUsuario registrarUsuario;
	private final ConsultarCorreoDisponible consultarCorreoDisponible;
	private final CatalogoMensajes catalogo;

	UsuarioController(RegistrarUsuario registrarUsuario, ConsultarCorreoDisponible consultarCorreoDisponible,
			CatalogoMensajes catalogo) {
		this.registrarUsuario = registrarUsuario;
		this.consultarCorreoDisponible = consultarCorreoDisponible;
		this.catalogo = catalogo;
	}

	// HU1
	@PostMapping
	ResponseEntity<RespuestaRegistro> registrar(@Valid @RequestBody SolicitudRegistro solicitud) {
		Usuario usuario = registrarUsuario.ejecutar(solicitud.aDatosRegistro());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(RespuestaRegistro.de(usuario, "EXI-USU-01", catalogo.texto("EXI-USU-01")));
	}

	// HU1, CA8
	@GetMapping("/correo-disponible")
	CorreoDisponible.Respuesta correoDisponible(@Valid CorreoDisponible.Solicitud solicitud) {
		if (consultarCorreoDisponible.ejecutar(solicitud.correoElectronico())) {
			return new CorreoDisponible.Respuesta(true, null, null);
		}
		return new CorreoDisponible.Respuesta(false, "ERR-USU-01", catalogo.texto("ERR-USU-01"));
	}

}
