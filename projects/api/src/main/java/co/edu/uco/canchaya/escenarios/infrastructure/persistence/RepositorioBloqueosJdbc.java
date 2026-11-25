package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.RangoBloqueado;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBloqueos;

/**
 * Consulta directa con SQL: todavía no hay clase Bloqueo ni entidad JPA (llegan con HU30) y aquí
 * solo se necesitan las horas; el tipo y el motivo no salen (P5 de HU20).
 */
@Repository
class RepositorioBloqueosJdbc implements RepositorioBloqueos {

	private final NamedParameterJdbcTemplate jdbc;

	RepositorioBloqueosJdbc(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<RangoBloqueado> rangosDelDia(UUID canchaId, LocalDate fecha) {
		return jdbc.query("""
				select hora_inicio, hora_fin from bloqueo
				where cancha_id = :cancha and fecha = :fecha
				order by hora_inicio
				""", Map.of("cancha", canchaId, "fecha", fecha),
				(fila, numero) -> new RangoBloqueado(fila.getObject("hora_inicio", LocalTime.class),
						fila.getObject("hora_fin", LocalTime.class)));
	}

	@Override
	public Map<UUID, List<RangoBloqueado>> rangosDelDia(Collection<UUID> canchaIds, LocalDate fecha) {
		// HU18: una sola consulta para todas las canchas; sin canchas no se consulta
		if (canchaIds.isEmpty()) {
			return Map.of();
		}
		Map<UUID, List<RangoBloqueado>> porCancha = new HashMap<>();
		jdbc.query("""
				select cancha_id, hora_inicio, hora_fin from bloqueo
				where cancha_id in (:canchas) and fecha = :fecha
				order by hora_inicio
				""", Map.of("canchas", canchaIds, "fecha", fecha),
				fila -> {
					porCancha.computeIfAbsent(fila.getObject("cancha_id", UUID.class), id -> new ArrayList<>())
							.add(new RangoBloqueado(fila.getObject("hora_inicio", LocalTime.class),
									fila.getObject("hora_fin", LocalTime.class)));
				});
		return porCancha;
	}

}
