package co.edu.uco.canchaya.reservas.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaFranjasOcupadas;
import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaFranjasOcupadas.RangoOcupado;

/**
 * HU18 (T6, P1): Reservas cumple el puerto que define Escenarios, con la base real. Es la
 * aplicación completa (@SpringBootTest): también confirma que vuelve a arrancar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConsultaFranjasOcupadasPersistenciaTest {

	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");
	private static final LocalDate DIA = LocalDate.of(2030, 1, 15);

	@Autowired
	private ConsultaFranjasOcupadas consulta;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID jugador;
	private UUID una;
	private UUID otra;
	private UUID sinReservas;
	private UUID noPedida;

	@BeforeEach
	void preparar() {
		UUID propietario = usuario("1000000183", "ADMINISTRADOR_CANCHA");
		jugador = usuario("1000000184", "JUGADOR");
		UUID escenario = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Cancha La 80', 'Calle 80', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, 'ACTIVO')""", escenario, propietario);
		una = cancha(escenario, "Cancha 1");
		otra = cancha(escenario, "Cancha 2");
		sinReservas = cancha(escenario, "Cancha 3");
		noPedida = cancha(escenario, "Cancha 4");
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
				id, escenario, F5, nombre);
		return id;
	}

	private void confirmada(UUID canchaId, LocalDate fecha, int desde, int hasta) {
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado,
				  confirmacion_asistencia, fecha_creacion)
				values (?, ?, ?, ?, ?, ?, ?, 'CONFIRMADA', 'NO_REQUERIDA', now())""",
				UUID.randomUUID(), canchaId, jugador, fecha, LocalTime.of(desde, 0), LocalTime.of(hasta, 0),
				new BigDecimal(80000 * (hasta - desde)));
	}

	private void cancelada(UUID canchaId, LocalDate fecha, int desde, int hasta) {
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado,
				  origen_cancelacion, fecha_creacion, fecha_cancelacion)
				values (?, ?, ?, ?, ?, ?, 80000, 'CANCELADA', 'JUGADOR', now(), now())""",
				UUID.randomUUID(), canchaId, jugador, fecha, LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
	}

	private static RangoOcupado rango(int desde, int hasta) {
		return new RangoOcupado(LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
	}

	// CA5: solo cuentan las CONFIRMADAS de las canchas pedidas en esa fecha, agrupadas por cancha y por hora de inicio.
	// La CANCELADA, la de otro día y la de una cancha que no se pidió no salen; una cancha sin reservas no viene
	@Test
	void ca5SoloConfirmadasDeLaFecha() {
		confirmada(una, DIA, 18, 20);
		confirmada(una, DIA, 8, 9);
		cancelada(una, DIA, 10, 11);
		confirmada(una, DIA.plusDays(1), 12, 13);
		confirmada(otra, DIA, 14, 15);
		confirmada(noPedida, DIA, 14, 15);

		Map<UUID, List<RangoOcupado>> porCancha = consulta.confirmadasDelDia(List.of(una, otra, sinReservas), DIA);

		assertThat(porCancha).containsOnlyKeys(una, otra);
		assertThat(porCancha.get(una)).containsExactly(rango(8, 9), rango(18, 20));
		assertThat(porCancha.get(otra)).containsExactly(rango(14, 15));
	}

	// Sin canchas no se consulta la base: la búsqueda puede quedarse sin candidatas
	@Test
	void sinCanchas_mapaVacio() {
		confirmada(una, DIA, 8, 9);

		assertThat(consulta.confirmadasDelDia(List.of(), DIA)).isEmpty();
	}

}
