package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.math.BigDecimal;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.in.ModificarCancha.DatosModificacionCancha;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * HU9: las mismas reglas de datos que {@link SolicitudRegistroCancha}. Los tres campos llegan
 * siempre: el formulario manda los datos actuales con los que cambiaron. El escenario y el estado
 * no llegan porque no se modifican.
 */
record SolicitudModificarCancha(

		@NotBlank(message = "ERR-GEN-01")
		@Size(min = 1, max = 50, message = "ERR-GEN-03")
		String nombre,

		@NotNull(message = "ERR-GEN-01")
		UUID tipoCanchaId,

		// Lo que cabe en la columna NUMERIC(10, 2)
		@NotNull(message = "ERR-GEN-01")
		@Digits(integer = 8, fraction = 2, message = "ERR-GEN-02")
		BigDecimal precioHora) {

	// AG-06: sin espacios en los extremos
	SolicitudModificarCancha {
		nombre = nombre == null ? null : nombre.strip();
	}

	DatosModificacionCancha aDatos(UUID canchaId) {
		return new DatosModificacionCancha(canchaId, tipoCanchaId, nombre, precioHora);
	}

}
