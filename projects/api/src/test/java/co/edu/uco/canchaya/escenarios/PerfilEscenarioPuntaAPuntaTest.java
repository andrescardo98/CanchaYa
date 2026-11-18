package co.edu.uco.canchaya.escenarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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

/**
 * HU19 completa: petición HTTP sin token → seguridad → caso de uso → repositorios → base en Docker.
 * Los datos se preparan con SQL porque registrar, activar y definir horarios ya tienen sus propias
 * pruebas (HU5, HU13, HU6 y HU7).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class PerfilEscenarioPuntaAPuntaTest {

	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID propietario;

	record RespuestaCancha(UUID id, String nombre, String tipoCancha, BigDecimal precioHora) {
	}

	record RespuestaPerfil(UUID id, String nombre, String descripcion, String direccion, String ciudad,
			String departamento, BigDecimal latitud, BigDecimal longitud, String telefonoContacto,
			String correoContacto, List<String> fotos, List<RespuestaCancha> canchas,
			BigDecimal calificacionPromedio, String codigo, String mensaje) {
	}

	@BeforeEach
	void preparar() {
		propietario = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Ana Pérez', 'ana.perfil@correo.com', ?, 'CC', '1000000191', '+573009998877',
				  'ADMINISTRADOR_CANCHA', now())""", propietario, "x".repeat(60));
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from horario_recurrente");
		jdbc.update("delete from cancha");
		jdbc.update("delete from foto_escenario");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private UUID escenario(String estado) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, descripcion, direccion, ciudad_id, latitud, longitud,
				  telefono_contacto, correo_contacto, administrador_id, estado)
				values (?, 'Cancha La 80', 'Grama sintética nueva', 'Calle 80 # 45-10',
				  (select id from ciudad where nombre = 'Rionegro'), 6.155123, -75.3737, '+576045551234',
				  'contacto@la80.com', ?, ?)""", id, propietario, estado);
		return id;
	}

	private UUID cancha(UUID escenario, String nombre, String estado) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora, estado)
				values (?, ?, ?, ?, 80000, ?)""", id, escenario, F5, nombre, estado);
		return id;
	}

	private void horario(UUID escenario) {
		jdbc.update("""
				insert into horario_recurrente (id, escenario_id, dia_semana, hora_inicio, hora_fin)
				values (?, ?, 'LUNES', '08:00', '22:00')""", UUID.randomUUID(), escenario);
	}

	private void foto(UUID escenario, String url) {
		jdbc.update("insert into foto_escenario (id, escenario_id, url, orden) values (?, ?, ?, 1)",
				UUID.randomUUID(), escenario, url);
	}

	// Sin cabecera Authorization: la ficha es pública (AG-11)
	private ResponseSpec consultarSinSesion(UUID id) {
		return cliente.get().uri("/api/escenarios/{id}/perfil", id).exchange();
	}

	@Test
	void ca1Y_ca6SinSesionUnEscenarioActivoMuestraSuPerfilConLasCanchasPublicables() {
		UUID escenario = escenario("ACTIVO");
		horario(escenario);
		foto(escenario, "https://fotos/1.jpg");
		UUID cancha1 = cancha(escenario, "Cancha 1", "DISPONIBLE");
		cancha(escenario, "Cancha 2", "INACTIVA");

		RespuestaPerfil perfil = consultarSinSesion(escenario)
				.expectStatus().isOk()
				.expectBody(RespuestaPerfil.class).returnResult().getResponseBody();

		assertThat(perfil.nombre()).isEqualTo("Cancha La 80");
		assertThat(perfil.descripcion()).isEqualTo("Grama sintética nueva");
		assertThat(perfil.ciudad()).isEqualTo("Rionegro");
		assertThat(perfil.departamento()).isEqualTo("Antioquia");
		assertThat(perfil.latitud()).isEqualByComparingTo("6.155123");
		assertThat(perfil.telefonoContacto()).isEqualTo("+576045551234");
		assertThat(perfil.correoContacto()).isEqualTo("contacto@la80.com");
		assertThat(perfil.fotos()).containsExactly("https://fotos/1.jpg");
		// La cancha INACTIVA no aparece (política 11 de Cancha)
		assertThat(perfil.canchas()).singleElement().satisfies(cancha -> {
			assertThat(cancha.id()).isEqualTo(cancha1);
			assertThat(cancha.tipoCancha()).isEqualTo("Fútbol 5");
			assertThat(cancha.precioHora()).isEqualByComparingTo("80000");
		});
	}

	@Test
	void ca1ElPerfilNoExponeElCelularPersonalDelAdministrador() {
		UUID escenario = escenario("ACTIVO");

		consultarSinSesion(escenario)
				.expectStatus().isOk()
				.expectBody(String.class)
				.value(cuerpo -> assertThat(cuerpo).doesNotContain("+573009998877", propietario.toString()));
	}

	@Test
	void ca1UnEscenarioActivoSinHorariosMuestraSuPerfilSinCanchas() {
		UUID escenario = escenario("ACTIVO");
		cancha(escenario, "Cancha 1", "DISPONIBLE");

		RespuestaPerfil perfil = consultarSinSesion(escenario)
				.expectStatus().isOk()
				.expectBody(RespuestaPerfil.class).returnResult().getResponseBody();

		assertThat(perfil.canchas()).isEmpty();
	}

	@Test
	void ca2Y_ca3SinResenasNiFotosNoHayPromedioYSeInformaInfRse01() {
		UUID escenario = escenario("ACTIVO");

		RespuestaPerfil perfil = consultarSinSesion(escenario)
				.expectStatus().isOk()
				.expectBody(RespuestaPerfil.class).returnResult().getResponseBody();

		assertThat(perfil.calificacionPromedio()).isNull();
		assertThat(perfil.codigo()).isEqualTo("INF-RSE-01");
		assertThat(perfil.mensaje()).isEqualTo("Este escenario aún no tiene reseñas.");
		assertThat(perfil.fotos()).isEmpty();
	}

	@Test
	void ca4UnEscenarioInactivoResponde404ConErrEsc06() {
		UUID escenario = escenario("INACTIVO");
		horario(escenario);
		cancha(escenario, "Cancha 1", "DISPONIBLE");

		consultarSinSesion(escenario)
				.expectStatus().isNotFound()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-ESC-06");
	}

	@Test
	void ca4UnEscenarioQueNoExisteResponde404ConErrEsc06() {
		consultarSinSesion(UUID.randomUUID())
				.expectStatus().isNotFound()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-ESC-06");
	}

	@Test
	void ca6LasDemasRutasDeEscenariosSiguenPidiendoSesion() {
		// Abrir el perfil no abre la lista de administración (HU13)
		cliente.get().uri("/api/escenarios").exchange()
				.expectStatus().isUnauthorized()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-GEN-05");
	}

}
