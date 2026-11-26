package co.edu.uco.canchaya.reservas.infrastructure.rest;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.application.port.in.CancelarReserva;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarMisReservas;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarMisReservas.MisReservas;
import co.edu.uco.canchaya.reservas.application.port.in.ReservarCancha;
import co.edu.uco.canchaya.reservas.application.port.in.ReservarCancha.ReservaConfirmada;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;
import jakarta.validation.Valid;

@RestController
class ReservaController {

	private final ReservarCancha reservarCancha;
	private final ConsultarMisReservas consultarMisReservas;
	private final CancelarReserva cancelarReserva;
	private final CatalogoMensajes catalogo;

	ReservaController(ReservarCancha reservarCancha, ConsultarMisReservas consultarMisReservas,
			CancelarReserva cancelarReserva, CatalogoMensajes catalogo) {
		this.reservarCancha = reservarCancha;
		this.consultarMisReservas = consultarMisReservas;
		this.cancelarReserva = cancelarReserva;
		this.catalogo = catalogo;
	}

	// HU21: exige sesión (AG-11); sin token responde 401 ERR-GEN-05 (CA15). ERR-RES-05 sale como 403,
	// ERR-RES-01 como 404, ERR-RES-06 como 409 y las demás reglas como 422 (ManejadorErrores)
	@PostMapping("/api/reservas")
	ResponseEntity<RespuestaReserva> reservar(@Valid @RequestBody SolicitudReserva solicitud,
			Solicitante solicitante) {
		ReservaConfirmada confirmada = reservarCancha.ejecutar(solicitud.canchaId(), solicitud.fecha(),
				solicitud.horaInicio(), solicitud.horaFin(), solicitante);
		return ResponseEntity.status(HttpStatus.CREATED).body(RespuestaReserva.de(confirmada, catalogo));
	}

	// HU23: exige sesión (AG-11) y rol JUGADOR; ERR-GEN-06 sale como 403 (ManejadorErrores, P2). El
	// jugador sale del token, nunca de la petición (CA5)
	@GetMapping("/api/reservas/mias")
	RespuestaMisReservas misReservas(Solicitante solicitante) {
		MisReservas mis = consultarMisReservas.ejecutar(solicitante);
		// CA2 (AG-09): sin reservas se informa con INF-RES-01
		if (mis.vacia()) {
			return RespuestaMisReservas.de(mis, "INF-RES-01", catalogo.texto("INF-RES-01"));
		}
		return RespuestaMisReservas.de(mis, null, null);
	}

	// HU24 (P1): exige sesión (AG-11) y no lleva cuerpo. Quien cancela sale del token (AG-12).
	// ERR-RES-14 sale como 404, ERR-RES-15 como 403 y ERR-RES-16 como 422 (ManejadorErrores)
	@PostMapping("/api/reservas/{id}/cancelacion")
	RespuestaCancelacion cancelar(@PathVariable UUID id, Solicitante solicitante) {
		return RespuestaCancelacion.de(cancelarReserva.ejecutar(id, solicitante), catalogo);
	}

}
