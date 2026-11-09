package co.edu.uco.canchaya.identidad.infrastructure.primaryadapters.controller;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;

final class CorreoDisponible {

	private CorreoDisponible() {
	}

	// Llega por la URL: ?correoElectronico=...
	record Solicitud(@NotBlank(message = "ERR-GEN-01") String correoElectronico) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record Respuesta(boolean disponible, String codigo, String mensaje) {
	}

}
