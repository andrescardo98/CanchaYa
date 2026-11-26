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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import co.edu.uco.canchaya.escenarios.domain.Franja;

/**
 * HU29: qué reservas ocupan una franja de la agenda (CA1) y qué opciones tiene quien la consulta
 * (CA3: cancelar por fuerza mayor y marcar inasistencia).
 */
class ReservaAgendaTest {

	private static final OffsetDateTime CREADA = OffsetDateTime.of(2026, 11, 20, 9, 0, 0, 0, ZoneOffset.ofHours(-5));
	// La reserva es el 25/11/2026 de 18:00 a 20:00
	private static final LocalDate FECHA = LocalDate.of(2026, 11, 25);

	// PAR-RES-09 = 24: la inasistencia se marca hasta 24 horas después del fin
	private static final ParametrosReserva PARAMETROS = new ParametrosReserva(30, 2, 2, 5, 3, 5, LocalTime.NOON, 3,
			24, 250, false);

	private static Reserva reserva(EstadoReserva estado) {
		return Reserva.reconstruir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), FECHA,
				LocalTime.of(18, 0), LocalTime.of(20, 0), new BigDecimal("160000.00"), estado,
				ConfirmacionAsistencia.NO_REQUERIDA, null, null, CREADA, null, null);
	}

	private static Franja franja(LocalDate fecha, int hora) {
		return new Franja(fecha, LocalTime.of(hora, 0), LocalTime.of(hora + 1, 0));
	}

	private static LocalDateTime a(LocalDate fecha, int hora, int minuto) {
		return fecha.atTime(hora, minuto);
	}

	// CA1: una reserva de 2 horas ocupa sus dos franjas; NO_ASISTIO también, porque el horario se usó
	@ParameterizedTest
	@EnumSource(value = EstadoReserva.class, names = { "CONFIRMADA", "NO_ASISTIO" })
	void CA1_ocupa_susFranjas(EstadoReserva estado) {
		assertThat(reserva(estado).ocupa(franja(FECHA, 18))).isTrue();
		assertThat(reserva(estado).ocupa(franja(FECHA, 19))).isTrue();
	}

	// CA1: la hora de fin es abierta, como el tsrange '[)' de la base
	@Test
	void CA1_ocupa_noLasFranjasDeAlLado() {
		assertThat(reserva(EstadoReserva.CONFIRMADA).ocupa(franja(FECHA, 17))).isFalse();
		assertThat(reserva(EstadoReserva.CONFIRMADA).ocupa(franja(FECHA, 20))).isFalse();
		assertThat(reserva(EstadoReserva.CONFIRMADA).ocupa(franja(FECHA.plusDays(1), 18))).isFalse();
	}

	// CA1 y restricción de la HU: las reservas canceladas no ocupan la franja (P4)
	@Test
	void CA1_ocupa_unaCanceladaNoOcupa() {
		assertThat(reserva(EstadoReserva.CANCELADA).ocupa(franja(FECHA, 18))).isFalse();
	}

	// CA3, políticas 15 y 17: el propietario cancela por fuerza mayor mientras la reserva no haya iniciado
	@Test
	void CA3_fuerzaMayor_elPropietarioAntesDelInicio() {
		Reserva confirmada = reserva(EstadoReserva.CONFIRMADA);
		assertThat(confirmada.sePuedeCancelarPorFuerzaMayor(a(FECHA, 17, 59), true)).isTrue();
		assertThat(confirmada.sePuedeCancelarPorFuerzaMayor(a(FECHA.minusDays(3), 8, 0), true)).isTrue();
	}

	// CA3, política 17 (ERR-RES-17): desde la hora de inicio ya no
	@Test
	void CA3_fuerzaMayor_noDesdeQueInicia() {
		Reserva confirmada = reserva(EstadoReserva.CONFIRMADA);
		assertThat(confirmada.sePuedeCancelarPorFuerzaMayor(a(FECHA, 18, 0), true)).isFalse();
		assertThat(confirmada.sePuedeCancelarPorFuerzaMayor(a(FECHA, 21, 0), true)).isFalse();
	}

	// CA3 y D02: el encargado de sede no cancela por fuerza mayor
	@Test
	void CA3_fuerzaMayor_elEncargadoNunca() {
		assertThat(reserva(EstadoReserva.CONFIRMADA).sePuedeCancelarPorFuerzaMayor(a(FECHA, 8, 0), false)).isFalse();
	}

	// CA3, política 14: solo una CONFIRMADA tiene opciones
	@ParameterizedTest
	@EnumSource(value = EstadoReserva.class, names = { "CANCELADA", "NO_ASISTIO" })
	void CA3_sinOpciones_siNoEstaConfirmada(EstadoReserva estado) {
		assertThat(reserva(estado).sePuedeCancelarPorFuerzaMayor(a(FECHA, 8, 0), true)).isFalse();
		assertThat(reserva(estado).sePuedeMarcarInasistencia(a(FECHA, 21, 0), PARAMETROS)).isFalse();
	}

	// CA3, política 22 (ERR-RES-22): la inasistencia se marca desde la hora de fin
	@Test
	void CA3_inasistencia_noAntesDelFin() {
		Reserva confirmada = reserva(EstadoReserva.CONFIRMADA);
		assertThat(confirmada.sePuedeMarcarInasistencia(a(FECHA, 17, 0), PARAMETROS)).isFalse();
		assertThat(confirmada.sePuedeMarcarInasistencia(a(FECHA, 19, 59), PARAMETROS)).isFalse();
	}

	// CA3, políticas 22 y 32 (RN-09, D20, PAR-RES-09): desde el fin y hasta 24 horas después, los dos incluidos
	@ParameterizedTest
	@CsvSource({ "0, 20, 0", "0, 23, 30", "1, 19, 59", "1, 20, 0" })
	void CA3_inasistencia_entreElFinY24HorasDespues(int dias, int hora, int minuto) {
		assertThat(reserva(EstadoReserva.CONFIRMADA)
				.sePuedeMarcarInasistencia(a(FECHA.plusDays(dias), hora, minuto), PARAMETROS)).isTrue();
	}

	// CA3, política 32 (ERR-RES-32): pasadas las 24 horas ya no
	@Test
	void CA3_inasistencia_noDespuesDe24Horas() {
		Reserva confirmada = reserva(EstadoReserva.CONFIRMADA);
		assertThat(confirmada.sePuedeMarcarInasistencia(a(FECHA.plusDays(1), 20, 1), PARAMETROS)).isFalse();
		assertThat(confirmada.sePuedeMarcarInasistencia(a(FECHA.plusDays(3), 10, 0), PARAMETROS)).isFalse();
	}

}
