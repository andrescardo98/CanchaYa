package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;

final class NombreCanchaDisponible {

	private NombreCanchaDisponible() {
	}

	// Llega por la URL: ?nombre=...
	record Solicitud(@NotBlank(message = "ERR-GEN-01") String nombre) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record Respuesta(boolean disponible, String codigo, String mensaje) {
	}

}
