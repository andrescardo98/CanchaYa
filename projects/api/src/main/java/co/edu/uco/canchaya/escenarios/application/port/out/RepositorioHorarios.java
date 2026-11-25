package co.edu.uco.canchaya.escenarios.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;

public interface RepositorioHorarios {

	boolean existePorId(UUID id);

	/** HU11, política 6. */
	Optional<HorarioRecurrente> buscarPorId(UUID id);

	/**
	 * Si otra petición guardó al mismo tiempo un horario que se cruza, la base lo rechaza
	 * (restricción de exclusión) y aquí sale como ERR-HOR-04.
	 */
	void guardar(HorarioRecurrente horario);

	/**
	 * HU11: guarda el día y las horas. Si otra petición dejó al mismo tiempo un horario que se
	 * cruza, la base lo rechaza (restricción de exclusión) y aquí sale como ERR-HOR-07.
	 */
	void actualizar(HorarioRecurrente horario);

	/** HU12: borrado definitivo. Ninguna tabla apunta a horario_recurrente, así que no choca con nada. */
	void eliminar(UUID id);

	/** Para la política 4: solo los del mismo día. */
	List<HorarioRecurrente> buscarPorEscenarioYDia(UUID escenarioId, DiaSemana diaSemana);

	/** De lunes a domingo y por hora de inicio. */
	List<HorarioRecurrente> buscarPorEscenario(UUID escenarioId);

}
