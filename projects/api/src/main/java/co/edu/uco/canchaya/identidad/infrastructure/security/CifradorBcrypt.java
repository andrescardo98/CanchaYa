package co.edu.uco.canchaya.identidad.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import co.edu.uco.canchaya.identidad.application.port.out.CifradorContrasena;

@Component
class CifradorBcrypt implements CifradorContrasena {

	private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

	// Hash de relleno para comparar cuando el correo no existe: así se tarda lo mismo (ESC-SEG-03)
	private final String hashRelleno = bcrypt.encode("hash-de-relleno-sin-usuario");

	@Override
	public String cifrar(String contrasena) {
		return bcrypt.encode(contrasena);
	}

	@Override
	public boolean coincide(String contrasena, String hash) {
		boolean coinciden = bcrypt.matches(contrasena, hash == null ? hashRelleno : hash);
		return hash != null && coinciden;
	}

}
