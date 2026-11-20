package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import co.edu.uco.canchaya.escenarios.domain.Cancha;

// HU9: con la cancha guardada, el formulario muestra los datos actualizados (AG-04) y la pantalla
// cambia la fila de la lista sin volver a consultarla (CA1)
record RespuestaModificarCancha(String codigo, String mensaje, RespuestaCancha cancha) {

	static RespuestaModificarCancha de(Cancha cancha, String codigo, String mensaje) {
		return new RespuestaModificarCancha(codigo, mensaje, RespuestaCancha.de(cancha));
	}

}
