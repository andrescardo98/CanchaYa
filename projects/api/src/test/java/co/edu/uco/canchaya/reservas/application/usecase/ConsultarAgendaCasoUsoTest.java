package co.edu.uco.canchaya.reservas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios.BloqueoAgenda;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios.CanchaAgenda;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios.DatosAgenda;
import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios.ContactoUsuario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarAgenda.Agenda;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarAgenda.CanchaDeAgenda;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarAgenda.DiaDeAgenda;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarAgenda.FranjaDeAgenda;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarAgenda.ReservaDeAgenda;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.ConfirmacionAsistencia;
import co.edu.uco.canchaya.reservas.domain.EstadoFranja;
import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.OrigenCancelacion;
import co.edu.uco.canchaya.reservas.domain.ParametrosReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.reservas.domain.VistaAgenda;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU29: el caso de uso arma la agenda con lo de Escenarios, las reservas y los datos del jugador. */
class ConsultarAgendaCasoUsoTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	// Miércoles 25 de noviembre de 2026, 10:00 a. m. en Bogotá (D27)
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 25, 10, 0);
	private static final Clock RELOJ = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
	private static final LocalDate HOY = AHORA.toLocalDate();
	private static final LocalDate LUNES = HOY.minusDays(2);
	private static final LocalDate DOMINGO = HOY.plusDays(4);
	private static final ParametrosReserva PARAMETROS = new ParametrosReserva(30, 2, 2, 5, 3, 5, LocalTime.NOON, 3,
			24, 250, false);
	private static final OffsetDateTime CREADA = OffsetDateTime.of(2026, 11, 20, 9, 0, 0, 0,
			BOGOTA.getRules().getOffset(AHORA));

	private final UUID escenarioId = UUID.randomUUID();
	private final UUID cancha1 = UUID.randomUUID();
	private final UUID cancha2 = UUID.randomUUID();
	private final UUID ana = UUID.randomUUID();
	private final Solicitante propietario = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

	private ConsultaEscenarios escenarios;
	private RepositorioReservas reservas;
	private ConsultaUsuarios usuarios;
	private ConsultarAgendaCasoUso casoUso;
	private final List<BloqueoAgenda> bloqueos = new ArrayList<>();

	@BeforeEach
	void preparar() {
		escenarios = mock(ConsultaEscenarios.class);
		reservas = mock(RepositorioReservas.class);
		usuarios = mock(ConsultaUsuarios.class);
		when(usuarios.buscarContactos(any()))
				.thenReturn(List.of(new ContactoUsuario(ana, "Ana Gómez", "ana@correo.com", "+573001112233")));
		casoUso = new ConsultarAgendaCasoUso(escenarios, reservas, usuarios, PARAMETROS, RELOJ);
	}

	/** Lo que entrega Escenarios: dos canchas y franjas de 17:00 a 21:00 los miércoles. */
	private void escenarioCon(boolean esPropietario, LocalDate desde, LocalDate hasta) {
		Map<LocalDate, List<Franja>> franjas = new LinkedHashMap<>();
		for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
			List<Franja> delDia = new ArrayList<>();
			if (fecha.equals(HOY)) {
				for (int hora = 17; hora < 21; hora++) {
					delDia.add(new Franja(fecha, LocalTime.of(hora, 0), LocalTime.of(hora + 1, 0)));
				}
			}
			franjas.put(fecha, delDia);
		}
		when(escenarios.datosAgenda(eq(escenarioId), any(), eq(desde), eq(hasta))).thenReturn(new DatosAgenda(
				escenarioId, "Canchas La 70", esPropietario,
				List.of(new CanchaAgenda(cancha1, "Cancha 1", "DISPONIBLE"),
						new CanchaAgenda(cancha2, "Cancha 2", "INACTIVA")),
				franjas, bloqueos));
	}

	private Reserva reserva(UUID canchaId, LocalDate fecha, int desde, int hasta, EstadoReserva estado) {
		return Reserva.reconstruir(UUID.randomUUID(), canchaId, ana, fecha, LocalTime.of(desde, 0),
				LocalTime.of(hasta, 0), new BigDecimal("80000.00"), estado, ConfirmacionAsistencia.NO_REQUERIDA,
				estado == EstadoReserva.CANCELADA ? OrigenCancelacion.JUGADOR : null, null, CREADA, null, null);
	}

	private BloqueoAgenda bloqueo(UUID canchaId, LocalDate fecha, int desde, int hasta) {
		BloqueoAgenda bloqueo = new BloqueoAgenda(UUID.randomUUID(), canchaId, fecha, LocalTime.of(desde, 0),
				LocalTime.of(hasta, 0), "MANTENIMIENTO", "Cambio de malla");
		bloqueos.add(bloqueo);
		return bloqueo;
	}

	private static DiaDeAgenda dia(Agenda agenda, UUID canchaId, LocalDate fecha) {
		return agenda.canchas().stream().filter(c -> c.id().equals(canchaId)).findFirst().orElseThrow().dias()
				.stream().filter(d -> d.fecha().equals(fecha)).findFirst().orElseThrow();
	}

	// CA1: todas las canchas, con cada franja libre, reservada o bloqueada; la cancelada no ocupa (P4)
	@Test
	void ca1VistaPorDiaConFranjasLibresReservadasYBloqueadas() {
		escenarioCon(true, HOY, HOY);
		Reserva confirmada = reserva(cancha1, HOY, 18, 20, EstadoReserva.CONFIRMADA);
		Reserva cancelada = reserva(cancha1, HOY, 17, 18, EstadoReserva.CANCELADA);
		when(reservas.deCanchasEntre(anyCollection(), eq(HOY), eq(HOY))).thenReturn(List.of(cancelada, confirmada));
		BloqueoAgenda mantenimiento = bloqueo(cancha1, HOY, 20, 21);

		Agenda agenda = casoUso.ejecutar(escenarioId, HOY, VistaAgenda.DIA, propietario);

		assertThat(agenda.escenario()).isEqualTo("Canchas La 70");
		assertThat(agenda.desde()).isEqualTo(HOY);
		assertThat(agenda.hasta()).isEqualTo(HOY);
		assertThat(agenda.canchas()).extracting(CanchaDeAgenda::nombre, CanchaDeAgenda::estado)
				.containsExactly(tuple("Cancha 1", "DISPONIBLE"), tuple("Cancha 2", "INACTIVA"));
		DiaDeAgenda cancha1Hoy = dia(agenda, cancha1, HOY);
		assertThat(cancha1Hoy.franjas())
				.extracting(f -> f.franja().horaInicio().getHour(), FranjaDeAgenda::estado, FranjaDeAgenda::reservaId,
						FranjaDeAgenda::bloqueoId)
				.containsExactly(tuple(17, EstadoFranja.LIBRE, null, null),
						tuple(18, EstadoFranja.RESERVADA, confirmada.getId(), null),
						tuple(19, EstadoFranja.RESERVADA, confirmada.getId(), null),
						tuple(20, EstadoFranja.BLOQUEADA, null, mantenimiento.id()));
		assertThat(cancha1Hoy.reservas()).extracting(r -> r.reserva().getId())
				.containsExactly(cancelada.getId(), confirmada.getId());
		assertThat(cancha1Hoy.bloqueos()).containsExactly(mantenimiento);
		// La otra cancha tiene las mismas franjas, todas libres
		assertThat(dia(agenda, cancha2, HOY).franjas()).extracting(FranjaDeAgenda::estado)
				.containsOnly(EstadoFranja.LIBRE).hasSize(4);
	}

	// CA1 y ESC-USA-04: sin fecha ni vista, la agenda del día de hoy
	@Test
	void ca1SinFechaNiVistaEsHoy() {
		escenarioCon(true, HOY, HOY);

		Agenda agenda = casoUso.ejecutar(escenarioId, null, null, propietario);

		assertThat(agenda.desde()).isEqualTo(HOY);
		assertThat(agenda.hasta()).isEqualTo(HOY);
	}

	// CA2: los siete días de cada cancha, con las reservas y los bloqueos en su día y su cancha
	@Test
	void ca2VistaPorSemanaConSieteDias() {
		escenarioCon(true, LUNES, DOMINGO);
		Reserva delViernes = reserva(cancha2, LUNES.plusDays(4), 8, 9, EstadoReserva.CONFIRMADA);
		when(reservas.deCanchasEntre(anyCollection(), eq(LUNES), eq(DOMINGO))).thenReturn(List.of(delViernes));
		BloqueoAgenda delLunes = bloqueo(cancha1, LUNES, 6, 8);

		Agenda agenda = casoUso.ejecutar(escenarioId, HOY, VistaAgenda.SEMANA, propietario);

		assertThat(agenda.desde()).isEqualTo(LUNES);
		assertThat(agenda.hasta()).isEqualTo(DOMINGO);
		for (CanchaDeAgenda cancha : agenda.canchas()) {
			assertThat(cancha.dias()).extracting(DiaDeAgenda::fecha).hasSize(7).startsWith(LUNES).endsWith(DOMINGO);
		}
		assertThat(dia(agenda, cancha2, LUNES.plusDays(4)).reservas()).extracting(r -> r.reserva().getId())
				.containsExactly(delViernes.getId());
		assertThat(dia(agenda, cancha1, LUNES).bloqueos()).containsExactly(delLunes);
		assertThat(dia(agenda, cancha1, LUNES.plusDays(4)).reservas()).isEmpty();
		assertThat(dia(agenda, cancha2, LUNES).bloqueos()).isEmpty();
	}

	// CA3: nombre y celular del jugador, confirmación de asistencia y las opciones del propietario
	@Test
	void ca3DatosDelJugadorYOpcionesDelPropietario() {
		escenarioCon(true, LUNES, DOMINGO);
		Reserva deHoy = reserva(cancha1, HOY, 18, 20, EstadoReserva.CONFIRMADA);
		Reserva deAnoche = reserva(cancha1, HOY.minusDays(1), 18, 20, EstadoReserva.CONFIRMADA);
		when(reservas.deCanchasEntre(anyCollection(), eq(LUNES), eq(DOMINGO))).thenReturn(List.of(deAnoche, deHoy));

		Agenda agenda = casoUso.ejecutar(escenarioId, HOY, VistaAgenda.SEMANA, propietario);

		ReservaDeAgenda hoy = dia(agenda, cancha1, HOY).reservas().get(0);
		assertThat(hoy.jugador()).isEqualTo("Ana Gómez");
		assertThat(hoy.celularJugador()).isEqualTo("+573001112233");
		assertThat(hoy.reserva().getConfirmacionAsistencia()).isEqualTo(ConfirmacionAsistencia.NO_REQUERIDA);
		// Aún no inicia: se puede cancelar por fuerza mayor; todavía no se puede marcar inasistencia
		assertThat(hoy.puedeCancelarPorFuerzaMayor()).isTrue();
		assertThat(hoy.puedeMarcarInasistencia()).isFalse();
		// Terminó anoche a las 20:00: dentro de las 24 horas se puede marcar la inasistencia
		ReservaDeAgenda anoche = dia(agenda, cancha1, HOY.minusDays(1)).reservas().get(0);
		assertThat(anoche.puedeCancelarPorFuerzaMayor()).isFalse();
		assertThat(anoche.puedeMarcarInasistencia()).isTrue();
		// Los jugadores se buscan en una sola llamada
		verify(usuarios, times(1)).buscarContactos(any());
	}

	// CA3 y D02: al encargado nunca le sale la cancelación por fuerza mayor
	@Test
	void ca3ElEncargadoNoCancelaPorFuerzaMayor() {
		escenarioCon(false, HOY, HOY);
		when(reservas.deCanchasEntre(anyCollection(), eq(HOY), eq(HOY)))
				.thenReturn(List.of(reserva(cancha1, HOY, 18, 20, EstadoReserva.CONFIRMADA)));

		Agenda agenda = casoUso.ejecutar(escenarioId, HOY, VistaAgenda.DIA,
				new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA));

		assertThat(agenda.propietario()).isFalse();
		assertThat(dia(agenda, cancha1, HOY).reservas().get(0).puedeCancelarPorFuerzaMayor()).isFalse();
	}

	// CA6: si Escenarios niega el permiso, no se lee ninguna reserva ni ningún dato de jugador
	@Test
	void ca6SinPermisoNoSeLeeNada() {
		when(escenarios.datosAgenda(any(), any(), any(), any())).thenThrow(new ReglaNegocioException("ERR-GEN-06"));

		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(
				() -> casoUso.ejecutar(escenarioId, HOY, VistaAgenda.DIA, propietario))
				.isInstanceOf(ReglaNegocioException.class).actual();

		assertThat(error.getCodigoMensaje()).isEqualTo("ERR-GEN-06");
		verifyNoInteractions(reservas, usuarios);
	}

}
