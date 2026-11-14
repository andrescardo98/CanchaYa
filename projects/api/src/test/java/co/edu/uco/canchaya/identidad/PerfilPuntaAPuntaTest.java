package co.edu.uco.canchaya.identidad;

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

/**
 * HU3 completa: petición HTTP de verdad → seguridad → caso de uso → base en Docker.
 *
 * <p>El límite de intentos de HU2 vive en memoria y se comparte entre pruebas: cada prueba inicia
 * sesión desde su propia IP (cabecera X-Forwarded-For) y los correos son distintos a los de
 * InicioSesionPuntaAPuntaTest, que deja bloqueada la cuenta ana@correo.com.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class PerfilPuntaAPuntaTest {

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID idAna;
	private UUID idLuis;

	@BeforeEach
	void preparar() {
		idAna = registrar("Ana Pérez", "ana.perfil@correo.com", "1036123456", "JUGADOR");
		idLuis = registrar("Luis Gómez", "luis.perfil@correo.com", "98765432", "ADMINISTRADOR_CANCHA");
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	@Test
	void consultar_elFormularioSeLlenaConLosDatosGuardados() {
		String token = tokenDe("ana.perfil@correo.com", "Clave2026", "203.0.113.1");

		cliente.get().uri("/api/usuarios/" + idAna).header("Authorization", "Bearer " + token).exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.perfil.nombre").isEqualTo("Ana Pérez")
				.jsonPath("$.perfil.tipoDocumento").isEqualTo("CC")
				.jsonPath("$.perfil.numeroDocumento").isEqualTo("1036123456")
				.jsonPath("$.perfil.rol").isEqualTo("JUGADOR");
	}

	@Test
	void ca1_cambiaCorreoYContrasena_yConEllosSePuedeIniciarSesion() {
		String token = tokenDe("ana.perfil@correo.com", "Clave2026", "203.0.113.2");

		actualizar(idAna, token, perfil("Ana María Pérez", "Ana.Maria@Correo.com", "\"nuevaContrasena\": \"Nueva2026\""))
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.codigo").isEqualTo("EXI-USU-02")
				.jsonPath("$.mensaje").isEqualTo("Su perfil se actualizó correctamente.")
				.jsonPath("$.perfil.nombre").isEqualTo("Ana María Pérez")
				.jsonPath("$.perfil.correoElectronico").isEqualTo("ana.maria@correo.com");

		iniciar("ana.maria@correo.com", "Nueva2026", "203.0.113.2").expectStatus().isOk();
		iniciar("ana.maria@correo.com", "Clave2026", "203.0.113.2").expectStatus().isEqualTo(422);
		// El token solo lleva el id y el rol: sigue sirviendo después de cambiar el correo
		cliente.get().uri("/api/usuarios/" + idAna).header("Authorization", "Bearer " + token).exchange()
				.expectStatus().isOk();
	}

	@Test
	void ca4_sinNuevaContrasena_sigueConLaActual() {
		String token = tokenDe("ana.perfil@correo.com", "Clave2026", "203.0.113.3");

		actualizar(idAna, token, perfil("Ana María Pérez", "ana.perfil@correo.com", "\"nuevaContrasena\": \"\""))
				.expectStatus().isOk();

		iniciar("ana.perfil@correo.com", "Clave2026", "203.0.113.3").expectStatus().isOk();
	}

	@Test
	void ca3_elCorreoDeOtroUsuario_respondeErrUsu10() {
		String token = tokenDe("ana.perfil@correo.com", "Clave2026", "203.0.113.4");

		actualizar(idAna, token, perfil("Ana Pérez", "LUIS.Perfil@correo.com", null))
				.expectStatus().isEqualTo(422)
				.expectBody()
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-USU-10");
	}

	@Test
	void ca5_cambiarElRol_respondeErrUsu11() {
		String token = tokenDe("ana.perfil@correo.com", "Clave2026", "203.0.113.5");

		actualizar(idAna, token, perfil("Ana Pérez", "ana.perfil@correo.com", "\"rol\": \"ADMINISTRADOR_SISTEMA\""))
				.expectStatus().isEqualTo(422)
				.expectBody()
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-USU-11");
	}

	@Test
	void ca6_conElTokenDeOtroUsuario_respondeErrUsu09YNoCambiaNada() {
		String tokenDeLuis = tokenDe("luis.perfil@correo.com", "Clave2026", "203.0.113.6");

		actualizar(idAna, tokenDeLuis, perfil("Intruso", "intruso@correo.com", null))
				.expectStatus().isForbidden()
				.expectBody()
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-USU-09");
		cliente.get().uri("/api/usuarios/" + idAna).header("Authorization", "Bearer " + tokenDeLuis).exchange()
				.expectStatus().isForbidden();

		iniciar("ana.perfil@correo.com", "Clave2026", "203.0.113.6").expectStatus().isOk();
	}

	@Test
	void ca8_unaContrasenaDebil_respondeErrUsu12() {
		String token = tokenDe("ana.perfil@correo.com", "Clave2026", "203.0.113.7");

		actualizar(idAna, token, perfil("Ana Pérez", "ana.perfil@correo.com", "\"nuevaContrasena\": \"soloLetras\""))
				.expectStatus().isEqualTo(422)
				.expectBody()
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-USU-12");
	}

	@Test
	void sinToken_respondeErrGen05() {
		actualizar(idAna, null, perfil("Ana Pérez", "ana.perfil@correo.com", null))
				.expectStatus().isUnauthorized()
				.expectBody()
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-GEN-05");
	}

	private static String perfil(String nombre, String correo, String extra) {
		return """
				{"nombre": "%s", "correoElectronico": "%s", "numeroCelular": "+573109876543"%s}
				""".formatted(nombre, correo, extra == null ? "" : ", " + extra);
	}

	private ResponseSpec actualizar(UUID id, String token, String json) {
		var peticion = cliente.put().uri("/api/usuarios/" + id).contentType(MediaType.APPLICATION_JSON);
		if (token != null) {
			peticion = peticion.header("Authorization", "Bearer " + token);
		}
		return peticion.body(json).exchange();
	}

	private ResponseSpec iniciar(String correo, String contrasena, String ip) {
		return cliente.post().uri("/api/sesiones")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Forwarded-For", ip)
				.body("""
						{"correoElectronico": "%s", "contrasena": "%s"}
						""".formatted(correo, contrasena))
				.exchange();
	}

	private String tokenDe(String correo, String contrasena, String ip) {
		return iniciar(correo, contrasena, ip)
				.expectStatus().isOk()
				.expectBody(RespuestaToken.class).returnResult().getResponseBody().token();
	}

	private UUID registrar(String nombre, String correo, String documento, String rol) {
		cliente.post().uri("/api/usuarios")
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{
						  "nombre": "%s",
						  "correoElectronico": "%s",
						  "contrasena": "Clave2026",
						  "tipoDocumento": "CC",
						  "numeroDocumento": "%s",
						  "numeroCelular": "+573001234567",
						  "rol": "%s"
						}
						""".formatted(nombre, correo, documento, rol))
				.exchange()
				.expectStatus().isCreated();
		return jdbc.queryForObject("select id from usuario where correo_electronico = ?", UUID.class, correo);
	}

	private record RespuestaToken(String token) {
	}

}
