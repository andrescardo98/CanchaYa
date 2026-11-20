package co.edu.uco.canchaya.escenarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * HU13 completa: petición HTTP con token real → seguridad → caso de uso → base en Docker → correo.
 * El servidor SMTP es un doble (JavaMailSender): así se revisa el correo que arma el adaptador
 * real y se puede simular que el servidor falla (CA8).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class CambioEstadoEscenarioPuntaAPuntaTest {

	private static final String F5 = "27d013e3-f5b3-5378-a7df-7c80429554fe";

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	@MockitoBean
	private JavaMailSender servidorCorreo;

	private UUID propietario;
	private String tokenAdministrador;
	private String tokenPropietario;

	record RespuestaToken(String token) {
	}

	@BeforeEach
	void preparar() {
		when(servidorCorreo.createMimeMessage()).thenAnswer(invocacion -> new MimeMessage((Session) null));
		tokenAdministrador = token("admin@canchaya.local", "Admin12345", "203.0.113.61");

		cliente.post().uri("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"nombre": "Ana Pérez", "correoElectronico": "propietario.hu13@correo.com", "contrasena": "Clave2026",
						 "tipoDocumento": "CC", "numeroDocumento": "1000000061", "numeroCelular": "+573001234567",
						 "rol": "ADMINISTRADOR_CANCHA"}
						""")
				.exchange().expectStatus().isCreated();
		propietario = jdbc.queryForObject("select id from usuario where correo_electronico = 'propietario.hu13@correo.com'",
				UUID.class);
		tokenPropietario = token("propietario.hu13@correo.com", "Clave2026", "203.0.113.62");
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from reserva");
		jdbc.update("delete from cancha");
		jdbc.update("delete from foto_escenario");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
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

	private UUID escenario(String nombre, String estado) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, ?, 'Calle 80 # 45-10', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, ?)""", id, nombre, propietario, estado);
		return id;
	}

	private void reservaConfirmadaDeManana(UUID escenario) {
		UUID cancha = UUID.randomUUID();
		jdbc.update("insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora) values (?, ?, ?, 'Cancha 1', 80000)",
				cancha, escenario, UUID.fromString(F5));
		UUID jugador = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Luis Gómez', 'luis@correo.com', ?, 'CC', '1000000063', '+573001234567', 'JUGADOR', now())""",
				jugador, "x".repeat(60));
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado, fecha_creacion)
				values (?, ?, ?, ?, ?, ?, 80000, 'CONFIRMADA', now())""",
				UUID.randomUUID(), cancha, jugador, LocalDate.now(ZoneId.of("America/Bogota")).plusDays(1),
				LocalTime.of(18, 0), LocalTime.of(19, 0));
	}

	private ResponseSpec cambiar(UUID id, String estado, String token) {
		return cliente.patch().uri("/api/escenarios/{id}/estado", id)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"estado\": \"" + estado + "\"}")
				.exchange();
	}

	private String estadoGuardado(UUID id) {
		return jdbc.queryForObject("select estado from escenario where id = ?", String.class, id);
	}

	private MimeMessage correoEnviado() {
		ArgumentCaptor<MimeMessage> correo = ArgumentCaptor.forClass(MimeMessage.class);
		verify(servidorCorreo).send(correo.capture());
		return correo.getValue();
	}

	@Test
	void ca1ElAdministradorFiltraLosInactivos() {
		escenario("Golazo", "INACTIVO");
		escenario("Cancha La 80", "INACTIVO");
		escenario("El Templo", "ACTIVO");

		cliente.get().uri("/api/escenarios?estado=INACTIVO")
				.header("Authorization", "Bearer " + tokenAdministrador)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(2)
				.jsonPath("$[0].nombre").isEqualTo("Cancha La 80")
				.jsonPath("$[0].ciudad").isEqualTo("Rionegro")
				.jsonPath("$[0].propietario").isEqualTo("Ana Pérez")
				.jsonPath("$[0].estado").isEqualTo("INACTIVO")
				.jsonPath("$[1].nombre").isEqualTo("Golazo");
	}

	@Test
	void ca2YCa8ActivaYAvisaAlPropietario() throws Exception {
		UUID id = escenario("Cancha La 80", "INACTIVO");

		cambiar(id, "ACTIVO", tokenAdministrador)
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.codigo").isEqualTo("EXI-ESC-03")
				.jsonPath("$.mensaje").isEqualTo("El escenario Cancha La 80 quedó en estado Activo.")
				.jsonPath("$.estado").isEqualTo("ACTIVO");

		assertThat(estadoGuardado(id)).isEqualTo("ACTIVO");
		MimeMessage correo = correoEnviado();
		assertThat(correo.getAllRecipients()).extracting(Object::toString).containsExactly("propietario.hu13@correo.com");
		assertThat(correo.getSubject()).isEqualTo("Su escenario Cancha La 80 ahora está Activo");
		assertThat((String) correo.getContent()).startsWith("Hola, Ana Pérez:");
	}

	@Test
	void ca3InactivaUnEscenarioSinReservasFuturas() throws Exception {
		UUID id = escenario("Cancha La 80", "ACTIVO");

		cambiar(id, "INACTIVO", tokenAdministrador)
				.expectStatus().isOk()
				.expectBody().jsonPath("$.mensaje").isEqualTo("El escenario Cancha La 80 quedó en estado Inactivo.");

		assertThat(estadoGuardado(id)).isEqualTo("INACTIVO");
		assertThat((String) correoEnviado().getContent()).contains("dejaron de aparecer en la búsqueda");
	}

	@Test
	void ca4UnEscenarioQueNoExiste() {
		cambiar(UUID.randomUUID(), "ACTIVO", tokenAdministrador)
				.expectStatus().isNotFound()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-ESC-06");
		verify(servidorCorreo, never()).send(any(MimeMessage.class));
	}

	@Test
	void ca5ElPropietarioNoPuedeActivarSuEscenarioNiVerLaLista() {
		UUID id = escenario("Cancha La 80", "INACTIVO");

		cambiar(id, "ACTIVO", tokenPropietario)
				.expectStatus().isForbidden()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-ESC-08");
		cliente.get().uri("/api/escenarios").header("Authorization", "Bearer " + tokenPropietario).exchange()
				.expectStatus().isForbidden()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-GEN-06");

		assertThat(estadoGuardado(id)).isEqualTo("INACTIVO");
		verify(servidorCorreo, never()).send(any(MimeMessage.class));
	}

	@Test
	void ca6ElEscenarioYaEstaActivo() {
		UUID id = escenario("Cancha La 80", "ACTIVO");

		cambiar(id, "ACTIVO", tokenAdministrador)
				.expectStatus().isEqualTo(422)
				.expectBody()
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-ESC-09")
				.jsonPath("$.errores[0].mensaje").isEqualTo("El escenario ya se encuentra en estado Activo.");
		verify(servidorCorreo, never()).send(any(MimeMessage.class));
	}

	@Test
	void ca7NoSeInactivaConUnaReservaConfirmadaFutura() {
		UUID id = escenario("Cancha La 80", "ACTIVO");
		reservaConfirmadaDeManana(id);

		cambiar(id, "INACTIVO", tokenAdministrador)
				.expectStatus().isEqualTo(422)
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-ESC-14");

		assertThat(estadoGuardado(id)).isEqualTo("ACTIVO");
		verify(servidorCorreo, never()).send(any(MimeMessage.class));
	}

	@Test
	void ca8SiElCorreoFallaElCambioDeEstadoSeMantiene() {
		UUID id = escenario("Cancha La 80", "INACTIVO");
		doThrow(new MailSendException("servidor SMTP caído")).when(servidorCorreo).send(any(MimeMessage.class));

		cambiar(id, "ACTIVO", tokenAdministrador)
				.expectStatus().isOk()
				.expectBody().jsonPath("$.codigo").isEqualTo("EXI-ESC-03");

		assertThat(estadoGuardado(id)).isEqualTo("ACTIVO");
		// Un solo intento: no se reintenta (PAR-GEN-02)
		verify(servidorCorreo).send(any(MimeMessage.class));
	}

}
