package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface CanchaJpa extends JpaRepository<CanchaEntidad, UUID> {

	// Con lower(), igual que el índice único de la V3
	@Query("""
			select count(c) > 0 from CanchaEntidad c
			where c.escenarioId = :escenarioId and lower(c.nombre) = lower(:nombre)""")
	boolean existeNombreEnEscenario(UUID escenarioId, String nombre);

	List<CanchaEntidad> findByEscenarioIdOrderByNombreAsc(UUID escenarioId);

}
