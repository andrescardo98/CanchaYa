package co.edu.uco.canchaya.escenarios.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** HU19: el perfil público de un escenario, que se ve sin iniciar sesión (AG-11). */
public interface ConsultarPerfilEscenario {

	/**
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-ESC-06 si el
	 *         escenario no existe o está INACTIVO (CA4)
	 */
	PerfilEscenario ejecutar(UUID escenarioId);

	/**
	 * Lo que muestra el perfil (CA1). No lleva el administrador ni su celular personal: solo el
	 * contacto del escenario.
	 *
	 * @param fotos vacía si el escenario no tiene fotos (CA3)
	 * @param canchas solo las que cumplen la política 11 de Cancha, ordenadas por nombre
	 * @param calificacionPromedio vacía (null) si aún no hay calificaciones (CA2); nunca cero
	 */
	record PerfilEscenario(UUID id, String nombre, String descripcion, String direccion, String ciudad,
			String departamento, BigDecimal latitud, BigDecimal longitud, String telefonoContacto,
			String correoContacto, List<String> fotos, List<CanchaPerfil> canchas,
			BigDecimal calificacionPromedio) {
	}

	/** Una cancha del perfil, con su id para ir a su disponibilidad (CA5, HU20). */
	record CanchaPerfil(UUID id, String nombre, String tipoCancha, BigDecimal precioHora) {
	}

}
