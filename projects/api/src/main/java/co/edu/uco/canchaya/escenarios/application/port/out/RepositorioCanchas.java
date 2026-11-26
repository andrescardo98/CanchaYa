package co.edu.uco.canchaya.escenarios.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.NombresCancha;
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

	/**
	 * HU9, política 8: compara sin distinguir mayúsculas de minúsculas y no cuenta la cancha que se
	 * modifica, así se puede dejar el mismo nombre o cambiarle solo las mayúsculas.
	 */
	boolean existeNombreEnOtraCancha(UUID escenarioId, String nombre, UUID canchaId);

	/**
	 * HU9: guarda el nombre, el tipo y el precio. Si otra solicitud dejó el mismo nombre al mismo
	 * tiempo, la base la rechaza.
	 *
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-CAN-08
	 */
	void actualizar(Cancha cancha);

	/** Ordenadas por nombre. */
	List<Cancha> listarPorEscenario(UUID escenarioId);

	/** HU20 (P7): si HU9 se une antes con el mismo método, se usa ese. */
	Optional<Cancha> buscarPorId(UUID id);

	/**
	 * D29 punto 2 y D36: bloquea la fila de la cancha (select ... for update) hasta que termina la
	 * transacción. Falla si no hay una transacción abierta (Propagation.MANDATORY).
	 */
	void tomarTurno(UUID id);

	/** HU10: guarda solo el estado. */
	void actualizarEstado(Cancha cancha);

	/**
	 * HU23: nombre de cada cancha y de su escenario, en una sola consulta y sin mirar el estado.
	 * Las que no existen no vienen; vacío si no se pide ninguna.
	 */
	Map<UUID, NombresCancha> nombresConEscenario(Collection<UUID> canchaIds);

}
