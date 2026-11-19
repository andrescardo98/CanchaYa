package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import co.edu.uco.canchaya.escenarios.application.port.in.CambiarEstadoEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarEscenarioPropietario;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarPerfilEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarPerfilEscenario.PerfilEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarEscenariosAdministracion;
import co.edu.uco.canchaya.escenarios.application.port.in.ModificarEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario.FotoAdjunta;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/escenarios")
class EscenarioController {

	private final RegistrarEscenario registrarEscenario;
	private final ListarEscenariosAdministracion listarEscenarios;
	private final CambiarEstadoEscenario cambiarEstado;
	private final ConsultarPerfilEscenario consultarPerfil;
	private final ConsultarEscenarioPropietario consultarEscenario;
	private final ModificarEscenario modificarEscenario;
	private final CatalogoMensajes catalogo;

	EscenarioController(RegistrarEscenario registrarEscenario, ListarEscenariosAdministracion listarEscenarios,
			CambiarEstadoEscenario cambiarEstado, ConsultarPerfilEscenario consultarPerfil,
			ConsultarEscenarioPropietario consultarEscenario, ModificarEscenario modificarEscenario,
			CatalogoMensajes catalogo) {
		this.registrarEscenario = registrarEscenario;
		this.listarEscenarios = listarEscenarios;
		this.cambiarEstado = cambiarEstado;
		this.consultarPerfil = consultarPerfil;
		this.consultarEscenario = consultarEscenario;
		this.modificarEscenario = modificarEscenario;
		this.catalogo = catalogo;
	}

	// HU5. Multipart: la parte «datos» en JSON y las fotos como archivos en la parte «fotos»
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<RespuestaRegistroEscenario> registrar(@Valid @RequestPart("datos") SolicitudRegistroEscenario datos,
			@RequestPart(name = "fotos", required = false) List<MultipartFile> fotos, Solicitante solicitante) {
		Escenario escenario = registrarEscenario.ejecutar(datos.aDatos(adjuntas(fotos)), solicitante);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(RespuestaRegistroEscenario.de(escenario, "EXI-ESC-01", catalogo.texto("EXI-ESC-01")));
	}

	// HU13, CA1: administración de escenarios, con filtro opcional por estado
	@GetMapping
	List<RespuestaEscenarioAdministracion> listar(@RequestParam(required = false) EstadoEscenario estado,
			Solicitante solicitante) {
		return listarEscenarios.ejecutar(estado, solicitante).stream().map(RespuestaEscenarioAdministracion::de)
				.toList();
	}

	// HU13, CA2 a CA8: activar o inactivar
	@PatchMapping("/{id}/estado")
	RespuestaCambioEstado cambiarEstado(@PathVariable UUID id, @Valid @RequestBody SolicitudCambioEstado solicitud,
			Solicitante solicitante) {
		Escenario escenario = cambiarEstado.ejecutar(id, solicitud.estado(), solicitante);
		String mensaje = catalogo.texto("EXI-ESC-03", List.of(escenario.getNombre(),
				new TextoCatalogo("estado." + escenario.getEstado().name())));
		return new RespuestaCambioEstado("EXI-ESC-03", mensaje, escenario.getId(), escenario.getEstado());
	}

	// HU19: perfil público del escenario, sin sesión (AG-11). ERR-ESC-06 sale como 404 (ManejadorErrores)
	@GetMapping("/{id}/perfil")
	RespuestaPerfilEscenario perfil(@PathVariable UUID id) {
		PerfilEscenario perfil = consultarPerfil.ejecutar(id);
		// CA2 (AG-09): sin calificaciones no hay promedio y se informa con INF-RSE-01
		if (perfil.calificacionPromedio() == null) {
			return RespuestaPerfilEscenario.de(perfil, "INF-RSE-01", catalogo.texto("INF-RSE-01"));
		}
		return RespuestaPerfilEscenario.de(perfil, null, null);
	}

	// HU8, CA1: los datos actuales para el formulario, solo al propietario (P2), aunque esté INACTIVO
	@GetMapping("/{id}")
	RespuestaEscenario consultar(@PathVariable UUID id, Solicitante solicitante) {
		return RespuestaEscenario.de(consultarEscenario.ejecutar(id, solicitante));
	}

	// HU8. Multipart como el registro: «datos» con fotosConservadas y las fotos nuevas en «fotos» (P1)
	@PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	RespuestaModificarEscenario modificar(@PathVariable UUID id,
			@Valid @RequestPart("datos") SolicitudModificarEscenario datos,
			@RequestPart(name = "fotos", required = false) List<MultipartFile> fotos, Solicitante solicitante) {
		Escenario escenario = modificarEscenario.ejecutar(id, datos.aDatos(adjuntas(fotos)), solicitante);
		return RespuestaModificarEscenario.de(escenario, "EXI-ESC-02", catalogo.texto("EXI-ESC-02"));
	}

	private static List<FotoAdjunta> adjuntas(List<MultipartFile> fotos) {
		return fotos == null ? List.of() : fotos.stream().map(EscenarioController::aFoto).toList();
	}

	private static FotoAdjunta aFoto(MultipartFile archivo) {
		try {
			return new FotoAdjunta(archivo.getOriginalFilename(), archivo.getBytes());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
