package co.edu.uco.canchaya.transversal.infrastructure.rest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record RespuestaError(List<Mensaje> errores) {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Mensaje(String codigo, String campo, String mensaje) {
	}

}
