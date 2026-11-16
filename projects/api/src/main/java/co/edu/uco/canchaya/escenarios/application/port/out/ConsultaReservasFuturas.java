package co.edu.uco.canchaya.escenarios.application.port.out;

import java.util.Collection;
import java.util.UUID;

/**
 * Escenarios la define y Reservas la implementa (D08). Así Reservas depende de Escenarios y no
 * al revés, sin dependencia en círculo. Escenarios entrega los identificadores de sus canchas
 * para que Reservas no tenga que leer la tabla cancha.
 */
public interface ConsultaReservasFuturas {

	/**
	 * Si alguna de las canchas tiene una reserva CONFIRMADA que todavía no empieza, con la hora
	 * de America/Bogota (D27). RN-17.
	 */
	boolean existenConfirmadasFuturas(Collection<UUID> canchaIds);

}
