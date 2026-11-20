package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.canchaya.escenarios.application.port.in.CambiarEstadoCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarNombreCanchaDisponible;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarCanchasEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarTiposCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.ModificarCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarCancha;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;
import jakarta.validation.Valid;

@RestController
class CanchaController {

	private final RegistrarCancha registrarCancha;
	private final ModificarCancha modificarCancha;
	private final CambiarEstadoCancha cambiarEstado;
	private final ConsultarNombreCanchaDisponible consultarNombreDisponible;
	private final ListarCanchasEscenario listarCanchas;
	private final ListarTiposCancha listarTipos;
	private final CatalogoMensajes catalogo;

	CanchaController(RegistrarCancha registrarCancha, ModificarCancha modificarCancha,
			CambiarEstadoCancha cambiarEstado, ConsultarNombreCanchaDisponible consultarNombreDisponible, ListarCanchasEscenario listarCanchas,
			ListarTiposCancha listarTipos, CatalogoMensajes catalogo) {
		this.registrarCancha = registrarCancha;
		this.modificarCancha = modificarCancha;
		this.cambiarEstado = cambiarEstado;
		this.consultarNombreDisponible = consultarNombreDisponible;
		this.listarCanchas = listarCanchas;
		this.listarTipos = listarTipos;
		this.catalogo = catalogo;
	}

	// HU6, CA1 a CA7
	@PostMapping("/api/escenarios/{escenarioId}/canchas")
	ResponseEntity<RespuestaRegistroCancha> registrar(@PathVariable UUID escenarioId,
			@Valid @RequestBody SolicitudRegistroCancha solicitud, Solicitante solicitante) {
		Cancha cancha = registrarCancha.ejecutar(solicitud.aDatos(escenarioId), solicitante);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new RespuestaRegistroCancha("EXI-CAN-01", catalogo.texto("EXI-CAN-01"), cancha.getId()));
	}

	// HU9, CA1 a CA7. El escenario sale de la cancha (P1 de la especificación)
	@PutMapping("/api/canchas/{id}")
	RespuestaModificarCancha modificar(@PathVariable UUID id, @Valid @RequestBody SolicitudModificarCancha solicitud,
			Solicitante solicitante) {
		Cancha cancha = modificarCancha.ejecutar(solicitud.aDatos(id), solicitante);
		return RespuestaModificarCancha.de(cancha, "EXI-CAN-02", catalogo.texto("EXI-CAN-02"));
	}

	// HU10, CA1 a CA6: poner la cancha DISPONIBLE o INACTIVA (P1 de la especificación)
	@PatchMapping("/api/canchas/{id}/estado")
	RespuestaCambioEstadoCancha cambiarEstado(@PathVariable UUID id,
			@Valid @RequestBody SolicitudCambioEstadoCancha solicitud, Solicitante solicitante) {
		Cancha cancha = cambiarEstado.ejecutar(id, solicitud.estado(), solicitante);
		String mensaje = catalogo.texto("EXI-CAN-03", List.of(cancha.getNombre(),
				new TextoCatalogo("estado." + cancha.getEstado().name())));
		return new RespuestaCambioEstadoCancha("EXI-CAN-03", mensaje, cancha.getId(), cancha.getEstado());
	}

	// HU6, CA1: la lista que se actualiza después de registrar
	@GetMapping("/api/escenarios/{escenarioId}/canchas")
	List<RespuestaCancha> listar(@PathVariable UUID escenarioId, Solicitante solicitante) {
		return listarCanchas.ejecutar(escenarioId, solicitante).stream().map(RespuestaCancha::de).toList();
	}

	// HU6, CA8
	@GetMapping("/api/escenarios/{escenarioId}/canchas/nombre-disponible")
	NombreCanchaDisponible.Respuesta nombreDisponible(@PathVariable UUID escenarioId,
			@Valid NombreCanchaDisponible.Solicitud solicitud, Solicitante solicitante) {
		if (consultarNombreDisponible.ejecutar(escenarioId, solicitud.nombre(), solicitante)) {
			return new NombreCanchaDisponible.Respuesta(true, null, null);
		}
		String nombre = solicitud.nombre().strip();
		return new NombreCanchaDisponible.Respuesta(false, "ERR-CAN-03", catalogo.texto("ERR-CAN-03", List.of(nombre)));
	}

	// HU6, CA10: solo lectura, sin opciones para crear ni editar
	@GetMapping("/api/tipos-cancha")
	List<RespuestaTipoCancha> tiposCancha() {
		return listarTipos.ejecutar().stream().map(RespuestaTipoCancha::de).toList();
	}

}
