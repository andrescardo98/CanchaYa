package co.edu.uco.canchaya.reservas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
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
 * HU20 completa: petición HTTP sin token → seguridad → caso de uso de Reservas → ConsultaCanchas
 * de Escenarios → base en Docker. Los datos se preparan con SQL porque escenarios, canchas,
 * horarios, reservas y bloqueos se crean en otras HU. Usa el reloj real del servidor (D27), por eso
 * las fechas se calculan desde hoy en Bogotá.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class DisponibilidadPuntaAPuntaTest {

	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");
	private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Bogota"));
	// Pasado mañana: ninguna franja ha empezado, sea cual sea la hora en que corra la prueba
	private static final LocalDate FECHA = HOY.plusDays(2);

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID propietario;
	private UUID jugador;

	record RespuestaFranja(String horaInicio, String horaFin, boolean libre) {
	}

	record RespuestaDisponibilidad(UUID canchaId, LocalDate fecha, LocalDate fechaMinima, LocalDate fechaMaxima,
			int franjasMaximasPorReserva, List<RespuestaFranja> franjas, String codigo, String mensaje) {
	}

	@BeforeEach
	void preparar() {
		propietario = usuario("1000000201", "ADMINISTRADOR_CANCHA");
		jugador = usuario("1000000202", "JUGADOR");
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

	private UUID escenario(String estado) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Cancha La 80', 'Calle 80', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, ?)""", id, propietario, estado);
		return id;
	}

	private UUID cancha(UUID escenario, String estado) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora, estado)
				values (?, ?, ?, 'Cancha 1', 80000, ?)""", id, escenario, F5, estado);
		return id;
	}

	private void horario(UUID escenario, LocalDate fecha, int desde, int hasta) {
		jdbc.update("""
				insert into horario_recurrente (id, escenario_id, dia_semana, hora_inicio, hora_fin)
				values (?, ?, ?, ?, ?)""", UUID.randomUUID(), escenario, DiaSemana.de(fecha.getDayOfWeek()).name(),
				LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
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

	// CA8: sin cabecera Authorization
	private ResponseSpec consultarSinSesion(UUID cancha, LocalDate fecha) {
		return cliente.get().uri(uri -> {
			uri.path("/api/canchas/{canchaId}/disponibilidad");
			if (fecha != null) {
				uri.queryParam("fecha", fecha);
			}
			return uri.build(cancha);
		}).exchange();
	}

	private RespuestaDisponibilidad disponibilidad(UUID cancha, LocalDate fecha) {
		return consultarSinSesion(cancha, fecha)
				.expectStatus().isOk()
				.expectBody(RespuestaDisponibilidad.class).returnResult().getResponseBody();
	}

	private static RespuestaFranja franja(int hora, boolean libre) {
		return new RespuestaFranja(String.format("%02d:00", hora), String.format("%02d:00", hora + 1), libre);
	}

	@Test
	void CA1_CA2_CA8_sinSesionLasReservasConfirmadasYLosBloqueosOcupanLasCanceladasNo() {
		UUID escenario = escenario("ACTIVO");
		UUID cancha = cancha(escenario, "DISPONIBLE");
		horario(escenario, FECHA, 8, 14);
		reserva(cancha, "CONFIRMADA", 9, 11);
		reserva(cancha, "CANCELADA", 12, 13);
		bloqueo(cancha, 13, 14);

		RespuestaDisponibilidad respuesta = disponibilidad(cancha, FECHA);

		assertThat(respuesta.canchaId()).isEqualTo(cancha);
		assertThat(respuesta.fecha()).isEqualTo(FECHA);
		assertThat(respuesta.fechaMinima()).isEqualTo(HOY);
		assertThat(respuesta.fechaMaxima()).isEqualTo(HOY.plusDays(30));
		assertThat(respuesta.franjasMaximasPorReserva()).isEqualTo(2);
		assertThat(respuesta.franjas()).containsExactly(franja(8, true), franja(9, false), franja(10, false),
				franja(11, true), franja(12, true), franja(13, false));
		assertThat(respuesta.codigo()).isNull();
	}

	@Test
	void CA2_laRespuestaNoDiceQuienNiPorQue() {
		UUID escenario = escenario("ACTIVO");
		UUID cancha = cancha(escenario, "DISPONIBLE");
		horario(escenario, FECHA, 8, 14);
		reserva(cancha, "CONFIRMADA", 9, 10);
		bloqueo(cancha, 13, 14);

		consultarSinSesion(cancha, FECHA)
				.expectStatus().isOk()
				.expectBody(String.class)
				.value(cuerpo -> assertThat(cuerpo).doesNotContain(jugador.toString(), "MANTENIMIENTO",
						"Cambio de grama", "Ana Pérez"));
	}

	@Test
	void CA1_sinFecha_seUsaLaDeHoyEnBogota() {
		UUID escenario = escenario("ACTIVO");
		UUID cancha = cancha(escenario, "DISPONIBLE");

		assertThat(disponibilidad(cancha, null).fecha()).isEqualTo(HOY);
	}

	@Test
	void CA4_sinHorarioEseDia_INF_HOR_01() {
		UUID escenario = escenario("ACTIVO");
		UUID cancha = cancha(escenario, "DISPONIBLE");
		horario(escenario, FECHA, 8, 14);

		RespuestaDisponibilidad respuesta = disponibilidad(cancha, FECHA.plusDays(1));

		assertThat(respuesta.franjas()).isEmpty();
		assertThat(respuesta.codigo()).isEqualTo("INF-HOR-01");
		assertThat(respuesta.mensaje()).isEqualTo("No hay franjas disponibles para esta fecha.");
	}

	@Test
	void CA5_fueraDeLaVentana_422_ERR_HOR_10() {
		UUID escenario = escenario("ACTIVO");
		UUID cancha = cancha(escenario, "DISPONIBLE");

		consultarSinSesion(cancha, HOY.plusDays(31))
				.expectStatus().isEqualTo(422)
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-HOR-10");
		consultarSinSesion(cancha, HOY.minusDays(1))
				.expectStatus().isEqualTo(422)
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-HOR-10");
		// Hoy + 30 todavía se puede consultar (P6)
		consultarSinSesion(cancha, HOY.plusDays(30)).expectStatus().isOk();
	}

	@Test
	void CA6_canchaInexistenteInactivaODeEscenarioInactivo_404_ERR_RES_01() {
		UUID activo = escenario("ACTIVO");
		UUID inactiva = cancha(activo, "INACTIVA");
		UUID inactivo = escenario("INACTIVO");
		UUID deEscenarioInactivo = cancha(inactivo, "DISPONIBLE");
		horario(inactivo, FECHA, 8, 14);

		for (UUID cancha : List.of(UUID.randomUUID(), inactiva, deEscenarioInactivo)) {
			consultarSinSesion(cancha, FECHA)
					.expectStatus().isNotFound()
					.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-RES-01");
		}
	}

}
