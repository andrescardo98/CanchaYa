package co.edu.uco.canchaya.identidad.application.primaryports;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;

/** HU1: registrarse. */
public interface RegistrarUsuario {

	/**
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-USU-01, 02, 03,
	 *         12, 13, 14 o 15
	 */
	Usuario ejecutar(DatosRegistro datos);

	record DatosRegistro(String nombre, String correoElectronico, String contrasena,
			TipoDocumento tipoDocumento, String numeroDocumento,
			String numeroCelular, Rol rol, boolean autorizacionAcudiente) {

		/** Sin la contraseña, para que no termine en un registro de eventos. */
		@Override
		public String toString() {
			return "DatosRegistro[correoElectronico=" + correoElectronico + ", rol=" + rol + "]";
		}
	}

}
