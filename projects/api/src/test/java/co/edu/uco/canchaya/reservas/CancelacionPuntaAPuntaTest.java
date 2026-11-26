package co.edu.uco.canchaya.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;

/**
 * HU24 completa: petición HTTP con token real → seguridad → caso de uso de Reservas → base en
 * Docker. Usa el reloj real del servidor (D27): las fechas se calculan desde ahora en Bogotá.
 *
 * <p>HU25: el servidor de correo es un doble, para ver qué se envía sin depender de Mailpit.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class CancelacionPuntaAPuntaTest {

	private static final String F5 = "27d013e3-f5b3-5378-a7df-7c80429554fe";
	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	// Pasado mañana: faltan más de 3 horas para cualquier franja, sea cual sea la hora de la prueba
	private static final LocalDate FECHA = LocalDate.now(BOGOTA).plusDays(2);
	private static final String FECHA_MENSAJE = FECHA.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	@MockitoBean
	private JavaMailSender servidorCorreo;

	private UUID cancha;
	private UUID jugador;
	private String tokenJugador;
	private String tokenOtroJugador;

	record RespuestaToken(String token) {
	}

	record ReservaCreada(UUID id) {
	}

	record RespuestaCancelacion(String codigo, String mensaje, UUID id, String estado) {
	}

	record Franja(String horaInicio, boolean libre) {
	}

	record Disponibilidad(List<Franja> franjas) {
	}

	record Errores(List<Error> errores) {
	}

	record Error(String codigo, String mensaje) {
	}

	@BeforeEach
	void preparar() {
		when(servidorCorreo.createMimeMessage()).thenAnswer(invocacion -> new MimeMessage((Session) null));
		// Correos e IP propios: otras pruebas punta a punta pueden dejar bloqueado un correo (D28)
		UUID propietario = usuario("propietario.hu24@correo.com", "1000000241", "ADMINISTRADOR_CANCHA");
		jugador = usuario("jugador.hu24@correo.com", "1000000242", "JUGADOR");
		usuario("otro.jugador.hu24@correo.com", "1000000243", "JUGADOR");
		tokenJugador = token("jugador.hu24@correo.com", "203.0.113.242");
		tokenOtroJugador = token("otro.jugador.hu24@correo.com", "203.0.113.243");

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

	private UUID reservarALas18() {
		return cliente.post().uri("/api/reservas").contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + tokenJugador)
				.body("""
						{"canchaId": "%s", "fecha": "%s", "horaInicio": "18:00", "horaFin": "19:00"}
						""".formatted(cancha, FECHA))
				.exchange().expectStatus().isCreated()
				.expectBody(ReservaCreada.class).returnResult().getResponseBody().id();
	}

	private ResponseSpec cancelar(UUID reserva, String token) {
		return cliente.post().uri("/api/reservas/{id}/cancelacion", reserva)
				.header("Authorization", "Bearer " + token).exchange();
	}

	private boolean franjaDeLas18Libre() {
		Disponibilidad disponibilidad = cliente.get()
				.uri(uri -> uri.path("/api/canchas/{canchaId}/disponibilidad").queryParam("fecha", FECHA).build(cancha))
				.exchange().expectStatus().isOk()
				.expectBody(Disponibilidad.class).returnResult().getResponseBody();
		return disponibilidad.franjas().stream().filter(f -> f.horaInicio().equals("18:00")).findFirst()
				.orElseThrow().libre();
	}

	private void esperarError(ResponseSpec respuesta, int estado, String codigo) {
		Errores errores = respuesta.expectStatus().isEqualTo(estado)
				.expectBody(Errores.class).returnResult().getResponseBody();
		assertThat(errores.errores()).extracting(Error::codigo).containsExactly(codigo);
	}

	private List<MimeMessage> correosEnviados(int cuantos) {
		ArgumentCaptor<MimeMessage> correos = ArgumentCaptor.forClass(MimeMessage.class);
		verify(servidorCorreo, times(cuantos)).send(correos.capture());
		return correos.getAllValues();
	}

	private String estadoEnLaBase(UUID reserva) {
		return jdbc.queryForObject("select estado from reserva where id = ?", String.class, reserva);
	}

	@Test
	void ca1Cancelar_200_yLaFranjaVuelveAEstarDisponible() {
		UUID reserva = reservarALas18();
		assertThat(franjaDeLas18Libre()).isFalse();

		RespuestaCancelacion respuesta = cancelar(reserva, tokenJugador).expectStatus().isOk()
				.expectBody(RespuestaCancelacion.class).returnResult().getResponseBody();

		assertThat(respuesta).isEqualTo(new RespuestaCancelacion("EXI-RES-02",
				"La reserva se canceló correctamente.", reserva, "CANCELADA"));
		// Política 19: origen JUGADOR y fecha de cancelación guardados
		Map<String, Object> fila = jdbc.queryForMap(
				"select estado, origen_cancelacion, fecha_cancelacion from reserva where id = ?", reserva);
		assertThat(fila.get("estado")).isEqualTo("CANCELADA");
		assertThat(fila.get("origen_cancelacion")).isEqualTo("JUGADOR");
		assertThat(fila.get("fecha_cancelacion")).isNotNull();
		// Política 20: la franja vuelve a estar libre para los demás
		assertThat(franjaDeLas18Libre()).isTrue();
	}

	@Test
	void ca3FaltanDosHoras_422_ERR_RES_16() {
		// Empieza en una o dos horas (o acaba de empezar, cerca de la medianoche); siempre faltan 3 o menos
		LocalDateTime inicio = LocalDateTime.now(BOGOTA).truncatedTo(ChronoUnit.HOURS).plusHours(2);
		if (inicio.getHour() == 23) {
			inicio = inicio.minusHours(1);
		}
		UUID reserva = UUID.randomUUID();
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado,
				  confirmacion_asistencia, fecha_creacion)
				values (?, ?, ?, ?, ?, ?, 80000, 'CONFIRMADA', 'NO_REQUERIDA', now())""",
				reserva, cancha, jugador, inicio.toLocalDate(), inicio.toLocalTime(), inicio.toLocalTime().plusHours(1));

		esperarError(cancelar(reserva, tokenJugador), 422, "ERR-RES-16");
		assertThat(estadoEnLaBase(reserva)).isEqualTo("CONFIRMADA");
	}

	@Test
	void ca4CancelarDosVeces_404_ERR_RES_14() {
		UUID reserva = reservarALas18();
		cancelar(reserva, tokenJugador).expectStatus().isOk();

		esperarError(cancelar(reserva, tokenJugador), 404, "ERR-RES-14");
		esperarError(cancelar(UUID.randomUUID(), tokenJugador), 404, "ERR-RES-14");
	}

	@Test
	void ca5OtroJugador_403_ERR_RES_15() {
		UUID reserva = reservarALas18();

		esperarError(cancelar(reserva, tokenOtroJugador), 403, "ERR-RES-15");
		assertThat(estadoEnLaBase(reserva)).isEqualTo("CONFIRMADA");
		assertThat(franjaDeLas18Libre()).isFalse();
	}

	// HU25 CA1: COR-RES-03 al jugador y al propietario, a las direcciones registradas, después de guardar
	@Test
	void HU25_CA1_cancelarEnviaLosDosCorreos() throws Exception {
		UUID reserva = reservarALas18();
		// Los dos correos de la reserva (HU22) no cuentan aquí
		clearInvocations(servidorCorreo);

		cancelar(reserva, tokenJugador).expectStatus().isOk();

		List<MimeMessage> correos = correosEnviados(2);
		assertThat(correos).extracting(correo -> correo.getAllRecipients()[0].toString())
				.containsExactly("jugador.hu24@correo.com", "propietario.hu24@correo.com");
		for (MimeMessage correo : correos) {
			assertThat(correo.getSubject()).isEqualTo("Reserva cancelada: Canchas La 70, " + FECHA_MENSAJE + " a las 18:00");
			assertThat((String) correo.getContent())
					.contains("La siguiente reserva fue cancelada por el jugador el ")
					.contains("Escenario: Canchas La 70<br>Dirección: Calle 70, Rionegro<br>Cancha: Cancha 1")
					.contains("Fecha: " + FECHA_MENSAJE + "<br>Hora: 18:00 a 19:00<br>Valor: $ 80.000")
					.contains("El horario volvió a quedar disponible.");
		}
	}

	// HU25 CA4: con el SMTP caído la reserva queda CANCELADA, cada correo se intenta una sola vez
	// (PAR-GEN-02 = 0) y el error queda en el log
	@Test
	@ExtendWith(OutputCaptureExtension.class)
	void HU25_CA4_smtpFalla_reservaSigueCancelada(CapturedOutput salida) {
		UUID reserva = reservarALas18();
		clearInvocations(servidorCorreo);
		doThrow(new MailSendException("servidor SMTP caído")).when(servidorCorreo).send(any(MimeMessage.class));

		cancelar(reserva, tokenJugador).expectStatus().isOk()
				.expectBody().jsonPath("$.codigo").isEqualTo("EXI-RES-02");

		assertThat(estadoEnLaBase(reserva)).isEqualTo("CANCELADA");
		correosEnviados(2);
		assertThat(salida.getAll())
				.contains("ERR-GEN-08 No se pudo enviar el correo COR-RES-03 a jugador.hu24@correo.com")
				.contains("ERR-GEN-08 No se pudo enviar el correo COR-RES-03 a propietario.hu24@correo.com");
	}

}
