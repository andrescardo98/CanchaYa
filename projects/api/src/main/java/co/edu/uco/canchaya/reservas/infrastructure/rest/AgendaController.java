package co.edu.uco.canchaya.reservas.infrastructure.rest;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarAgenda;
import co.edu.uco.canchaya.reservas.domain.VistaAgenda;

@RestController
class AgendaController {

	private final ConsultarAgenda consultarAgenda;

	AgendaController(ConsultarAgenda consultarAgenda) {
		this.consultarAgenda = consultarAgenda;
	}

	// HU29: exige sesión (AG-11, ESC-SEG-02). Como DisponibilidadController, vive en Reservas aunque la
	// ruta empiece por el escenario. ERR-GEN-06 sale como 403 (CA6) y una fecha o vista ilegible como
	// 400 con ERR-GEN-07 (ManejadorErrores)
	@GetMapping("/api/escenarios/{escenarioId}/agenda")
	RespuestaAgenda consultar(@PathVariable UUID escenarioId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false) VistaAgenda vista, Solicitante solicitante) {
		return RespuestaAgenda.de(consultarAgenda.ejecutar(escenarioId, fecha, vista, solicitante));
	}

}
