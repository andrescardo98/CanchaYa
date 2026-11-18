package co.edu.uco.canchaya.escenarios.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Escenarios la define y Reputación la implementará en el Release 3 (HU19, P1 de su
 * especificación), igual que {@link ConsultaReservasFuturas} (D08): así Reputación depende de
 * Escenarios y no al revés. Escenarios entrega los identificadores de sus canchas para que
 * Reputación no lea la tabla cancha; Reputación llega a las reseñas por medio de Reservas, porque
 * resena solo guarda reserva_id.
 */
public interface ConsultaCalificaciones {

	/**
	 * Calificaciones (1 a 5) de las reseñas de las reservas jugadas en esas canchas.
	 *
	 * @return vacía si no hay reseñas; nunca un valor inventado
	 */
	List<Short> calificacionesDeCanchas(Collection<UUID> canchaIds);

}
