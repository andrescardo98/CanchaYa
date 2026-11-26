package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios.BloqueoAgenda;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBloqueos;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEncargados;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios.ResumenEscenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;

/**
 * HU29: las consultas de Escenarios para la agenda, en la base real. Los bloqueos y los encargados
 * se insertan con SQL porque HU30 y HU34 todavía no existen (P1, P2).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AgendaPersistenciaTest {

	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");
	private static final LocalDate LUNES = LocalDate.of(2030, 1, 14);
	private static final LocalDate DOMINGO = LUNES.plusDays(6);

	@Autowired
	private RepositorioEncargados repositorioEncargados;

	@Autowired
	private RepositorioEscenarios repositorioEscenarios;

	@Autowired
	private RepositorioBloqueos repositorioBloqueos;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID ana;
	private UUID luis;
	private UUID encargada;
	private UUID sedeNorte;
	private UUID la70;
	private UUID golazo;
	private UUID cancha;
	private UUID otraCancha;

	@BeforeEach
	void preparar() {
		ana = usuario("1000000061");
		luis = usuario("1000000062");
		encargada = usuario("1000000063");
		sedeNorte = escenario("Sede Norte", ana, "ACTIVO");
		la70 = escenario("Canchas La 70", ana, "INACTIVO");
		golazo = escenario("Golazo", luis, "ACTIVO");
		cancha = cancha(sedeNorte, "Cancha 1");
		otraCancha = cancha(golazo, "Cancha 1");
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from bloqueo");
		jdbc.update("delete from encargado_escenario");
		jdbc.update("delete from cancha");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private UUID usuario(String documento) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Administrador', ?, ?, 'CC', ?, '+573001234567', 'ADMINISTRADOR_CANCHA', now())""",
				id, documento + "@correo.com", "x".repeat(60), documento);
		return id;
	}

	private UUID escenario(String nombre, UUID propietario, String estado) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, ?, 'Calle 80', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, ?)""", id, nombre, propietario, estado);
		return id;
	}

	private UUID cancha(UUID escenario, String nombre) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora) values (?, ?, ?, ?, 80000)",
				id, escenario, F5, nombre);
		return id;
	}

	private void asignar(UUID escenario, UUID usuario) {
		jdbc.update("insert into encargado_escenario (id, escenario_id, usuario_id, fecha_asignacion) values (?, ?, ?, now())",
				UUID.randomUUID(), escenario, usuario);
	}

	private UUID bloqueo(UUID canchaId, LocalDate fecha, int desde, int hasta, String tipo, String motivo) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into bloqueo (id, cancha_id, fecha, hora_inicio, hora_fin, tipo_bloqueo, motivo, creado_por_id)
				values (?, ?, ?, ?, ?, ?, ?, ?)""",
				id, canchaId, fecha, LocalTime.of(desde, 0), LocalTime.of(hasta, 0), tipo, motivo, ana);
		return id;
	}

	// RepositorioEncargadosJdbc (P1)

	@Test
	void ca6EstaAsignadoSoloEnElEscenarioDeLaAsignacion() {
		asignar(sedeNorte, encargada);

		assertThat(repositorioEncargados.estaAsignado(sedeNorte, encargada)).isTrue();
		assertThat(repositorioEncargados.estaAsignado(golazo, encargada)).isFalse();
		assertThat(repositorioEncargados.estaAsignado(sedeNorte, luis)).isFalse();
	}

	@Test
	void ca5LosEscenariosAsignadosDelEncargado() {
		asignar(sedeNorte, encargada);
		asignar(golazo, encargada);
		asignar(la70, luis);

		assertThat(repositorioEncargados.escenariosAsignados(encargada)).containsExactlyInAnyOrder(sedeNorte, golazo);
		assertThat(repositorioEncargados.escenariosAsignados(ana)).isEmpty();
	}

	// RepositorioEscenariosJpa (CA4, CA5, P8)

	@Test
	void ca4LosEscenariosDelPropietarioPorNombreEnCualquierEstado() {
		assertThat(repositorioEscenarios.listarPorAdministrador(ana))
				.extracting(ResumenEscenario::id, ResumenEscenario::nombre, ResumenEscenario::estado)
				.containsExactly(tuple(la70, "Canchas La 70", EstadoEscenario.INACTIVO),
						tuple(sedeNorte, "Sede Norte", EstadoEscenario.ACTIVO));
		assertThat(repositorioEscenarios.listarPorAdministrador(encargada)).isEmpty();
	}

	@Test
	void ca5LosEscenariosPorIdentificador() {
		assertThat(repositorioEscenarios.listarPorIds(List.of(golazo, sedeNorte, UUID.randomUUID())))
				.extracting(ResumenEscenario::nombre).containsExactly("Golazo", "Sede Norte");
		assertThat(repositorioEscenarios.listarPorIds(List.of())).isEmpty();
	}

	// RepositorioBloqueosJdbc.deCanchasEntre (P2)

	@Test
	void ca2LosBloqueosDelRangoConTipoYMotivoEnOrden() {
		UUID delDomingo = bloqueo(cancha, DOMINGO, 8, 9, "TORNEO", "Copa Rionegro");
		UUID delLunesTarde = bloqueo(cancha, LUNES, 18, 20, "MANTENIMIENTO", "Cambio de grama");
		UUID delLunesManana = bloqueo(cancha, LUNES, 6, 7, "USO_PROPIO", null);
		bloqueo(cancha, LUNES.minusDays(1), 10, 11, "TORNEO", null);
		bloqueo(cancha, DOMINGO.plusDays(1), 10, 11, "TORNEO", null);
		bloqueo(otraCancha, LUNES, 10, 11, "TORNEO", null);

		assertThat(repositorioBloqueos.deCanchasEntre(List.of(cancha), LUNES, DOMINGO)).containsExactly(
				new BloqueoAgenda(delLunesManana, cancha, LUNES, LocalTime.of(6, 0), LocalTime.of(7, 0), "USO_PROPIO",
						null),
				new BloqueoAgenda(delLunesTarde, cancha, LUNES, LocalTime.of(18, 0), LocalTime.of(20, 0),
						"MANTENIMIENTO", "Cambio de grama"),
				new BloqueoAgenda(delDomingo, cancha, DOMINGO, LocalTime.of(8, 0), LocalTime.of(9, 0), "TORNEO",
						"Copa Rionegro"));
	}

	@Test
	void ca2SinCanchasNoHayBloqueos() {
		bloqueo(cancha, LUNES, 6, 7, "USO_PROPIO", null);

		assertThat(repositorioBloqueos.deCanchasEntre(List.of(), LUNES, DOMINGO)).isEmpty();
	}

}
