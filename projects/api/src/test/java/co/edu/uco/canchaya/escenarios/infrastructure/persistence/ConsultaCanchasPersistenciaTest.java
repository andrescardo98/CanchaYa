package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.NombresCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.RangoBloqueado;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBloqueos;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;

/** HU20 y HU21: lo que lee y bloquea ConsultaCanchas en la base real (P5, D29 punto 2, D36). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConsultaCanchasPersistenciaTest {

	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");
	private static final LocalDate DIA = LocalDate.of(2030, 1, 15);

	@Autowired
	private RepositorioCanchas repositorioCanchas;

	@Autowired
	private RepositorioBloqueos repositorioBloqueos;

	@Autowired
	private TransactionTemplate transaccion;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID propietario;
	private UUID escenario;
	private UUID cancha;
	private UUID otraCancha;

	@BeforeEach
	void preparar() {
		propietario = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Dueño', 'dueno@correo.com', ?, 'CC', '1000000051', '+573001234567',
				  'ADMINISTRADOR_CANCHA', now())""", propietario, "x".repeat(60));
		escenario = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Cancha La 80', 'Calle 80', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, 'ACTIVO')""", escenario, propietario);
		cancha = cancha("Cancha 1");
		otraCancha = cancha("Cancha 2");
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from bloqueo");
		jdbc.update("delete from cancha");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private UUID cancha(String nombre) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora) values (?, ?, ?, ?, 80000)",
				id, escenario, F5, nombre);
		return id;
	}

	private void bloqueo(UUID canchaId, LocalDate fecha, int desde, int hasta) {
		jdbc.update("""
				insert into bloqueo (id, cancha_id, fecha, hora_inicio, hora_fin, tipo_bloqueo, motivo, creado_por_id)
				values (?, ?, ?, ?, ?, 'MANTENIMIENTO', 'Cambio de grama', ?)""",
				UUID.randomUUID(), canchaId, fecha, LocalTime.of(desde, 0), LocalTime.of(hasta, 0), propietario);
	}

	// RepositorioBloqueosJdbc (P5)

	@Test
	void ca2LosBloqueosDelDiaSalenSoloConSusHorasYEnOrden() {
		bloqueo(cancha, DIA, 18, 20);
		bloqueo(cancha, DIA, 8, 9);
		bloqueo(cancha, DIA.plusDays(1), 10, 11);
		bloqueo(otraCancha, DIA, 12, 13);

		assertThat(repositorioBloqueos.rangosDelDia(cancha, DIA)).containsExactly(
				new RangoBloqueado(LocalTime.of(8, 0), LocalTime.of(9, 0)),
				new RangoBloqueado(LocalTime.of(18, 0), LocalTime.of(20, 0)));
	}

	@Test
	void ca2SinBloqueosLaListaEstaVacia() {
		assertThat(repositorioBloqueos.rangosDelDia(cancha, DIA)).isEmpty();
	}

	// RepositorioCanchasJpa.buscarPorId (P7)

	@Test
	void buscarPorIdTraeLaCanchaGuardada() {
		Cancha encontrada = repositorioCanchas.buscarPorId(cancha).orElseThrow();

		assertThat(encontrada.getEscenarioId()).isEqualTo(escenario);
		assertThat(encontrada.getNombre()).isEqualTo("Cancha 1");
		assertThat(encontrada.getPrecioHora()).isEqualByComparingTo(new BigDecimal("80000"));
		assertThat(encontrada.getEstado()).isEqualTo(EstadoCancha.DISPONIBLE);
		assertThat(repositorioCanchas.buscarPorId(UUID.randomUUID())).isEmpty();
	}

	// RepositorioCanchasJpa.tomarTurno (D29 punto 2, D36)

	@Test
	void tomarTurno_sinTransaccion_falla() {
		assertThatThrownBy(() -> repositorioCanchas.tomarTurno(cancha))
				.isInstanceOf(IllegalTransactionStateException.class);
	}

	@Test
	void tomarTurno_segundaTransaccionEspera() throws Exception {
		CountDownLatch primeraTomoElTurno = new CountDownLatch(1);
		CountDownLatch segundaTomoElTurno = new CountDownLatch(1);

		CompletableFuture<Boolean> primera = CompletableFuture.supplyAsync(() -> transaccion.execute(estado -> {
			repositorioCanchas.tomarTurno(cancha);
			primeraTomoElTurno.countDown();
			// Mientras la primera no termine, la segunda no puede tomar el turno
			return !esperar(segundaTomoElTurno, 1);
		}));
		esperar(primeraTomoElTurno, 10);
		CompletableFuture<Void> segunda = CompletableFuture.runAsync(() -> transaccion.executeWithoutResult(estado -> {
			repositorioCanchas.tomarTurno(cancha);
			segundaTomoElTurno.countDown();
		}));

		assertThat(primera.get(10, TimeUnit.SECONDS)).as("la segunda esperó a la primera").isTrue();
		segunda.get(10, TimeUnit.SECONDS);
		assertThat(segundaTomoElTurno.getCount()).as("la segunda tomó el turno al terminar la primera").isZero();
	}

	@Test
	void tomarTurno_otraCanchaNoEspera() throws Exception {
		CountDownLatch otraTomoElTurno = new CountDownLatch(1);

		Boolean otraNoEspero = transaccion.execute(estado -> {
			repositorioCanchas.tomarTurno(cancha);
			CompletableFuture.runAsync(() -> transaccion.executeWithoutResult(e -> {
				repositorioCanchas.tomarTurno(otraCancha);
				otraTomoElTurno.countDown();
			}));
			return esperar(otraTomoElTurno, 10);
		});

		assertThat(otraNoEspero).isTrue();
	}

	// HU23, CA1: los nombres llegan aunque la cancha esté INACTIVA o el escenario INACTIVO; una
	// cancha que no existe no viene
	@Test
	void CA1_canchaInactiva_tambienTraeNombres() {
		jdbc.update("update cancha set estado = 'INACTIVA' where id = ?", otraCancha);
		jdbc.update("update escenario set estado = 'INACTIVO' where id = ?", escenario);
		UUID inexistente = UUID.randomUUID();

		Map<UUID, NombresCancha> nombres = repositorioCanchas.nombresConEscenario(List.of(cancha, otraCancha,
				inexistente));

		assertThat(nombres).containsOnly(
				Map.entry(cancha, new NombresCancha("Cancha 1", "Cancha La 80")),
				Map.entry(otraCancha, new NombresCancha("Cancha 2", "Cancha La 80")));
		assertThat(repositorioCanchas.nombresConEscenario(List.of())).isEmpty();
	}

	private static boolean esperar(CountDownLatch senal, int segundos) {
		try {
			return senal.await(segundos, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

}
