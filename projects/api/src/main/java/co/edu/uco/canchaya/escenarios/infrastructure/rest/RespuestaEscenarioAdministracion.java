package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.in.ListarEscenariosAdministracion.EscenarioAdministracion;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;

/**
 * Una fila de la lista de HU13, CA1: nombre, ciudad, administrador propietario y estado. El id va
 * para poder activarlo o inactivarlo; no se muestra en pantalla (AG-14). Del propietario solo
 * va el nombre: ni su correo ni su documento (AG-15).
 */
record RespuestaEscenarioAdministracion(UUID id, String nombre, String ciudad, String propietario,
		EstadoEscenario estado) {

	static RespuestaEscenarioAdministracion de(EscenarioAdministracion escenario) {
		return new RespuestaEscenarioAdministracion(escenario.id(), escenario.nombre(), escenario.ciudad(),
				escenario.propietario(), escenario.estado());
	}

}
