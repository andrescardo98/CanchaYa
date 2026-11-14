package co.edu.uco.canchaya.identidad.infrastructure.rest;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.canchaya.identidad.application.port.in.ActualizarPerfil;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultarCorreoDisponible;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultarPerfil;
import co.edu.uco.canchaya.identidad.application.port.in.RegistrarUsuario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
class UsuarioController {

	private final RegistrarUsuario registrarUsuario;
	private final ConsultarCorreoDisponible consultarCorreoDisponible;
	private final ConsultarPerfil consultarPerfil;
	private final ActualizarPerfil actualizarPerfil;
	private final CatalogoMensajes catalogo;

	UsuarioController(RegistrarUsuario registrarUsuario, ConsultarCorreoDisponible consultarCorreoDisponible,
			ConsultarPerfil consultarPerfil, ActualizarPerfil actualizarPerfil, CatalogoMensajes catalogo) {
		this.registrarUsuario = registrarUsuario;
		this.consultarCorreoDisponible = consultarCorreoDisponible;
		this.consultarPerfil = consultarPerfil;
		this.actualizarPerfil = actualizarPerfil;
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

	// HU3: datos que llenan el formulario «Mi perfil»
	@GetMapping("/{id}")
	RespuestaPerfil consultarPerfil(@PathVariable String id, @AuthenticationPrincipal Jwt token) {
		return RespuestaPerfil.de(consultarPerfil.ejecutar(solicitante(token), idDe(id)));
	}

	// HU3
	@PutMapping("/{id}")
	RespuestaPerfil actualizarPerfil(@PathVariable String id, @Valid @RequestBody SolicitudActualizarPerfil solicitud,
			@AuthenticationPrincipal Jwt token) {
		Usuario usuario = actualizarPerfil.ejecutar(solicitante(token), solicitud.aDatosPerfil(idDe(id)));
		return RespuestaPerfil.de(usuario, "EXI-USU-02", catalogo.texto("EXI-USU-02"));
	}

	/** Quién hace la petición sale del token, nunca de un dato que envíe el cliente (restricción de HU3). */
	private static Solicitante solicitante(Jwt token) {
		return new Solicitante(UUID.fromString(token.getSubject()), Rol.valueOf(token.getClaimAsString("rol")));
	}

	// Un id que ni siquiera es un UUID no puede ser de ningún usuario (política 8)
	private static UUID idDe(String id) {
		try {
			return UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			throw new ReglaNegocioException("ERR-USU-08");
		}
	}

}
