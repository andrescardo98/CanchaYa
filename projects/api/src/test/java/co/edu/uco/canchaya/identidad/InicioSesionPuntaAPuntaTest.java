package co.edu.uco.canchaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

import co.edu.uco.canchaya.TestcontainersConfiguration;

/**
 * HU2 completa: petición HTTP de verdad → seguridad → caso de uso → base en Docker.
 *
 * <p>El conteo de intentos (D28) vive en la memoria del servidor y se comparte entre pruebas, así
 * que cada prueba llega desde su propia IP (cabecera X-Forwarded-For) y no bloquea a las demás.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class InicioSesionPuntaAPuntaTest {

	// Una ruta que exige sesión y no tiene controlador: la seguridad responde antes.
	private static final String RUTA_PROTEGIDA = "/api/ruta-sin-controlador";

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private JwtDecoder decoder;

	@Autowired
	private JwtEncoder encoder;

	@AfterEach
	void limpiar() {
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	@Test
	void ca1_credencialesCorrectas_entregaUnTokenConElIdYElRolDelUsuario() {
		registrar("ana@correo.com", "1036123456", "ADMINISTRADOR_CANCHA");
		UUID id = jdbc.queryForObject("select id from usuario where correo_electronico = 'ana@correo.com'", UUID.class);

		RespuestaToken respuesta = iniciar("Ana@Correo.com", "Clave2026", "198.51.100.1")
				.expectStatus().isOk()
				.expectBody(RespuestaToken.class).returnResult().getResponseBody();

		// El rol le dice al frontend a qué página de inicio llevarlo
		assertThat(respuesta.rol()).isEqualTo("ADMINISTRADOR_CANCHA");
		Jwt jwt = decoder.decode(respuesta.token());
		assertThat(respuesta.venceEn()).isEqualTo(jwt.getExpiresAt());
		assertThat(jwt.getSubject()).isEqualTo(id.toString());
		assertThat(jwt.getClaimAsString("rol")).isEqualTo("ADMINISTRADOR_CANCHA");
		// PAR-USU-09: 60 minutos
		assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plus(60, ChronoUnit.MINUTES));
		// El token no lleva datos personales
		assertThat(jwt.getClaims()).doesNotContainKeys("correoElectronico", "nombre", "numeroDocumento");
	}

	@Test
	void ca1_elAdministradorDelSistemaIniciaSesionConSuRol() {
		// Lo crea la V2 con la contraseña local Admin12345 (RN-16)
		iniciar("admin@canchaya.local", "Admin12345", "198.51.100.2")
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.rol").isEqualTo("ADMINISTRADOR_SISTEMA");
	}

	@Test
	void ca1_conElTokenSePuedeEntrarAUnaRutaProtegida() {
		registrar("ana@correo.com", "1036123456", "JUGADOR");
		String token = tokenDe("ana@correo.com", "198.51.100.3");

		// 404 porque la ruta no existe; lo importante es que la seguridad la dejó pasar (no 401)
		cliente.get().uri(RUTA_PROTEGIDA).header("Authorization", "Bearer " + token).exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void ca3_ca4_correoNoRegistradoYContrasenaErrada_respondenIgual() {
		registrar("ana@correo.com", "1036123456", "JUGADOR");

		String noRegistrado = iniciar("nadie@correo.com", "Clave2026", "198.51.100.4")
				.expectStatus().isEqualTo(422)
				.expectBody(String.class).returnResult().getResponseBody();
		String contrasenaErrada = iniciar("ana@correo.com", "Otra2026", "198.51.100.4")
				.expectStatus().isEqualTo(422)
				.expectBody(String.class).returnResult().getResponseBody();

		assertThat(noRegistrado).contains("ERR-USU-06", "El correo electrónico o la contraseña son incorrectos.");
		assertThat(contrasenaErrada).contains("ERR-USU-07", "El correo electrónico o la contraseña son incorrectos.");
		// Solo cambia el código interno; el texto que ve el usuario es el mismo
		assertThat(noRegistrado.replace("ERR-USU-06", "")).isEqualTo(contrasenaErrada.replace("ERR-USU-07", ""));
	}

	@Test
	void ca5_sinToken_respondeErrGen05() {
		cliente.get().uri(RUTA_PROTEGIDA).exchange()
				.expectStatus().isUnauthorized()
				.expectBody()
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-GEN-05")
				.jsonPath("$.errores[0].mensaje").isEqualTo("Su sesión no está iniciada o venció. Inicie sesión para continuar.");
	}

	@Test
	void ca6_conElTokenVencido_respondeErrGen05() {
		Instant hace2Horas = Instant.now().minus(2, ChronoUnit.HOURS);
		String vencido = firmar(JwtClaimsSet.builder()
				.subject(UUID.randomUUID().toString())
				.claim("rol", "JUGADOR")
				.issuedAt(hace2Horas)
				.expiresAt(hace2Horas.plus(60, ChronoUnit.MINUTES))
				.build());

		cliente.get().uri(RUTA_PROTEGIDA).header("Authorization", "Bearer " + vencido).exchange()
				.expectStatus().isUnauthorized()
				.expectBody()
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-GEN-05");
	}

	@Test
	void unTokenAlterado_respondeErrGen05() {
		registrar("ana@correo.com", "1036123456", "JUGADOR");
		String token = tokenDe("ana@correo.com", "198.51.100.5");
		// Se cambia una letra del medio de la firma. La última no sirve: en base64url lleva bits de
		// relleno y a veces cambiarla deja la misma firma (la prueba fallaba 1 de cada 16 veces).
		int posicion = token.lastIndexOf('.') + 10;
		char letra = token.charAt(posicion);
		String alterado = token.substring(0, posicion) + (letra == 'A' ? 'B' : 'A') + token.substring(posicion + 1);

		cliente.get().uri(RUTA_PROTEGIDA).header("Authorization", "Bearer " + alterado).exchange()
				.expectStatus().isUnauthorized()
				.expectBody()
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-GEN-05");
	}

	@Test
	void ca7_cincoFallos_bloqueanAunqueLaContrasenaSeaCorrecta() {
		registrar("ana@correo.com", "1036123456", "JUGADOR");
		for (int i = 0; i < 5; i++) {
			iniciar("ana@correo.com", "Otra2026", "198.51.100.6").expectStatus().isEqualTo(422);
		}

		iniciar("ana@correo.com", "Clave2026", "198.51.100.7")
				.expectStatus().isEqualTo(422)
				.expectBody()
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-USU-16")
				.jsonPath("$.errores[0].mensaje").isEqualTo(
						"Superó el número de intentos permitidos para iniciar sesión. Intente de nuevo en 15 minutos.");
	}

	@Test
	void ca7_elOrigenBloqueado_respondeIgualConUnCorreoQueNoExiste() {
		for (int i = 0; i < 5; i++) {
			iniciar("nadie" + i + "@correo.com", "Clave2026", "198.51.100.8").expectStatus().isEqualTo(422);
		}

		iniciar("otro@correo.com", "Clave2026", "198.51.100.8")
				.expectStatus().isEqualTo(422)
				.expectBody()
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-USU-16");
	}

	private ResponseSpec iniciar(String correo, String contrasena, String ip) {
		return cliente.post().uri("/api/sesiones")
				.contentType(MediaType.APPLICATION_JSON)
				// Así llega la IP del cliente cuando hay un proxy delante, como en Render
				.header("X-Forwarded-For", ip)
				.body("""
						{"correoElectronico": "%s", "contrasena": "%s"}
						""".formatted(correo, contrasena))
				.exchange();
	}

	private String tokenDe(String correo, String ip) {
		return iniciar(correo, "Clave2026", ip)
				.expectStatus().isOk()
				.expectBody(RespuestaToken.class).returnResult().getResponseBody().token();
	}

	private String firmar(JwtClaimsSet datos) {
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), datos))
				.getTokenValue();
	}

	private void registrar(String correo, String documento, String rol) {
		cliente.post().uri("/api/usuarios")
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{
						  "nombre": "Ana Pérez",
						  "correoElectronico": "%s",
						  "contrasena": "Clave2026",
						  "tipoDocumento": "CC",
						  "numeroDocumento": "%s",
						  "numeroCelular": "+573001234567",
						  "rol": "%s"
						}
						""".formatted(correo, documento, rol))
				.exchange()
				.expectStatus().isCreated();
	}

	private record RespuestaToken(String token, String rol, Instant venceEn) {
	}

}
