package co.edu.uco.canchaya.reservas.infrastructure.rest;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.canchaya.reservas.application.port.in.ConsultarDisponibilidad;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarDisponibilidad.Disponibilidad;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

@RestController
class DisponibilidadController {

	private final ConsultarDisponibilidad consultarDisponibilidad;
	private final CatalogoMensajes catalogo;

	DisponibilidadController(ConsultarDisponibilidad consultarDisponibilidad, CatalogoMensajes catalogo) {
		this.consultarDisponibilidad = consultarDisponibilidad;
		this.catalogo = catalogo;
	}

	// HU20: pública, sin sesión (AG-11). ERR-RES-01 sale como 404 y ERR-HOR-10 como 422 (ManejadorErrores)
	@GetMapping("/api/canchas/{canchaId}/disponibilidad")
	RespuestaDisponibilidad consultar(@PathVariable UUID canchaId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		Disponibilidad disponibilidad = consultarDisponibilidad.ejecutar(canchaId, fecha);
		// CA4 (AG-09): sin franjas ese día se informa con INF-HOR-01
		if (disponibilidad.franjas().isEmpty()) {
			return RespuestaDisponibilidad.de(disponibilidad, "INF-HOR-01", catalogo.texto("INF-HOR-01"));
		}
		return RespuestaDisponibilidad.de(disponibilidad, null, null);
	}

}
