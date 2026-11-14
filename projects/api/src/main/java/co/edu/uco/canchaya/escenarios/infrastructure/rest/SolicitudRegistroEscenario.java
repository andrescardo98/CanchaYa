package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario.DatosRegistroEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario.FotoAdjunta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Parte «datos» del formulario. En message va el código del catálogo; el texto lo pone
 * ManejadorErrores. El rango de latitud y longitud no se revisa aquí: lo revisa el dominio
 * con ERR-ESC-03 (CA5).
 */
record SolicitudRegistroEscenario(

		@NotBlank(message = "ERR-GEN-01")
		@Size(min = 1, max = 100, message = "ERR-GEN-03")
		String nombre,

		@Size(max = 500, message = "ERR-GEN-03")
		String descripcion,

		@NotBlank(message = "ERR-GEN-01")
		@Size(min = 1, max = 200, message = "ERR-GEN-03")
		String direccion,

		@NotNull(message = "ERR-GEN-01")
		UUID ciudadId,

		@NotNull(message = "ERR-GEN-01")
		BigDecimal latitud,

		@NotNull(message = "ERR-GEN-01")
		BigDecimal longitud,

		@NotBlank(message = "ERR-GEN-01")
		@Size(max = 15, message = "ERR-GEN-03")
		@Pattern(regexp = "^\\+?[0-9]+$", message = "ERR-GEN-02")
		String telefonoContacto,

		@Size(min = 5, max = 150, message = "ERR-GEN-03")
		@Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "ERR-GEN-02")
		String correoContacto) {

	// AG-06: sin espacios en los extremos; un opcional en blanco cuenta como no enviado
	SolicitudRegistroEscenario {
		nombre = recortar(nombre);
		descripcion = vacioANulo(recortar(descripcion));
		direccion = recortar(direccion);
		telefonoContacto = recortar(telefonoContacto);
		correoContacto = vacioANulo(recortar(correoContacto));
	}

	DatosRegistroEscenario aDatos(List<FotoAdjunta> fotos) {
		return new DatosRegistroEscenario(nombre, descripcion, direccion, ciudadId, latitud, longitud,
				telefonoContacto, correoContacto, fotos);
	}

	private static String recortar(String texto) {
		return texto == null ? null : texto.strip();
	}

	private static String vacioANulo(String texto) {
		return texto == null || texto.isEmpty() ? null : texto;
	}

}
