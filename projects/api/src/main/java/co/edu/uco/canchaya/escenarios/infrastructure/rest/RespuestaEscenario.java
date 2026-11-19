package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;

/**
 * HU8: los datos actuales del escenario para el formulario de su propietario. El estado va solo para
 * mostrarlo; administradorId no se devuelve.
 */
record RespuestaEscenario(UUID id, String nombre, String descripcion, String direccion, UUID ciudadId,
		BigDecimal latitud, BigDecimal longitud, String telefonoContacto, String correoContacto, List<String> fotos,
		EstadoEscenario estado) {

	static RespuestaEscenario de(Escenario escenario) {
		return new RespuestaEscenario(escenario.getId(), escenario.getNombre(), escenario.getDescripcion(),
				escenario.getDireccion(), escenario.getCiudadId(), escenario.getLatitud(), escenario.getLongitud(),
				escenario.getTelefonoContacto(), escenario.getCorreoContacto(), escenario.getFotos(),
				escenario.getEstado());
	}

}
