package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import co.edu.uco.canchaya.escenarios.domain.Escenario;

// HU8: con el escenario completo la pantalla muestra los datos guardados (AG-04)
record RespuestaModificarEscenario(String codigo, String mensaje, RespuestaEscenario escenario) {

	static RespuestaModificarEscenario de(Escenario escenario, String codigo, String mensaje) {
		return new RespuestaModificarEscenario(codigo, mensaje, RespuestaEscenario.de(escenario));
	}

}
