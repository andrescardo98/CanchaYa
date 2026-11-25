package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;

interface CanchaJpa extends JpaRepository<CanchaEntidad, UUID> {

	// Con lower(), igual que el índice único de la V3
	@Query("""
			select count(c) > 0 from CanchaEntidad c
			where c.escenarioId = :escenarioId and lower(c.nombre) = lower(:nombre)""")
	boolean existeNombreEnEscenario(UUID escenarioId, String nombre);

	// HU9, política 8: lo mismo, sin contar la cancha que se modifica
	@Query("""
			select count(c) > 0 from CanchaEntidad c
			where c.escenarioId = :escenarioId and lower(c.nombre) = lower(:nombre) and c.id <> :canchaId""")
	boolean existeNombreEnOtraCancha(UUID escenarioId, String nombre, UUID canchaId);

	List<CanchaEntidad> findByEscenarioIdOrderByNombreAsc(UUID escenarioId);

	// D29 punto 2 y D36: la fila queda bloqueada hasta que termina la transacción
	@Query(value = "select id from cancha where id = :id for update", nativeQuery = true)
	Optional<UUID> bloquearFila(UUID id);

	// HU10: solo la columna estado, igual que EscenarioJpa.actualizarEstado (HU13)
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update CanchaEntidad c set c.estado = :estado where c.id = :id")
	int actualizarEstado(UUID id, EstadoCancha estado);

	// HU23: [id, nombre de la cancha, nombre del escenario], en cualquier estado
	@Query("""
			select c.id, c.nombre, e.nombre from CanchaEntidad c join EscenarioEntidad e on e.id = c.escenarioId
			where c.id in :ids""")
	List<Object[]> nombresConEscenario(Collection<UUID> ids);

}
