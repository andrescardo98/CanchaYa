package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.math.BigDecimal;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;

// Una fila de la lista de canchas del escenario (HU6, CA1). El id es para modificarla después (HU9)
record RespuestaCancha(UUID id, String nombre, UUID tipoCanchaId, BigDecimal precioHora, EstadoCancha estado) {

	static RespuestaCancha de(Cancha cancha) {
		return new RespuestaCancha(cancha.getId(), cancha.getNombre(), cancha.getTipoCanchaId(),
				cancha.getPrecioHora(), cancha.getEstado());
	}

}
