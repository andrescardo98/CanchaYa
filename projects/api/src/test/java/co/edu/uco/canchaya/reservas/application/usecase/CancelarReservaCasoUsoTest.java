package co.edu.uco.canchaya.reservas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.ConfirmacionAsistencia;
import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.OrigenCancelacion;
import co.edu.uco.canchaya.reservas.domain.ParametrosReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * HU24: el caso de uso busca la reserva, la cancela con la hora del servidor y la guarda solo si
 * sigue CONFIRMADA (P3). HU25: al terminar la transacción, avisa la cancelación por correo.
 */
class CancelarReservaCasoUsoTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	// Martes 24 de noviembre de 2026, 10:00 a. m. en Bogotá (D27)
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 24, 10, 0);
	private static final Clock RELOJ = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
	private static final ParametrosReserva PARAMETROS = new ParametrosReserva(30, 2, 2, 5, 3, 5, LocalTime.NOON, 3,
			24, 250, false);
	private static final Solicitante JUGADOR = new Solicitante(UUID.randomUUID(), Rol.JUGADOR);

	private RepositorioReservas reservas;
	private NotificacionCancelacion notificacion;
	private final List<String> eventos = new ArrayList<>();
	private CancelarReservaCasoUso casoUso;

	@BeforeEach
	void preparar() {
		reservas = mock(RepositorioReservas.class);
		notificacion = mock(NotificacionCancelacion.class);
		doAnswer(invocacion -> {
			Reserva avisada = invocacion.getArgument(0);
			eventos.add("notificar " + avisada.getEstado());
			return null;
		}).when(notificacion).notificar(any());
		// Sin transacción real: la prueba revisa que todo pase dentro de ella
		TransactionOperations transaccion = new TransactionOperations() {
			@Override
			public <T> T execute(TransactionCallback<T> accion) {
				T resultado = accion.doInTransaction(null);
				eventos.add("fin de la transacción");
				return resultado;
			}
		};
		casoUso = new CancelarReservaCasoUso(reservas, notificacion, PARAMETROS, RELOJ, transaccion);
	}

	/** Reserva CONFIRMADA de JUGADOR que empieza hoy a las 18:00, 8 horas después de AHORA. */
	private Reserva confirmada() {
		Reserva reserva = Reserva.reconstruir(UUID.randomUUID(), UUID.randomUUID(), JUGADOR.usuarioId(),
				LocalDate.of(2026, 11, 24), LocalTime.of(18, 0), LocalTime.of(19, 0), new BigDecimal("80000.00"),
				EstadoReserva.CONFIRMADA, ConfirmacionAsistencia.NO_REQUERIDA, null, null, null, null, null);
		when(reservas.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));
		return reserva;
	}

	private static void esperarError(Runnable accion, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
	}

	@Test
	void ca1ReservaConfirmada_seCancelaYSeGuarda() {
		Reserva reserva = confirmada();
		when(reservas.cancelarSiSigueConfirmada(any())).thenAnswer(invocacion -> {
			eventos.add("guardar");
			return true;
		});

		Reserva cancelada = casoUso.ejecutar(reserva.getId(), JUGADOR);

		// Política 19 con la hora del reloj del servidor, no la del dispositivo
		assertThat(cancelada.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
		assertThat(cancelada.getOrigenCancelacion()).isEqualTo(OrigenCancelacion.JUGADOR);
		assertThat(cancelada.getFechaCancelacion()).isEqualTo(AHORA.atZone(BOGOTA).toOffsetDateTime());
		verify(reservas).cancelarSiSigueConfirmada(cancelada);
		assertThat(eventos).containsExactly("guardar", "fin de la transacción", "notificar CANCELADA");
	}

	@Test
	void ca4NoExiste_ERR_RES_14() {
		UUID id = UUID.randomUUID();
		when(reservas.buscarPorId(id)).thenReturn(Optional.empty());

		esperarError(() -> casoUso.ejecutar(id, JUGADOR), "ERR-RES-14");
		verify(reservas, never()).cancelarSiSigueConfirmada(any());
	}

	@Test
	void ca4CambiadaAlMismoTiempo_ERR_RES_14() {
		// P3: otra operación la cambió entre la lectura y la escritura, y el update no tocó ninguna fila
		Reserva reserva = confirmada();
		when(reservas.cancelarSiSigueConfirmada(any())).thenReturn(false);

		esperarError(() -> casoUso.ejecutar(reserva.getId(), JUGADOR), "ERR-RES-14");
	}

	@Test
	void ca5OtroUsuario_noSeGuarda() {
		Reserva reserva = confirmada();
		Solicitante otro = new Solicitante(UUID.randomUUID(), Rol.JUGADOR);

		esperarError(() -> casoUso.ejecutar(reserva.getId(), otro), "ERR-RES-15");
		verify(reservas, never()).cancelarSiSigueConfirmada(any());
	}

	@Test
	void ca3TresHorasOMenos_noSeGuarda() {
		// PAR-RES-05 en 10 horas: con 8 de anticipación ya no se puede
		casoUso = new CancelarReservaCasoUso(reservas, notificacion,
				new ParametrosReserva(30, 2, 2, 5, 10, 5, LocalTime.NOON, 3, 24, 250, false), RELOJ,
				TransactionOperations.withoutTransaction());
		Reserva reserva = confirmada();

		esperarError(() -> casoUso.ejecutar(reserva.getId(), JUGADOR), "ERR-RES-16");
		verify(reservas, never()).cancelarSiSigueConfirmada(any());
	}

	// HU25 CA1, D15: el correo sale con la reserva ya CANCELADA y la transacción cerrada. Si
	// fallara dentro, podría revertir la cancelación o avisar una que al final no se guardó
	@Test
	void HU25_CA1_notificaDespuesDeLaTransaccion() {
		Reserva reserva = confirmada();
		when(reservas.cancelarSiSigueConfirmada(any())).thenAnswer(invocacion -> {
			eventos.add("guardar");
			return true;
		});

		Reserva cancelada = casoUso.ejecutar(reserva.getId(), JUGADOR);

		assertThat(eventos).containsExactly("guardar", "fin de la transacción", "notificar CANCELADA");
		verify(notificacion).notificar(cancelada);
	}

	// HU25 CA1: solo se avisa una cancelación que quedó guardada
	@Test
	void HU25_CA1_siNoSeCancela_noSeNotifica() {
		UUID inexistente = UUID.randomUUID();
		when(reservas.buscarPorId(inexistente)).thenReturn(Optional.empty());
		esperarError(() -> casoUso.ejecutar(inexistente, JUGADOR), "ERR-RES-14");

		Reserva ajena = confirmada();
		esperarError(() -> casoUso.ejecutar(ajena.getId(), new Solicitante(UUID.randomUUID(), Rol.JUGADOR)),
				"ERR-RES-15");

		// P3 de HU24: otra operación la cambió al mismo tiempo
		Reserva cambiada = confirmada();
		when(reservas.cancelarSiSigueConfirmada(any())).thenReturn(false);
		esperarError(() -> casoUso.ejecutar(cambiada.getId(), JUGADOR), "ERR-RES-14");

		verify(notificacion, never()).notificar(any());
	}

}
