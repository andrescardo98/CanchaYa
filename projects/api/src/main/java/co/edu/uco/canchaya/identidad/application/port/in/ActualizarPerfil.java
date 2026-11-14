package co.edu.uco.canchaya.identidad.application.port.in;

import java.util.UUID;

import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.domain.Usuario;

/** HU3: actualizar mi perfil. */
public interface ActualizarPerfil {

	/**
	 * @param solicitante usuario autenticado, tomado del token de acceso
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-USU-08, 09, 10, 11 o 12
	 */
	Usuario ejecutar(Solicitante solicitante, DatosPerfil datos);

	/**
	 * @param nuevaContrasena null o vacía si el usuario no la quiere cambiar (CA4)
	 * @param tipoDocumento   solo para la política 11: null si no se envió. Llega como texto para
	 *                        que un valor que no existe también cuente como «distinto»
	 * @param numeroDocumento solo para la política 11: null si no se envió
	 * @param rol             solo para la política 11: null si no se envió
	 */
	record DatosPerfil(UUID id, String nombre, String correoElectronico, String numeroCelular,
			String nuevaContrasena, String tipoDocumento, String numeroDocumento, String rol) {

		/** Sin la contraseña, para que no termine en un registro de eventos (AG-13). */
		@Override
		public String toString() {
			return "DatosPerfil[id=" + id + ", correoElectronico=" + correoElectronico + "]";
		}
	}

}
