package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.uco.canchaya.escenarios.domain.DiaSemana;

interface HorarioRecurrenteJpa extends JpaRepository<HorarioRecurrenteEntidad, UUID> {

	List<HorarioRecurrenteEntidad> findByEscenarioIdAndDiaSemana(UUID escenarioId, DiaSemana diaSemana);

	List<HorarioRecurrenteEntidad> findByEscenarioId(UUID escenarioId);

}
