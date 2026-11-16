package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;

interface EscenarioJpa extends JpaRepository<EscenarioEntidad, UUID> {

	List<EscenarioEntidad> findAllByOrderByNombreAsc();

	List<EscenarioEntidad> findByEstadoOrderByNombreAsc(EstadoEscenario estado);

	// Solo la columna estado: no se reescriben los demás datos ni las fotos
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update EscenarioEntidad e set e.estado = :estado where e.id = :id")
	int actualizarEstado(UUID id, EstadoEscenario estado);

	// La tabla cancha es del mismo módulo; todavía no tiene entidad JPA (llega con la HU6)
	@Query(value = "select id from cancha where escenario_id = :escenarioId", nativeQuery = true)
	List<UUID> idsCanchas(UUID escenarioId);

}
