package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.RangoBloqueado;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBloqueos;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBusquedaCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBusquedaCanchas.Candidata;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBusquedaCanchas.FiltroBusqueda;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioHorarios;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;

/**
 * HU18 (T5): lo que leen la búsqueda, los bloqueos y los horarios en la base real con PostGIS.
 * Solo levanta la base y estos repositorios (@DataJpaTest): cada prueba se deshace al terminar.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, RepositorioBusquedaCanchasJdbc.class, RepositorioBloqueosJdbc.class,
		RepositorioHorariosJpa.class })
class BusquedaCanchasPersistenciaTest {

	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");
	private static final UUID ANTIOQUIA = UUID.fromString("16b4a874-5a85-560b-83c8-c9e29247327d");
	private static final UUID RIONEGRO = UUID.fromString("c7f2d2ac-a264-597b-b927-c65662edf6a1");
	private static final UUID MARINILLA = UUID.fromString("ec37be2e-1820-56b4-a18e-27b9899ca17c");
	private static final LocalDate DIA = LocalDate.of(2030, 1, 15);

	/** El punto del jugador: parque principal de Rionegro. Un grado de latitud mide unos 111 km. */
	private static final BigDecimal LAT = new BigDecimal("6.155100");
	private static final BigDecimal LNG = new BigDecimal("-75.373700");

	@Autowired
	private RepositorioBusquedaCanchas repositorio;

	@Autowired
	private RepositorioBloqueos repositorioBloqueos;

	@Autowired
	private RepositorioHorarios repositorioHorarios;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID propietario;
	private UUID f7;
	private UUID otroDepartamento;
	private UUID ciudadDeOtroDepartamento;

	@BeforeEach
	void preparar() {
		propietario = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Dueño', 'dueno@correo.com', ?, 'CC', '1000000182', '+573001234567',
				  'ADMINISTRADOR_CANCHA', now())""", propietario, "x".repeat(60));
		f7 = jdbc.queryForObject("select id from tipo_cancha where codigo = 'F7'", UUID.class);
		// La V2 solo trae Antioquia (D16): para CA3 se agrega otro departamento con una ciudad
		otroDepartamento = UUID.randomUUID();
		jdbc.update("insert into departamento (id, codigo, nombre) values (?, '99', 'Prueba')", otroDepartamento);
		ciudadDeOtroDepartamento = UUID.randomUUID();
		jdbc.update("insert into ciudad (id, codigo, departamento_id, nombre) values (?, '99001', ?, 'Ciudad prueba')",
				ciudadDeOtroDepartamento, otroDepartamento);
	}

	/** Escenario a ~metrosAlNorte del punto del jugador. */
	private UUID escenario(String nombre, UUID ciudad, int metrosAlNorte, String estado) {
		UUID id = UUID.randomUUID();
		BigDecimal latitud = LAT.add(BigDecimal.valueOf(metrosAlNorte / 111_320.0)).setScale(6, java.math.RoundingMode.HALF_UP);
		jdbc.update("""
				insert into escenario (id, nombre, descripcion, direccion, ciudad_id, latitud, longitud,
				  telefono_contacto, correo_contacto, administrador_id, estado)
				values (?, ?, 'Grama nueva', 'Calle 1 # 2-3', ?, ?, ?, '+573001234567', 'contacto@la70.com', ?, ?)""",
				id, nombre, ciudad, latitud, LNG, propietario, estado);
		return id;
	}

	private UUID escenario(String nombre, int metrosAlNorte) {
		return escenario(nombre, RIONEGRO, metrosAlNorte, "ACTIVO");
	}

	private UUID cancha(UUID escenario, String nombre, UUID tipo, String estado) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora, estado)
				values (?, ?, ?, ?, 80000, ?)""", id, escenario, tipo, nombre, estado);
		return id;
	}

	private UUID cancha(UUID escenario, String nombre) {
		return cancha(escenario, nombre, F5, "DISPONIBLE");
	}

	private static FiltroBusqueda conUbicacion(int radioMetros) {
		return new FiltroBusqueda(LAT, LNG, radioMetros, null, null, null);
	}

	private static FiltroBusqueda sinUbicacion(UUID departamento, UUID ciudad, UUID tipo) {
		return new FiltroBusqueda(null, null, null, departamento, ciudad, tipo);
	}

	private static List<String> nombres(List<Candidata> candidatas) {
		return candidatas.stream().map(c -> c.escenario().getNombre() + " / " + c.cancha().getNombre()).toList();
	}

	// --- RepositorioBusquedaCanchasJdbc --------------------------------------------------------------

	// CA1: dentro del radio, de la más cercana a la más lejana y, a igual distancia, por nombre de escenario y cancha
	@Test
	void ca1ConUbicacion_dentroDelRadioYOrdenadasPorDistancia() {
		// Los nombres van al revés de la distancia, para que el orden por nombre no pase por orden por distancia
		UUID lejos = escenario("Alfa lejos", 8_000);
		cancha(lejos, "Cancha 2");
		cancha(lejos, "Cancha 1");
		cancha(escenario("Zeta cerca", 2_000), "Cancha 1");
		cancha(escenario("Fuera", 15_000), "Cancha 1");

		List<Candidata> candidatas = repositorio.buscar(conUbicacion(10_000));

		assertThat(nombres(candidatas)).containsExactly("Zeta cerca / Cancha 1", "Alfa lejos / Cancha 1",
				"Alfa lejos / Cancha 2");
		assertThat(candidatas.get(0).distanciaMetros()).isCloseTo(2_000, within(20.0));
		assertThat(candidatas.get(1).distanciaMetros()).isCloseTo(8_000, within(80.0));
	}

	// CA1: el radio se mide en metros e incluye el borde de la consulta
	@Test
	void ca1ElRadioRespetaElBorde() {
		cancha(escenario("A 950 m", 950), "Cancha 1");
		cancha(escenario("A 1050 m", 1_050), "Cancha 1");

		assertThat(nombres(repositorio.buscar(conUbicacion(1_000)))).containsExactly("A 950 m / Cancha 1");
	}

	// P7: la base no aplica RN-04; trae también las canchas INACTIVAS y las de escenarios INACTIVOS, con su estado,
	// para que lo decida Cancha.publicarEnBusqueda
	@Test
	void p7NoFiltraPorEstado_reconstruyeCanchaYEscenario() {
		UUID inactivo = escenario("Inactivo", RIONEGRO, 1_000, "INACTIVO");
		UUID canchaId = cancha(inactivo, "Cancha 1", f7, "INACTIVA");

		Candidata candidata = repositorio.buscar(conUbicacion(5_000)).getFirst();

		Cancha cancha = candidata.cancha();
		assertThat(cancha.getId()).isEqualTo(canchaId);
		assertThat(cancha.getEscenarioId()).isEqualTo(inactivo);
		assertThat(cancha.getTipoCanchaId()).isEqualTo(f7);
		assertThat(cancha.getPrecioHora()).isEqualByComparingTo("80000");
		assertThat(cancha.getEstado()).isEqualTo(EstadoCancha.INACTIVA);
		Escenario escenario = candidata.escenario();
		assertThat(escenario.getId()).isEqualTo(inactivo);
		assertThat(escenario.getEstado()).isEqualTo(EstadoEscenario.INACTIVO);
		assertThat(escenario.getCiudadId()).isEqualTo(RIONEGRO);
		assertThat(escenario.getLongitud()).isEqualByComparingTo(LNG);
		assertThat(escenario.getAdministradorId()).isEqualTo(propietario);
		assertThat(escenario.getFotos()).isEmpty();
		assertThat(candidata.nombreTipoCancha()).isEqualTo(
				jdbc.queryForObject("select nombre from tipo_cancha where codigo = 'F7'", String.class));
	}

	// CA2: sin ubicación no hay radio ni distancia, y el orden es por nombre del escenario y de la cancha
	@Test
	void ca2SinUbicacion_sinDistanciaYOrdenadasPorNombre() {
		cancha(escenario("Zeta", 1_000), "Cancha 1");
		cancha(escenario("Alfa", 50_000), "Cancha 1");

		List<Candidata> candidatas = repositorio.buscar(sinUbicacion(null, null, null));

		assertThat(nombres(candidatas)).containsExactly("Alfa / Cancha 1", "Zeta / Cancha 1");
		assertThat(candidatas).extracting(Candidata::distanciaMetros).containsOnlyNulls();
	}

	// CA3: por departamento salen solo los de ese departamento; con ciudad, solo los de la ciudad; una ciudad que
	// no es del departamento no da resultados
	@Test
	void ca3PorDepartamentoYCiudad_soloLosDeAhi() {
		cancha(escenario("En Rionegro", RIONEGRO, 1_000, "ACTIVO"), "Cancha 1");
		cancha(escenario("En Marinilla", MARINILLA, 1_000, "ACTIVO"), "Cancha 1");
		cancha(escenario("En otro departamento", ciudadDeOtroDepartamento, 1_000, "ACTIVO"), "Cancha 1");

		assertThat(nombres(repositorio.buscar(sinUbicacion(ANTIOQUIA, null, null))))
				.containsExactly("En Marinilla / Cancha 1", "En Rionegro / Cancha 1");
		assertThat(nombres(repositorio.buscar(sinUbicacion(ANTIOQUIA, RIONEGRO, null))))
				.containsExactly("En Rionegro / Cancha 1");
		assertThat(nombres(repositorio.buscar(sinUbicacion(null, ciudadDeOtroDepartamento, null))))
				.containsExactly("En otro departamento / Cancha 1");
		assertThat(repositorio.buscar(sinUbicacion(otroDepartamento, RIONEGRO, null))).isEmpty();
	}

	// CA4: por tipo salen solo las canchas de ese tipo, aunque el escenario tenga de otros
	@Test
	void ca4PorTipo_soloLasDeEseTipo() {
		UUID escenario = escenario("La 70", 1_000);
		cancha(escenario, "Cancha 5", F5, "DISPONIBLE");
		cancha(escenario, "Cancha 7", f7, "DISPONIBLE");

		assertThat(nombres(repositorio.buscar(sinUbicacion(null, null, f7)))).containsExactly("La 70 / Cancha 7");
	}

	// Los filtros se combinan: ubicación, departamento, ciudad y tipo a la vez
	@Test
	void losFiltrosSeCombinan() {
		UUID cerca = escenario("Cerca en Rionegro", RIONEGRO, 1_000, "ACTIVO");
		cancha(cerca, "Cancha 5", F5, "DISPONIBLE");
		cancha(cerca, "Cancha 7", f7, "DISPONIBLE");
		cancha(escenario("Lejos en Rionegro", RIONEGRO, 20_000, "ACTIVO"), "Cancha 7", f7, "DISPONIBLE");
		cancha(escenario("Cerca en Marinilla", MARINILLA, 1_000, "ACTIVO"), "Cancha 7", f7, "DISPONIBLE");

		assertThat(nombres(repositorio.buscar(new FiltroBusqueda(LAT, LNG, 5_000, ANTIOQUIA, RIONEGRO, f7))))
				.containsExactly("Cerca en Rionegro / Cancha 7");
	}

	// --- RepositorioBloqueosJdbc y RepositorioHorariosJpa (adelantados en T4) ----------------------

	// CA5: los bloqueos de varias canchas en una consulta, por cancha y solo de esa fecha
	@Test
	void ca5LosBloqueosDeVariasCanchasPorCancha() {
		UUID escenario = escenario("La 70", 1_000);
		UUID una = cancha(escenario, "Cancha 1");
		UUID otra = cancha(escenario, "Cancha 2");
		UUID sinBloqueos = cancha(escenario, "Cancha 3");
		UUID noPedida = cancha(escenario, "Cancha 4");
		bloqueo(una, DIA, 18, 20);
		bloqueo(una, DIA, 8, 9);
		bloqueo(una, DIA.plusDays(1), 10, 11);
		bloqueo(otra, DIA, 12, 13);
		bloqueo(noPedida, DIA, 12, 13);

		Map<UUID, List<RangoBloqueado>> porCancha = repositorioBloqueos.rangosDelDia(List.of(una, otra, sinBloqueos), DIA);

		assertThat(porCancha).containsOnlyKeys(una, otra);
		assertThat(porCancha.get(una)).containsExactly(rango(8, 9), rango(18, 20));
		assertThat(porCancha.get(otra)).containsExactly(rango(12, 13));
		assertThat(repositorioBloqueos.rangosDelDia(List.of(), DIA)).isEmpty();
	}

	// RN-04 y CA5: los horarios de varios escenarios, de todos los días, en una consulta
	@Test
	void losHorariosDeVariosEscenarios() {
		UUID uno = escenario("Uno", 1_000);
		UUID dos = escenario("Dos", 1_000);
		UUID noPedido = escenario("No pedido", 1_000);
		horario(uno, DiaSemana.LUNES, 8, 10);
		horario(uno, DiaSemana.SABADO, 14, 16);
		horario(dos, DiaSemana.MARTES, 6, 9);
		horario(noPedido, DiaSemana.LUNES, 8, 10);

		assertThat(repositorioHorarios.buscarPorEscenarios(List.of(uno, dos)))
				.extracting(HorarioRecurrente::getEscenarioId, HorarioRecurrente::getDiaSemana)
				.containsExactlyInAnyOrder(tuple(uno, DiaSemana.LUNES), tuple(uno, DiaSemana.SABADO),
						tuple(dos, DiaSemana.MARTES));
		assertThat(repositorioHorarios.buscarPorEscenarios(List.of())).isEmpty();
	}

	private static RangoBloqueado rango(int desde, int hasta) {
		return new RangoBloqueado(LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
	}

	private void bloqueo(UUID canchaId, LocalDate fecha, int desde, int hasta) {
		jdbc.update("""
				insert into bloqueo (id, cancha_id, fecha, hora_inicio, hora_fin, tipo_bloqueo, motivo, creado_por_id)
				values (?, ?, ?, ?, ?, 'MANTENIMIENTO', 'Cambio de grama', ?)""",
				UUID.randomUUID(), canchaId, fecha, LocalTime.of(desde, 0), LocalTime.of(hasta, 0), propietario);
	}

	private void horario(UUID escenario, DiaSemana dia, int desde, int hasta) {
		jdbc.update("""
				insert into horario_recurrente (id, escenario_id, dia_semana, hora_inicio, hora_fin)
				values (?, ?, ?, ?, ?)""", UUID.randomUUID(), escenario, dia.name(), LocalTime.of(desde, 0),
				LocalTime.of(hasta, 0));
	}

}
