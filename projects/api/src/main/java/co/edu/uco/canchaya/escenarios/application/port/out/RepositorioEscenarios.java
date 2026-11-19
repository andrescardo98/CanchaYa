package co.edu.uco.canchaya.escenarios.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;

public interface RepositorioEscenarios {

	boolean existePorId(UUID id);

	/** Guarda el escenario con sus fotos en una sola transacción. */
	void guardar(Escenario escenario);

	/** Con sus fotos (HU13). */
	Optional<Escenario> buscarPorId(UUID id);

	/**
	 * Guarda los datos del escenario y reemplaza sus fotos en una sola transacción (HU8). El
	 * propietario y el estado no cambian.
	 */
	void actualizar(Escenario escenario);

	/** Guarda solo el estado; nada más del escenario cambia (HU13). */
	void actualizarEstado(Escenario escenario);

	/**
	 * Lista de administración (HU13, CA1), ordenada por nombre. Consulta de solo lectura: trae
	 * solo lo que la lista muestra, sin las fotos.
	 *
	 * @param estado si es vacío (null), trae todos
	 */
	List<ResumenEscenario> listar(EstadoEscenario estado);

	/** Identificadores de las canchas del escenario, para preguntar por sus reservas (D08). */
	List<UUID> idsCanchas(UUID escenarioId);

	record ResumenEscenario(UUID id, String nombre, UUID ciudadId, UUID administradorId, EstadoEscenario estado) {
	}

}
