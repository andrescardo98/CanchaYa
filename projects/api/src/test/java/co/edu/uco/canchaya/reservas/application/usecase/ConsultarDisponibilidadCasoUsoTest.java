package co.edu.uco.canchaya.reservas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas;
import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarDisponibilidad.Disponibilidad;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarDisponibilidad.FranjaDisponible;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.ConfirmacionAsistencia;
import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.ParametrosReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU20: armar la disponibilidad de una cancha para una fecha (CA1 a CA6). */
class ConsultarDisponibilidadCasoUsoTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	// Lunes 23 de noviembre de 2026, 7:30 a. m. en Bogotá (D27)
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 7, 30);
	private static final Clock RELOJ = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
	private static final LocalDate HOY = AHORA.toLocalDate();
	private static final LocalDate MANANA = HOY.plusDays(1);
	private static final ParametrosReserva PARAMETROS = new ParametrosReserva(30, 2, 2, 5, 3, 5, LocalTime.NOON, 3,
			24, 250, false);

	private CanchasFalsas canchas;
	private ReservasFalsas reservas;
	private ConsultarDisponibilidadCasoUso consulta;
	private UUID canchaId;

	@BeforeEach
	void preparar() {
		canchaId = UUID.randomUUID();
		canchas = new CanchasFalsas();
		canchas.reservable = Optional.of(new ConsultaCanchas.CanchaReservable(canchaId, UUID.randomUUID(), "Cancha 1",
				"Cancha La 80", new BigDecimal("80000.00")));
		reservas = new ReservasFalsas();
		consulta = new ConsultarDisponibilidadCasoUso(canchas, reservas, PARAMETROS, RELOJ);
	}

	private static FranjaDisponible franja(int hora, boolean libre) {
		return new FranjaDisponible(LocalTime.of(hora, 0), LocalTime.of(hora + 1, 0), libre);
	}

	private Reserva reserva(LocalDate fecha, int desde, int hasta) {
		return Reserva.reconstruir(UUID.randomUUID(), canchaId, UUID.randomUUID(), fecha, LocalTime.of(desde, 0),
				LocalTime.of(hasta, 0), new BigDecimal(80000 * (hasta - desde)), EstadoReserva.CONFIRMADA,
				ConfirmacionAsistencia.NO_REQUERIDA, null, null, OffsetDateTime.now(RELOJ), null, null);
	}

	private static void esperarError(Runnable accion, String codigo) {
		assertThatThrownBy(accion::run).isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje()).isEqualTo(codigo);
	}

	@Test
	void CA1_canchaDisponible_devuelveFranjasLibresYOcupadas() {
		canchas.horario(8, 12);
		reservas.confirmadas.add(reserva(MANANA, 9, 10));

		Disponibilidad disponibilidad = consulta.ejecutar(canchaId, MANANA);

		assertThat(disponibilidad.canchaId()).isEqualTo(canchaId);
		assertThat(disponibilidad.fecha()).isEqualTo(MANANA);
		assertThat(disponibilidad.fechaMinima()).isEqualTo(HOY);
		assertThat(disponibilidad.fechaMaxima()).isEqualTo(HOY.plusDays(30));
		assertThat(disponibilidad.franjasMaximasPorReserva()).isEqualTo(2);
		assertThat(disponibilidad.franjas()).containsExactly(franja(8, true), franja(9, false), franja(10, true),
				franja(11, true));
	}

	@Test
	void CA1_sinFecha_usaLaFechaDeHoy() {
		canchas.horario(8, 9);

		Disponibilidad disponibilidad = consulta.ejecutar(canchaId, null);

		assertThat(disponibilidad.fecha()).isEqualTo(HOY);
		assertThat(canchas.fechasConsultadas).containsExactly(HOY);
	}

	@Test
	void CA2_reservaDeDosHoras_ocupaDosFranjas() {
		canchas.horario(17, 21);
		reservas.confirmadas.add(reserva(MANANA, 18, 20));

		assertThat(consulta.ejecutar(canchaId, MANANA).franjas()).containsExactly(franja(17, true), franja(18, false),
				franja(19, false), franja(20, true));
	}

	@Test
	void CA2_bloqueoOcupaSusFranjas() {
		canchas.horario(8, 13);
		canchas.bloqueos.add(new ConsultaCanchas.RangoBloqueado(LocalTime.of(10, 0), LocalTime.of(12, 0)));

		assertThat(consulta.ejecutar(canchaId, MANANA).franjas()).containsExactly(franja(8, true), franja(9, true),
				franja(10, false), franja(11, false), franja(12, true));
	}

	@Test
	void CA2_lasReservasSeBuscanDeEsaCanchaYEsaFecha() {
		canchas.horario(8, 9);

		consulta.ejecutar(canchaId, MANANA);

		assertThat(reservas.consultas).containsExactly(canchaId + " " + MANANA);
	}

	@Test
	void CA4_sinHorarioEseDia_franjasVacias() {
		// También cubre el escenario ACTIVO sin ningún horario (P3): franjasDelDia devuelve []
		Disponibilidad disponibilidad = consulta.ejecutar(canchaId, MANANA);

		assertThat(disponibilidad.franjas()).isEmpty();
		assertThat(disponibilidad.fechaMaxima()).isEqualTo(HOY.plusDays(30));
	}

	@Test
	void CA5_fueraDeLaVentana_ERR_HOR_10() {
		canchas.errorFranjas = new ReglaNegocioException("ERR-HOR-10");

		esperarError(() -> consulta.ejecutar(canchaId, HOY.plusDays(31)), "ERR-HOR-10");
	}

	@Test
	void CA6_canchaNoReservable_ERR_RES_01_sinCalcularFranjas() {
		// Cancha inexistente, INACTIVA o de un escenario INACTIVO: buscarReservable responde vacío (P2)
		canchas.reservable = Optional.empty();

		esperarError(() -> consulta.ejecutar(canchaId, MANANA), "ERR-RES-01");
		assertThat(canchas.fechasConsultadas).isEmpty();
	}

	/** ConsultaCanchas falsa: las franjas salen de un horario de ese día, sin las que ya empezaron. */
	private static final class CanchasFalsas implements ConsultaCanchas {

		Optional<CanchaReservable> reservable = Optional.empty();
		final List<RangoBloqueado> bloqueos = new ArrayList<>();
		final List<LocalDate> fechasConsultadas = new ArrayList<>();
		ReglaNegocioException errorFranjas;
		private int desde;
		private int hasta;

		void horario(int desde, int hasta) {
			this.desde = desde;
			this.hasta = hasta;
		}

		@Override
		public Optional<CanchaReservable> buscarReservable(UUID canchaId) {
			return reservable;
		}

		@Override
		public List<Franja> franjasDelDia(UUID canchaId, LocalDate fecha) {
			fechasConsultadas.add(fecha);
			if (errorFranjas != null) {
				throw errorFranjas;
			}
			List<Franja> franjas = new ArrayList<>();
			for (int hora = desde; hora < hasta; hora++) {
				Franja franja = new Franja(fecha, LocalTime.of(hora, 0), LocalTime.of(hora + 1, 0));
				if (franja.inicio().isAfter(AHORA)) {
					franjas.add(franja);
				}
			}
			return franjas;
		}

		@Override
		public List<RangoBloqueado> bloqueosDelDia(UUID canchaId, LocalDate fecha) {
			return bloqueos;
		}

		@Override
		public void tomarTurno(UUID canchaId) {
			throw new UnsupportedOperationException("HU20 no toma el turno");
		}

	}

	private static final class ReservasFalsas implements RepositorioReservas {

		final List<Reserva> confirmadas = new ArrayList<>();
		final List<String> consultas = new ArrayList<>();

		@Override
		public boolean existenConfirmadasQueEmpiezanDespuesDe(Collection<UUID> canchaIds, LocalDateTime momento) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Reserva> confirmadasDeCanchaEnFecha(UUID canchaId, LocalDate fecha) {
			consultas.add(canchaId + " " + fecha);
			return confirmadas.stream().filter(r -> r.getFecha().equals(fecha)).toList();
		}

	}

}
