package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.math.BigDecimal;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarCancha.DatosRegistroCancha;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * En message va el código del catálogo; el texto lo pone ManejadorErrores. Que el precio sea
 * mayor que cero no se revisa aquí: lo revisa el dominio con ERR-CAN-05 (CA7). El id y el estado
 * no llegan: los pone el servidor.
 */
record SolicitudRegistroCancha(

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
	SolicitudRegistroCancha {
		nombre = nombre == null ? null : nombre.strip();
	}

	DatosRegistroCancha aDatos(UUID escenarioId) {
		return new DatosRegistroCancha(escenarioId, tipoCanchaId, nombre, precioHora);
	}

}
