package co.edu.uco.canchaya.reservas.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.application.port.in.ReservarCancha;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.ConfirmacionAsistencia;
import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * HU21, CA4 y spike RT-03 (P5): dos jugadores piden la misma franja en el mismo instante. Los dos
 * hilos esperan en un CountDownLatch y salen a la vez. Se prueba cada defensa de D29 por separado:
 * <ul>
 * <li>Sin turno: dos transacciones insertan directo con el repositorio. Solo queda la restricción
 * de exclusión de la base (punto 1).</li>
 * <li>Con turno: el caso de uso completo, que toma el turno de la cancha (punto 2) antes de leer.</li>
 * </ul>
 * En los dos casos debe quedar una sola reserva y el otro jugador recibe ERR-RES-06.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReservaConcurrenciaTest {

	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");
	// Pasado mañana: ninguna franja ha empezado, sea cual sea la hora en que corra la prueba
	private static final LocalDate FECHA = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(2);
	private static final int REPETICIONES = 20;
	private static final String BLOQUEO_MUTUO = "BLOQUEO_MUTUO";

	@Autowired
	private ReservarCancha reservarCancha;

	@Autowired
	private RepositorioReservas repositorio;

	@Autowired
	private TransactionTemplate transaccion;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID cancha;
	private UUID jugadorA;
	private UUID jugadorB;

	@BeforeEach
	void preparar() {
		UUID propietario = usuario("1000000301", "ADMINISTRADOR_CANCHA");
		jugadorA = usuario("1000000302", "JUGADOR");
		jugadorB = usuario("1000000303", "JUGADOR");
		UUID escenario = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Canchas La 70', 'Calle 70', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, 'ACTIVO')""", escenario, propietario);
		cancha = UUID.randomUUID();
		jdbc.update("insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora) values (?, ?, ?, 'Cancha 1', 80000)",
				cancha, escenario, F5);
		jdbc.update("""
				insert into horario_recurrente (id, escenario_id, dia_semana, hora_inicio, hora_fin)
				values (?, ?, ?, '06:00', '22:00')""", UUID.randomUUID(), escenario,
				DiaSemana.de(FECHA.getDayOfWeek()).name());
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from reserva");
		jdbc.update("delete from horario_recurrente");
		jdbc.update("delete from cancha");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private UUID usuario(String documento, String rol) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Ana Pérez', ?, ?, 'CC', ?, '+573001234567', ?, now())""",
				id, documento + "@correo.com", "x".repeat(60), documento, rol);
		return id;
	}

	private Reserva nueva(UUID jugador, int desde, int hasta) {
		return Reserva.reconstruir(UUID.randomUUID(), cancha, jugador, FECHA, LocalTime.of(desde, 0),
				LocalTime.of(hasta, 0), new BigDecimal("80000.00"), EstadoReserva.CONFIRMADA,
				ConfirmacionAsistencia.NO_REQUERIDA, null, null, OffsetDateTime.now(), null, null);
	}

	/** Corre las dos tareas a la vez y devuelve lo que pasó en cada una: "OK" o el código del error. */
	private static List<String> aLaVez(Callable<?> primera, Callable<?> segunda) throws Exception {
		CountDownLatch salida = new CountDownLatch(1);
		ExecutorService hilos = Executors.newFixedThreadPool(2);
		try {
			// Las dos quedan esperando en el latch; countDown las suelta en el mismo instante
			Future<String> a = hilos.submit(enCuantoSalga(salida, primera));
			Future<String> b = hilos.submit(enCuantoSalga(salida, segunda));
			salida.countDown();
			return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
		} finally {
			hilos.shutdownNow();
		}
	}

	private static Callable<String> enCuantoSalga(CountDownLatch salida, Callable<?> tarea) {
		return () -> {
			salida.await();
			try {
				tarea.call();
				return "OK";
			} catch (ReglaNegocioException e) {
				return e.getCodigoMensaje();
			} catch (PessimisticLockingFailureException e) {
				// 40P01 de PostgreSQL: aborta una de las dos transacciones que se esperan entre sí
				return BLOQUEO_MUTUO;
			}
		};
	}

	private int confirmadasEnLaFranja() {
		return jdbc.queryForObject("select count(*) from reserva where cancha_id = ? and estado = 'CONFIRMADA'",
				Integer.class, cancha);
	}

	/**
	 * Spike RT-03: sin turno, a veces las dos inserciones se esperan entre sí en el índice GiST y
	 * PostgreSQL aborta una por bloqueo mutuo (40P01). El repositorio lo traduce a ERR-RES-06 (P10),
	 * así el perdedor recibe siempre la misma respuesta. Si una no se tradujera, saldría BLOQUEO_MUTUO.
	 */
	@Test
	void CA4_sinTurno_laRestriccionDeExclusionDejaUnaSola() throws Exception {
		for (int vez = 0; vez < REPETICIONES; vez++) {
			jdbc.update("delete from reserva");
			// Rangos que se cruzan (18–20 y 19–20): sin turno ni lectura previa, solo la base los separa
			Reserva deA = nueva(jugadorA, 18, 20);
			Reserva deB = nueva(jugadorB, 19, 20);

			List<String> resultados = aLaVez(
					() -> transaccion.execute(estado -> { repositorio.guardar(deA); return null; }),
					() -> transaccion.execute(estado -> { repositorio.guardar(deB); return null; }));

			assertThat(resultados).containsExactlyInAnyOrder("OK", "ERR-RES-06");
			assertThat(confirmadasEnLaFranja()).isEqualTo(1);
		}
	}

	@Test
	void CA4_dosSolicitudesSimultaneas_soloUnaSeConfirma() throws Exception {
		Solicitante a = new Solicitante(jugadorA, Rol.JUGADOR);
		Solicitante b = new Solicitante(jugadorB, Rol.JUGADOR);
		for (int vez = 0; vez < REPETICIONES; vez++) {
			jdbc.update("delete from reserva");

			// Con el turno, la segunda espera, lee la reserva de la primera y confirmar lanza ERR-RES-06
			List<String> resultados = aLaVez(
					() -> reservarCancha.ejecutar(cancha, FECHA, LocalTime.of(18, 0), LocalTime.of(19, 0), a),
					() -> reservarCancha.ejecutar(cancha, FECHA, LocalTime.of(18, 0), LocalTime.of(19, 0), b));

			assertThat(resultados).containsExactlyInAnyOrder("OK", "ERR-RES-06");
			assertThat(confirmadasEnLaFranja()).isEqualTo(1);
		}
	}

}
