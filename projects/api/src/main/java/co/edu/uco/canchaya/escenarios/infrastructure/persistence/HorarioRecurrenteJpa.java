package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.uco.canchaya.escenarios.domain.DiaSemana;

interface HorarioRecurrenteJpa extends JpaRepository<HorarioRecurrenteEntidad, UUID> {

	List<HorarioRecurrenteEntidad> findByEscenarioIdAndDiaSemana(UUID escenarioId, DiaSemana diaSemana);

	List<HorarioRecurrenteEntidad> findByEscenarioId(UUID escenarioId);

	// HU18: los de varios escenarios en una sola consulta (where escenario_id in ...)
	List<HorarioRecurrenteEntidad> findByEscenarioIdIn(Collection<UUID> escenarioIds);

}
