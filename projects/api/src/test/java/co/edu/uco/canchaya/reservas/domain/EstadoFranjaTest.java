package co.edu.uco.canchaya.reservas.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.escenarios.domain.Franja;

/** HU29, CA1: cada franja de la agenda está libre, reservada o bloqueada. */
class EstadoFranjaTest {

	private static final OffsetDateTime CREADA = OffsetDateTime.of(2026, 11, 20, 9, 0, 0, 0, ZoneOffset.ofHours(-5));
	private static final LocalDate FECHA = LocalDate.of(2026, 11, 25);
	private static final Franja DE_18_A_19 = new Franja(FECHA, LocalTime.of(18, 0), LocalTime.of(19, 0));

	private static Reserva reserva(EstadoReserva estado, int horaInicio, int horaFin) {
		return Reserva.reconstruir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), FECHA,
				LocalTime.of(horaInicio, 0), LocalTime.of(horaFin, 0), new BigDecimal("80000.00"), estado,
				ConfirmacionAsistencia.NO_REQUERIDA, null, null, CREADA, null, null);
	}

	@Test
	void CA1_sinReservaNiBloqueo_esLibre() {
		assertThat(EstadoFranja.de(DE_18_A_19, List.of(reserva(EstadoReserva.CONFIRMADA, 16, 18)), false))
				.isEqualTo(EstadoFranja.LIBRE);
	}

	@Test
	void CA1_conReservaQueLaOcupa_esReservada() {
		assertThat(EstadoFranja.de(DE_18_A_19, List.of(reserva(EstadoReserva.CONFIRMADA, 17, 19)), false))
				.isEqualTo(EstadoFranja.RESERVADA);
	}

	@Test
	void CA1_conBloqueo_esBloqueada() {
		assertThat(EstadoFranja.de(DE_18_A_19, List.of(), true)).isEqualTo(EstadoFranja.BLOQUEADA);
	}

	// CA1 y restricción de la HU: la cancelada no ocupa; la franja sigue libre (P4)
	@Test
	void CA1_soloConUnaCancelada_esLibre() {
		assertThat(EstadoFranja.de(DE_18_A_19, List.of(reserva(EstadoReserva.CANCELADA, 18, 19)), false))
				.isEqualTo(EstadoFranja.LIBRE);
	}

	// HU30 no deja crear un bloqueo encima de una reserva; si pasara, manda la reserva (spec, EstadoFranja)
	@Test
	void CA1_reservadaYBloqueada_esReservada() {
		assertThat(EstadoFranja.de(DE_18_A_19, List.of(reserva(EstadoReserva.CONFIRMADA, 18, 19)), true))
				.isEqualTo(EstadoFranja.RESERVADA);
	}

}
