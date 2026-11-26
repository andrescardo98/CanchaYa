package co.edu.uco.canchaya.identidad.application.port.in;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Lo que Identidad ofrece a los demás módulos sobre los usuarios, sin entregar la clase Usuario
 * (HU13: el nombre del propietario en la lista y su correo para avisarle).
 */
public interface ConsultaUsuarios {

	/** Los identificadores que no existen simplemente no vienen en la respuesta. */
	List<ContactoUsuario> buscarContactos(Collection<UUID> usuarioIds);

	/**
	 * numeroCelular lo usa COR-RES-02 (HU22, P4). Es un dato de contacto: va en el cuerpo del
	 * correo, nunca en el log (RT-12).
	 */
	record ContactoUsuario(UUID id, String nombre, String correoElectronico, String numeroCelular) {
	}

}
