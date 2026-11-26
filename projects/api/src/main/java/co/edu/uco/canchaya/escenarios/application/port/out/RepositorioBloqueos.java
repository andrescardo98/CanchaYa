package co.edu.uco.canchaya.escenarios.application.port.out;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.RangoBloqueado;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios.BloqueoAgenda;

/**
 * Lectura de los bloqueos de una cancha. Todavía no hay clase Bloqueo: llega con HU30 (P5 de HU20).
 */
public interface RepositorioBloqueos {

	/** Los bloqueos de la cancha en esa fecha, solo con sus horas y por hora de inicio. */
	List<RangoBloqueado> rangosDelDia(UUID canchaId, LocalDate fecha);

	/**
	 * HU18: los bloqueos de varias canchas en esa fecha, en una sola consulta, por cancha. Una
	 * cancha sin bloqueos no viene en el mapa.
	 */
	Map<UUID, List<RangoBloqueado>> rangosDelDia(Collection<UUID> canchaIds, LocalDate fecha);

	/**
	 * HU29 (P2): los bloqueos de esas canchas de desde a hasta (las dos incluidas), con su tipo y su
	 * motivo, por fecha y hora de inicio. Vacía si no se pide ninguna cancha.
	 */
	List<BloqueoAgenda> deCanchasEntre(Collection<UUID> canchaIds, LocalDate desde, LocalDate hasta);

}
