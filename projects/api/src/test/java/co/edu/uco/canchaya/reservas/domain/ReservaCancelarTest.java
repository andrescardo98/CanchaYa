package co.edu.uco.canchaya.reservas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU24: responsabilidad «cancelar» de Reserva cuando cancela el jugador (políticas 14, 15, 16 y 19). */
class ReservaCancelarTest {

	/** La reserva empieza el martes 24 de noviembre de 2026 a las 18:00 en Bogotá. */
	private static final LocalDate FECHA = LocalDate.of(2026, 11, 24);
	private static final LocalDateTime INICIO = FECHA.atTime(18, 0);
	private static final UUID JUGADOR_ID = UUID.randomUUID();
	private static final Solicitante JUGADOR = new Solicitante(JUGADOR_ID, Rol.JUGADOR);

	// PAR-RES-05 en 3 horas
	private static final ParametrosReserva PARAMETROS = new ParametrosReserva(30, 2, 2, 5, 3, 5, LocalTime.NOON, 3,
			24, 250, false);

	private static Reserva reserva(EstadoReserva estado) {
		OrigenCancelacion origen = estado == EstadoReserva.CANCELADA ? OrigenCancelacion.SISTEMA_SIN_CONFIRMACION
				: null;
		return Reserva.reconstruir(UUID.randomUUID(), UUID.randomUUID(), JUGADOR_ID, FECHA, LocalTime.of(18, 0),
				LocalTime.of(19, 0), new BigDecimal("80000.00"), estado, ConfirmacionAsistencia.NO_REQUERIDA, origen,
				null, INICIO.minusDays(2).atZone(ZoneId.of("America/Bogota")).toOffsetDateTime(), null, null);
	}

	private static Reserva confirmada() {
		return reserva(EstadoReserva.CONFIRMADA);
	}

	private static void esperarError(Reserva reserva, Solicitante solicitante, LocalDateTime ahora,
			ParametrosReserva parametros, String codigo) {
		EstadoReserva antes = reserva.getEstado();
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(
				() -> reserva.cancelar(solicitante, ahora, parametros)).isInstanceOf(ReglaNegocioException.class)
				.actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
		// Si falla una regla, la reserva no cambia
		assertThat(reserva.getEstado()).isEqualTo(antes);
		assertThat(reserva.getFechaCancelacion()).isNull();
	}

	@Test
	void ca1MasDeTresHoras_quedaCanceladaPorElJugador() {
		Reserva reserva = confirmada();
		LocalDateTime ahora = INICIO.minusHours(3).minusMinutes(1);

		reserva.cancelar(JUGADOR, ahora, PARAMETROS);

		// Política 19: estado, origen y fecha de cancelación en Bogotá (D27); sin motivo
		assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
		assertThat(reserva.estaConfirmada()).isFalse();
		assertThat(reserva.getOrigenCancelacion()).isEqualTo(OrigenCancelacion.JUGADOR);
		assertThat(reserva.getFechaCancelacion())
				.isEqualTo(ahora.atZone(ZoneId.of("America/Bogota")).toOffsetDateTime());
		assertThat(reserva.getMotivoCancelacion()).isNull();
	}

	// Exactamente 3 horas, 2 horas y 59 minutos, en el momento en que empieza y ya empezada
	@ParameterizedTest
	@ValueSource(ints = { 180, 179, 0, -30 })
	void ca3TresHorasOMenos_ERR_RES_16(int minutosAntesDelInicio) {
		esperarError(confirmada(), JUGADOR, INICIO.minusMinutes(minutosAntesDelInicio), PARAMETROS, "ERR-RES-16");
	}

	@Test
	void ca3ElLimiteSaleDelParametro() {
		// PAR-RES-05 en 5 horas: faltando 4 ya no se puede
		ParametrosReserva cincoHoras = new ParametrosReserva(30, 2, 2, 5, 5, 5, LocalTime.NOON, 3, 24, 250, false);
		esperarError(confirmada(), JUGADOR, INICIO.minusHours(4), cincoHoras, "ERR-RES-16");
	}

	@ParameterizedTest
	@EnumSource(value = EstadoReserva.class, names = { "CANCELADA", "NO_ASISTIO" })
	void ca4NoConfirmada_ERR_RES_14(EstadoReserva estado) {
		Reserva reserva = reserva(estado);
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(
				() -> reserva.cancelar(JUGADOR, INICIO.minusDays(1), PARAMETROS))
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo("ERR-RES-14");
		assertThat(reserva.getEstado()).isEqualTo(estado);
	}

	// Otro jugador y un administrador de cancha (el propietario cancela por HU32)
	@ParameterizedTest
	@EnumSource(value = Rol.class, names = { "JUGADOR", "ADMINISTRADOR_CANCHA" })
	void ca5OtroUsuario_ERR_RES_15(Rol rol) {
		Solicitante otro = new Solicitante(UUID.randomUUID(), rol);
		esperarError(confirmada(), otro, INICIO.minusDays(1), PARAMETROS, "ERR-RES-15");
	}

	@Test
	void ca5OtroUsuarioConMenosDeTresHoras_ERR_RES_15() {
		// La política 15 va antes que la 16: a quien no es el jugador no se le habla del plazo
		Solicitante otro = new Solicitante(UUID.randomUUID(), Rol.JUGADOR);
		esperarError(confirmada(), otro, INICIO.minusHours(1), PARAMETROS, "ERR-RES-15");
	}

}
