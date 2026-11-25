package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaReservasFuturas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU12: caso de uso «eliminar» de HorarioRecurrente (política 6, turnos y el orden de las demás). */
class EliminarHorarioCasoUsoTest {

	private static final LocalTime SEIS = LocalTime.of(6, 0);
	private static final LocalTime DOCE = LocalTime.of(12, 0);
	private static final UUID MENOR = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID MAYOR = UUID.fromString("00000000-0000-0000-0000-000000000002");

	private HorariosEnMemoria.Escenarios escenarios;
	private HorariosEnMemoria horarios;
	// Mockito, para revisar con InOrder que los turnos van antes de la consulta
	private RepositorioCanchas canchas;
	private ConsultaReservasFuturas reservas;
	private TransaccionFalsa transaccion;
	private EliminarHorarioCasoUso casoUso;
	private Escenario escenario;
	private Solicitante propietario;
	private HorarioRecurrente lunes;

	@BeforeEach
	void preparar() {
		escenarios = new HorariosEnMemoria.Escenarios();
		horarios = new HorariosEnMemoria();
		canchas = mock(RepositorioCanchas.class);
		reservas = mock(ConsultaReservasFuturas.class);
		transaccion = new TransaccionFalsa();
		casoUso = new EliminarHorarioCasoUso(escenarios, horarios, canchas, reservas, transaccion);

		escenario = Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null, "Calle 80 # 45-10",
				UUID.randomUUID(), new BigDecimal("6.155123"), new BigDecimal("-75.373700"), "+573001234567", null,
				List.of(), UUID.randomUUID(), EstadoEscenario.ACTIVO);
		escenarios.escenarios.add(escenario);
		// Desordenadas a propósito: los turnos se toman por id
		escenarios.canchas.put(escenario.getId(), List.of(MAYOR, MENOR));
		propietario = new Solicitante(escenario.getAdministradorId(), Rol.ADMINISTRADOR_CANCHA);

		lunes = HorarioRecurrente.reconstruir(UUID.randomUUID(), escenario.getId(), DiaSemana.LUNES, SEIS, DOCE);
		horarios.guardar(lunes);
	}

	private void hayReservas(boolean hay) {
		when(reservas.existenConfirmadasFuturasEnHorario(any(), any(), any(), any())).thenReturn(hay);
	}

	// Ningún error borra el horario
	private void esperarError(UUID id, Solicitante solicitante, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(
				() -> casoUso.ejecutar(id, solicitante)).isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
		assertThat(horarios.guardados).containsExactly(lunes);
	}

	// CA1 y D29 punto 2, D36: con los turnos tomados, una reserva de HU21 no entra entre la consulta
	// y el borrado. Siempre en el mismo orden (por id) para evitar un bloqueo mutuo
	@Test
	void ca1Eliminar_tomaTurnosConsultaYBorra() {
		hayReservas(false);

		casoUso.ejecutar(lunes.getId(), propietario);

		InOrder orden = inOrder(canchas, reservas);
		orden.verify(canchas).tomarTurno(MENOR);
		orden.verify(canchas).tomarTurno(MAYOR);
		orden.verify(reservas).existenConfirmadasFuturasEnHorario(List.of(MENOR, MAYOR), DayOfWeek.MONDAY, SEIS,
				DOCE);
		assertThat(horarios.guardados).isEmpty();
		// Todo en una sola transacción: el turno se suelta al terminar el borrado
		assertThat(transaccion.aperturas).isEqualTo(1);
		assertThat(transaccion.abierta).isFalse();
	}

	// Sin canchas no hay reservas que buscar ni turnos que tomar
	@Test
	void ca1EscenarioSinCanchas_borraSinConsultarReservas() {
		escenarios.canchas.remove(escenario.getId());

		casoUso.ejecutar(lunes.getId(), propietario);

		assertThat(horarios.guardados).isEmpty();
		verifyNoInteractions(canchas, reservas);
	}

	// Solo se borra el horario pedido
	@Test
	void ca1Eliminar_noTocaLosOtrosHorarios() {
		hayReservas(false);
		HorarioRecurrente martes = HorarioRecurrente.reconstruir(UUID.randomUUID(), escenario.getId(),
				DiaSemana.MARTES, SEIS, DOCE);
		horarios.guardar(martes);

		casoUso.ejecutar(lunes.getId(), propietario);

		assertThat(horarios.guardados).containsExactly(martes);
	}

	// CA4 / política 6
	@Test
	void ca4HorarioInexistente_ERR_HOR_06() {
		esperarError(UUID.randomUUID(), propietario, "ERR-HOR-06");
		verifyNoInteractions(canchas, reservas);
	}

	// CA5 y P5: el encargado de sede tiene el mismo rol, pero no es el dueño. Sin permiso no toma
	// turnos ni pregunta a Reservas, así no detiene las reservas de nadie
	@Test
	void ca5NoPropietario_noBorraNiTomaTurnos() {
		Solicitante encargado = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(lunes.getId(), encargado, "ERR-HOR-02");
		verifyNoInteractions(canchas, reservas);
	}

	// CA6 / política 15
	@Test
	void ca6ConReservasFuturas_noBorra() {
		hayReservas(true);

		esperarError(lunes.getId(), propietario, "ERR-HOR-15");
	}

	private static final class TransaccionFalsa implements TransactionOperations {

		boolean abierta;
		int aperturas;

		@Override
		public <T> T execute(TransactionCallback<T> accion) {
			aperturas++;
			abierta = true;
			try {
				return accion.doInTransaction(null);
			} finally {
				abierta = false;
			}
		}

	}

}
