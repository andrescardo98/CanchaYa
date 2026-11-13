package co.edu.uco.canchaya.identidad.application.port.out;

public interface CifradorContrasena {

	String cifrar(String contrasena);

	/**
	 * Si {@code hash} es null (el correo no está registrado) igual hace una comparación de
	 * relleno y devuelve false, para que la respuesta tarde lo mismo y no revele si el correo
	 * existe (ESC-SEG-03).
	 */
	boolean coincide(String contrasena, String hash);

}
