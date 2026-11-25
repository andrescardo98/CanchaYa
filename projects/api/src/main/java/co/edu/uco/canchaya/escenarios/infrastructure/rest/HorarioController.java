package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.canchaya.escenarios.application.port.in.DefinirHorario;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarHorarios;
import co.edu.uco.canchaya.escenarios.application.port.in.ModificarHorario;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;
import jakarta.validation.Valid;

/**
 * Los horarios son del escenario y rigen para todas sus canchas, por eso se definen y se listan
 * desde su URL. Para modificar un horario basta su id: el escenario sale del horario y no se
 * cambia (P1 de HU11).
 */
@RestController
class HorarioController {

	private static final String HORARIOS_ESCENARIO = "/api/escenarios/{escenarioId}/horarios";

	private final DefinirHorario definirHorario;
	private final ListarHorarios listarHorarios;
	private final ModificarHorario modificarHorario;
	private final CatalogoMensajes catalogo;

	HorarioController(DefinirHorario definirHorario, ListarHorarios listarHorarios,
			ModificarHorario modificarHorario, CatalogoMensajes catalogo) {
		this.definirHorario = definirHorario;
		this.listarHorarios = listarHorarios;
		this.modificarHorario = modificarHorario;
		this.catalogo = catalogo;
	}

	// HU7, CA1 a CA8
	@PostMapping(HORARIOS_ESCENARIO)
	ResponseEntity<RespuestaDefinirHorario> definir(@PathVariable UUID escenarioId,
			@Valid @RequestBody SolicitudDefinirHorario solicitud, Solicitante solicitante) {
		HorarioRecurrente horario = definirHorario.ejecutar(escenarioId, solicitud.dia(), solicitud.inicio(),
				solicitud.fin(), solicitante);
		return ResponseEntity.status(HttpStatus.CREATED).body(
				new RespuestaDefinirHorario("EXI-HOR-01", catalogo.texto("EXI-HOR-01"), RespuestaHorario.de(horario)));
	}

	// HU7, CA1: la lista que se actualiza después de guardar
	@GetMapping(HORARIOS_ESCENARIO)
	List<RespuestaHorario> listar(@PathVariable UUID escenarioId, Solicitante solicitante) {
		return listarHorarios.ejecutar(escenarioId, solicitante).stream().map(RespuestaHorario::de).toList();
	}

	// HU11, CA1 a CA8. La entrada tiene los mismos campos y reglas que al definir (P2)
	@PutMapping("/api/horarios/{id}")
	RespuestaModificarHorario modificar(@PathVariable UUID id, @Valid @RequestBody SolicitudDefinirHorario solicitud,
			Solicitante solicitante) {
		HorarioRecurrente horario = modificarHorario.ejecutar(id, solicitud.dia(), solicitud.inicio(),
				solicitud.fin(), solicitante);
		return new RespuestaModificarHorario("EXI-HOR-02", catalogo.texto("EXI-HOR-02"), RespuestaHorario.de(horario));
	}

}
