package co.edu.uco.canchaya.escenarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

import co.edu.uco.canchaya.TestcontainersConfiguration;

/**
 * HU10 completa: petición HTTP con token real → seguridad → caso de uso (con su transacción y el
 * turno de la cancha, P2) → base en Docker. Revisa además lo que ven otras HU después del cambio:
 * el perfil del escenario (HU19) y la disponibilidad (HU20).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class CambioEstadoCanchaPuntaAPuntaTest {

	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");
	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID escenario;
	private UUID cancha;
	private String tokenPropietario;
	// Documento de cada jugador de prueba: la base solo acepta números
	private int siguienteDocumento = 1000000110;

	record RespuestaToken(String token) {
	}

	@BeforeEach
	void preparar() {
		// Correos, documentos e IP propios para no chocar con otras pruebas (D28)
		UUID propietario = usuario("Ana Pérez", "propietario.hu10@correo.com", "1000000101", "ADMINISTRADOR_CANCHA");
		tokenPropietario = token("propietario.hu10@correo.com", "203.0.113.101");
		escenario = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Cancha La 80', 'Calle 80 # 45-10', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, 'ACTIVO')""", escenario, propietario);
		// Con un horario el escenario cumple RN-04 y la cancha DISPONIBLE sale en el perfil
		jdbc.update("""
				insert into horario_recurrente (id, escenario_id, dia_semana, hora_inicio, hora_fin)
				values (?, ?, 'LUNES', '08:00', '22:00')""", UUID.randomUUID(), escenario);
		cancha = UUID.randomUUID();
		jdbc.update("""
				insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora, estado)
				values (?, ?, ?, 'Cancha 1', 80000, 'DISPONIBLE')""", cancha, escenario, F5);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from reserva");
		jdbc.update("delete from horario_recurrente");
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
				.header("X-Forwarded-For", ip)
				.body("""
						{"correoElectronico": "%s", "contrasena": "Clave2026"}
						""".formatted(correo))
				.exchange().expectStatus().isOk()
				.expectBody(RespuestaToken.class).returnResult().getResponseBody().token();
	}

	private ResponseSpec cambiar(String token, String estado) {
		return cliente.patch().uri("/api/canchas/{id}/estado", cancha)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"estado\": \"" + estado + "\"}")
				.exchange();
	}

	private String estadoGuardado() {
		return jdbc.queryForObject("select estado from cancha where id = ?", String.class, cancha);
	}

	private int canchasEnElPerfil() {
		return cliente.get().uri("/api/escenarios/{id}/perfil", escenario).exchange()
				.expectStatus().isOk()
				.expectBody(RespuestaPerfil.class).returnResult().getResponseBody().canchas().size();
	}

	record RespuestaPerfil(List<Object> canchas) {
	}

	private ResponseSpec disponibilidad() {
		return cliente.get().uri("/api/canchas/{id}/disponibilidad", cancha).exchange();
	}

	private void reserva(LocalDate fecha, String estado) {
		UUID jugador = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Juan Ruiz', ?, ?, 'CC', ?, '+573001234567', 'JUGADOR', now())""",
				jugador, jugador + "@correo.com", "x".repeat(60), String.valueOf(siguienteDocumento++));
		boolean cancelada = estado.equals("CANCELADA");
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado,
				  origen_cancelacion, fecha_cancelacion, fecha_creacion)
				values (?, ?, ?, ?, ?, ?, 80000, ?, ?, ?, now())""",
				UUID.randomUUID(), cancha, jugador, fecha, LocalTime.of(18, 0), LocalTime.of(19, 0), estado,
				cancelada ? "JUGADOR" : null, cancelada ? Timestamp.from(Instant.now()) : null);
	}

	// CA1: queda INACTIVA, deja de verse en el perfil (HU19) y no se puede reservar (HU20)
	@Test
	void ca1Inactivar_200_EXI_CAN_03_yDejaDeVerseYDeReservarse() {
		assertThat(canchasEnElPerfil()).isEqualTo(1);
		disponibilidad().expectStatus().isOk();

		cambiar(tokenPropietario, "INACTIVA").expectStatus().isOk()
				.expectBody()
				.jsonPath("$.codigo").isEqualTo("EXI-CAN-03")
				.jsonPath("$.mensaje").isEqualTo("La cancha Cancha 1 quedó en estado Inactiva.")
				.jsonPath("$.id").isEqualTo(cancha.toString())
				.jsonPath("$.estado").isEqualTo("INACTIVA");

		assertThat(estadoGuardado()).isEqualTo("INACTIVA");
		assertThat(canchasEnElPerfil()).isZero();
		disponibilidad().expectStatus().isNotFound()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-RES-01");
	}

	// CA2: vuelve a DISPONIBLE y a verse en el perfil, porque el escenario cumple RN-04
	@Test
	void ca2Activar_200_EXI_CAN_03_yVuelveAlPerfil() {
		jdbc.update("update cancha set estado = 'INACTIVA' where id = ?", cancha);
		assertThat(canchasEnElPerfil()).isZero();

		cambiar(tokenPropietario, "DISPONIBLE").expectStatus().isOk()
				.expectBody()
				.jsonPath("$.mensaje").isEqualTo("La cancha Cancha 1 quedó en estado Disponible.")
				.jsonPath("$.estado").isEqualTo("DISPONIBLE");

		assertThat(estadoGuardado()).isEqualTo("DISPONIBLE");
		assertThat(canchasEnElPerfil()).isEqualTo(1);
		disponibilidad().expectStatus().isOk();
	}

	// CA4: otro administrador de cancha, que es como queda un encargado de sede
	@Test
	void ca4NoPropietario_403_ERR_CAN_02() {
		usuario("Luis Gómez", "otro.hu10@correo.com", "1000000102", "ADMINISTRADOR_CANCHA");
		String tokenOtro = token("otro.hu10@correo.com", "203.0.113.102");

		cambiar(tokenOtro, "INACTIVA").expectStatus().isForbidden()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-CAN-02");
		assertThat(estadoGuardado()).isEqualTo("DISPONIBLE");
	}

	// CA6: una reserva CONFIRMADA de mañana impide inactivar
	@Test
	void ca6ReservaFutura_422_ERR_CAN_12() {
		reserva(LocalDate.now(BOGOTA).plusDays(1), "CONFIRMADA");

		cambiar(tokenPropietario, "INACTIVA").expectStatus().isEqualTo(422)
				.expectBody()
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-CAN-12")
				.jsonPath("$.errores[0].mensaje")
				.isEqualTo("No se puede inactivar la cancha porque tiene reservas confirmadas futuras.");
		assertThat(estadoGuardado()).isEqualTo("DISPONIBLE");
	}

	// CA6: una reserva que ya pasó o una cancelada no cuentan
	@Test
	void ca6ReservaPasadaOCancelada_seInactiva() {
		reserva(LocalDate.now(BOGOTA).minusDays(1), "CONFIRMADA");
		reserva(LocalDate.now(BOGOTA).plusDays(1), "CANCELADA");

		cambiar(tokenPropietario, "INACTIVA").expectStatus().isOk();
		assertThat(estadoGuardado()).isEqualTo("INACTIVA");
	}

}
