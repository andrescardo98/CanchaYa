package co.edu.uco.canchaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
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

// Prueba todo el registro: peticion HTTP de verdad -> caso de uso -> base en Docker
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class RegistroUsuarioPuntaAPuntaTest {

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	@AfterEach
	void limpiar() {
		// el admin del sistema lo carga la V2, ese no se borra
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	@Test
	void ca1_registroValido_responde201ConExiUsu01YQuedaGuardado() {
		registrar(solicitud("Ana@Correo.com", "1036123456"))
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.codigo").isEqualTo("EXI-USU-01")
				.jsonPath("$.mensaje").isEqualTo("Su cuenta se creó correctamente. Inicie sesión para continuar.")
				.jsonPath("$.usuario.id").isNotEmpty()
				.jsonPath("$.usuario.correoElectronico").isEqualTo("ana@correo.com")
				// la respuesta no debe traer la contraseña
				.jsonPath("$.usuario.contrasena").doesNotExist();

		assertThat(contarUsuarios()).isEqualTo(1);
	}

	@Test
	void ca2_datosInvalidos_devuelveTodosLosErroresYNoGuardaNada() {
		String invalida = """
				{
				  "nombre": "",
				  "correoElectronico": "ana-correo.com",
				  "contrasena": "Clave2026",
				  "tipoDocumento": "CC",
				  "numeroDocumento": "1036123456",
				  "numeroCelular": "3001234567",
				  "rol": "JUGADOR"
				}
				""";

		registrar(invalida)
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.errores.length()").isEqualTo(2)
				.jsonPath("$.errores[?(@.campo == 'nombre')].codigo").isEqualTo("ERR-GEN-01")
				.jsonPath("$.errores[?(@.campo == 'correoElectronico')].codigo").isEqualTo("ERR-GEN-02");

		assertThat(contarUsuarios()).isZero();
	}

	@Test
	void ca3_correoYaRegistrado_respondeErrUsu01YNoGuardaElSegundo() {
		registrar(solicitud("ana@correo.com", "1036123456")).expectStatus().isCreated();

		// mismo correo (con mayusculas) pero otro documento
		registrar(solicitud("ANA@correo.com", "1036999999"))
				.expectStatus().isEqualTo(422)
				.expectBody()
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-USU-01")
				.jsonPath("$.errores[0].mensaje").isEqualTo("Ya existe una cuenta registrada con este correo electrónico.");

		assertThat(contarUsuarios()).isEqualTo(1);
	}

	@Test
	void ca8_correoDisponible_cambiaDespuesDeRegistrarse() {
		consultarCorreo("ana@correo.com")
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.disponible").isEqualTo(true)
				.jsonPath("$.codigo").doesNotExist();

		registrar(solicitud("ana@correo.com", "1036123456")).expectStatus().isCreated();

		consultarCorreo("ana@correo.com")
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.disponible").isEqualTo(false)
				.jsonPath("$.codigo").isEqualTo("ERR-USU-01");
	}

	private ResponseSpec registrar(String json) {
		return cliente.post().uri("/api/usuarios")
				.contentType(MediaType.APPLICATION_JSON)
				.body(json)
				.exchange();
	}

	private ResponseSpec consultarCorreo(String correo) {
		return cliente.get().uri("/api/usuarios/correo-disponible?correoElectronico={correo}", correo)
				.exchange();
	}

	private int contarUsuarios() {
		return jdbc.queryForObject("select count(*) from usuario where rol <> 'ADMINISTRADOR_SISTEMA'", Integer.class);
	}

	private static String solicitud(String correo, String documento) {
		return """
				{
				  "nombre": "Ana Pérez",
				  "correoElectronico": "%s",
				  "contrasena": "Clave2026",
				  "tipoDocumento": "CC",
				  "numeroDocumento": "%s",
				  "numeroCelular": "+573001234567",
				  "rol": "JUGADOR",
				  "autorizacionAcudiente": false
				}
				""".formatted(correo, documento);
	}

}
