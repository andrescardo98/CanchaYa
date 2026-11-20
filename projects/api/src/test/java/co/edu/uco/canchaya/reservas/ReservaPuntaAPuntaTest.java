package co.edu.uco.canchaya.reservas;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
 * HU21 completa: petición HTTP con token real → seguridad → caso de uso de Reservas →
 * ConsultaCanchas de Escenarios → base en Docker. Usa el reloj real del servidor (D27), por eso
 * las fechas se calculan desde hoy en Bogotá.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ReservaPuntaAPuntaTest {

	private static final String F5 = "27d013e3-f5b3-5378-a7df-7c80429554fe";
	// Pasado mañana: ninguna franja ha empezado, sea cual sea la hora en que corra la prueba
	private static final LocalDate FECHA = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(2);
	private static final String FECHA_MENSAJE = FECHA.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID cancha;
	private String tokenPropietario;
	private String tokenJugador;
	private String tokenOtroJugador;

	record RespuestaToken(String token) {
	}

	record RespuestaFranja(String horaInicio, String horaFin, boolean libre) {
	}

	record RespuestaDisponibilidad(List<RespuestaFranja> franjas) {
	}

	@BeforeEach
	void preparar() {
		// Correos e IP propios: InicioSesionPuntaAPuntaTest deja bloqueado a ana@correo.com (D28)
		UUID propietario = usuario("propietario.hu21@correo.com", "1000000211", "ADMINISTRADOR_CANCHA");
		usuario("jugador.hu21@correo.com", "1000000212", "JUGADOR");
		usuario("otro.jugador.hu21@correo.com", "1000000213", "JUGADOR");
		tokenPropietario = token("propietario.hu21@correo.com", "203.0.113.211");
		tokenJugador = token("jugador.hu21@correo.com", "203.0.113.212");
		tokenOtroJugador = token("otro.jugador.hu21@correo.com", "203.0.113.213");

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

	private ResponseSpec reservar(String token, String horaInicio, String horaFin) {
		RestTestClient.RequestBodySpec peticion = cliente.post().uri("/api/reservas")
				.contentType(MediaType.APPLICATION_JSON);
		if (token != null) {
			peticion.header("Authorization", "Bearer " + token);
		}
		return peticion.body("""
				{"canchaId": "%s", "fecha": "%s", "horaInicio": "%s", "horaFin": "%s"}
				""".formatted(cancha, FECHA, horaInicio, horaFin)).exchange();
	}

	private List<RespuestaFranja> disponibilidad() {
		return cliente.get().uri("/api/canchas/{id}/disponibilidad?fecha={fecha}", cancha, FECHA).exchange()
				.expectStatus().isOk()
				.expectBody(RespuestaDisponibilidad.class).returnResult().getResponseBody().franjas();
	}

	private static RespuestaFranja franja(int hora, boolean libre) {
		return new RespuestaFranja(String.format("%02d:00", hora), String.format("%02d:00", hora + 1), libre);
	}

	private List<Map<String, Object>> reservasGuardadas() {
		return jdbc.queryForList("""
				select hora_inicio, hora_fin, precio, estado, confirmacion_asistencia from reserva
				where cancha_id = ? order by hora_inicio""", cancha);
	}

	@Test
	void CA1_reservar_franjaDejaDeOfrecerse() {
		reservar(tokenJugador, "18:00", "19:00").expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.codigo").isEqualTo("EXI-RES-01")
				.jsonPath("$.mensaje").isEqualTo("¡Reserva confirmada! Canchas La 70, Cancha 1, " + FECHA_MENSAJE
						+ " de 18:00 a 19:00. Valor: $ 80.000. Le enviamos los datos a su correo.")
				.jsonPath("$.precio").isEqualTo(80000.00)
				.jsonPath("$.estado").doesNotExist();

		// La franja sale ocupada en la disponibilidad de HU20; las vecinas siguen libres
		assertThat(disponibilidad()).contains(franja(17, true), franja(18, false), franja(19, true));
		// CA2: con PAR-RES-12 en «No» la confirmación de asistencia no se pide
		assertThat(reservasGuardadas()).singleElement().satisfies(fila -> {
			assertThat(fila).containsEntry("estado", "CONFIRMADA")
					.containsEntry("confirmacion_asistencia", "NO_REQUERIDA");
			assertThat((BigDecimal) fila.get("precio")).isEqualByComparingTo("80000");
		});
	}

	@Test
	void CA11_dosFranjas_unaReservaConPrecioDoble() {
		reservar(tokenJugador, "18:00", "20:00").expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.horaInicio").isEqualTo("18:00")
				.jsonPath("$.horaFin").isEqualTo("20:00")
				.jsonPath("$.precio").isEqualTo(160000.00);

		assertThat(disponibilidad()).contains(franja(18, false), franja(19, false), franja(20, true));
		// D29 punto 1: una sola fila con un solo rango
		assertThat(reservasGuardadas()).hasSize(1);
	}

	// CA3 por HTTP: la franja ya es de otro jugador
	@Test
	void CA3_franjaYaReservada_409() {
		reservar(tokenOtroJugador, "18:00", "19:00").expectStatus().isCreated();

		reservar(tokenJugador, "18:00", "20:00").expectStatus().isEqualTo(409)
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-RES-06");
		assertThat(reservasGuardadas()).hasSize(1);
	}

	// CA4: dos jugadores piden la misma franja en el mismo instante
	@Test
	void CA4_dosSolicitudesSimultaneas_soloUnaSeConfirma() throws Exception {
		CountDownLatch salida = new CountDownLatch(1);
		ExecutorService hilos = Executors.newFixedThreadPool(2);
		try {
			Callable<Integer> deA = () -> {
				salida.await();
				return reservar(tokenJugador, "18:00", "19:00").returnResult().getStatus().value();
			};
			Callable<Integer> deB = () -> {
				salida.await();
				return reservar(tokenOtroJugador, "18:00", "19:00").returnResult().getStatus().value();
			};
			Future<Integer> a = hilos.submit(deA);
			Future<Integer> b = hilos.submit(deB);
			salida.countDown();

			assertThat(List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS)))
					.containsExactlyInAnyOrder(201, 409);
		} finally {
			hilos.shutdownNow();
		}
		assertThat(reservasGuardadas()).hasSize(1);
	}

	// CA10 con el token real: el rol sale del token, no del cuerpo
	@Test
	void CA10_propietario_403_ERR_RES_05() {
		reservar(tokenPropietario, "18:00", "19:00").expectStatus().isForbidden()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-RES-05");
		assertThat(reservasGuardadas()).isEmpty();
	}

	// CA15: sin sesión. INF-RES-02 y volver al resumen son de la pantalla
	@Test
	void CA15_sinSesion_401() {
		reservar(null, "18:00", "19:00").expectStatus().isUnauthorized()
				.expectBody().jsonPath("$.errores[0].codigo").isEqualTo("ERR-GEN-05");
		assertThat(reservasGuardadas()).isEmpty();
	}

	// HU9, CA8: la reserva nueva toma el precio actualizado y la anterior conserva el suyo (política 11)
	@Test
	void HU9_CA8_reservaNueva_tomaElPrecioActualizado() {
		reservar(tokenJugador, "18:00", "19:00").expectStatus().isCreated();

		cliente.put().uri("/api/canchas/{id}", cancha)
				.header("Authorization", "Bearer " + tokenPropietario)
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"nombre": "Cancha 1", "tipoCanchaId": "%s", "precioHora": 95000}
						""".formatted(F5))
				.exchange().expectStatus().isOk();

		reservar(tokenJugador, "20:00", "21:00").expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.precio").isEqualTo(95000.00)
				.jsonPath("$.mensaje").isEqualTo("¡Reserva confirmada! Canchas La 70, Cancha 1, " + FECHA_MENSAJE
						+ " de 20:00 a 21:00. Valor: $ 95.000. Le enviamos los datos a su correo.");

		assertThat(reservasGuardadas()).extracting(fila -> ((BigDecimal) fila.get("precio")).intValue())
				.containsExactly(80000, 95000);
	}

}
