package co.edu.uco.canchaya.reservas.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;

/**
 * Consulta directa con SQL: todavía no hay entidad JPA de reserva (llega con la HU21) y esta
 * pregunta solo necesita un sí o un no. Usa el índice ix_reserva_cancha_fecha.
 */
@Repository
class RepositorioReservasJdbc implements RepositorioReservas {

	private final NamedParameterJdbcTemplate jdbc;

	RepositorioReservasJdbc(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public boolean existenConfirmadasQueEmpiezanDespuesDe(Collection<UUID> canchaIds, LocalDateTime momento) {
		// fecha + hora_inicio es la hora de Bogotá sin zona, igual que momento (D27)
		Boolean existen = jdbc.queryForObject("""
				select exists (
				  select 1 from reserva
				  where cancha_id in (:canchas)
				    and estado = 'CONFIRMADA'
				    and fecha >= cast(:momento as date)
				    and fecha + hora_inicio > :momento)
				""", Map.of("canchas", canchaIds, "momento", momento), Boolean.class);
		return Boolean.TRUE.equals(existen);
	}

}
