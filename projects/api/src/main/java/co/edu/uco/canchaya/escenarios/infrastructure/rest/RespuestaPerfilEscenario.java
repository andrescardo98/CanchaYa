package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarPerfilEscenario.CanchaPerfil;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarPerfilEscenario.PerfilEscenario;

/**
 * HU19: lo que muestra la ficha del escenario. Sin administrador, celular personal ni estado
 * (restricción de la HU). codigo y mensaje solo vienen con INF-RSE-01, cuando no hay
 * calificaciones (CA2); en los demás casos van vacíos (null).
 */
record RespuestaPerfilEscenario(UUID id, String nombre, String descripcion, String direccion, String ciudad,
		String departamento, BigDecimal latitud, BigDecimal longitud, String telefonoContacto,
		String correoContacto, List<String> fotos, List<RespuestaCanchaPerfil> canchas,
		BigDecimal calificacionPromedio, String codigo, String mensaje) {

	static RespuestaPerfilEscenario de(PerfilEscenario perfil, String codigo, String mensaje) {
		return new RespuestaPerfilEscenario(perfil.id(), perfil.nombre(), perfil.descripcion(), perfil.direccion(),
				perfil.ciudad(), perfil.departamento(), perfil.latitud(), perfil.longitud(),
				perfil.telefonoContacto(), perfil.correoContacto(), perfil.fotos(),
				perfil.canchas().stream().map(RespuestaCanchaPerfil::de).toList(), perfil.calificacionPromedio(),
				codigo, mensaje);
	}

	// CA5: con su id, para ir a la disponibilidad de la cancha (HU20)
	record RespuestaCanchaPerfil(UUID id, String nombre, String tipoCancha, BigDecimal precioHora) {

		static RespuestaCanchaPerfil de(CanchaPerfil cancha) {
			return new RespuestaCanchaPerfil(cancha.id(), cancha.nombre(), cancha.tipoCancha(), cancha.precioHora());
		}

	}

}
