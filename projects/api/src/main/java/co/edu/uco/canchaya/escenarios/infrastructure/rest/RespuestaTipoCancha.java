package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.TipoCancha;

record RespuestaTipoCancha(UUID id, String codigo, String nombre, int capacidadJugadores) {

	static RespuestaTipoCancha de(TipoCancha tipo) {
		return new RespuestaTipoCancha(tipo.getId(), tipo.getCodigo(), tipo.getNombre(), tipo.getCapacidadJugadores());
	}

}
