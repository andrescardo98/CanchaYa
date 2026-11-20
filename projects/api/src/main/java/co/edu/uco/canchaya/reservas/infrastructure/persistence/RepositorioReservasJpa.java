package co.edu.uco.canchaya.reservas.infrastructure.persistence;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * Único adaptador de RepositorioReservas (HU21). Guarda y lee reservas con JPA; las consultas
 * que suman o comparan fecha + hora van en SQL, porque JPQL no suma una fecha con una hora.
 */
@Repository
class RepositorioReservasJpa implements RepositorioReservas {

	private static final Logger LOG = LoggerFactory.getLogger(RepositorioReservasJpa.class);

	// Código de PostgreSQL para "viola una restricción de exclusión"; la tabla solo tiene ex_reserva_sin_cruce
	private static final String EXCLUSION = "23P01";
	// Código de PostgreSQL para "bloqueo mutuo" (deadlock)
	private static final String BLOQUEO_MUTUO = "40P01";

	private final ReservaJpa jpa;
	private final NamedParameterJdbcTemplate jdbc;

	RepositorioReservasJpa(ReservaJpa jpa, NamedParameterJdbcTemplate jdbc) {
		this.jpa = jpa;
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
	@Transactional(readOnly = true)
	public List<Reserva> confirmadasDeCanchaEnFecha(UUID canchaId, LocalDate fecha) {
		return jpa.findByCanchaIdAndFechaAndEstadoOrderByHoraInicioAsc(canchaId, fecha, EstadoReserva.CONFIRMADA)
				.stream()
				.map(ReservaEntidad::aDominio)
				.toList();
	}

	@Override
	public boolean existePorId(UUID id) {
		return jpa.existsById(id);
	}

	@Override
	@Transactional
	public void guardar(Reserva reserva) {
		try {
			// flush para que la base rechace aquí y no al confirmar, donde ya no se podría traducir.
			// Las dos respuestas de abajo llegan en este flush, que es cuando se ejecuta el insert
			jpa.saveAndFlush(ReservaEntidad.desde(reserva));
		} catch (DataIntegrityViolationException e) {
			// D29 punto 1: dos reservas se cruzaron y la base frenó la segunda (CA3, CA4)
			if (tieneEstado(e, EXCLUSION)) {
				throw new ReglaNegocioException("ERR-RES-06");
			}
			throw e;
		} catch (PessimisticLockingFailureException e) {
			// P10 y spike RT-03: si dos inserciones que se cruzan llegan a la vez sin el turno, cada una
			// espera a la otra en el índice de ex_reserva_sin_cruce y PostgreSQL aborta una. Es otra
			// reserva que ganó la franja, así que el jugador recibe lo mismo que con la restricción
			if (tieneEstado(e, BLOQUEO_MUTUO)) {
				LOG.warn("ERR-RES-06 por bloqueo mutuo (40P01) al guardar la reserva: cancha={} fecha={} franja={}-{}",
						reserva.getCanchaId(), reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());
				throw new ReglaNegocioException("ERR-RES-06");
			}
			throw e;
		}
	}

	@Override
	public int contarActivasDeJugador(UUID jugadorId, LocalDateTime ahora) {
		// P4: activa = CONFIRMADA y todavía no termina. Usa ix_reserva_jugador_fecha
		Integer activas = jdbc.queryForObject("""
				select count(*) from reserva
				where jugador_id = :jugador
				  and estado = 'CONFIRMADA'
				  and fecha >= cast(:ahora as date)
				  and fecha + hora_fin > :ahora
				""", Map.of("jugador", jugadorId, "ahora", ahora), Integer.class);
		return activas == null ? 0 : activas;
	}

	@Override
	public int horasConfirmadasDeJugadorEnFecha(UUID jugadorId, LocalDate fecha) {
		// hora_fin - hora_inicio es un intervalo; se pasa a horas. Cuenta todas las canchas
		Integer horas = jdbc.queryForObject("""
				select coalesce(sum(extract(epoch from hora_fin - hora_inicio)) / 3600, 0)::int from reserva
				where jugador_id = :jugador and fecha = :fecha and estado = 'CONFIRMADA'
				""", Map.of("jugador", jugadorId, "fecha", fecha), Integer.class);
		return horas == null ? 0 : horas;
	}

	// Hibernate no saca el nombre de una restricción de exclusión, así que se mira el estado SQL
	private static boolean tieneEstado(Throwable error, String estadoSql) {
		for (Throwable causa = error; causa != null; causa = causa.getCause()) {
			if (causa instanceof SQLException sql && estadoSql.equals(sql.getSQLState())) {
				return true;
			}
		}
		return false;
	}

}
