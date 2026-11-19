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

/** HU20: una reserva guardada se vuelve a armar tal cual y dice cuándo empieza, cuándo termina y cuántas horas cubre. */
class ReservaTest {

	private static final OffsetDateTime CREADA = OffsetDateTime.of(2026, 11, 18, 9, 15, 0, 0, ZoneOffset.ofHours(-5));

	private static Reserva guardada(String inicio, String fin, EstadoReserva estado) {
		return Reserva.reconstruir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 11, 23),
				LocalTime.parse(inicio), LocalTime.parse(fin), new BigDecimal("160000.00"), estado,
				ConfirmacionAsistencia.NO_REQUERIDA, null, null, CREADA, null, null);
	}

	@Test
	void reconstruirConservaTodosLosDatos() {
		UUID id = UUID.randomUUID();
		UUID canchaId = UUID.randomUUID();
		UUID jugadorId = UUID.randomUUID();
		OffsetDateTime cancelada = CREADA.plusHours(2);
		OffsetDateTime solicitud = CREADA.plusHours(1);

		Reserva reserva = Reserva.reconstruir(id, canchaId, jugadorId, LocalDate.of(2026, 11, 23),
				LocalTime.of(18, 0), LocalTime.of(20, 0), new BigDecimal("160000.00"), EstadoReserva.CANCELADA,
				ConfirmacionAsistencia.PENDIENTE, OrigenCancelacion.ADMINISTRADOR, "Tormenta eléctrica", CREADA,
				cancelada, solicitud);

		assertThat(reserva.getId()).isEqualTo(id);
		assertThat(reserva.getCanchaId()).isEqualTo(canchaId);
		assertThat(reserva.getJugadorId()).isEqualTo(jugadorId);
		assertThat(reserva.getFecha()).isEqualTo(LocalDate.of(2026, 11, 23));
		assertThat(reserva.getHoraInicio()).isEqualTo(LocalTime.of(18, 0));
		assertThat(reserva.getHoraFin()).isEqualTo(LocalTime.of(20, 0));
		assertThat(reserva.getPrecio()).isEqualByComparingTo("160000");
		assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
		assertThat(reserva.getConfirmacionAsistencia()).isEqualTo(ConfirmacionAsistencia.PENDIENTE);
		assertThat(reserva.getOrigenCancelacion()).isEqualTo(OrigenCancelacion.ADMINISTRADOR);
		assertThat(reserva.getMotivoCancelacion()).isEqualTo("Tormenta eléctrica");
		assertThat(reserva.getFechaCreacion()).isEqualTo(CREADA);
		assertThat(reserva.getFechaCancelacion()).isEqualTo(cancelada);
		assertThat(reserva.getFechaSolicitudConfirmacion()).isEqualTo(solicitud);
	}

	// CA2: una reserva de 2 horas cubre dos franjas (D10)
	@Test
	void ca2InicioFinYHorasDeUnaReservaDeDosHoras() {
		Reserva reserva = guardada("18:00", "20:00", EstadoReserva.CONFIRMADA);

		assertThat(reserva.inicio()).isEqualTo(LocalDateTime.of(2026, 11, 23, 18, 0));
		assertThat(reserva.fin()).isEqualTo(LocalDateTime.of(2026, 11, 23, 20, 0));
		assertThat(reserva.horas()).isEqualTo(2);
		assertThat(guardada("18:00", "19:00", EstadoReserva.CONFIRMADA).horas()).isEqualTo(1);
	}

	// CA2: solo una reserva CONFIRMADA ocupa la franja
	@ParameterizedTest
	@EnumSource(EstadoReserva.class)
	void ca2SoloLaConfirmadaEstaConfirmada(EstadoReserva estado) {
		assertThat(guardada("18:00", "19:00", estado).estaConfirmada()).isEqualTo(estado == EstadoReserva.CONFIRMADA);
	}

}
