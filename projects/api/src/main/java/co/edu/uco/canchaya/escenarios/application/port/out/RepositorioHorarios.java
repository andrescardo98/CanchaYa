package co.edu.uco.canchaya.escenarios.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;

public interface RepositorioHorarios {

	boolean existePorId(UUID id);

	/**
	 * Si otra petición guardó al mismo tiempo un horario que se cruza, la base lo rechaza
	 * (restricción de exclusión) y aquí sale como ERR-HOR-04.
	 */
	void guardar(HorarioRecurrente horario);

	/** Para la política 4: solo los del mismo día. */
	List<HorarioRecurrente> buscarPorEscenarioYDia(UUID escenarioId, DiaSemana diaSemana);

	/** De lunes a domingo y por hora de inicio. */
	List<HorarioRecurrente> buscarPorEscenario(UUID escenarioId);

	/** HU18: los de varios escenarios en una sola consulta, de cualquier día. */
	List<HorarioRecurrente> buscarPorEscenarios(Collection<UUID> escenarioIds);

}
