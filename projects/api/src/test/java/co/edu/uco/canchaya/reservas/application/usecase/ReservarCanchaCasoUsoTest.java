package co.edu.uco.canchaya.reservas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionOperations;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.CanchaReservable;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.RangoBloqueado;
import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.application.port.in.ReservarCancha.ReservaConfirmada;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.ConfirmacionAsistencia;
import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.ParametrosReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU21: el caso de uso reúne lo que necesita confirmar dentro del turno de la cancha. */
class ReservarCanchaCasoUsoTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	// Lunes 23 de noviembre de 2026, 7:30 a. m. en Bogotá (D27)
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 7, 30);
	private static final Clock RELOJ = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
	private static final LocalDate HOY = AHORA.toLocalDate();
	private static final LocalDate MANANA = HOY.plusDays(1);
	private static final ParametrosReserva PARAMETROS = new ParametrosReserva(30, 2, 2, 5, 3, 5, LocalTime.NOON, 3,
			24, 250, false);
	private static final Solicitante JUGADOR = new Solicitante(UUID.randomUUID(), Rol.JUGADOR);

	private final UUID canchaId = UUID.randomUUID();
	private ConsultaCanchas canchas;
	private RepositorioReservas reservas;
	private ReservarCanchaCasoUso casoUso;

	@BeforeEach
	void preparar() {
		canchas = mock(ConsultaCanchas.class);
		reservas = mock(RepositorioReservas.class);
		when(canchas.buscarReservable(canchaId)).thenReturn(Optional.of(new CanchaReservable(canchaId,
				UUID.randomUUID(), "Cancha 1", "Canchas La 70", new BigDecimal("80000.00"))));
		horario(MANANA, 6, 22);
		// Sin transacción real: la prueba revisa el orden y las reglas, no la base
		casoUso = new ReservarCanchaCasoUso(canchas, reservas, PARAMETROS, RELOJ,
				TransactionOperations.withoutTransaction());
	}

	private void horario(LocalDate fecha, int desde, int hasta) {
		List<Franja> franjas = new ArrayList<>();
		for (int hora = desde; hora < hasta; hora++) {
			franjas.add(new Franja(fecha, LocalTime.of(hora, 0), LocalTime.of(hora + 1, 0)));
		}
		when(canchas.franjasDelDia(canchaId, fecha)).thenReturn(franjas);
	}

	private Reserva confirmadaDe(int desde, int hasta) {
		return Reserva.reconstruir(UUID.randomUUID(), canchaId, UUID.randomUUID(), MANANA, LocalTime.of(desde, 0),
				LocalTime.of(hasta, 0), new BigDecimal("80000.00"), EstadoReserva.CONFIRMADA,
				ConfirmacionAsistencia.NO_REQUERIDA, null, null, null, null, null);
	}

	private ReservaConfirmada reservar(LocalDate fecha, int desde, int hasta) {
		return casoUso.ejecutar(canchaId, fecha, LocalTime.of(desde, 0), LocalTime.of(hasta, 0), JUGADOR);
	}

	private void esperarErrorSinGuardar(Runnable accion, String codigo) {
		assertThatThrownBy(accion::run).isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje()).isEqualTo(codigo);
		verify(reservas, never()).guardar(any());
	}

	@Test
	void CA1_franjaLibre_seGuardaYDevuelveLosNombres() {
		ReservaConfirmada confirmada = reservar(MANANA, 18, 19);

		verify(reservas).guardar(confirmada.reserva());
		assertThat(confirmada.cancha()).isEqualTo("Cancha 1");
		assertThat(confirmada.escenario()).isEqualTo("Canchas La 70");
		assertThat(confirmada.reserva().getJugadorId()).isEqualTo(JUGADOR.usuarioId());
		assertThat(confirmada.reserva().getPrecio()).isEqualByComparingTo("80000");
	}

	@Test
	void CA3_franjaYaReservada_ERR_RES_06() {
		when(reservas.confirmadasDeCanchaEnFecha(canchaId, MANANA)).thenReturn(List.of(confirmadaDe(18, 19)));

		esperarErrorSinGuardar(() -> reservar(MANANA, 18, 19), "ERR-RES-06");
	}

	@Test
	void CA5_franjaBloqueada_ERR_RES_07() {
		when(canchas.bloqueosDelDia(canchaId, MANANA))
				.thenReturn(List.of(new RangoBloqueado(LocalTime.of(18, 0), LocalTime.of(19, 0))));

		esperarErrorSinGuardar(() -> reservar(MANANA, 18, 19), "ERR-RES-07");
	}

	// Un bloqueo que termina donde empieza la reserva no la cubre (hora de fin abierta)
	@Test
	void CA5_bloqueoContiguo_noCubreLaReserva() {
		when(canchas.bloqueosDelDia(canchaId, MANANA))
				.thenReturn(List.of(new RangoBloqueado(LocalTime.of(16, 0), LocalTime.of(18, 0))));

		assertThat(reservar(MANANA, 18, 19).reserva().getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
	}

	// CA6 y CA7: los límites del jugador salen del repositorio con su id y la hora del servidor
	@Test
	void CA6_CA7_limitesDelJugador_seConsultanConSuIdYLaHoraActual() {
		when(reservas.contarActivasDeJugador(JUGADOR.usuarioId(), AHORA)).thenReturn(5);
		esperarErrorSinGuardar(() -> reservar(MANANA, 18, 19), "ERR-RES-08");

		when(reservas.contarActivasDeJugador(JUGADOR.usuarioId(), AHORA)).thenReturn(0);
		when(reservas.horasConfirmadasDeJugadorEnFecha(JUGADOR.usuarioId(), MANANA)).thenReturn(1);
		esperarErrorSinGuardar(() -> reservar(MANANA, 18, 20), "ERR-RES-09");
	}

	// CA8 y P1: Escenarios responde ERR-HOR-10, pero el jugador ve ERR-RES-02
	@Test
	void CA8_fueraDeVentana_ERR_RES_02() {
		LocalDate lejana = HOY.plusDays(31);
		when(canchas.franjasDelDia(canchaId, lejana)).thenThrow(new ReglaNegocioException("ERR-HOR-10"));

		esperarErrorSinGuardar(() -> reservar(lejana, 18, 19), "ERR-RES-02");
	}

	// Otro error de Escenarios no se cambia
	@Test
	void otroErrorDeFranjas_seDejaPasar() {
		when(canchas.franjasDelDia(canchaId, MANANA)).thenThrow(new ReglaNegocioException("ERR-HOR-09"));

		esperarErrorSinGuardar(() -> reservar(MANANA, 18, 19), "ERR-HOR-09");
	}

	// CA9: cancha inexistente, INACTIVA o con el escenario INACTIVO; no se piden sus franjas
	@Test
	void CA9_canchaNoDisponible_ERR_RES_01() {
		when(canchas.buscarReservable(canchaId)).thenReturn(Optional.empty());

		esperarErrorSinGuardar(() -> reservar(MANANA, 18, 19), "ERR-RES-01");
		verify(canchas, never()).franjasDelDia(any(), any());
	}

	// CA10: el rol se revisa antes que la cancha, aunque la cancha no exista
	@Test
	void CA10_noJugadorConCanchaInexistente_ERR_RES_05() {
		when(canchas.buscarReservable(canchaId)).thenReturn(Optional.empty());
		Solicitante propietario = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarErrorSinGuardar(() -> casoUso.ejecutar(canchaId, MANANA, LocalTime.of(18, 0), LocalTime.of(19, 0),
				propietario), "ERR-RES-05");
	}

	// CA11 y P3: con horarios 06–12 y 12–14, las franjas de 11 y 12 sirven
	@Test
	void CA11_franjasSeguidasDeHorariosContiguos_seConfirma() {
		List<Franja> franjas = new ArrayList<>();
		for (int hora : new int[] { 6, 7, 8, 9, 10, 11, 12, 13 }) {
			franjas.add(new Franja(MANANA, LocalTime.of(hora, 0), LocalTime.of(hora + 1, 0)));
		}
		when(canchas.franjasDelDia(canchaId, MANANA)).thenReturn(franjas);

		ReservaConfirmada confirmada = reservar(MANANA, 11, 13);

		assertThat(confirmada.reserva().horas()).isEqualTo(2);
		assertThat(confirmada.reserva().getPrecio()).isEqualByComparingTo("160000");
	}

	@Test
	void CA13_segundaFranjaNoDisponible_noSeGuardaNinguna() {
		when(reservas.confirmadasDeCanchaEnFecha(canchaId, MANANA)).thenReturn(List.of(confirmadaDe(19, 20)));
		esperarErrorSinGuardar(() -> reservar(MANANA, 18, 20), "ERR-RES-06");

		when(reservas.confirmadasDeCanchaEnFecha(canchaId, MANANA)).thenReturn(List.of());
		when(canchas.bloqueosDelDia(canchaId, MANANA))
				.thenReturn(List.of(new RangoBloqueado(LocalTime.of(19, 0), LocalTime.of(20, 0))));
		esperarErrorSinGuardar(() -> reservar(MANANA, 18, 20), "ERR-RES-07");

		// El horario va hasta las 22:00: la franja de 22 a 23 no existe
		when(canchas.bloqueosDelDia(canchaId, MANANA)).thenReturn(List.of());
		esperarErrorSinGuardar(() -> reservar(MANANA, 21, 23), "ERR-RES-06");
	}

	@Test
	void politica10_idRepetido_reintentaConOtro() {
		when(reservas.existePorId(any())).thenReturn(true, false);

		ReservaConfirmada confirmada = reservar(MANANA, 18, 19);

		ArgumentCaptor<UUID> ids = ArgumentCaptor.forClass(UUID.class);
		verify(reservas, times(2)).existePorId(ids.capture());
		assertThat(ids.getAllValues().get(0)).isNotEqualTo(ids.getAllValues().get(1));
		assertThat(confirmada.reserva().getId()).isEqualTo(ids.getAllValues().get(1));
		verify(reservas).guardar(confirmada.reserva());
	}

	// D29 punto 2 y D36: el turno va antes de cualquier lectura, y guardar al final
	@Test
	void turno_seTomaAntesDeLeerFranjas() {
		reservar(MANANA, 18, 19);

		InOrder orden = inOrder(canchas, reservas);
		orden.verify(canchas).tomarTurno(canchaId);
		orden.verify(canchas).buscarReservable(canchaId);
		orden.verify(canchas).franjasDelDia(canchaId, MANANA);
		orden.verify(reservas).confirmadasDeCanchaEnFecha(canchaId, MANANA);
		orden.verify(canchas).bloqueosDelDia(canchaId, MANANA);
		orden.verify(reservas).contarActivasDeJugador(eq(JUGADOR.usuarioId()), any());
		orden.verify(reservas).guardar(any());
	}

}
