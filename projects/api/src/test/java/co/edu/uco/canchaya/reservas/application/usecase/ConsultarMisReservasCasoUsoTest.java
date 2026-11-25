package co.edu.uco.canchaya.reservas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.NombresCancha;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarMisReservas.MisReservas;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarMisReservas.ReservaDelJugador;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.ConfirmacionAsistencia;
import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.OrigenCancelacion;
import co.edu.uco.canchaya.reservas.domain.ParametrosReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU23: el caso de uso separa, ordena y completa las reservas del jugador del token. */
class ConsultarMisReservasCasoUsoTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	// Martes 24 de noviembre de 2026, 2:00 p. m. en Bogotá (D27)
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 24, 14, 0);
	private static final Clock RELOJ = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
	private static final LocalDate HOY = AHORA.toLocalDate();
	private static final ParametrosReserva PARAMETROS = new ParametrosReserva(30, 2, 2, 5, 3, 5, LocalTime.NOON, 3,
			24, 250, false);
	private static final OffsetDateTime CREADA = OffsetDateTime.of(2026, 11, 20, 9, 0, 0, 0,
			BOGOTA.getRules().getOffset(AHORA));

	private final Solicitante jugador = new Solicitante(UUID.randomUUID(), Rol.JUGADOR);
	private final UUID cancha1 = UUID.randomUUID();
	private final UUID cancha2 = UUID.randomUUID();
	private RepositorioReservas reservas;
	private ConsultaCanchas canchas;
	private ConsultarMisReservasCasoUso casoUso;

	@BeforeEach
	void preparar() {
		reservas = mock(RepositorioReservas.class);
		canchas = mock(ConsultaCanchas.class);
		when(canchas.nombres(any())).thenReturn(Map.of(
				cancha1, new NombresCancha("Cancha 1", "Canchas La 70"),
				cancha2, new NombresCancha("Cancha 2", "Canchas La 80")));
		casoUso = new ConsultarMisReservasCasoUso(reservas, canchas, PARAMETROS, RELOJ);
	}

	private Reserva reserva(UUID canchaId, LocalDate fecha, int desde, int hasta, EstadoReserva estado) {
		return Reserva.reconstruir(UUID.randomUUID(), canchaId, jugador.usuarioId(), fecha, LocalTime.of(desde, 0),
				LocalTime.of(hasta, 0), new BigDecimal("80000.00").multiply(BigDecimal.valueOf(hasta - desde)),
				estado, ConfirmacionAsistencia.NO_REQUERIDA,
				estado == EstadoReserva.CANCELADA ? OrigenCancelacion.ADMINISTRADOR : null,
				estado == EstadoReserva.CANCELADA ? "Tormenta eléctrica" : null, CREADA, null, null);
	}

	private static List<UUID> ids(List<ReservaDelJugador> lista) {
		return lista.stream().map(r -> r.reserva().getId()).toList();
	}

	// CA1 y P1: vigentes de la más próxima a la más lejana; pasadas de la más reciente a la más antigua
	@Test
	void CA1_separaVigentesYPasadas_yLasOrdena() {
		Reserva lejana = reserva(cancha1, HOY.plusDays(5), 18, 19, EstadoReserva.CONFIRMADA);
		Reserva enCurso = reserva(cancha2, HOY, 13, 15, EstadoReserva.CONFIRMADA);
		Reserva proxima = reserva(cancha1, HOY.plusDays(1), 8, 9, EstadoReserva.CONFIRMADA);
		Reserva ayer = reserva(cancha1, HOY.minusDays(1), 20, 21, EstadoReserva.CONFIRMADA);
		Reserva canceladaDeManana = reserva(cancha2, HOY.plusDays(1), 19, 20, EstadoReserva.CANCELADA);
		Reserva antigua = reserva(cancha2, HOY.minusDays(10), 18, 19, EstadoReserva.NO_ASISTIO);
		when(reservas.deJugador(jugador.usuarioId()))
				.thenReturn(List.of(lejana, ayer, enCurso, antigua, proxima, canceladaDeManana));

		MisReservas mis = casoUso.ejecutar(jugador);

		assertThat(ids(mis.vigentes())).containsExactly(enCurso.getId(), proxima.getId(), lejana.getId());
		assertThat(ids(mis.pasadas())).containsExactly(canceladaDeManana.getId(), ayer.getId(), antigua.getId());
		assertThat(mis.vacia()).isFalse();
	}

	// CA1: cada reserva trae el nombre de su cancha y de su escenario, pedidos en una sola llamada
	@Test
	void CA1_completaLosNombresDeCanchaYEscenario() {
		when(reservas.deJugador(jugador.usuarioId())).thenReturn(List.of(
				reserva(cancha1, HOY.plusDays(1), 8, 9, EstadoReserva.CONFIRMADA),
				reserva(cancha2, HOY.minusDays(1), 8, 9, EstadoReserva.CONFIRMADA),
				reserva(cancha1, HOY.minusDays(2), 8, 9, EstadoReserva.CONFIRMADA)));

		MisReservas mis = casoUso.ejecutar(jugador);

		verify(canchas).nombres(Set.of(cancha1, cancha2));
		assertThat(mis.vigentes().getFirst().cancha()).isEqualTo("Cancha 1");
		assertThat(mis.vigentes().getFirst().escenario()).isEqualTo("Canchas La 70");
		assertThat(mis.pasadas().getFirst().cancha()).isEqualTo("Cancha 2");
		assertThat(mis.pasadas().getFirst().escenario()).isEqualTo("Canchas La 80");
	}

	// CA2: sin reservas, las dos listas vacías
	@Test
	void CA2_sinReservas_quedaVacia() {
		when(reservas.deJugador(jugador.usuarioId())).thenReturn(List.of());

		MisReservas mis = casoUso.ejecutar(jugador);

		assertThat(mis.vigentes()).isEmpty();
		assertThat(mis.pasadas()).isEmpty();
		assertThat(mis.vacia()).isTrue();
	}

	// CA3: con el reloj fijo a las 14:00, la de las 18:00 (faltan 4 horas) sí se puede
	// cancelar y la de las 17:00 (exactamente 3 horas) no
	@Test
	void CA3_marcaCancelableConElReloj() {
		Reserva aLasSeis = reserva(cancha1, HOY, 18, 19, EstadoReserva.CONFIRMADA);
		Reserva aLasCinco = reserva(cancha2, HOY, 17, 18, EstadoReserva.CONFIRMADA);
		when(reservas.deJugador(jugador.usuarioId())).thenReturn(List.of(aLasSeis, aLasCinco));

		MisReservas mis = casoUso.ejecutar(jugador);

		assertThat(mis.vigentes()).extracting(r -> r.reserva().getId(), ReservaDelJugador::cancelable)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(aLasCinco.getId(), false),
						org.assertj.core.groups.Tuple.tuple(aLasSeis.getId(), true));
	}

	// CA5: se consulta con el id del token, nunca con otro
	@Test
	void CA5_consultaConElIdDelSolicitante() {
		when(reservas.deJugador(any())).thenReturn(List.of());

		casoUso.ejecutar(jugador);

		verify(reservas).deJugador(jugador.usuarioId());
	}

	// P2: solo el JUGADOR consulta sus reservas; los demás reciben ERR-GEN-06 sin tocar la base
	@ParameterizedTest
	@EnumSource(value = Rol.class, names = "JUGADOR", mode = EnumSource.Mode.EXCLUDE)
	void P2_noJugador_ERR_GEN_06(Rol rol) {
		Solicitante otro = new Solicitante(UUID.randomUUID(), rol);

		assertThatThrownBy(() -> casoUso.ejecutar(otro))
				.isInstanceOfSatisfying(ReglaNegocioException.class,
						e -> assertThat(e.getCodigoMensaje()).isEqualTo("ERR-GEN-06"));
		verifyNoInteractions(reservas, canchas);
	}

}
