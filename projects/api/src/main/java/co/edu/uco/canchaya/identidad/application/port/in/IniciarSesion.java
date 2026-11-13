package co.edu.uco.canchaya.identidad.application.port.in;

import java.time.Instant;

import co.edu.uco.canchaya.identidad.domain.Rol;

/** HU2: iniciar sesión (responsabilidad «autenticar» de Usuario). */
public interface IniciarSesion {

	/**
	 * @param origen dirección IP desde donde llega la petición; se usa para el límite de
	 *        intentos por origen (D28)
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-USU-06, 07 o 16
	 */
	Sesion ejecutar(Credenciales credenciales, String origen);

	record Credenciales(String correoElectronico, String contrasena) {

		/** Sin la contraseña, para que no termine en un registro de eventos. */
		@Override
		public String toString() {
			return "Credenciales[correoElectronico=" + correoElectronico + "]";
		}
	}

	/** El rol le dice al frontend a qué página de inicio llevar al usuario (CA1). */
	record Sesion(String token, Rol rol, Instant venceEn) {
	}

}
