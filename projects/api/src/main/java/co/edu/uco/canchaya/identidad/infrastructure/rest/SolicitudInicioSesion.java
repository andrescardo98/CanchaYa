package co.edu.uco.canchaya.identidad.infrastructure.rest;

import co.edu.uco.canchaya.identidad.application.port.in.IniciarSesion.Credenciales;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// CA2: si falta un dato o el correo no tiene formato válido, no se intenta la autenticación
record SolicitudInicioSesion(

		@NotBlank(message = "ERR-GEN-01")
		@Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "ERR-GEN-02")
		String correoElectronico,

		@NotBlank(message = "ERR-GEN-01")
		String contrasena) {

	// AG-06: se quitan los espacios de los extremos del correo. La contraseña se deja igual.
	SolicitudInicioSesion {
		correoElectronico = correoElectronico == null ? null : correoElectronico.strip();
	}

	Credenciales aCredenciales() {
		return new Credenciales(correoElectronico, contrasena);
	}

	// AG-13: que la contraseña no salga en el log
	@Override
	public String toString() {
		return "SolicitudInicioSesion[correoElectronico=" + correoElectronico + "]";
	}

}
