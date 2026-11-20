package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import jakarta.validation.constraints.NotNull;

/** HU10. Un valor distinto de DISPONIBLE o INACTIVA no se puede leer y responde ERR-GEN-07, como en HU13. */
record SolicitudCambioEstadoCancha(

		@NotNull(message = "ERR-GEN-01")
		EstadoCancha estado) {
}
