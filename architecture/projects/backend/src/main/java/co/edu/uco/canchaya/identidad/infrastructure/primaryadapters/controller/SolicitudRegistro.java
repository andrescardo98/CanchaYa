package co.edu.uco.canchaya.identidad.infrastructure.primaryadapters.controller;

import co.edu.uco.canchaya.identidad.application.primaryports.RegistrarUsuario.DatosRegistro;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// En message va el código del catálogo; el texto lo pone ManejadorErrores
record SolicitudRegistro(

		@NotBlank(message = "ERR-GEN-01")
		@Size(min = 1, max = 100, message = "ERR-GEN-03")
		@Pattern(regexp = "^[\\p{L} ]+$", message = "ERR-GEN-02")
		String nombre,

		@NotBlank(message = "ERR-GEN-01")
		@Size(min = 5, max = 150, message = "ERR-GEN-03")
		@Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "ERR-GEN-02")
		String correoElectronico,

		// Longitud y composición las revisa el dominio (política 12)
		@NotBlank(message = "ERR-GEN-01")
		String contrasena,

		@NotBlank(message = "ERR-GEN-01")
		String tipoDocumento,

		// El formato depende del tipo y lo revisa el dominio (política 13)
		@NotBlank(message = "ERR-GEN-01")
		@Size(min = 5, max = 20, message = "ERR-GEN-03")
		String numeroDocumento,

		@NotBlank(message = "ERR-GEN-01")
		@Size(max = 15, message = "ERR-GEN-03")
		@Pattern(regexp = "^\\+?[0-9]+$", message = "ERR-GEN-02")
		String numeroCelular,

		@NotBlank(message = "ERR-GEN-01")
		String rol,

		// Boolean y no boolean: si no llega, Jackson rechazaría toda la petición
		Boolean autorizacionAcudiente) {

	// AG-06: se quitan los espacios de los extremos antes de validar. La contraseña se deja igual.
	SolicitudRegistro {
		nombre = recortar(nombre);
		correoElectronico = recortar(correoElectronico);
		tipoDocumento = recortar(tipoDocumento);
		numeroDocumento = recortar(numeroDocumento);
		numeroCelular = recortar(numeroCelular);
		rol = recortar(rol);
	}

	DatosRegistro aDatosRegistro() {
		return new DatosRegistro(nombre, correoElectronico, contrasena, buscar(TipoDocumento.class, tipoDocumento),
				numeroDocumento, numeroCelular, buscar(Rol.class, rol), Boolean.TRUE.equals(autorizacionAcudiente));
	}

	// AG-13: que la contraseña no salga en el log
	@Override
	public String toString() {
		return "SolicitudRegistro[correoElectronico=" + correoElectronico + ", rol=" + rol + "]";
	}

	private static String recortar(String texto) {
		return texto == null ? null : texto.strip();
	}

	// Un valor que no existe llega como null y el dominio responde ERR-USU-03 o ERR-USU-13
	private static <E extends Enum<E>> E buscar(Class<E> tipo, String valor) {
		try {
			return Enum.valueOf(tipo, valor);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

}
