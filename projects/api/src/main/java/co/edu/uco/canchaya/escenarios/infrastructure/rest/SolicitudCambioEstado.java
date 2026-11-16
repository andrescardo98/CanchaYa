package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import jakarta.validation.constraints.NotNull;

/** HU13. Un valor distinto de ACTIVO o INACTIVO no se puede leer y responde ERR-GEN-07, como en HU1. */
record SolicitudCambioEstado(

		@NotNull(message = "ERR-GEN-01")
		EstadoEscenario estado) {
}
