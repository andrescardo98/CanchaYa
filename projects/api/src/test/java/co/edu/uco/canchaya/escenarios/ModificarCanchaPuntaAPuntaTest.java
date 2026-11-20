package co.edu.uco.canchaya.escenarios;

import static org.assertj.core.api.Assertions.assertThat;

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

/** HU9 completa: petición HTTP con token real → seguridad → caso de uso → base en Docker. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ModificarCanchaPuntaAPuntaTest {

	// Identificadores de F5 y F7 en la V2
	private static final String F5 = "27d013e3-f5b3-5378-a7df-7c80429554fe";
	private static final String F7 = "91339eab-b705-528d-b608-55f9f44c044d";

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID escenario;
	private UUID cancha;
	private String tokenPropietario;

	record RespuestaToken(String token) {
	}

	@BeforeEach
	void preparar() {
		// Correos propios: InicioSesionPuntaAPuntaTest deja bloqueado a ana@correo.com (D28)
		UUID propietario = usuario("Ana Pérez", "propietario.hu9@correo.com", "1000000091", "ADMINISTRADOR_CANCHA");
		tokenPropietario = token("propietario.hu9@correo.com", "Clave2026", "203.0.113.91");
		escenario = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Cancha La 80', 'Calle 80 # 45-10', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, 'ACTIVO')""", escenario, propietario);
		cancha = UUID.randomUUID();
		jdbc.update("""
				insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora)
				values (?, ?, ?, 'Cancha 1', 80000)""", cancha, escenario, UUID.fromString(F5));
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

	private String token(String correo, String contrasena, String ip) {
		return cliente.post().uri("/api/sesiones").contentType(MediaType.APPLICATION_JSON)
				// IP propia para no chocar con el límite de intentos de otras pruebas (D28)
				.header("X-Forwarded-For", ip)
				.body("""
						{"correoElectronico": "%s", "contrasena": "%s"}
						""".formatted(correo, contrasena))
				.exchange().expectStatus().isOk()
				.expectBody(RespuestaToken.class).returnResult().getResponseBody().token();
	}

	private ResponseSpec modificar(String token, UUID id) {
		return cliente.put().uri("/api/canchas/{id}", id)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"nombre": "  Cancha Principal ", "tipoCanchaId": "%s", "precioHora": 95000}
						""".formatted(F7))
				.exchange();
	}

	private void sinCambios() {
		Map<String, Object> fila = jdbc.queryForMap("select nombre, tipo_cancha_id from cancha where id = ?", cancha);
		assertThat(fila).containsEntry("nombre", "Cancha 1").containsEntry("tipo_cancha_id", UUID.fromString(F5));
	}

	@Test
	void ca1ModificarCancha_200_EXI_CAN_02_yListaActualizada() {
		modificar(tokenPropietario, cancha).expectStatus().isOk()
				.expectBody()
				.jsonPath("$.codigo").isEqualTo("EXI-CAN-02")
				.jsonPath("$.mensaje").isEqualTo("La cancha se modificó correctamente.")
				.jsonPath("$.cancha.id").isEqualTo(cancha.toString())
				.jsonPath("$.cancha.nombre").isEqualTo("Cancha Principal")
				.jsonPath("$.cancha.estado").isEqualTo("DISPONIBLE");

		// La lista del escenario (HU6) muestra el cambio
		cliente.get().uri("/api/escenarios/{id}/canchas", escenario)
				.header("Authorization", "Bearer " + tokenPropietario).exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(1)
				.jsonPath("$[0].id").isEqualTo(cancha.toString())
				.jsonPath("$[0].nombre").isEqualTo("Cancha Principal")
				.jsonPath("$[0].tipoCanchaId").isEqualTo(F7)
				.jsonPath("$[0].precioHora").isEqualTo(95000.00);
	}

	@Test
	void ca3CanchaInexistente_404_ERR_CAN_07() {
		modificar(tokenPropietario, UUID.randomUUID()).expectStatus().isNotFound()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-CAN-07")
				.jsonPath("$.errores[0].mensaje").isEqualTo("La cancha no existe.");
		sinCambios();
	}

	// CA4: otro administrador de cancha, que es como queda un encargado de sede
	@Test
	void ca4NoPropietario_403_ERR_CAN_02() {
		usuario("Luis Gómez", "otro.hu9@correo.com", "1000000092", "ADMINISTRADOR_CANCHA");
		String tokenOtro = token("otro.hu9@correo.com", "Clave2026", "203.0.113.92");

		modificar(tokenOtro, cancha).expectStatus().isForbidden()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-CAN-02");
		sinCambios();
	}

	// CA4 y P3 de la especificación: el administrador del sistema tampoco puede
	@Test
	void ca4AdministradorDelSistema_403_ERR_CAN_02() {
		String tokenAdministrador = token("admin@canchaya.local", "Admin12345", "203.0.113.93");

		modificar(tokenAdministrador, cancha).expectStatus().isForbidden()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-CAN-02");
		sinCambios();
	}

}
