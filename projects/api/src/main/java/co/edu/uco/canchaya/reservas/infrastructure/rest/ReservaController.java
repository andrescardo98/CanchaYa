package co.edu.uco.canchaya.reservas.infrastructure.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.application.port.in.ReservarCancha;
import co.edu.uco.canchaya.reservas.application.port.in.ReservarCancha.ReservaConfirmada;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;
import jakarta.validation.Valid;

@RestController
class ReservaController {

	private final ReservarCancha reservarCancha;
	private final CatalogoMensajes catalogo;

	ReservaController(ReservarCancha reservarCancha, CatalogoMensajes catalogo) {
		this.reservarCancha = reservarCancha;
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

}
