package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.CanchaReservable;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.RangoBloqueado;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU20: lo que Escenarios le ofrece a Reservas sobre una cancha (puerto ConsultaCanchas, D36). */
class ConsultaCanchasCasoUsoTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	// Lunes 23 de noviembre de 2026, 7:30 a. m. en Bogotá (D27)
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 7, 30);
	private static final Clock RELOJ = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
	private static final LocalDate HOY = AHORA.toLocalDate();
	private static final int VENTANA_DIAS = 30;

	private CanchasEnMemoria memoria;
	private HorariosEnMemoria horarios;
	private List<RangoBloqueado> bloqueos;
	private ConsultaCanchasCasoUso consulta;
	private Escenario escenario;
	private Cancha cancha;

	@BeforeEach
	void preparar() {
		memoria = new CanchasEnMemoria();
		horarios = new HorariosEnMemoria();
		bloqueos = List.of(new RangoBloqueado(LocalTime.of(18, 0), LocalTime.of(20, 0)));
		consulta = new ConsultaCanchasCasoUso(memoria.repositorioCanchas(), memoria.repositorioEscenarios(), horarios,
				(canchaId, fecha) -> canchaId.equals(cancha.getId()) && fecha.equals(HOY) ? bloqueos : List.of(),
				RELOJ, VENTANA_DIAS);
		escenario = memoria.escenarioDe(UUID.randomUUID(), EstadoEscenario.ACTIVO);
		cancha = memoria.cancha(escenario, "Cancha 1", EstadoCancha.DISPONIBLE);
	}

	private void horario(DiaSemana dia, int desde, int hasta) {
		horarios.guardar(HorarioRecurrente.reconstruir(UUID.randomUUID(), escenario.getId(), dia,
				LocalTime.of(desde, 0), LocalTime.of(hasta, 0)));
	}

	private static void esperarError(Runnable accion, String codigo) {
		assertThatThrownBy(accion::run).isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje()).isEqualTo(codigo);
	}

	// buscarReservable: política 1 de Reserva y P2

	@Test
	void ca2LaCanchaDisponibleDeUnEscenarioActivoSePuedeReservar() {
		assertThat(consulta.buscarReservable(cancha.getId())).contains(new CanchaReservable(cancha.getId(),
				escenario.getId(), "Cancha 1", escenario.getNombre(), new BigDecimal("80000.00")));
	}

	@Test
	void ca6LaCanchaQueNoExisteNoSePuedeReservar() {
		assertThat(consulta.buscarReservable(UUID.randomUUID())).isEmpty();
	}

	@Test
	void ca6LaCanchaInactivaNoSePuedeReservar() {
		Cancha inactiva = memoria.cancha(escenario, "Cancha 2", EstadoCancha.INACTIVA);

		assertThat(consulta.buscarReservable(inactiva.getId())).isEmpty();
	}

	@Test
	void p2LaCanchaDeUnEscenarioInactivoNoSePuedeReservar() {
		Escenario inactivo = memoria.escenarioDe(UUID.randomUUID(), EstadoEscenario.INACTIVO);
		Cancha deInactivo = memoria.cancha(inactivo, "Cancha 1", EstadoCancha.DISPONIBLE);

		assertThat(consulta.buscarReservable(deInactivo.getId())).isEmpty();
	}

	// franjasDelDia: políticas 9 a 12 de HorarioRecurrente

	@Test
	void ca2LasFranjasSalenDeLosHorariosDelEscenarioParaEseDia() {
		horario(DiaSemana.MARTES, 8, 10);
		horario(DiaSemana.LUNES, 20, 22);

		assertThat(consulta.franjasDelDia(cancha.getId(), HOY.plusDays(1))).containsExactly(
				new Franja(HOY.plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0)),
				new Franja(HOY.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0)));
	}

	@Test
	void ca3HoyNoSalenLasFranjasQueYaEmpezaronSegunElRelojDelServidor() {
		horario(DiaSemana.LUNES, 6, 9);

		// A las 7:30 ya empezaron las de 6:00 y 7:00
		assertThat(consulta.franjasDelDia(cancha.getId(), HOY))
				.containsExactly(new Franja(HOY, LocalTime.of(8, 0), LocalTime.of(9, 0)));
	}

	@Test
	void ca5LaVentanaSaleDeLaConfiguracion() {
		horario(DiaSemana.LUNES, 8, 9);
		ConsultaCanchasCasoUso conVentanaDeSieteDias = new ConsultaCanchasCasoUso(memoria.repositorioCanchas(),
				memoria.repositorioEscenarios(), horarios, (canchaId, fecha) -> List.of(), RELOJ, 7);

		assertThat(conVentanaDeSieteDias.franjasDelDia(cancha.getId(), HOY.plusDays(7))).hasSize(1);
		esperarError(() -> conVentanaDeSieteDias.franjasDelDia(cancha.getId(), HOY.plusDays(8)), "ERR-HOR-10");
	}

	@Test
	void ca5FueraDeLaVentanaSaleErrHor10() {
		esperarError(() -> consulta.franjasDelDia(cancha.getId(), HOY.minusDays(1)), "ERR-HOR-10");
		esperarError(() -> consulta.franjasDelDia(cancha.getId(), HOY.plusDays(VENTANA_DIAS + 1)), "ERR-HOR-10");
	}

	@Test
	void politica9LasFranjasDeUnaCanchaQueNoExisteSonErrHor09() {
		esperarError(() -> consulta.franjasDelDia(UUID.randomUUID(), HOY), "ERR-HOR-09");
	}

	// bloqueosDelDia y tomarTurno (D36)

	@Test
	void ca2LosBloqueosDelDiaSonLosDelRepositorio() {
		assertThat(consulta.bloqueosDelDia(cancha.getId(), HOY)).isEqualTo(bloqueos);
		assertThat(consulta.bloqueosDelDia(cancha.getId(), HOY.plusDays(1))).isEmpty();
	}

	@Test
	void d36TomarTurnoBloqueaLaCanchaEnElRepositorio() {
		consulta.tomarTurno(cancha.getId());

		assertThat(memoria.turnosTomados).containsExactly(cancha.getId());
	}

}
