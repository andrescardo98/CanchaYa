package co.edu.uco.canchaya.reservas.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.ConfirmacionAsistencia;
import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.OrigenCancelacion;
import co.edu.uco.canchaya.reservas.domain.Reserva;

/**
 * Consultas directas con SQL: todavía no hay entidad JPA de reserva (llega con la HU21). Las dos
 * usan el índice ix_reserva_cancha_fecha.
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

	@Override
	public List<Reserva> confirmadasDeCanchaEnFecha(UUID canchaId, LocalDate fecha) {
		return jdbc.query("""
				select id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado,
				  confirmacion_asistencia, fecha_solicitud_confirmacion, origen_cancelacion, motivo_cancelacion,
				  fecha_creacion, fecha_cancelacion
				from reserva
				where cancha_id = :cancha and fecha = :fecha and estado = 'CONFIRMADA'
				order by hora_inicio
				""", Map.of("cancha", canchaId, "fecha", fecha), (fila, numero) -> aDominio(fila));
	}

	private static Reserva aDominio(ResultSet fila) throws SQLException {
		String origen = fila.getString("origen_cancelacion");
		return Reserva.reconstruir(fila.getObject("id", UUID.class), fila.getObject("cancha_id", UUID.class),
				fila.getObject("jugador_id", UUID.class), fila.getObject("fecha", LocalDate.class),
				fila.getObject("hora_inicio", LocalTime.class), fila.getObject("hora_fin", LocalTime.class),
				fila.getBigDecimal("precio"), EstadoReserva.valueOf(fila.getString("estado")),
				ConfirmacionAsistencia.valueOf(fila.getString("confirmacion_asistencia")),
				origen == null ? null : OrigenCancelacion.valueOf(origen), fila.getString("motivo_cancelacion"),
				fila.getObject("fecha_creacion", OffsetDateTime.class),
				fila.getObject("fecha_cancelacion", OffsetDateTime.class),
				fila.getObject("fecha_solicitud_confirmacion", OffsetDateTime.class));
	}

}
