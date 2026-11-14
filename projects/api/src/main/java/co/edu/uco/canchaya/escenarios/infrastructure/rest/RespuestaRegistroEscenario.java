package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Escenario;

// Solo el id: administrador, estado y calificación no los ve el usuario (restricción de la HU)
record RespuestaRegistroEscenario(String codigo, String mensaje, UUID id) {

	static RespuestaRegistroEscenario de(Escenario escenario, String codigo, String mensaje) {
		return new RespuestaRegistroEscenario(codigo, mensaje, escenario.getId());
	}

}
