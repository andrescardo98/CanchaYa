package co.edu.uco.canchaya.reservas.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** HU23: qué reservas son vigentes (P1) y cuáles puede cancelar el jugador (CA3, política 16). */
class ReservaConsultaTest {

	private static final OffsetDateTime CREADA = OffsetDateTime.of(2026, 11, 20, 9, 0, 0, 0, ZoneOffset.ofHours(-5));
	// La reserva es el 25/11/2026 de 18:00 a 20:00
	private static final LocalDate FECHA = LocalDate.of(2026, 11, 25);

	// PAR-RES-05 = 3: el jugador cancela si faltan más de 3 horas
	private static final ParametrosReserva PARAMETROS = new ParametrosReserva(30, 2, 2, 5, 3, 5, LocalTime.NOON, 3,
			24, 250, false);

	private static Reserva reserva(EstadoReserva estado) {
		return Reserva.reconstruir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), FECHA,
				LocalTime.of(18, 0), LocalTime.of(20, 0), new BigDecimal("160000.00"), estado,
				ConfirmacionAsistencia.NO_REQUERIDA, null, null, CREADA, null, null);
	}

	private static LocalDateTime a(int hora, int minuto) {
		return FECHA.atTime(hora, minuto);
	}

	// CA1 y P1: una CONFIRMADA que aún no empieza es vigente
	@Test
	void CA1_esVigente_confirmadaQueNoHaEmpezado() {
		assertThat(reserva(EstadoReserva.CONFIRMADA).esVigente(a(10, 0))).isTrue();
	}

	// CA1 y P1: una CONFIRMADA en curso (empezó y no ha terminado) sigue vigente
	@Test
	void CA1_esVigente_confirmadaEnCurso() {
		assertThat(reserva(EstadoReserva.CONFIRMADA).esVigente(a(19, 30))).isTrue();
	}

	// CA1 y P1: al llegar a la hora de fin ya es pasada
	@Test
	void CA1_esVigente_confirmadaTerminada_esPasada() {
		assertThat(reserva(EstadoReserva.CONFIRMADA).esVigente(a(20, 0))).isFalse();
		assertThat(reserva(EstadoReserva.CONFIRMADA).esVigente(a(21, 0))).isFalse();
	}

	// CA1 y P1: una CANCELADA o NO_ASISTIO es pasada aunque su fecha sea futura
	@ParameterizedTest
	@EnumSource(value = EstadoReserva.class, names = { "CANCELADA", "NO_ASISTIO" })
	void CA1_esVigente_noConfirmada_esPasada(EstadoReserva estado) {
		assertThat(reserva(estado).esVigente(a(10, 0))).isFalse();
	}

	// CA3 y política 16: faltan 3 horas y 1 minuto
	@Test
	void CA3_faltanMasDeTresHoras_sePuedeCancelar() {
		assertThat(reserva(EstadoReserva.CONFIRMADA).sePuedeCancelarPorJugador(a(14, 59), PARAMETROS)).isTrue();
		assertThat(reserva(EstadoReserva.CONFIRMADA).sePuedeCancelarPorJugador(a(8, 0), PARAMETROS)).isTrue();
	}

	// CA3 y PAR-RES-05: el límite es estricto; con exactamente 3 horas o menos ya no se puede (D06)
	@Test
	void CA3_faltanTresHorasOMenos_noSePuedeCancelar() {
		Reserva confirmada = reserva(EstadoReserva.CONFIRMADA);
		assertThat(confirmada.sePuedeCancelarPorJugador(a(15, 0), PARAMETROS)).isFalse();
		assertThat(confirmada.sePuedeCancelarPorJugador(a(15, 1), PARAMETROS)).isFalse();
		assertThat(confirmada.sePuedeCancelarPorJugador(a(18, 30), PARAMETROS)).isFalse();
	}

	// CA3: solo una CONFIRMADA se puede cancelar (política 14)
	@ParameterizedTest
	@EnumSource(value = EstadoReserva.class, names = { "CANCELADA", "NO_ASISTIO" })
	void CA3_noConfirmada_noSePuedeCancelar(EstadoReserva estado) {
		assertThat(reserva(estado).sePuedeCancelarPorJugador(a(8, 0), PARAMETROS)).isFalse();
	}

	// CA3: el límite sale de PAR-RES-05, no está fijo en el código
	@Test
	void CA3_usaElParametroDeAntelacion() {
		ParametrosReserva cincoHoras = new ParametrosReserva(30, 2, 2, 5, 5, 5, LocalTime.NOON, 3, 24, 250, false);
		assertThat(reserva(EstadoReserva.CONFIRMADA).sePuedeCancelarPorJugador(a(14, 0), cincoHoras)).isFalse();
		assertThat(reserva(EstadoReserva.CONFIRMADA).sePuedeCancelarPorJugador(a(12, 59), cincoHoras)).isTrue();
	}

}
