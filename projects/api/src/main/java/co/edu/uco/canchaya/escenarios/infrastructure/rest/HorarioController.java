package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.canchaya.escenarios.application.port.in.DefinirHorario;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarHorarios;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;
import jakarta.validation.Valid;

// Los horarios son del escenario y rigen para todas sus canchas, por eso cuelgan de su URL
@RestController
@RequestMapping("/api/escenarios/{escenarioId}/horarios")
class HorarioController {

	private final DefinirHorario definirHorario;
	private final ListarHorarios listarHorarios;
	private final CatalogoMensajes catalogo;

	HorarioController(DefinirHorario definirHorario, ListarHorarios listarHorarios, CatalogoMensajes catalogo) {
		this.definirHorario = definirHorario;
		this.listarHorarios = listarHorarios;
		this.catalogo = catalogo;
	}

	// HU7, CA1 a CA8
	@PostMapping
	ResponseEntity<RespuestaDefinirHorario> definir(@PathVariable UUID escenarioId,
			@Valid @RequestBody SolicitudDefinirHorario solicitud, Solicitante solicitante) {
		HorarioRecurrente horario = definirHorario.ejecutar(escenarioId, solicitud.dia(), solicitud.inicio(),
				solicitud.fin(), solicitante);
		return ResponseEntity.status(HttpStatus.CREATED).body(
				new RespuestaDefinirHorario("EXI-HOR-01", catalogo.texto("EXI-HOR-01"), RespuestaHorario.de(horario)));
	}

	// HU7, CA1: la lista que se actualiza después de guardar
	@GetMapping
	List<RespuestaHorario> listar(@PathVariable UUID escenarioId, Solicitante solicitante) {
		return listarHorarios.ejecutar(escenarioId, solicitante).stream().map(RespuestaHorario::de).toList();
	}

}
