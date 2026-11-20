package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU10: guardar el estado de una cancha, con PostGIS real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CambioEstadoCanchaPersistenciaTest {

	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");

	@Autowired
	private RepositorioCanchas repositorio;

	@Autowired
	private RepositorioEscenarios repositorioEscenarios;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID propietario;
	private UUID escenarioId;

	@BeforeEach
	void preparar() {
		propietario = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Ana Pérez', 'ana@correo.com', ?, 'CC', '1000000041', '+573001234567',
				  'ADMINISTRADOR_CANCHA', now())""", propietario, "x".repeat(60));
		UUID rionegro = jdbc.queryForObject("select id from ciudad where nombre = 'Rionegro'", UUID.class);
		escenarioId = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Cancha La 80', 'Calle 80 # 45-10', ?, 6.155123, -75.3737, '+573001234567', ?, 'ACTIVO')""",
				escenarioId, rionegro, propietario);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from cancha");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private UUID cancha(String nombre, String estado) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora, estado)
				values (?, ?, ?, ?, 80000, ?)""", id, escenarioId, F5, nombre, estado);
		return id;
	}

	private String estadoGuardado(UUID canchaId) {
		return jdbc.queryForObject("select estado from cancha where id = ?", String.class, canchaId);
	}

	// CA1 y CA2: cambia solo la columna estado de esa cancha, en los dos sentidos
	@Test
	void ca1ActualizarEstado_cambiaLaFila() {
		UUID id = cancha("Cancha 1", "DISPONIBLE");
		UUID otra = cancha("Cancha 2", "DISPONIBLE");
		Escenario escenario = repositorioEscenarios.buscarPorId(escenarioId).orElseThrow();
		Solicitante solicitante = new Solicitante(propietario, Rol.ADMINISTRADOR_CANCHA);

		Cancha cancha = repositorio.buscarPorId(id).orElseThrow();
		cancha.cambiarEstado(escenario, EstadoCancha.INACTIVA, solicitante, false);
		repositorio.actualizarEstado(cancha);

		assertThat(estadoGuardado(id)).isEqualTo("INACTIVA");
		assertThat(estadoGuardado(otra)).isEqualTo("DISPONIBLE");
		// Los demás datos no se tocan
		Map<String, Object> fila = jdbc.queryForMap("select nombre, precio_hora, tipo_cancha_id from cancha where id = ?", id);
		assertThat(fila.get("nombre")).isEqualTo("Cancha 1");
		assertThat((BigDecimal) fila.get("precio_hora")).isEqualByComparingTo("80000");
		assertThat(fila.get("tipo_cancha_id")).isEqualTo(F5);

		// Y de vuelta a DISPONIBLE (CA2); lo que se lee después ya trae el estado nuevo
		Cancha inactiva = repositorio.buscarPorId(id).orElseThrow();
		assertThat(inactiva.getEstado()).isEqualTo(EstadoCancha.INACTIVA);
		inactiva.cambiarEstado(escenario, EstadoCancha.DISPONIBLE, solicitante, false);
		repositorio.actualizarEstado(inactiva);

		assertThat(estadoGuardado(id)).isEqualTo("DISPONIBLE");
	}

}
