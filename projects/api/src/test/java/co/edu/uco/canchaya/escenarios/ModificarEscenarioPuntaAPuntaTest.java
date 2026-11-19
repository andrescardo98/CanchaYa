package co.edu.uco.canchaya.escenarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import co.edu.uco.canchaya.TestcontainersConfiguration;

/**
 * HU8 completa: petición HTTP con token real → seguridad → caso de uso → base en Docker y fotos en disco.
 * El escenario se registra por HU5, así su foto existe de verdad en la carpeta.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ModificarEscenarioPuntaAPuntaTest {

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

	private UUID medellin;
	private String tokenPropietario;
	private String tokenOtroAdministrador;
	private UUID escenario;
	private String fotoOriginal;

	record RespuestaToken(String token) {
	}

	record RespuestaRegistro(String codigo, String mensaje, UUID id) {
	}

	record Escenario(UUID id, String nombre, UUID ciudadId, List<String> fotos, String estado) {
	}

	record RespuestaModificacion(String codigo, String mensaje, Escenario escenario) {
	}

	@BeforeEach
	void preparar() {
		UUID rionegro = jdbc.queryForObject("select id from ciudad where nombre = 'Rionegro'", UUID.class);
		medellin = jdbc.queryForObject("select id from ciudad where nombre = 'Medellín'", UUID.class);
		tokenPropietario = tokenDeUsuarioNuevo("1000000801", "203.0.113.181");
		tokenOtroAdministrador = tokenDeUsuarioNuevo("1000000802", "203.0.113.182");

		escenario = registrarEscenario(rionegro);
		fotoOriginal = jdbc.queryForObject("select url from foto_escenario where escenario_id = ?", String.class,
				escenario);
		// El perfil público (HU19) solo muestra escenarios activos; la activación es de HU13
		jdbc.update("update escenario set estado = 'ACTIVO' where id = ?", escenario);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from foto_escenario");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private String tokenDeUsuarioNuevo(String documento, String ip) {
		String correo = documento + "@correo.com";
		cliente.post().uri("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"nombre": "Ana Pérez", "correoElectronico": "%s", "contrasena": "Clave2026",
						 "tipoDocumento": "CC", "numeroDocumento": "%s", "numeroCelular": "+573001234567",
						 "rol": "ADMINISTRADOR_CANCHA"}
						""".formatted(correo, documento))
				.exchange().expectStatus().isCreated();
		return cliente.post().uri("/api/sesiones").contentType(MediaType.APPLICATION_JSON)
				// IP propia para no chocar con el límite de intentos de otras pruebas (D28)
				.header("X-Forwarded-For", ip)
				.body("""
						{"correoElectronico": "%s", "contrasena": "Clave2026"}
						""".formatted(correo))
				.exchange().expectStatus().isOk()
				.expectBody(RespuestaToken.class).returnResult().getResponseBody().token();
	}

	private static HttpEntity<String> json(String contenido) {
		HttpHeaders cabeceras = new HttpHeaders();
		cabeceras.setContentType(MediaType.APPLICATION_JSON);
		return new HttpEntity<>(contenido, cabeceras);
	}

	private static HttpEntity<ByteArrayResource> foto(String nombre, byte[] contenido, MediaType tipo) {
		HttpHeaders cabeceras = new HttpHeaders();
		cabeceras.setContentType(tipo);
		return new HttpEntity<>(new ByteArrayResource(contenido) {
			@Override
			public String getFilename() {
				return nombre;
			}
		}, cabeceras);
	}

	private static byte[] jpg() {
		return new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3 };
	}

	private static byte[] png() {
		return new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 4, 5, 6 };
	}

	private UUID registrarEscenario(UUID ciudad) {
		MultiValueMap<String, Object> formulario = new LinkedMultiValueMap<>();
		formulario.add("datos", json("""
				{"nombre": "Cancha La 80", "direccion": "Calle 80 # 45-10", "ciudadId": "%s",
				 "latitud": 6.155123, "longitud": -75.3737, "telefonoContacto": "+573001234567"}
				""".formatted(ciudad)));
		formulario.add("fotos", foto("original.jpg", jpg(), MediaType.IMAGE_JPEG));
		return cliente.post().uri("/api/escenarios").header("Authorization", "Bearer " + tokenPropietario)
				.contentType(MediaType.MULTIPART_FORM_DATA).body(formulario)
				.exchange().expectStatus().isCreated()
				.expectBody(RespuestaRegistro.class).returnResult().getResponseBody().id();
	}

	private ResponseSpec modificar(UUID id, String token, String fotosConservadas, boolean conFotoNueva) {
		MultiValueMap<String, Object> formulario = new LinkedMultiValueMap<>();
		formulario.add("datos", json("""
				{"nombre": "Cancha El Templo", "direccion": "Carrera 70 # 30-20", "ciudadId": "%s",
				 "latitud": 6.244203, "longitud": -75.581215, "telefonoContacto": "+573109876543"%s}
				""".formatted(medellin, fotosConservadas == null ? "" : ", \"fotosConservadas\": " + fotosConservadas)));
		if (conFotoNueva) {
			formulario.add("fotos", foto("nueva.png", png(), MediaType.IMAGE_PNG));
		}
		return cliente.put().uri("/api/escenarios/{id}", id).header("Authorization", "Bearer " + token)
				.contentType(MediaType.MULTIPART_FORM_DATA).body(formulario).exchange();
	}

	private ResponseSpec consultar(UUID id, String token) {
		return cliente.get().uri("/api/escenarios/{id}", id).header("Authorization", "Bearer " + token).exchange();
	}

	private static String nombreArchivo(String url) {
		return url.substring(url.lastIndexOf('/') + 1);
	}

	private String nombreGuardado() {
		return jdbc.queryForObject("select nombre from escenario where id = ?", String.class, escenario);
	}

	@Test
	void ca1ElPropietarioModificaYElPerfilPublicoMuestraElCambio() {
		// El formulario se llena con los datos actuales
		consultar(escenario, tokenPropietario).expectStatus().isOk()
				.expectBody().jsonPath("$.nombre").isEqualTo("Cancha La 80")
				.jsonPath("$.fotos[0]").isEqualTo(fotoOriginal);

		// Conserva la foto original y agrega una nueva, que va después
		RespuestaModificacion respuesta = modificar(escenario, tokenPropietario, "[\"" + fotoOriginal + "\"]", true)
				.expectStatus().isOk()
				.expectBody(RespuestaModificacion.class).returnResult().getResponseBody();

		assertThat(respuesta.codigo()).isEqualTo("EXI-ESC-02");
		assertThat(respuesta.mensaje()).isEqualTo("El escenario se modificó correctamente.");
		assertThat(respuesta.escenario().nombre()).isEqualTo("Cancha El Templo");
		assertThat(respuesta.escenario().ciudadId()).isEqualTo(medellin);
		// Modificar no cambia el estado
		assertThat(respuesta.escenario().estado()).isEqualTo("ACTIVO");
		assertThat(respuesta.escenario().fotos()).hasSize(2).first().isEqualTo(fotoOriginal);
		String fotoNueva = respuesta.escenario().fotos().get(1);
		assertThat(Files.exists(carpetaFotos.resolve(nombreArchivo(fotoNueva)))).isTrue();

		cliente.get().uri("/api/escenarios/{id}/perfil", escenario).exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.nombre").isEqualTo("Cancha El Templo")
				.jsonPath("$.ciudad").isEqualTo("Medellín")
				.jsonPath("$.fotos[0]").isEqualTo(fotoOriginal)
				.jsonPath("$.fotos[1]").isEqualTo(fotoNueva);
	}

	@Test
	void ca1LaFotoQueSeQuitaSeBorraDelAlmacenamiento() {
		assertThat(Files.exists(carpetaFotos.resolve(nombreArchivo(fotoOriginal)))).isTrue();

		modificar(escenario, tokenPropietario, "[]", false).expectStatus().isOk()
				.expectBody().jsonPath("$.escenario.fotos").isEmpty();

		assertThat(Files.exists(carpetaFotos.resolve(nombreArchivo(fotoOriginal)))).isFalse();
		assertThat(jdbc.queryForObject("select count(*) from foto_escenario where escenario_id = ?", Integer.class,
				escenario)).isZero();
	}

	@Test
	void ca2SinFotosConservadasRespondeErrGen01YNoCambiaNada() {
		modificar(escenario, tokenPropietario, null, false)
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errores[0].campo").isEqualTo("fotosConservadas")
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-GEN-01");

		assertThat(nombreGuardado()).isEqualTo("Cancha La 80");
		assertThat(Files.exists(carpetaFotos.resolve(nombreArchivo(fotoOriginal)))).isTrue();
	}

	@Test
	void ca3UnEscenarioQueNoExisteRespondeErrEsc06() {
		modificar(UUID.randomUUID(), tokenPropietario, "[]", true)
				.expectStatus().isNotFound()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-ESC-06");
		consultar(UUID.randomUUID(), tokenPropietario)
				.expectStatus().isNotFound()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-ESC-06");
	}

	@Test
	void ca4OtroAdministradorDeCanchaNoPuedeModificarNiConsultar() {
		long fotosAntes = archivosEnCarpeta();

		modificar(escenario, tokenOtroAdministrador, "[]", true)
				.expectStatus().isForbidden()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-ESC-07")
				.jsonPath("$.errores[0].mensaje").isEqualTo("No tiene permiso para modificar este escenario.");
		consultar(escenario, tokenOtroAdministrador)
				.expectStatus().isForbidden()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-ESC-07");

		// No se guardó nada, no se subió la foto nueva ni se borró la original
		assertThat(nombreGuardado()).isEqualTo("Cancha La 80");
		assertThat(archivosEnCarpeta()).isEqualTo(fotosAntes);
		assertThat(Files.exists(carpetaFotos.resolve(nombreArchivo(fotoOriginal)))).isTrue();
	}

	@Test
	void p1UnaFotoConservadaQueNoEsDelEscenarioRespondeErrGen07YNoCambiaNada() {
		long fotosAntes = archivosEnCarpeta();

		modificar(escenario, tokenPropietario, "[\"https://otro.sitio/ajena.jpg\"]", true)
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-GEN-07");

		assertThat(nombreGuardado()).isEqualTo("Cancha La 80");
		assertThat(archivosEnCarpeta()).isEqualTo(fotosAntes);
	}

	private long archivosEnCarpeta() {
		try (var archivos = Files.list(carpetaFotos)) {
			return archivos.count();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
