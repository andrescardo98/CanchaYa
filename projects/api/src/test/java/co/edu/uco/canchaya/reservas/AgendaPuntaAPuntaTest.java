package co.edu.uco.canchaya.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
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
 * HU29 completa: petición HTTP con token real → seguridad → casos de uso de Escenarios y Reservas →
 * base en Docker. Usa el reloj real del servidor (D27): las fechas se calculan desde hoy en Bogotá.
 * Los bloqueos y los encargados se insertan con SQL porque HU30 y HU34 todavía no existen (P1, P2).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class AgendaPuntaAPuntaTest {

	private static final String F5 = "27d013e3-f5b3-5378-a7df-7c80429554fe";
	private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Bogota"));
	// Pasado mañana: ninguna franja ha empezado, sea cual sea la hora en que corra la prueba
	private static final LocalDate FECHA = HOY.plusDays(2);
	private static final LocalDate LUNES = FECHA.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

	@Autowired
	private RestTestClient cliente;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID la70;
	private UUID sedeNorte;
	private UUID cancha1;
	private UUID cancha2;
	private UUID reserva;
	private UUID cancelada;
	private UUID bloqueo;
	private String tokenPropietario;
	private String tokenEncargada;
	private String tokenOtroAdministrador;
	private String tokenJugador;

	record RespuestaToken(String token) {
	}

	record ReservaCreada(UUID id) {
	}

	record EscenarioAgenda(UUID id, String nombre, String estado, boolean propietario) {
	}

	record Agenda(UUID escenarioId, String escenario, boolean propietario, LocalDate desde, LocalDate hasta,
			List<Cancha> canchas) {

		Dia dia(UUID canchaId, LocalDate fecha) {
			return canchas.stream().filter(c -> c.id().equals(canchaId)).findFirst().orElseThrow().dias().stream()
					.filter(d -> d.fecha().equals(fecha)).findFirst().orElseThrow();
		}

	}

	record Cancha(UUID id, String nombre, String estado, List<Dia> dias) {
	}

	record Dia(LocalDate fecha, List<Franja> franjas, List<Reserva> reservas, List<Bloqueo> bloqueos) {
	}

	record Franja(String horaInicio, String horaFin, String estado, UUID reservaId, UUID bloqueoId) {
	}

	record Reserva(UUID id, String horaInicio, String horaFin, String estado, String jugador, String celularJugador,
			String confirmacionAsistencia, String origenCancelacion, String motivoCancelacion, String fechaCancelacion,
			boolean puedeCancelarPorFuerzaMayor, boolean puedeMarcarInasistencia) {
	}

	record Bloqueo(UUID id, String horaInicio, String horaFin, String tipoBloqueo, String motivo) {
	}

	record Errores(List<Error> errores) {
	}

	record Error(String codigo) {
	}

	@BeforeEach
	void preparar() {
		// Correos e IP propios: otras pruebas punta a punta pueden dejar bloqueado un correo (D28)
		UUID propietario = usuario("Ana Pérez", "propietaria.hu29@correo.com", "1000000291", "+573001234567",
				"ADMINISTRADOR_CANCHA");
		UUID encargada = usuario("Laura Ríos", "encargada.hu29@correo.com", "1000000292", "+573001234568",
				"ADMINISTRADOR_CANCHA");
		UUID otroAdministrador = usuario("Luis Gómez", "otro.admin.hu29@correo.com", "1000000293", "+573001234569",
				"ADMINISTRADOR_CANCHA");
		usuario("Carlos Mejía", "jugador.hu29@correo.com", "1000000294", "+573001112233", "JUGADOR");
		tokenPropietario = token("propietaria.hu29@correo.com", "203.0.113.291");
		tokenEncargada = token("encargada.hu29@correo.com", "203.0.113.292");
		tokenOtroAdministrador = token("otro.admin.hu29@correo.com", "203.0.113.293");
		tokenJugador = token("jugador.hu29@correo.com", "203.0.113.294");

		la70 = escenario("Canchas La 70", propietario);
		sedeNorte = escenario("Sede Norte", propietario);
		escenario("Golazo", otroAdministrador);
		cancha1 = cancha(la70, "Cancha 1");
		cancha2 = cancha(la70, "Cancha 2");
		jdbc.update("""
				insert into horario_recurrente (id, escenario_id, dia_semana, hora_inicio, hora_fin)
				values (?, ?, ?, '17:00', '21:00')""", UUID.randomUUID(), la70,
				DiaSemana.de(FECHA.getDayOfWeek()).name());
		jdbc.update("""
				insert into encargado_escenario (id, escenario_id, usuario_id, fecha_asignacion)
				values (?, ?, ?, now())""", UUID.randomUUID(), la70, encargada);

		// Una reserva hecha por el servicio de HU21, una cancelada por el propietario y un bloqueo
		reserva = reservar(tokenJugador, cancha1, "18:00", "20:00");
		cancelada = UUID.randomUUID();
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado,
				  confirmacion_asistencia, origen_cancelacion, motivo_cancelacion, fecha_creacion, fecha_cancelacion)
				values (?, ?, (select id from usuario where correo_electronico = 'jugador.hu29@correo.com'), ?,
				  '17:00', '18:00', 80000, 'CANCELADA', 'NO_REQUERIDA', 'ADMINISTRADOR', 'Tormenta eléctrica',
				  now(), now())""", cancelada, cancha1, FECHA);
		bloqueo = UUID.randomUUID();
		jdbc.update("""
				insert into bloqueo (id, cancha_id, fecha, hora_inicio, hora_fin, tipo_bloqueo, motivo, creado_por_id)
				values (?, ?, ?, '20:00', '21:00', 'MANTENIMIENTO', 'Cambio de malla', ?)""",
				bloqueo, cancha1, FECHA, propietario);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from bloqueo");
		jdbc.update("delete from encargado_escenario");
		jdbc.update("delete from reserva");
		jdbc.update("delete from horario_recurrente");
		jdbc.update("delete from cancha");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private UUID usuario(String nombre, String correo, String documento, String celular, String rol) {
		cliente.post().uri("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"nombre": "%s", "correoElectronico": "%s", "contrasena": "Clave2026",
						 "tipoDocumento": "CC", "numeroDocumento": "%s", "numeroCelular": "%s", "rol": "%s"}
						""".formatted(nombre, correo, documento, celular, rol))
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

	private UUID escenario(String nombre, UUID propietario) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, ?, 'Calle 70', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, 'ACTIVO')""", id, nombre, propietario);
		return id;
	}

	private UUID cancha(UUID escenario, String nombre) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora) values (?, ?, ?, ?, 80000)",
				id, escenario, UUID.fromString(F5), nombre);
		return id;
	}

	private UUID reservar(String token, UUID canchaId, String horaInicio, String horaFin) {
		return cliente.post().uri("/api/reservas").contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + token)
				.body("""
						{"canchaId": "%s", "fecha": "%s", "horaInicio": "%s", "horaFin": "%s"}
						""".formatted(canchaId, FECHA, horaInicio, horaFin))
				.exchange().expectStatus().isCreated()
				.expectBody(ReservaCreada.class).returnResult().getResponseBody().id();
	}

	private ResponseSpec pedir(String ruta, String token) {
		var peticion = cliente.get().uri(ruta);
		if (token != null) {
			peticion.header("Authorization", "Bearer " + token);
		}
		return peticion.exchange();
	}

	private Agenda agenda(String token, String parametros) {
		return pedir("/api/escenarios/" + la70 + "/agenda" + parametros, token).expectStatus().isOk()
				.expectBody(Agenda.class).returnResult().getResponseBody();
	}

	private List<EscenarioAgenda> escenarios(String token) {
		return List.of(pedir("/api/agenda/escenarios", token).expectStatus().isOk()
				.expectBody(EscenarioAgenda[].class).returnResult().getResponseBody());
	}

	private static List<String> codigos(ResponseSpec respuesta) {
		return respuesta.expectBody(Errores.class).returnResult().getResponseBody().errores().stream()
				.map(Error::codigo).toList();
	}

	// CA1: todas las canchas con sus franjas del día libres, reservadas o bloqueadas; la cancelada no ocupa
	@Test
	void ca1VistaPorDiaConTodasLasCanchas() {
		Agenda agenda = agenda(tokenPropietario, "?fecha=" + FECHA + "&vista=DIA");

		assertThat(agenda.escenario()).isEqualTo("Canchas La 70");
		assertThat(agenda.desde()).isEqualTo(FECHA);
		assertThat(agenda.hasta()).isEqualTo(FECHA);
		assertThat(agenda.canchas()).extracting(Cancha::nombre).containsExactly("Cancha 1", "Cancha 2");
		Dia cancha1 = agenda.dia(this.cancha1, FECHA);
		assertThat(cancha1.franjas())
				.extracting(Franja::horaInicio, Franja::horaFin, Franja::estado, Franja::reservaId, Franja::bloqueoId)
				.containsExactly(tuple("17:00", "18:00", "LIBRE", null, null),
						tuple("18:00", "19:00", "RESERVADA", reserva, null),
						tuple("19:00", "20:00", "RESERVADA", reserva, null),
						tuple("20:00", "21:00", "BLOQUEADA", null, bloqueo));
		assertThat(cancha1.bloqueos()).extracting(Bloqueo::id, Bloqueo::horaInicio, Bloqueo::horaFin,
				Bloqueo::tipoBloqueo, Bloqueo::motivo)
				.containsExactly(tuple(bloqueo, "20:00", "21:00", "MANTENIMIENTO", "Cambio de malla"));
		assertThat(agenda.dia(cancha2, FECHA).franjas()).extracting(Franja::estado)
				.containsExactly("LIBRE", "LIBRE", "LIBRE", "LIBRE");
	}

	// CA1 y ESC-USA-04: sin parámetros abre el día de hoy
	@Test
	void ca1SinParametrosAbreHoy() {
		Agenda agenda = agenda(tokenPropietario, "");

		assertThat(agenda.desde()).isEqualTo(HOY);
		assertThat(agenda.hasta()).isEqualTo(HOY);
	}

	// CA2: de lunes a domingo, los siete días de cada cancha, con la reserva y el bloqueo en su día
	@Test
	void ca2VistaSemanaConSieteDias() {
		Agenda agenda = agenda(tokenPropietario, "?fecha=" + FECHA + "&vista=SEMANA");

		assertThat(agenda.desde()).isEqualTo(LUNES);
		assertThat(agenda.hasta()).isEqualTo(LUNES.plusDays(6));
		for (Cancha cancha : agenda.canchas()) {
			assertThat(cancha.dias()).extracting(Dia::fecha).hasSize(7).startsWith(LUNES).endsWith(LUNES.plusDays(6));
		}
		assertThat(agenda.dia(cancha1, FECHA).reservas()).extracting(Reserva::id).containsExactly(cancelada, reserva);
		assertThat(agenda.dia(cancha1, FECHA).bloqueos()).extracting(Bloqueo::id).containsExactly(bloqueo);
	}

	// CA3: nombre y celular del jugador, confirmación de asistencia y opciones; la cancelada trae su origen (P4)
	@Test
	void ca3DatosDelJugadorYOpciones() {
		List<Reserva> reservas = agenda(tokenPropietario, "?fecha=" + FECHA).dia(cancha1, FECHA).reservas();

		assertThat(reservas).filteredOn(r -> r.id().equals(reserva)).singleElement().satisfies(r -> {
			assertThat(r.horaInicio()).isEqualTo("18:00");
			assertThat(r.horaFin()).isEqualTo("20:00");
			assertThat(r.estado()).isEqualTo("CONFIRMADA");
			assertThat(r.jugador()).isEqualTo("Carlos Mejía");
			assertThat(r.celularJugador()).isEqualTo("+573001112233");
			assertThat(r.confirmacionAsistencia()).isEqualTo("NO_REQUERIDA");
			// Faltan dos días: el propietario puede cancelar por fuerza mayor; la inasistencia, todavía no
			assertThat(r.puedeCancelarPorFuerzaMayor()).isTrue();
			assertThat(r.puedeMarcarInasistencia()).isFalse();
		});
		assertThat(reservas).filteredOn(r -> r.id().equals(cancelada)).singleElement().satisfies(r -> {
			assertThat(r.estado()).isEqualTo("CANCELADA");
			assertThat(r.origenCancelacion()).isEqualTo("ADMINISTRADOR");
			assertThat(r.motivoCancelacion()).isEqualTo("Tormenta eléctrica");
			assertThat(r.fechaCancelacion()).isNotNull();
			assertThat(r.puedeCancelarPorFuerzaMayor()).isFalse();
		});
	}

	// CA3 y D02: la encargada ve la agenda y los datos del jugador, pero no la cancelación por fuerza mayor
	@Test
	void ca3LaEncargadaNoCancelaPorFuerzaMayor() {
		Agenda agenda = agenda(tokenEncargada, "?fecha=" + FECHA);

		assertThat(agenda.propietario()).isFalse();
		assertThat(agenda.dia(cancha1, FECHA).reservas()).filteredOn(r -> r.id().equals(reserva)).singleElement()
				.satisfies(r -> {
					assertThat(r.celularJugador()).isEqualTo("+573001112233");
					assertThat(r.puedeCancelarPorFuerzaMayor()).isFalse();
				});
	}

	// CA4: el propietario elige entre todos sus escenarios
	@Test
	void ca4PropietarioVeSusEscenarios() {
		assertThat(escenarios(tokenPropietario))
				.extracting(EscenarioAgenda::id, EscenarioAgenda::nombre, EscenarioAgenda::estado,
						EscenarioAgenda::propietario)
				.containsExactly(tuple(la70, "Canchas La 70", "ACTIVO", true),
						tuple(sedeNorte, "Sede Norte", "ACTIVO", true));
	}

	// CA5: la encargada solo ve el escenario que tiene asignado
	@Test
	void ca5EncargadoSoloVeLosAsignados() {
		assertThat(escenarios(tokenEncargada)).extracting(EscenarioAgenda::nombre, EscenarioAgenda::propietario)
				.containsExactly(tuple("Canchas La 70", false));
	}

	// CA6: otro administrador de cancha recibe 403 con ERR-GEN-06 y ningún dato
	@Test
	void ca6OtroAdministrador_403_ERR_GEN_06() {
		assertThat(codigos(pedir("/api/escenarios/" + la70 + "/agenda", tokenOtroAdministrador).expectStatus()
				.isForbidden())).containsExactly("ERR-GEN-06");
	}

	// CA6 y P7: un jugador tampoco ve la agenda ni la lista de escenarios
	@Test
	void ca6Jugador_403_ERR_GEN_06() {
		assertThat(codigos(pedir("/api/escenarios/" + la70 + "/agenda", tokenJugador).expectStatus().isForbidden()))
				.containsExactly("ERR-GEN-06");
		assertThat(codigos(pedir("/api/agenda/escenarios", tokenJugador).expectStatus().isForbidden()))
				.containsExactly("ERR-GEN-06");
	}

	// P7: el escenario que no existe responde igual que el ajeno
	@Test
	void ca6EscenarioQueNoExiste_403_ERR_GEN_06() {
		assertThat(codigos(pedir("/api/escenarios/" + UUID.randomUUID() + "/agenda", tokenPropietario)
				.expectStatus().isForbidden())).containsExactly("ERR-GEN-06");
	}

	// AG-11 y ESC-SEG-02: sin sesión, 401 en los dos servicios
	@Test
	void sinToken_401() {
		assertThat(codigos(pedir("/api/escenarios/" + la70 + "/agenda", null).expectStatus().isUnauthorized()))
				.containsExactly("ERR-GEN-05");
		assertThat(codigos(pedir("/api/agenda/escenarios", null).expectStatus().isUnauthorized()))
				.containsExactly("ERR-GEN-05");
	}

	// Una vista que no existe no se puede leer: 400 con ERR-GEN-07 (ManejadorErrores)
	@Test
	void vistaQueNoExiste_400() {
		assertThat(codigos(pedir("/api/escenarios/" + la70 + "/agenda?vista=MES", tokenPropietario).expectStatus()
				.isBadRequest())).containsExactly("ERR-GEN-07");
	}

}
