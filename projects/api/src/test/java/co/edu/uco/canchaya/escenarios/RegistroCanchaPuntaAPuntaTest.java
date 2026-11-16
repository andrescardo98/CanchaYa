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

/** HU6 completa: petición HTTP con token real → seguridad → caso de uso → base en Docker. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class RegistroCanchaPuntaAPuntaTest {

	private static final String F7 = "91339eab-b705-528d-b608-55f9f44c044d";

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID escenario;
	private String tokenPropietario;

	record RespuestaToken(String token) {
	}

	@BeforeEach
	void preparar() {
		UUID propietario = usuario("Ana Pérez", "ana@correo.com", "1000000071", "ADMINISTRADOR_CANCHA");
		tokenPropietario = token("ana@correo.com", "203.0.113.71");
		escenario = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Cancha La 80', 'Calle 80 # 45-10', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, 'INACTIVO')""", escenario, propietario);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from cancha");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private UUID usuario(String nombre, String correo, String documento, String rol) {
		cliente.post().uri("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"nombre": "%s", "correoElectronico": "%s", "contrasena": "Clave2026",
						 "tipoDocumento": "CC", "numeroDocumento": "%s", "numeroCelular": "+573001234567",
						 "rol": "%s"}
						""".formatted(nombre, correo, documento, rol))
				.exchange().expectStatus().isCreated();
		return jdbc.queryForObject("select id from usuario where correo_electronico = ?", UUID.class, correo);
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

	private ResponseSpec registrar(String token, String nombre) {
		return cliente.post().uri("/api/escenarios/{id}/canchas", escenario)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"nombre": "%s", "tipoCanchaId": "%s", "precioHora": 95000}
						""".formatted(nombre, F7))
				.exchange();
	}

	@Test
	void ca1ElPropietarioRegistraLaCanchaYApareceEnLaLista() {
		registrar(tokenPropietario, "Cancha 1").expectStatus().isCreated()
				.expectBody().jsonPath("$.codigo").isEqualTo("EXI-CAN-01");

		assertThat(jdbc.queryForObject("select estado from cancha where escenario_id = ?", String.class, escenario))
				.isEqualTo("DISPONIBLE");
		cliente.get().uri("/api/escenarios/{id}/canchas", escenario)
				.header("Authorization", "Bearer " + tokenPropietario).exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$[0].nombre").isEqualTo("Cancha 1")
				.jsonPath("$[0].tipoCanchaId").isEqualTo(F7);
	}

	@Test
	void ca5ElMismoNombreConOtrasMayusculasSeRechaza() {
		registrar(tokenPropietario, "Cancha 1").expectStatus().isCreated();

		registrar(tokenPropietario, "CANCHA 1").expectStatus().isEqualTo(422)
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-CAN-03");
		assertThat(jdbc.queryForObject("select count(*) from cancha", Integer.class)).isEqualTo(1);
	}

	// CA4: otro administrador de cancha, que es como queda un encargado de sede
	@Test
	void ca4OtroAdministradorDeCanchaRecibe403() {
		usuario("Luis Gómez", "luis@correo.com", "1000000072", "ADMINISTRADOR_CANCHA");
		String tokenOtro = token("luis@correo.com", "203.0.113.72");

		registrar(tokenOtro, "Cancha 1").expectStatus().isForbidden()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-CAN-02");
		assertThat(jdbc.queryForObject("select count(*) from cancha", Integer.class)).isZero();
	}

	@Test
	void ca4UnJugadorRecibe403() {
		usuario("Sara Ruiz", "sara@correo.com", "1000000073", "JUGADOR");
		String tokenJugador = token("sara@correo.com", "203.0.113.73");

		registrar(tokenJugador, "Cancha 1").expectStatus().isForbidden()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-CAN-02");
		assertThat(jdbc.queryForObject("select count(*) from cancha", Integer.class)).isZero();
	}

	@Test
	void ca10LosTiposSalenDelCatalogoCargado() {
		cliente.get().uri("/api/tipos-cancha").header("Authorization", "Bearer " + tokenPropietario).exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.length()").isEqualTo(3)
				.jsonPath("$[0].codigo").isEqualTo("F11");
	}

}
