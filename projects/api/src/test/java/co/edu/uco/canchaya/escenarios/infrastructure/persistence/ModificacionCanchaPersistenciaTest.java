package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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
import co.edu.uco.canchaya.escenarios.application.port.in.ModificarCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.ModificarCancha.DatosModificacionCancha;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU9 con la base real (PostGIS), los tipos de la V2 y el índice único de la V3. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ModificacionCanchaPersistenciaTest {

	// Identificador de F5 en la V2
	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");

	@Autowired
	private RepositorioCanchas repositorioCanchas;

	@Autowired
	private RepositorioEscenarios repositorioEscenarios;

	@Autowired
	private ModificarCancha modificarCancha;

	@Autowired
	private JdbcTemplate jdbc;

	private Solicitante propietario;
	private UUID escenarioId;
	private UUID f7;

	@BeforeEach
	void preparar() {
		UUID usuarioId = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Dueño', 'dueno@correo.com', ?, 'CC', '1000000002', '+573001234567',
				  'ADMINISTRADOR_CANCHA', now())""", usuarioId, "x".repeat(60));
		propietario = new Solicitante(usuarioId, Rol.ADMINISTRADOR_CANCHA);
		escenarioId = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Cancha La 80', 'Calle 80 # 45-10', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, 'ACTIVO')""", escenarioId, usuarioId);
		f7 = jdbc.queryForObject("select id from tipo_cancha where codigo = 'F7'", UUID.class);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from reserva");
		jdbc.update("delete from cancha");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private UUID cancha(String nombre) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora, estado)
				values (?, ?, ?, ?, 80000, 'INACTIVA')""", id, escenarioId, F5, nombre);
		return id;
	}

	private Cancha modificada(UUID id, String nombre) {
		Cancha cancha = repositorioCanchas.buscarPorId(id).orElseThrow();
		Escenario escenario = repositorioEscenarios.buscarPorId(cancha.getEscenarioId()).orElseThrow();
		cancha.modificar(escenario, f7, nombre, new BigDecimal("95000.5"), propietario);
		return cancha;
	}

	@Test
	void ca1Actualizar_cambiaNombreTipoYPrecio() {
		UUID id = cancha("Cancha 1");
		cancha("Cancha 2");

		repositorioCanchas.actualizar(modificada(id, "Cancha Principal"));

		Map<String, Object> fila = jdbc.queryForMap("select * from cancha where id = ?", id);
		assertThat(fila).containsEntry("nombre", "Cancha Principal").containsEntry("tipo_cancha_id", f7)
				.containsEntry("escenario_id", escenarioId)
				// El estado no lo cambia modificar (lo cambia HU10)
				.containsEntry("estado", "INACTIVA");
		assertThat((BigDecimal) fila.get("precio_hora")).isEqualByComparingTo("95000.50");
		// La otra cancha no cambia
		assertThat(jdbc.queryForObject("select count(*) from cancha where nombre = 'Cancha 2'", Integer.class))
				.isEqualTo(1);
	}

	// CA1 y política 8: la propia cancha no cuenta; las demás del escenario sí, sin mayúsculas
	@Test
	void ca1ExisteNombreEnOtraCancha_excluyeLaPropiaCancha() {
		UUID id = cancha("Cancha 1");
		cancha("Cancha 2");

		assertThat(repositorioCanchas.existeNombreEnOtraCancha(escenarioId, "CANCHA 1", id)).isFalse();
		assertThat(repositorioCanchas.existeNombreEnOtraCancha(escenarioId, "cancha 2", id)).isTrue();
		assertThat(repositorioCanchas.existeNombreEnOtraCancha(UUID.randomUUID(), "Cancha 2", id)).isFalse();
	}

	// CA5 con dos solicitudes al mismo tiempo: la que llega segunda pasó la revisión del caso de
	// uso, pero el índice único de la V3 la rechaza al guardar
	@Test
	void ca5Actualizar_nombreRepetido_ERR_CAN_08() {
		UUID id = cancha("Cancha 1");
		cancha("Cancha 2");
		Cancha conNombreRepetido = modificada(id, "cancha 2");

		assertThatThrownBy(() -> repositorioCanchas.actualizar(conNombreRepetido))
				.isInstanceOf(ReglaNegocioException.class).satisfies(e -> {
					ReglaNegocioException error = (ReglaNegocioException) e;
					assertThat(error.getCodigoMensaje()).isEqualTo("ERR-CAN-08");
					assertThat(error.getParametros()).containsExactly("cancha 2");
				});
		Map<String, Object> fila = jdbc.queryForMap("select * from cancha where id = ?", id);
		assertThat(fila).containsEntry("nombre", "Cancha 1").containsEntry("tipo_cancha_id", F5);
	}

	// CA8 y política 9 (P2 de la especificación): la reserva guarda su propio precio. La parte
	// «las reservas nuevas toman el precio actualizado» la prueba HU21
	@Test
	void ca8CambiarPrecio_noCambiaElPrecioDeLasReservas() {
		UUID canchaId = cancha("Cancha 1");
		jdbc.update("update cancha set estado = 'DISPONIBLE' where id = ?", canchaId);
		UUID jugador = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Luis Gómez', 'luis@correo.com', ?, 'CC', '1000000063', '+573001234567', 'JUGADOR', now())""",
				jugador, "x".repeat(60));
		UUID reserva = UUID.randomUUID();
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado, fecha_creacion)
				values (?, ?, ?, ?, ?, ?, 80000, 'CONFIRMADA', now())""",
				reserva, canchaId, jugador, LocalDate.now(ZoneId.of("America/Bogota")).plusDays(1),
				LocalTime.of(18, 0), LocalTime.of(19, 0));

		modificarCancha.ejecutar(new DatosModificacionCancha(canchaId, F5, "Cancha 1", new BigDecimal("120000")),
				propietario);

		assertThat(jdbc.queryForObject("select precio_hora from cancha where id = ?", BigDecimal.class, canchaId))
				.isEqualByComparingTo("120000.00");
		assertThat(jdbc.queryForObject("select precio from reserva where id = ?", BigDecimal.class, reserva))
				.isEqualByComparingTo("80000.00");
		assertThat(repositorioCanchas.buscarPorId(canchaId)).map(Cancha::getEstado).contains(EstadoCancha.DISPONIBLE);
	}

}
