package co.edu.uco.canchaya.identidad.application.usecase;

import co.edu.uco.canchaya.identidad.application.port.out.CifradorContrasena;

/** Cifrador de mentiras para las pruebas unitarias: el «hash» de x es "hash-de-x". */
final class CifradorFalso implements CifradorContrasena {

	int comparaciones;

	@Override
	public String cifrar(String contrasena) {
		return "hash-de-" + contrasena;
	}

	@Override
	public boolean coincide(String contrasena, String hash) {
		comparaciones++;
		return hash != null && hash.equals(cifrar(contrasena));
	}

}
