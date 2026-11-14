package co.edu.uco.canchaya.identidad.infrastructure.rest;

import java.util.UUID;

import co.edu.uco.canchaya.identidad.application.port.in.ActualizarPerfil.DatosPerfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// En message va el código del catálogo; el texto lo pone ManejadorErrores. Mismas reglas que en el registro.
record SolicitudActualizarPerfil(

		@NotBlank(message = "ERR-GEN-01")
		@Size(min = 1, max = 100, message = "ERR-GEN-03")
		@Pattern(regexp = "^[\\p{L} ]+$", message = "ERR-GEN-02")
		String nombre,

		@NotBlank(message = "ERR-GEN-01")
		@Size(min = 5, max = 150, message = "ERR-GEN-03")
		@Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "ERR-GEN-02")
		String correoElectronico,

		@NotBlank(message = "ERR-GEN-01")
		@Size(max = 15, message = "ERR-GEN-03")
		@Pattern(regexp = "^\\+?[0-9]+$", message = "ERR-GEN-02")
		String numeroCelular,

		// Opcional (CA4). Longitud y composición las revisa el dominio (política 12)
		String nuevaContrasena,

		// Opcionales: el formulario los muestra como solo lectura. Si llegan distintos, ERR-USU-11 (CA5)
		String tipoDocumento,
		String numeroDocumento,
		String rol) {

	// AG-06: se quitan los espacios de los extremos antes de validar. La contraseña se deja igual.
	SolicitudActualizarPerfil {
		nombre = recortar(nombre);
		correoElectronico = recortar(correoElectronico);
		numeroCelular = recortar(numeroCelular);
		tipoDocumento = recortar(tipoDocumento);
		numeroDocumento = recortar(numeroDocumento);
		rol = recortar(rol);
	}

	DatosPerfil aDatosPerfil(UUID id) {
		return new DatosPerfil(id, nombre, correoElectronico, numeroCelular, nuevaContrasena, tipoDocumento,
				numeroDocumento, rol);
	}

	// AG-13: que la contraseña no salga en el log
	@Override
	public String toString() {
		return "SolicitudActualizarPerfil[correoElectronico=" + correoElectronico + "]";
	}

	private static String recortar(String texto) {
		return texto == null ? null : texto.strip();
	}

}
