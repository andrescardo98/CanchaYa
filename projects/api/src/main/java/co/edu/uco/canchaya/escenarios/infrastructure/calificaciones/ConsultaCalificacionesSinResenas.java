package co.edu.uco.canchaya.escenarios.infrastructure.calificaciones;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaCalificaciones;

/**
 * Adaptador TEMPORAL (HU19, P1 y P6 de su especificación). Mientras no exista la HU37 nadie puede
 * publicar reseñas, así que no hay calificaciones: se responde con una lista vacía, nunca con un
 * promedio inventado ni con cero.
 *
 * <p>En el Release 3, Reputación implementa {@link ConsultaCalificaciones} y este paquete se
 * elimina completo; si quedaran las dos implementaciones, Spring no arrancaría por tener dos
 * candidatas.
 */
@Component
class ConsultaCalificacionesSinResenas implements ConsultaCalificaciones {

	@Override
	public List<Short> calificacionesDeCanchas(Collection<UUID> canchaIds) {
		return List.of();
	}

}
