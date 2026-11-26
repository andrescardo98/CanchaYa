package co.edu.uco.canchaya.escenarios.application.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Lectura de las asignaciones de encargados de sede. Todavía no hay clase EncargadoEscenario:
 * llega con HU34 (P1 de HU29).
 */
public interface RepositorioEncargados {

	/** RN-15: si el usuario está asignado como encargado del escenario. */
	boolean estaAsignado(UUID escenarioId, UUID usuarioId);

	/** HU29, CA5: los escenarios que el usuario tiene asignados como encargado. */
	List<UUID> escenariosAsignados(UUID usuarioId);

}
