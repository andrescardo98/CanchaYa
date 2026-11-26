package co.edu.uco.canchaya.reservas;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;

/**
 * HU23 completa: petición HTTP con token real → seguridad → caso de uso de Reservas →
 * ConsultaCanchas de Escenarios → base en Docker. Usa el reloj real del servidor (D27): las fechas
 * se calculan desde hoy en Bogotá.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class MisReservasPuntaAPuntaTest {

	private static final String F5 = "27d013e3-f5b3-5378-a7df-7c80429554fe";
	private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Bogota"));
	// Pasado mañana: ninguna franja ha empezado, sea cual sea la hora en que corra la prueba
	private static final LocalDate FECHA = HOY.plusDays(2);

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID cancha;
	private UUID jugador;
	private String tokenPropietario;
	private String tokenJugador;
	private String tokenOtroJugador;

	record RespuestaToken(String token) {
	}

	record Reserva(UUID id, String escenario, String cancha, LocalDate fecha, String horaInicio, String horaFin,
			BigDecimal precio, String estado, String confirmacionAsistencia, String origenCancelacion,
			String motivoCancelacion, boolean cancelable) {
	}

	record MisReservas(List<Reserva> vigentes, List<Reserva> pasadas, String codigo, String mensaje) {
	}

	record ReservaCreada(UUID id) {
	}

	record Errores(List<Error> errores) {
	}

	record Error(String codigo) {
	}

	@BeforeEach
	void preparar() {
		// Correos e IP propios: otras pruebas punta a punta pueden dejar bloqueado un correo (D28)
		UUID propietario = usuario("propietario.hu23@correo.com", "1000000231", "ADMINISTRADOR_CANCHA");
		jugador = usuario("jugador.hu23@correo.com", "1000000232", "JUGADOR");
		usuario("otro.jugador.hu23@correo.com", "1000000233", "JUGADOR");
		tokenPropietario = token("propietario.hu23@correo.com", "203.0.113.231");
		tokenJugador = token("jugador.hu23@correo.com", "203.0.113.232");
		tokenOtroJugador = token("otro.jugador.hu23@correo.com", "203.0.113.233");

		UUID escenario = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Canchas La 70', 'Calle 70', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, 'ACTIVO')""", escenario, propietario);
		cancha = UUID.randomUUID();
		jdbc.update("""
				insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora)
				values (?, ?, ?, 'Cancha 1', 80000)""", cancha, escenario, UUID.fromString(F5));
		jdbc.update("""
				insert into horario_recurrente (id, escenario_id, dia_semana, hora_inicio, hora_fin)
				values (?, ?, ?, '06:00', '22:00')""", UUID.randomUUID(), escenario,
				DiaSemana.de(FECHA.getDayOfWeek()).name());
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from reserva");
		jdbc.update("delete from horario_recurrente");
		jdbc.update("delete from cancha");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private UUID usuario(String correo, String documento, String rol) {
		cliente.post().uri("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"nombre": "Ana Pérez", "correoElectronico": "%s", "contrasena": "Clave2026",
						 "tipoDocumento": "CC", "numeroDocumento": "%s", "numeroCelular": "+573001234567",
						 "rol": "%s"}
						""".formatted(correo, documento, rol))
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

	private UUID reservar(String token, String horaInicio, String horaFin) {
		return cliente.post().uri("/api/reservas").contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + token)
				.body("""
						{"canchaId": "%s", "fecha": "%s", "horaInicio": "%s", "horaFin": "%s"}
						""".formatted(cancha, FECHA, horaInicio, horaFin))
				.exchange().expectStatus().isCreated()
				.expectBody(ReservaCreada.class).returnResult().getResponseBody().id();
	}

	private ResponseSpec consultar(String token) {
		var peticion = cliente.get().uri("/api/reservas/mias");
		if (token != null) {
			peticion.header("Authorization", "Bearer " + token);
		}
		return peticion.exchange();
	}

	private MisReservas misReservas(String token) {
		return consultar(token).expectStatus().isOk()
				.expectBody(MisReservas.class).returnResult().getResponseBody();
	}

	// CA1: lo que se reserva con POST /api/reservas aparece en vigentes, con todos sus datos
	@Test
	void CA1_reservaHecha_apareceEnVigentes() {
		UUID id = reservar(tokenJugador, "18:00", "20:00");

		MisReservas mis = misReservas(tokenJugador);

		assertThat(mis.codigo()).isNull();
		assertThat(mis.pasadas()).isEmpty();
		assertThat(mis.vigentes()).singleElement().satisfies(r -> {
			assertThat(r.id()).isEqualTo(id);
			assertThat(r.escenario()).isEqualTo("Canchas La 70");
			assertThat(r.cancha()).isEqualTo("Cancha 1");
			assertThat(r.fecha()).isEqualTo(FECHA);
			assertThat(r.horaInicio()).isEqualTo("18:00");
			assertThat(r.horaFin()).isEqualTo("20:00");
			assertThat(r.precio()).isEqualByComparingTo("160000");
			assertThat(r.estado()).isEqualTo("CONFIRMADA");
			assertThat(r.confirmacionAsistencia()).isEqualTo("NO_REQUERIDA");
			// CA3: faltan al menos dos días, así que se puede cancelar
			assertThat(r.cancelable()).isTrue();
		});
	}

	// CA2: un jugador sin reservas recibe INF-RES-01
	@Test
	void CA2_sinReservas_INF_RES_01() {
		MisReservas mis = misReservas(tokenJugador);

		assertThat(mis.vigentes()).isEmpty();
		assertThat(mis.pasadas()).isEmpty();
		assertThat(mis.codigo()).isEqualTo("INF-RES-01");
		assertThat(mis.mensaje()).isEqualTo("Aún no tiene reservas. Busque una cancha cerca de usted para hacer la primera.");
	}

	// CA4: una reserva cancelada por el administrador trae el origen y la justificación. Se guarda por
	// SQL porque la cancelación (HU24, HU32) aún no existe
	@Test
	void CA4_reservaCancelada_traeOrigenYMotivo() {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado,
				  confirmacion_asistencia, origen_cancelacion, motivo_cancelacion, fecha_creacion, fecha_cancelacion)
				values (?, ?, ?, ?, '08:00', '09:00', 80000, 'CANCELADA', 'NO_REQUERIDA', 'ADMINISTRADOR',
				  'Tormenta eléctrica', now(), now())""", id, cancha, jugador, FECHA);

		MisReservas mis = misReservas(tokenJugador);

		// P1: cancelada es pasada aunque su fecha sea futura
		assertThat(mis.vigentes()).isEmpty();
		assertThat(mis.pasadas()).singleElement().satisfies(r -> {
			assertThat(r.id()).isEqualTo(id);
			assertThat(r.estado()).isEqualTo("CANCELADA");
			assertThat(r.origenCancelacion()).isEqualTo("ADMINISTRADOR");
			assertThat(r.motivoCancelacion()).isEqualTo("Tormenta eléctrica");
			assertThat(r.cancelable()).isFalse();
		});
	}

	// CA5: dos jugadores reservan; cada uno ve solo la suya
	@Test
	void CA5_cadaJugadorVeSoloLasSuyas() {
		UUID delJugador = reservar(tokenJugador, "08:00", "09:00");
		UUID delOtro = reservar(tokenOtroJugador, "10:00", "11:00");

		assertThat(misReservas(tokenJugador).vigentes()).extracting(Reserva::id).containsExactly(delJugador);
		assertThat(misReservas(tokenOtroJugador).vigentes()).extracting(Reserva::id).containsExactly(delOtro);
	}

	// P2: un administrador de cancha con sesión recibe 403 con ERR-GEN-06
	@Test
	void P2_administrador_403_ERR_GEN_06() {
		Errores errores = consultar(tokenPropietario).expectStatus().isForbidden()
				.expectBody(Errores.class).returnResult().getResponseBody();

		assertThat(errores.errores()).extracting(Error::codigo).containsExactly("ERR-GEN-06");
	}

	// AG-11: sin sesión
	@Test
	void sinToken_401() {
		Errores errores = consultar(null).expectStatus().isUnauthorized()
				.expectBody(Errores.class).returnResult().getResponseBody();

		assertThat(errores.errores()).extracting(Error::codigo).containsExactly("ERR-GEN-05");
	}

}
