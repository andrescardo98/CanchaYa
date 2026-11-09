package co.edu.uco.canchaya.identidad.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import co.edu.uco.canchaya.identidad.application.port.out.CifradorContrasena;

@Component
class CifradorBcrypt implements CifradorContrasena {

	private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

	@Override
	public String cifrar(String contrasena) {
		return bcrypt.encode(contrasena);
	}

}
