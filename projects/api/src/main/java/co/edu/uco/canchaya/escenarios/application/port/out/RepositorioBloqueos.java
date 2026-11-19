package co.edu.uco.canchaya.escenarios.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.RangoBloqueado;

/**
 * Lectura de los bloqueos de una cancha. Todavía no hay clase Bloqueo: llega con HU30 (P5 de HU20).
 */
public interface RepositorioBloqueos {

	/** Los bloqueos de la cancha en esa fecha, solo con sus horas y por hora de inicio. */
	List<RangoBloqueado> rangosDelDia(UUID canchaId, LocalDate fecha);

}
