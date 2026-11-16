package co.edu.uco.canchaya.reservas.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;

/** HU13, CA7: qué cuenta como reserva confirmada futura (RN-17, D08), con la base real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RepositorioReservasJdbcTest {

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
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado, fecha_creacion)
				values (?, ?, ?, ?, ?, ?, 80000, 'CONFIRMADA', now())""",
				UUID.randomUUID(), canchaId, jugador, fecha, LocalTime.of(hora, 0), LocalTime.of(hora + 1, 0));
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

}
