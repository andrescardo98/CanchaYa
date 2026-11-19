package co.edu.uco.canchaya.escenarios.application.port.out;

import java.util.List;
import java.util.Optional;
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

	/** HU20 (P7): si HU9 se une antes con el mismo método, se usa ese. */
	Optional<Cancha> buscarPorId(UUID id);

	/**
	 * D29 punto 2 y D36: bloquea la fila de la cancha (select ... for update) hasta que termina la
	 * transacción. Falla si no hay una transacción abierta (Propagation.MANDATORY).
	 */
	void tomarTurno(UUID id);

}
