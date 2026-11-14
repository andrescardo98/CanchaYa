package co.edu.uco.canchaya.escenarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

import co.edu.uco.canchaya.TestcontainersConfiguration;

/** HU5 completa: petición HTTP con token real → seguridad → caso de uso → base en Docker y fotos en disco. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class RegistroEscenarioPuntaAPuntaTest {

	private static final int MB = 1024 * 1024;

	@TempDir
	static Path carpetaFotos;

	@DynamicPropertySource
	static void fotosEnCarpetaTemporal(DynamicPropertyRegistry registro) {
		registro.add("canchaya.fotos.almacenamiento", () -> "local");
		registro.add("canchaya.fotos.local.carpeta", carpetaFotos::toString);
	}

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID rionegro;

	record RespuestaToken(String token) {
	}

	record RespuestaRegistro(String codigo, String mensaje, UUID id) {
	}

	@BeforeEach
	void preparar() {
		rionegro = jdbc.queryForObject("select id from ciudad where nombre = 'Rionegro'", UUID.class);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from foto_escenario");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private String tokenDeUsuarioNuevo(String rol, String documento) {
		String correo = documento + "@correo.com";
		cliente.post().uri("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"nombre": "Ana Pérez", "correoElectronico": "%s", "contrasena": "Clave2026",
						 "tipoDocumento": "CC", "numeroDocumento": "%s", "numeroCelular": "+573001234567", "rol": "%s"}
						""".formatted(correo, documento, rol))
				.exchange().expectStatus().isCreated();
		return cliente.post().uri("/api/sesiones").contentType(MediaType.APPLICATION_JSON)
				// IP propia para no chocar con el límite de intentos de otras pruebas (D28)
				.header("X-Forwarded-For", "203.0.113." + documento.substring(documento.length() - 2))
				.body("""
						{"correoElectronico": "%s", "contrasena": "Clave2026"}
						""".formatted(correo))
				.exchange().expectStatus().isOk()
				.expectBody(RespuestaToken.class).returnResult().getResponseBody().token();
	}

	private ResponseSpec registrar(String token, byte[] foto) {
		HttpHeaders json = new HttpHeaders();
		json.setContentType(MediaType.APPLICATION_JSON);
		HttpHeaders imagen = new HttpHeaders();
		imagen.setContentType(MediaType.IMAGE_JPEG);

		MultiValueMap<String, Object> formulario = new LinkedMultiValueMap<>();
		formulario.add("datos", new HttpEntity<>("""
				{"nombre": "Cancha La 80", "direccion": "Calle 80 # 45-10", "ciudadId": "%s",
				 "latitud": 6.155123, "longitud": -75.3737, "telefonoContacto": "+573001234567"}
				""".formatted(rionegro), json));
		formulario.add("fotos", new HttpEntity<>(new ByteArrayResource(foto) {
			@Override
			public String getFilename() {
				return "cancha.jpg";
			}
		}, imagen));
		return cliente.post().uri("/api/escenarios")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(formulario)
				.exchange();
	}

	private static byte[] jpg(int bytes) {
		byte[] contenido = new byte[bytes];
		contenido[0] = (byte) 0xFF;
		contenido[1] = (byte) 0xD8;
		contenido[2] = (byte) 0xFF;
		return contenido;
	}

	@Test
	void ca1ElAdministradorDeCanchaRegistraYLaFotoQuedaPublicada() {
		String token = tokenDeUsuarioNuevo("ADMINISTRADOR_CANCHA", "1000000011");

		RespuestaRegistro respuesta = registrar(token, jpg(100))
				.expectStatus().isCreated()
				.expectBody(RespuestaRegistro.class).returnResult().getResponseBody();

		assertThat(respuesta.codigo()).isEqualTo("EXI-ESC-01");
		assertThat(jdbc.queryForObject("select estado from escenario where id = ?", String.class, respuesta.id()))
				.isEqualTo("INACTIVO");
		String url = jdbc.queryForObject("select url from foto_escenario where escenario_id = ?", String.class,
				respuesta.id());
		// La foto se puede ver sin sesión, como en el bucket público de Supabase
		cliente.get().uri(url.substring(url.indexOf("/fotos-locales/"))).exchange()
				.expectStatus().isOk()
				.expectBody(byte[].class).isEqualTo(jpg(100));
	}

	@Test
	void ca3UnJugadorNoPuedeRegistrar() {
		String token = tokenDeUsuarioNuevo("JUGADOR", "1000000012");

		registrar(token, jpg(100))
				.expectStatus().isEqualTo(422)
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-ESC-01");
		assertThat(jdbc.queryForObject("select count(*) from escenario", Integer.class)).isZero();
	}

	@Test
	void ca11UnaFotoDeMasDeDosMbLlegaAlCasoDeUsoYRespondeErrEsc13() {
		String token = tokenDeUsuarioNuevo("ADMINISTRADOR_CANCHA", "1000000013");

		registrar(token, jpg(2 * MB + 1))
				.expectStatus().isEqualTo(422)
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-ESC-13")
				.jsonPath("$.errores[0].mensaje").value(mensaje -> assertThat((String) mensaje)
						.contains("la foto cancha.jpg pesa más de 2 MB"));
		assertThat(jdbc.queryForObject("select count(*) from escenario", Integer.class)).isZero();
	}

}
