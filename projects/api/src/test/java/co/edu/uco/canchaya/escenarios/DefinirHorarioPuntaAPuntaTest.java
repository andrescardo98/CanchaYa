package co.edu.uco.canchaya.escenarios;

import static org.assertj.core.api.Assertions.assertThat;

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

/** HU7 completa: petición HTTP con token real → seguridad → caso de uso → base en Docker. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class DefinirHorarioPuntaAPuntaTest {

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID escenario;
	private String tokenPropietario;
	private String tokenOtroAdministrador;

	record RespuestaToken(String token) {
	}

	@BeforeEach
	void preparar() {
		registrar("Ana Pérez", "ana@correo.com", "1000000081");
		registrar("Luis Gómez", "luis@correo.com", "1000000082");
		tokenPropietario = token("ana@correo.com", "203.0.113.81");
		tokenOtroAdministrador = token("luis@correo.com", "203.0.113.82");

		UUID propietario = jdbc.queryForObject("select id from usuario where correo_electronico = 'ana@correo.com'",
				UUID.class);
		escenario = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Cancha La 80', 'Calle 80 # 45-10', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, 'ACTIVO')""", escenario, propietario);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from horario_recurrente");
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

	private ResponseSpec definir(UUID escenarioId, String dia, String inicio, String fin, String token) {
		return cliente.post().uri("/api/escenarios/{id}/horarios", escenarioId)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"diaSemana": "%s", "horaInicio": "%s", "horaFin": "%s"}
						""".formatted(dia, inicio, fin))
				.exchange();
	}

	private int horariosGuardados() {
		return jdbc.queryForObject("select count(*) from horario_recurrente where escenario_id = ?", Integer.class,
				escenario);
	}

	@Test
	void ca1DefineElHorarioYApareceEnLaLista() {
		definir(escenario, "LUNES", "06:00", "12:00", tokenPropietario)
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.codigo").isEqualTo("EXI-HOR-01")
				.jsonPath("$.horario.diaSemana").isEqualTo("LUNES")
				.jsonPath("$.horario.horaInicio").isEqualTo("06:00");

		cliente.get().uri("/api/escenarios/{id}/horarios", escenario)
				.header("Authorization", "Bearer " + tokenPropietario)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(1)
				.jsonPath("$[0].horaFin").isEqualTo("12:00");
	}

	@Test
	void ca3ElEscenarioNoExiste() {
		definir(UUID.randomUUID(), "LUNES", "06:00", "12:00", tokenPropietario)
				.expectStatus().isNotFound()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-HOR-01");
	}

	@Test
	void ca4OtroAdministradorNoDefineNiVeLosHorarios() {
		definir(escenario, "LUNES", "06:00", "12:00", tokenOtroAdministrador)
				.expectStatus().isForbidden()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-HOR-02");
		cliente.get().uri("/api/escenarios/{id}/horarios", escenario)
				.header("Authorization", "Bearer " + tokenOtroAdministrador)
				.exchange()
				.expectStatus().isForbidden();

		assertThat(horariosGuardados()).isZero();
	}

	@Test
	void ca5Ca6Ca8NoSeGuardaNada() {
		definir(escenario, "LUNES", "06:00", "12:00", tokenPropietario).expectStatus().isCreated();

		// CA5
		definir(escenario, "MARTES", "12:00", "12:00", tokenPropietario)
				.expectStatus().isEqualTo(422)
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-HOR-03");
		// CA6
		definir(escenario, "LUNES", "10:00", "14:00", tokenPropietario)
				.expectStatus().isEqualTo(422)
				.expectBody().jsonPath("$.errores[0].mensaje")
				.isEqualTo("El horario se cruza con otro horario ya definido para el lunes.");
		// CA8
		definir(escenario, "MARTES", "06:30", "12:00", tokenPropietario)
				.expectStatus().isEqualTo(422)
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-HOR-13");

		assertThat(horariosGuardados()).isEqualTo(1);
	}

	@Test
	void ca7DosHorariosDelMismoDiaQuedanVigentes() {
		definir(escenario, "SABADO", "06:00", "12:00", tokenPropietario).expectStatus().isCreated();
		definir(escenario, "SABADO", "14:00", "23:00", tokenPropietario).expectStatus().isCreated();

		cliente.get().uri("/api/escenarios/{id}/horarios", escenario)
				.header("Authorization", "Bearer " + tokenPropietario)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(2)
				.jsonPath("$[0].horaInicio").isEqualTo("06:00")
				.jsonPath("$[1].horaInicio").isEqualTo("14:00");
	}

}
