package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEncargados;

/**
 * Consulta directa con SQL sobre encargado_escenario: la clase EncargadoEscenario y su entidad JPA
 * llegan con HU34, que es la que asigna y retira encargados (P1 de HU29).
 */
@Repository
class RepositorioEncargadosJdbc implements RepositorioEncargados {

	private final NamedParameterJdbcTemplate jdbc;

	RepositorioEncargadosJdbc(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public boolean estaAsignado(UUID escenarioId, UUID usuarioId) {
		Boolean asignado = jdbc.queryForObject("""
				select exists (select 1 from encargado_escenario
				               where escenario_id = :escenario and usuario_id = :usuario)
				""", Map.of("escenario", escenarioId, "usuario", usuarioId), Boolean.class);
		return Boolean.TRUE.equals(asignado);
	}

	@Override
	public List<UUID> escenariosAsignados(UUID usuarioId) {
		return jdbc.queryForList("""
				select escenario_id from encargado_escenario where usuario_id = :usuario
				""", Map.of("usuario", usuarioId), UUID.class);
	}

}
