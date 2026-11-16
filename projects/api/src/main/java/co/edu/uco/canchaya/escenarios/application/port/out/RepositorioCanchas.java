package co.edu.uco.canchaya.escenarios.application.port.out;

import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Cancha;

public interface RepositorioCanchas {

	boolean existePorId(UUID id);

	/** Compara el nombre sin distinguir mayúsculas de minúsculas (HU6). */
	boolean existeNombreEnEscenario(UUID escenarioId, String nombre);

	/**
	 * Si otra solicitud guardó el mismo nombre al mismo tiempo, la base la rechaza.
	 *
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-CAN-03
	 */
	void guardar(Cancha cancha);

	/** Ordenadas por nombre. */
	List<Cancha> listarPorEscenario(UUID escenarioId);

}
