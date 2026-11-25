package co.edu.uco.canchaya.escenarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;

/**
 * HU18 completa: petición HTTP sin token → seguridad → caso de uso de Escenarios → PostGIS y
 * ConsultaFranjasOcupadas de Reservas → base en Docker. Los datos se preparan con SQL porque
 * escenarios, canchas, horarios, reservas y bloqueos se crean en otras HU. Usa el reloj real del
 * servidor (D27), por eso las fechas se calculan desde hoy en Bogotá.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class BusquedaCanchasPuntaAPuntaTest {

	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");
	private static final UUID ANTIOQUIA = UUID.fromString("16b4a874-5a85-560b-83c8-c9e29247327d");
	private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Bogota"));
	// Pasado mañana: ninguna franja ha empezado, sea cual sea la hora en que corra la prueba
	private static final LocalDate FECHA = HOY.plusDays(2);

	/** El punto del jugador: parque principal de Rionegro. */
	private static final String LAT = "6.155100";
	private static final String LNG = "-75.373700";

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID propietario;
	private UUID jugador;

	record Resultado(UUID escenarioId, String nombreEscenario, BigDecimal latitud, BigDecimal longitud, UUID canchaId,
			String nombreCancha, String tipoCancha, BigDecimal precioHora, BigDecimal distanciaKm,
			BigDecimal calificacionPromedio) {
	}

	record Respuesta(List<Resultado> resultados, Integer radioKm, int radioMinimoKm, int radioMaximoKm,
			LocalDate fechaMinima, LocalDate fechaMaxima, String codigo, String mensaje) {
	}

	record Error(String codigo, String campo, String mensaje) {
	}

	record RespuestaError(List<Error> errores) {
	}

	@BeforeEach
	void preparar() {
		propietario = usuario("1000000185", "ADMINISTRADOR_CANCHA");
		jugador = usuario("1000000186", "JUGADOR");
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from reserva");
		jdbc.update("delete from bloqueo");
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

	/** Escenario ACTIVO a ~metrosAlNorte del jugador, que atiende el día de FECHA de 08:00 a 14:00. */
	private UUID escenario(String nombre, int metrosAlNorte) {
		UUID id = UUID.randomUUID();
		BigDecimal latitud = new BigDecimal(LAT).add(BigDecimal.valueOf(metrosAlNorte / 111_320.0))
				.setScale(6, RoundingMode.HALF_UP);
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, ?, 'Calle 80', (select id from ciudad where nombre = 'Rionegro'), ?, ?, '+573001234567', ?,
				  'ACTIVO')""", id, nombre, latitud, new BigDecimal(LNG), propietario);
		jdbc.update("""
				insert into horario_recurrente (id, escenario_id, dia_semana, hora_inicio, hora_fin)
				values (?, ?, ?, '08:00', '14:00')""", UUID.randomUUID(), id,
				DiaSemana.de(FECHA.getDayOfWeek()).name());
		return id;
	}

	private UUID cancha(UUID escenario, String nombre) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora) values (?, ?, ?, ?, 80000)",
				id, escenario, F5, nombre);
		return id;
	}

	private void reserva(UUID cancha, String estado, int desde, int hasta) {
		// Una CANCELADA lleva su origen y su fecha de cancelación
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado,
				  origen_cancelacion, fecha_creacion, fecha_cancelacion)
				values (?, ?, ?, ?, ?, ?, 80000, ?,
				  case when ? = 'CANCELADA' then 'JUGADOR' end, now(), case when ? = 'CANCELADA' then now() end)""",
				UUID.randomUUID(), cancha, jugador, FECHA, LocalTime.of(desde, 0), LocalTime.of(hasta, 0), estado,
				estado, estado);
	}

	private void bloqueo(UUID cancha, int desde, int hasta) {
		jdbc.update("""
				insert into bloqueo (id, cancha_id, fecha, hora_inicio, hora_fin, tipo_bloqueo, motivo, creado_por_id)
				values (?, ?, ?, ?, ?, 'MANTENIMIENTO', 'Cambio de grama', ?)""", UUID.randomUUID(), cancha, FECHA,
				LocalTime.of(desde, 0), LocalTime.of(hasta, 0), propietario);
	}

	// CA9: sin cabecera Authorization
	private ResponseSpec buscarSinSesion(Map<String, Object> parametros) {
		return cliente.get().uri(uri -> {
			uri.path("/api/canchas/busqueda");
			parametros.forEach(uri::queryParam);
			return uri.build();
		}).exchange();
	}

	private Respuesta buscar(Map<String, Object> parametros) {
		return buscarSinSesion(parametros).expectStatus().isOk()
				.expectBody(Respuesta.class).returnResult().getResponseBody();
	}

	private static List<String> nombres(Respuesta respuesta) {
		return respuesta.resultados().stream().map(r -> r.nombreEscenario() + " / " + r.nombreCancha()).toList();
	}

	// CA1, CA9 y CA10: sin sesión, con ubicación y sin radio se busca a 5 km; con 10 km salen los dos, del más
	// cercano al más lejano y con su distancia
	@Test
	void CA1_CA9_CA10_sinSesionConUbicacionOrdenadasPorDistanciaYRadioPorDefecto() {
		UUID lejos = escenario("Alfa a 7 km", 7_000);
		cancha(lejos, "Cancha 1");
		UUID cerca = escenario("Zeta a 2 km", 2_000);
		UUID canchaCerca = cancha(cerca, "Cancha 1");

		Respuesta porDefecto = buscar(Map.of("latitud", LAT, "longitud", LNG));

		assertThat(nombres(porDefecto)).containsExactly("Zeta a 2 km / Cancha 1");
		assertThat(porDefecto.radioKm()).isEqualTo(5);
		Resultado resultado = porDefecto.resultados().getFirst();
		assertThat(resultado.escenarioId()).isEqualTo(cerca);
		assertThat(resultado.canchaId()).isEqualTo(canchaCerca);
		assertThat(resultado.tipoCancha()).isEqualTo("Fútbol 5");
		assertThat(resultado.precioHora()).isEqualByComparingTo("80000");
		assertThat(resultado.distanciaKm().doubleValue()).isCloseTo(2.0, within(0.1));
		assertThat(resultado.latitud()).isNotNull();
		assertThat(resultado.calificacionPromedio()).isNull();
		assertThat(porDefecto.codigo()).isNull();

		Respuesta conDiez = buscar(Map.of("latitud", LAT, "longitud", LNG, "radioKm", 10));
		assertThat(nombres(conDiez)).containsExactly("Zeta a 2 km / Cancha 1", "Alfa a 7 km / Cancha 1");
	}

	// CA2 y CA3: sin ubicación, por departamento; orden por nombre e INF-ESC-03
	@Test
	void CA2_CA3_sinUbicacionPorDepartamento_INF_ESC_03() {
		cancha(escenario("Zeta", 1_000), "Cancha 1");
		cancha(escenario("Alfa", 40_000), "Cancha 1");

		Respuesta respuesta = buscar(Map.of("departamentoId", ANTIOQUIA));

		assertThat(nombres(respuesta)).containsExactly("Alfa / Cancha 1", "Zeta / Cancha 1");
		assertThat(respuesta.resultados()).extracting(Resultado::distanciaKm).containsOnlyNulls();
		assertThat(respuesta.codigo()).isEqualTo("INF-ESC-03");
	}

	// CA5: con fecha y hora no salen la cancha con reserva CONFIRMADA ni la bloqueada; la que tiene una reserva
	// CANCELADA sí. Con solo la fecha (P2) salen las que tienen alguna franja libre ese día
	@Test
	void CA5_fechaYHora_soloConLaFranjaLibre() {
		UUID escenario = escenario("La 70", 1_000);
		reserva(cancha(escenario, "Reservada"), "CONFIRMADA", 10, 11);
		reserva(cancha(escenario, "Con cancelada"), "CANCELADA", 10, 11);
		bloqueo(cancha(escenario, "Bloqueada"), 10, 11);
		UUID llena = cancha(escenario, "Llena");
		reserva(llena, "CONFIRMADA", 8, 10);
		bloqueo(llena, 10, 14);

		assertThat(nombres(buscar(Map.of("fecha", FECHA, "hora", "10:00"))))
				.containsExactly("La 70 / Con cancelada");
		assertThat(nombres(buscar(Map.of("fecha", FECHA))))
				.containsExactly("La 70 / Bloqueada", "La 70 / Con cancelada", "La 70 / Reservada");
	}

	// CA6: fuera de los próximos 30 días no se busca
	@Test
	void CA6_fechaFueraDeVentana_422_ERR_HOR_10() {
		RespuestaError error = buscarSinSesion(Map.of("fecha", HOY.plusDays(31))).expectStatus().isEqualTo(422)
				.expectBody(RespuestaError.class).returnResult().getResponseBody();

		assertThat(error.errores()).extracting(Error::codigo).containsExactly("ERR-HOR-10");
	}

	// CA7: ningún escenario en el radio → lista vacía e INF-ESC-01
	@Test
	void CA7_sinResultados_INF_ESC_01() {
		cancha(escenario("Lejos", 20_000), "Cancha 1");

		Respuesta respuesta = buscar(Map.of("latitud", LAT, "longitud", LNG, "radioKm", 3));

		assertThat(respuesta.resultados()).isEmpty();
		assertThat(respuesta.codigo()).isEqualTo("INF-ESC-01");
	}

	// CA11: radio mayor de 30 km → 400 con ERR-GEN-04 y los límites en el texto
	@Test
	void CA11_radioFueraDeRango_ERR_GEN_04() {
		RespuestaError error = buscarSinSesion(Map.of("latitud", LAT, "longitud", LNG, "radioKm", 31))
				.expectStatus().isBadRequest()
				.expectBody(RespuestaError.class).returnResult().getResponseBody();

		assertThat(error.errores()).containsExactly(
				new Error("ERR-GEN-04", null, "El valor de radio de búsqueda debe estar entre 1 y 30."));
	}

	// CA9 (P8): sin sesión, el visitante también llena los filtros de departamento, ciudad y tipo de cancha
	@Test
	void CA9_filtrosSinSesion_200() {
		cliente.get().uri("/api/departamentos").exchange().expectStatus().isOk();
		cliente.get().uri("/api/departamentos/{id}/ciudades", ANTIOQUIA).exchange().expectStatus().isOk();
		cliente.get().uri("/api/tipos-cancha").exchange().expectStatus().isOk();
	}

}
