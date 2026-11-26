package co.edu.uco.canchaya.reservas.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.ConfirmacionAsistencia;
import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.OrigenCancelacion;
import co.edu.uco.canchaya.reservas.domain.ParametrosReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * Con la base real: HU12, CA6 (reservas confirmadas futuras dentro de un horario, D08), HU13, CA7
 * (qué cuenta como reserva confirmada futura, RN-17, D08), HU20, CA2 (las reservas que ocupan
 * franjas de una cancha en una fecha), HU21 (guardar y los límites del jugador) y HU24 (buscar y
 * cancelar con actualización condicional).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RepositorioReservasJpaTest {

	private static final String F5 = "27d013e3-f5b3-5378-a7df-7c80429554fe";
	// «Ahora» fijo: 15 de enero de 2030 a las 10:00 en Bogotá
	private static final LocalDate HOY = LocalDate.of(2030, 1, 15);
	private static final LocalDateTime AHORA = HOY.atTime(10, 0);

	@Autowired
	private RepositorioReservas repositorio;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID jugador;
	private UUID cancha;
	private UUID otraCancha;

	@BeforeEach
	void preparar() {
		UUID propietario = usuario("1000000041", "ADMINISTRADOR_CANCHA");
		jugador = usuario("1000000042", "JUGADOR");
		UUID escenario = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Cancha La 80', 'Calle 80', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, 'ACTIVO')""", escenario, propietario);
		cancha = cancha(escenario, "Cancha 1");
		otraCancha = cancha(escenario, "Cancha 2");
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from reserva");
		jdbc.update("delete from cancha");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private UUID usuario(String documento, String rol) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Ana Pérez', ?, ?, 'CC', ?, '+573001234567', ?, now())""",
				id, documento + "@correo.com", "x".repeat(60), documento, rol);
		return id;
	}

	private UUID cancha(UUID escenario, String nombre) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora) values (?, ?, ?, ?, 80000)",
				id, escenario, UUID.fromString(F5), nombre);
		return id;
	}

	private void confirmada(UUID canchaId, LocalDate fecha, int hora) {
		confirmada(canchaId, fecha, hora, hora + 1);
	}

	private UUID confirmada(UUID canchaId, LocalDate fecha, int desde, int hasta) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado,
				  confirmacion_asistencia, fecha_creacion)
				values (?, ?, ?, ?, ?, ?, ?, 'CONFIRMADA', 'NO_REQUERIDA', now())""",
				id, canchaId, jugador, fecha, LocalTime.of(desde, 0), LocalTime.of(hasta, 0),
				new BigDecimal(80000 * (hasta - desde)));
		return id;
	}

	private void cancelada(UUID canchaId, LocalDate fecha, int hora) {
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado,
				  origen_cancelacion, fecha_creacion, fecha_cancelacion)
				values (?, ?, ?, ?, ?, ?, 80000, 'CANCELADA', 'JUGADOR', now(), now())""",
				UUID.randomUUID(), canchaId, jugador, fecha, LocalTime.of(hora, 0), LocalTime.of(hora + 1, 0));
	}

	private boolean hayFuturas() {
		return repositorio.existenConfirmadasQueEmpiezanDespuesDe(List.of(cancha), AHORA);
	}

	@Test
	void ca7UnaReservaConfirmadaDeOtroDiaCuenta() {
		confirmada(cancha, HOY.plusDays(1), 8);

		assertThat(hayFuturas()).isTrue();
	}

	@Test
	void ca7UnaReservaConfirmadaDeHoyMasTardeCuenta() {
		confirmada(cancha, HOY, 11);

		assertThat(hayFuturas()).isTrue();
	}

	@Test
	void ca3LasQueYaEmpezaronOPasaronNoCuentan() {
		confirmada(cancha, HOY, 10); // empieza justo ahora: ya no es futura
		confirmada(cancha, HOY, 8);
		confirmada(cancha, HOY.minusDays(1), 18);

		assertThat(hayFuturas()).isFalse();
	}

	@Test
	void ca3LasCanceladasNoCuentan() {
		cancelada(cancha, HOY.plusDays(1), 8);

		assertThat(hayFuturas()).isFalse();
	}

	@Test
	void ca3LasDeOtrasCanchasNoCuentan() {
		confirmada(otraCancha, HOY.plusDays(1), 8);

		assertThat(hayFuturas()).isFalse();
		assertThat(repositorio.existenConfirmadasQueEmpiezanDespuesDe(List.of(cancha, otraCancha), AHORA)).isTrue();
	}

	// HU12, CA6 (política 15, D08, P2): existenConfirmadasEnHorarioQueEmpiezanDespuesDe. HOY es
	// martes; el horario que se quiere eliminar es el del martes de 06:00 a 12:00

	private boolean hayEnHorarioDelMartes(List<UUID> canchas) {
		return repositorio.existenConfirmadasEnHorarioQueEmpiezanDespuesDe(canchas, DayOfWeek.TUESDAY,
				LocalTime.of(6, 0), LocalTime.of(12, 0), AHORA);
	}

	private boolean hayEnHorarioDelMartes() {
		return hayEnHorarioDelMartes(List.of(cancha));
	}

	@Test
	void ca6ExistenConfirmadasEnHorario_elMartesSiguienteDentroDelRangoCuenta() {
		assertThat(HOY.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
		confirmada(cancha, HOY.plusWeeks(1), 8);

		assertThat(hayEnHorarioDelMartes()).isTrue();
	}

	@Test
	void ca6ExistenConfirmadasEnHorario_hoyMasTardeDentroDelRangoCuenta() {
		confirmada(cancha, HOY, 11);

		assertThat(hayEnHorarioDelMartes()).isTrue();
	}

	// P2: cuenta aunque solo una parte de la reserva quede dentro del rango (puede pasar después de HU11)
	@Test
	void ca6ExistenConfirmadasEnHorario_laQueSeCruzaEnParteCuenta() {
		confirmada(cancha, HOY.plusWeeks(1), 11, 13);
		assertThat(hayEnHorarioDelMartes()).isTrue();

		jdbc.update("delete from reserva");
		confirmada(cancha, HOY.plusWeeks(2), 5, 7);
		assertThat(hayEnHorarioDelMartes()).isTrue();
	}

	@Test
	void ca6ExistenConfirmadasEnHorario_lasDeOtroDiaNoCuentan() {
		confirmada(cancha, HOY.plusDays(1), 8); // miércoles
		confirmada(cancha, HOY.plusDays(6), 8); // lunes

		assertThat(hayEnHorarioDelMartes()).isFalse();
	}

	@Test
	void ca6ExistenConfirmadasEnHorario_lasDeFueraDelRangoNoCuentan() {
		confirmada(cancha, HOY.plusWeeks(1), 14, 16);

		assertThat(hayEnHorarioDelMartes()).isFalse();
	}

	// La hora de fin es abierta, como en la restricción de exclusión: pegadas al borde no se cruzan
	@Test
	void ca6ExistenConfirmadasEnHorario_lasPegadasAlBordeNoCuentan() {
		confirmada(cancha, HOY.plusWeeks(1), 5); // de 05:00 a 06:00
		confirmada(cancha, HOY.plusWeeks(1), 12); // de 12:00 a 13:00

		assertThat(hayEnHorarioDelMartes()).isFalse();
	}

	@Test
	void ca6ExistenConfirmadasEnHorario_lasCanceladasNoCuentan() {
		cancelada(cancha, HOY.plusWeeks(1), 8);

		assertThat(hayEnHorarioDelMartes()).isFalse();
	}

	@Test
	void ca6ExistenConfirmadasEnHorario_lasQueYaEmpezaronOPasaronNoCuentan() {
		confirmada(cancha, HOY, 10); // empieza justo ahora: ya no es futura
		confirmada(cancha, HOY, 8);
		confirmada(cancha, HOY.minusWeeks(1), 8);

		assertThat(hayEnHorarioDelMartes()).isFalse();
	}

	@Test
	void ca6ExistenConfirmadasEnHorario_lasDeOtrasCanchasNoCuentan() {
		confirmada(otraCancha, HOY.plusWeeks(1), 8);

		assertThat(hayEnHorarioDelMartes()).isFalse();
		assertThat(hayEnHorarioDelMartes(List.of(cancha, otraCancha))).isTrue();
	}

	// HU20: confirmadasDeCanchaEnFecha

	@Test
	void hu20Ca2SoloLasConfirmadasDeLaCanchaEnLaFechaYPorHoraDeInicio() {
		UUID deDosHoras = confirmada(cancha, HOY, 18, 20);
		confirmada(cancha, HOY, 8);
		cancelada(cancha, HOY, 10);
		confirmada(otraCancha, HOY, 9);
		confirmada(cancha, HOY.plusDays(1), 8);

		List<Reserva> reservas = repositorio.confirmadasDeCanchaEnFecha(cancha, HOY);

		assertThat(reservas).extracting(Reserva::getHoraInicio).containsExactly(LocalTime.of(8, 0), LocalTime.of(18, 0));
		Reserva ultima = reservas.get(1);
		assertThat(ultima.getId()).isEqualTo(deDosHoras);
		assertThat(ultima.getCanchaId()).isEqualTo(cancha);
		assertThat(ultima.getJugadorId()).isEqualTo(jugador);
		assertThat(ultima.getFecha()).isEqualTo(HOY);
		assertThat(ultima.getHoraFin()).isEqualTo(LocalTime.of(20, 0));
		assertThat(ultima.horas()).isEqualTo(2);
		assertThat(ultima.getPrecio()).isEqualByComparingTo("160000");
		assertThat(ultima.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
		assertThat(ultima.getConfirmacionAsistencia()).isEqualTo(ConfirmacionAsistencia.NO_REQUERIDA);
		assertThat(ultima.getFechaCreacion()).isNotNull();
		assertThat(ultima.getOrigenCancelacion()).isNull();
		assertThat(ultima.getFechaSolicitudConfirmacion()).isNull();
	}

	@Test
	void hu20Ca2SinReservasLaListaEstaVacia() {
		cancelada(cancha, HOY, 10);

		assertThat(repositorio.confirmadasDeCanchaEnFecha(cancha, HOY)).isEmpty();
	}

	// HU21: guardar, contarActivasDeJugador y horasConfirmadasDeJugadorEnFecha

	private Reserva nueva(UUID canchaId, LocalDate fecha, int desde, int hasta) {
		return Reserva.reconstruir(UUID.randomUUID(), canchaId, jugador, fecha, LocalTime.of(desde, 0),
				LocalTime.of(hasta, 0), new BigDecimal("80000.00").multiply(BigDecimal.valueOf(hasta - desde)),
				EstadoReserva.CONFIRMADA, ConfirmacionAsistencia.NO_REQUERIDA, null, null,
				// PostgreSQL guarda microsegundos; Java en Windows da nanosegundos
				OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS), null, null);
	}

	private int filas() {
		return jdbc.queryForObject("select count(*) from reserva", Integer.class);
	}

	@Test
	void hu21GuardarYLeerConservaTodosLosDatos() {
		Reserva reserva = nueva(cancha, HOY, 18, 20);

		repositorio.guardar(reserva);

		assertThat(repositorio.existePorId(reserva.getId())).isTrue();
		assertThat(repositorio.existePorId(UUID.randomUUID())).isFalse();
		Reserva leida = repositorio.confirmadasDeCanchaEnFecha(cancha, HOY).get(0);
		assertThat(leida).usingRecursiveComparison().ignoringFields("fechaCreacion").isEqualTo(reserva);
		assertThat(leida.getFechaCreacion().toInstant()).isEqualTo(reserva.getFechaCreacion().toInstant());
	}

	// CA3, D29 punto 1: si el turno no la frenara, la restricción de exclusión rechaza el cruce
	@Test
	void CA3_restriccionDeExclusion_ERR_RES_06() {
		confirmada(cancha, HOY, 18, 20);

		assertThatThrownBy(() -> repositorio.guardar(nueva(cancha, HOY, 19, 21)))
				.isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje()).isEqualTo("ERR-RES-06");
		assertThat(filas()).isEqualTo(1);

		// Seguidas no se cruzan (hora de fin abierta), ni en otra cancha, ni sobre una cancelada
		cancelada(cancha, HOY, 21);
		repositorio.guardar(nueva(cancha, HOY, 20, 22));
		repositorio.guardar(nueva(otraCancha, HOY, 18, 20));
		assertThat(filas()).isEqualTo(4);
	}

	// CA6 y P4: activa = CONFIRMADA y que no ha terminado (fecha + horaFin > ahora)
	@Test
	void CA6_reservaTerminada_noCuentaComoActiva() {
		confirmada(cancha, HOY, 8, 10); // terminó justo ahora (10:00): ya no cuenta
		confirmada(otraCancha, HOY, 9, 11); // está en juego: cuenta
		confirmada(cancha, HOY.plusDays(1), 8); // futura: cuenta
		confirmada(cancha, HOY.minusDays(1), 18); // de ayer: no cuenta
		cancelada(cancha, HOY.plusDays(2), 8); // cancelada: no cuenta

		assertThat(repositorio.contarActivasDeJugador(jugador, AHORA)).isEqualTo(2);
		assertThat(repositorio.contarActivasDeJugador(UUID.randomUUID(), AHORA)).isZero();
	}

	// CA7 y CA14: horas CONFIRMADAS del jugador en la fecha, sumando todas las canchas
	@Test
	void CA7_superaDosHorasEnElDia_ERR_RES_09() {
		confirmada(cancha, HOY, 8); // 1 h
		confirmada(otraCancha, HOY, 14, 16); // 2 h en otra cancha
		cancelada(cancha, HOY, 18); // no cuenta
		confirmada(cancha, HOY.plusDays(1), 8); // otro día

		assertThat(repositorio.horasConfirmadasDeJugadorEnFecha(jugador, HOY)).isEqualTo(3);
		assertThat(repositorio.horasConfirmadasDeJugadorEnFecha(jugador, HOY.plusDays(5))).isZero();
		assertThat(repositorio.horasConfirmadasDeJugadorEnFecha(UUID.randomUUID(), HOY)).isZero();
	}

	// HU23, CA5: solo las reservas del jugador pedido, en cualquier estado y fecha
	@Test
	void CA5_deJugador_soloLasSuyas() {
		UUID suyaFutura = confirmada(cancha, HOY.plusDays(1), 8, 10);
		UUID suyaPasada = confirmada(otraCancha, HOY.minusDays(3), 18, 19);
		cancelada(cancha, HOY.plusDays(2), 8);
		UUID otroJugador = usuario("1000000043", "JUGADOR");
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado,
				  confirmacion_asistencia, fecha_creacion)
				values (?, ?, ?, ?, '12:00', '13:00', 80000, 'CONFIRMADA', 'NO_REQUERIDA', now())""",
				UUID.randomUUID(), cancha, otroJugador, HOY.plusDays(1));

		List<Reserva> suyas = repositorio.deJugador(jugador);

		assertThat(suyas).hasSize(3).allSatisfy(r -> assertThat(r.getJugadorId()).isEqualTo(jugador));
		assertThat(suyas).extracting(Reserva::getId).contains(suyaFutura, suyaPasada);
		assertThat(suyas).extracting(Reserva::getEstado)
				.containsExactlyInAnyOrder(EstadoReserva.CONFIRMADA, EstadoReserva.CONFIRMADA, EstadoReserva.CANCELADA);
		assertThat(repositorio.deJugador(otroJugador)).hasSize(1);
		assertThat(repositorio.deJugador(UUID.randomUUID())).isEmpty();
	}

	// HU24: buscarPorId y cancelarSiSigueConfirmada (P3, D29 punto 3)

	private static final ParametrosReserva PARAMETROS = new ParametrosReserva(30, 2, 2, 5, 3, 5, LocalTime.NOON, 3,
			24, 250, false);

	/** Guarda una reserva de HOY de 18:00 a 19:00 y la devuelve leída de la base. */
	private Reserva guardadaALas18() {
		Reserva reserva = nueva(cancha, HOY, 18, 19);
		repositorio.guardar(reserva);
		return repositorio.buscarPorId(reserva.getId()).orElseThrow();
	}

	@Test
	void ca1BuscarPorId_devuelveLaReserva() {
		Reserva guardada = nueva(cancha, HOY, 18, 20);
		repositorio.guardar(guardada);

		Reserva leida = repositorio.buscarPorId(guardada.getId()).orElseThrow();

		// La base devuelve el mismo instante en UTC: se compara como instante, igual que en HU21
		assertThat(leida).usingRecursiveComparison().ignoringFields("fechaCreacion").isEqualTo(guardada);
		assertThat(leida.getFechaCreacion().toInstant()).isEqualTo(guardada.getFechaCreacion().toInstant());
		assertThat(repositorio.buscarPorId(UUID.randomUUID())).isEmpty();
	}

	@Test
	void ca1CancelarSiSigueConfirmada_cambiaLaFilaYLiberaLaFranja() {
		Reserva reserva = guardadaALas18();
		reserva.cancelar(new Solicitante(jugador, Rol.JUGADOR), AHORA, PARAMETROS);

		assertThat(repositorio.cancelarSiSigueConfirmada(reserva)).isTrue();

		// Política 19: estado, origen y fecha guardados; ck_reserva_cancelacion los exige juntos
		Reserva leida = repositorio.buscarPorId(reserva.getId()).orElseThrow();
		assertThat(leida.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
		assertThat(leida.getOrigenCancelacion()).isEqualTo(OrigenCancelacion.JUGADOR);
		assertThat(leida.getFechaCancelacion().toInstant())
				.isEqualTo(AHORA.atZone(ZoneId.of("America/Bogota")).toInstant());
		// Política 20: la franja vuelve a estar libre; la restricción de exclusión ya no la cuenta
		assertThat(repositorio.confirmadasDeCanchaEnFecha(cancha, HOY)).isEmpty();
		repositorio.guardar(nueva(cancha, HOY, 18, 19));
		assertThat(repositorio.confirmadasDeCanchaEnFecha(cancha, HOY)).hasSize(1);
	}

	@Test
	void ca4CancelarSiSigueConfirmada_yaCancelada_devuelveFalso() {
		Solicitante solicitante = new Solicitante(jugador, Rol.JUGADOR);
		Reserva primera = guardadaALas18();
		// Otra operación leyó la misma reserva cuando todavía estaba CONFIRMADA
		Reserva segunda = repositorio.buscarPorId(primera.getId()).orElseThrow();
		primera.cancelar(solicitante, AHORA, PARAMETROS);
		assertThat(repositorio.cancelarSiSigueConfirmada(primera)).isTrue();

		segunda.cancelar(solicitante, AHORA.plusMinutes(5), PARAMETROS);

		assertThat(repositorio.cancelarSiSigueConfirmada(segunda)).isFalse();
		// No se pisa lo que guardó la primera
		Reserva leida = repositorio.buscarPorId(primera.getId()).orElseThrow();
		assertThat(leida.getFechaCancelacion().toInstant())
				.isEqualTo(AHORA.atZone(ZoneId.of("America/Bogota")).toInstant());
	}

	// HU29 (P4): deCanchasEntre trae todas las reservas del rango, en cualquier estado

	@Test
	void hu29DeCanchasEntre_todasLasDelRangoEnCualquierEstadoYEnOrden() {
		UUID delDomingo = confirmada(cancha, HOY.plusDays(6), 8, 9);
		UUID deHoyTarde = confirmada(cancha, HOY, 18, 20);
		cancelada(otraCancha, HOY, 7);
		confirmada(cancha, HOY.minusDays(1), 10, 11);
		confirmada(cancha, HOY.plusDays(7), 10, 11);

		List<Reserva> reservas = repositorio.deCanchasEntre(List.of(cancha, otraCancha), HOY, HOY.plusDays(6));

		assertThat(reservas).extracting(Reserva::getCanchaId, Reserva::getFecha, Reserva::getHoraInicio,
				Reserva::getEstado)
				.containsExactly(tuple(otraCancha, HOY, LocalTime.of(7, 0), EstadoReserva.CANCELADA),
						tuple(cancha, HOY, LocalTime.of(18, 0), EstadoReserva.CONFIRMADA),
						tuple(cancha, HOY.plusDays(6), LocalTime.of(8, 0), EstadoReserva.CONFIRMADA));
		assertThat(reservas.get(1).getId()).isEqualTo(deHoyTarde);
		assertThat(reservas.get(2).getId()).isEqualTo(delDomingo);
		// La cancelada conserva su origen para mostrarlo en la agenda (ESC-AUD-01)
		assertThat(reservas.get(0).getOrigenCancelacion()).isEqualTo(OrigenCancelacion.JUGADOR);
	}

	@Test
	void hu29DeCanchasEntre_soloLasCanchasPedidas() {
		confirmada(otraCancha, HOY, 18);

		assertThat(repositorio.deCanchasEntre(List.of(cancha), HOY, HOY)).isEmpty();
		assertThat(repositorio.deCanchasEntre(List.of(), HOY, HOY)).isEmpty();
	}

}
