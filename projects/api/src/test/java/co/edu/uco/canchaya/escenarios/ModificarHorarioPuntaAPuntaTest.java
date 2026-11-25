package co.edu.uco.canchaya.escenarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;

/**
 * HU11 completa: petición HTTP con token real → seguridad → caso de uso → base en Docker, y después
 * la lista de horarios (HU7) y la disponibilidad de una cancha (HU20) con el horario nuevo. Usa el
 * reloj real del servidor (D27), por eso la fecha se calcula desde hoy en Bogotá.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ModificarHorarioPuntaAPuntaTest {

	// Identificador de F5 en la V2
	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");
	// Pasado mañana: ninguna franja ha empezado, sea cual sea la hora en que corra la prueba
	private static final LocalDate FECHA = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(2);
	private static final String DIA = DiaSemana.de(FECHA.getDayOfWeek()).name();

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID escenario;
	private UUID cancha;
	private UUID horario;
	private String tokenPropietario;
	private String tokenOtroAdministrador;

	record RespuestaToken(String token) {
	}

	@BeforeEach
	void preparar() {
		registrar("Ana Pérez", "propietario.hu11@correo.com", "1000000121");
		registrar("Luis Gómez", "otro.hu11@correo.com", "1000000122");
		tokenPropietario = token("propietario.hu11@correo.com", "203.0.113.121");
		tokenOtroAdministrador = token("otro.hu11@correo.com", "203.0.113.122");

		UUID propietario = jdbc.queryForObject(
				"select id from usuario where correo_electronico = 'propietario.hu11@correo.com'", UUID.class);
		escenario = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Cancha La 80', 'Calle 80 # 45-10', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, 'ACTIVO')""", escenario, propietario);
		cancha = UUID.randomUUID();
		jdbc.update("""
				insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora, estado)
				values (?, ?, ?, 'Cancha 1', 80000, 'DISPONIBLE')""", cancha, escenario, F5);
		horario = UUID.randomUUID();
		jdbc.update("""
				insert into horario_recurrente (id, escenario_id, dia_semana, hora_inicio, hora_fin)
				values (?, ?, ?, '18:00', '22:00')""", horario, escenario, DIA);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from reserva");
		jdbc.update("delete from horario_recurrente");
		jdbc.update("delete from cancha");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private void registrar(String nombre, String correo, String documento) {
		cliente.post().uri("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"nombre": "%s", "correoElectronico": "%s", "contrasena": "Clave2026",
						 "tipoDocumento": "CC", "numeroDocumento": "%s", "numeroCelular": "+573001234567",
						 "rol": "ADMINISTRADOR_CANCHA"}
						""".formatted(nombre, correo, documento))
				.exchange().expectStatus().isCreated();
	}

	private String token(String correo, String ip) {
		return cliente.post().uri("/api/sesiones").contentType(MediaType.APPLICATION_JSON)
				// IP propia para no chocar con el límite de intentos de otras pruebas (D28)
				.header("X-Forwarded-For", ip)
				.body("""
						{"correoElectronico": "%s", "contrasena": "Clave2026"}
						""".formatted(correo))
				.exchange().expectStatus().isOk()
				.expectBody(RespuestaToken.class).returnResult().getResponseBody().token();
	}

	private ResponseSpec modificar(UUID id, String inicio, String fin, String token) {
		return cliente.put().uri("/api/horarios/{id}", id)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"diaSemana": "%s", "horaInicio": "%s", "horaFin": "%s"}
						""".formatted(DIA, inicio, fin))
				.exchange();
	}

	private Map<String, Object> horarioGuardado() {
		return jdbc.queryForMap("select dia_semana, hora_inicio, hora_fin from horario_recurrente where id = ?",
				horario);
	}

	// CA1 y CA8: se guarda, la lista y la disponibilidad salen con el horario nuevo y la reserva se conserva
	@Test
	void ca1ModificarHorario_200_EXI_HOR_02_listaYDisponibilidadActualizadas() {
		UUID jugador = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Juan Ruiz', 'jugador.hu11@correo.com', ?, 'CC', '1000000123', '+573001234567', 'JUGADOR', now())""",
				jugador, "x".repeat(60));
		UUID reserva = UUID.randomUUID();
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado, fecha_creacion)
				values (?, ?, ?, ?, ?, ?, 80000, 'CONFIRMADA', now())""",
				reserva, cancha, jugador, FECHA, LocalTime.of(20, 0), LocalTime.of(21, 0));

		modificar(horario, "06:00", "09:00", tokenPropietario)
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.codigo").isEqualTo("EXI-HOR-02")
				.jsonPath("$.mensaje").isEqualTo("El horario de atención se modificó correctamente.")
				.jsonPath("$.horario.id").isEqualTo(horario.toString())
				.jsonPath("$.horario.horaInicio").isEqualTo("06:00")
				.jsonPath("$.horario.horaFin").isEqualTo("09:00");

		cliente.get().uri("/api/escenarios/{id}/horarios", escenario)
				.header("Authorization", "Bearer " + tokenPropietario)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(1)
				.jsonPath("$[0].horaInicio").isEqualTo("06:00")
				.jsonPath("$[0].horaFin").isEqualTo("09:00");

		// Las franjas se calculan en cada consulta: solo las del horario nuevo, sin la de las 20:00
		cliente.get().uri("/api/canchas/{id}/disponibilidad?fecha={fecha}", cancha, FECHA)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.franjas.length()").isEqualTo(3)
				.jsonPath("$.franjas[0].horaInicio").isEqualTo("06:00")
				.jsonPath("$.franjas[2].horaFin").isEqualTo("09:00");

		// CA8 (política 14, D09)
		assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, reserva))
				.isEqualTo("CONFIRMADA");
	}

	@Test
	void ca3HorarioInexistente_404_ERR_HOR_06() {
		modificar(UUID.randomUUID(), "06:00", "09:00", tokenPropietario)
				.expectStatus().isNotFound()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-HOR-06");
	}

	@Test
	void ca4NoPropietario_403_ERR_HOR_02() {
		Map<String, Object> antes = horarioGuardado();

		modificar(horario, "06:00", "09:00", tokenOtroAdministrador)
				.expectStatus().isForbidden()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-HOR-02");

		assertThat(horarioGuardado()).isEqualTo(antes);
	}

}
