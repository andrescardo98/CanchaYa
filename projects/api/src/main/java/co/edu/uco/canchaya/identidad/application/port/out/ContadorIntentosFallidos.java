package co.edu.uco.canchaya.identidad.application.port.out;

/**
 * Límite de intentos de inicio de sesión (D28): lleva la cuenta de los intentos fallidos por
 * cuenta (correo) y por origen (IP).
 */
public interface ContadorIntentosFallidos {

	/** Si la cuenta o el origen superaron los intentos permitidos y siguen en el tiempo de bloqueo. */
	boolean estaBloqueado(String correoElectronico, String origen);

	/** Cuenta un intento fallido para la cuenta y para el origen. */
	void registrarFallo(String correoElectronico, String origen);

}
