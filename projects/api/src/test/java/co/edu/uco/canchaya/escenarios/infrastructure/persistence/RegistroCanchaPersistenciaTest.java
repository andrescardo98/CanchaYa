package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

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
import co.edu.uco.canchaya.escenarios.application.port.in.ListarCanchasEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarTiposCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarCancha.DatosRegistroCancha;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.escenarios.domain.TipoCancha;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU6 con la base real (PostGIS) y los tipos de cancha que carga la V2. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RegistroCanchaPersistenciaTest {

	// Identificador de F5 en la V2
	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");

	@Autowired
	private RegistrarCancha registrarCancha;

	@Autowired
	private ListarCanchasEscenario listarCanchas;

	@Autowired
	private ListarTiposCancha listarTipos;

	@Autowired
	private RepositorioCanchas repositorioCanchas;

	@Autowired
	private JdbcTemplate jdbc;

	private Solicitante propietario;
	private UUID escenarioId;

	@BeforeEach
	void preparar() {
		UUID usuarioId = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Dueño', 'dueno@correo.com', ?, 'CC', '1000000002', '+573001234567',
				  'ADMINISTRADOR_CANCHA', now())""", usuarioId, "x".repeat(60));
		propietario = new Solicitante(usuarioId, Rol.ADMINISTRADOR_CANCHA);
		UUID rionegro = jdbc.queryForObject("select id from ciudad where nombre = 'Rionegro'", UUID.class);
		escenarioId = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Cancha La 80', 'Calle 80 # 45-10', ?, 6.155123, -75.3737, '+573001234567', ?, 'INACTIVO')""",
				escenarioId, rionegro, usuarioId);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from cancha");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private Cancha registrar(String nombre) {
		return registrarCancha.ejecutar(
				new DatosRegistroCancha(escenarioId, F5, nombre, new BigDecimal("80000.5")), propietario);
	}

	@Test
	void ca1QuedaGuardadaDisponibleConSuTipoYPrecio() {
		Cancha cancha = registrar("Cancha 1");

		Map<String, Object> fila = jdbc.queryForMap("select * from cancha where id = ?", cancha.getId());
		assertThat(fila).containsEntry("escenario_id", escenarioId).containsEntry("tipo_cancha_id", F5)
				.containsEntry("nombre", "Cancha 1").containsEntry("estado", "DISPONIBLE");
		assertThat((BigDecimal) fila.get("precio_hora")).isEqualByComparingTo("80000.50");
	}

	@Test
	void ca1LaListaDelEscenarioTraeLaCanchaNueva() {
		registrar("Cancha B");
		registrar("Cancha A");

		assertThat(listarCanchas.ejecutar(escenarioId, propietario)).extracting(Cancha::getNombre)
				.containsExactly("Cancha A", "Cancha B");
	}

	// CA5: la consulta del caso de uso compara sin mayúsculas
	@Test
	void ca5ElNombreRepetidoSeDetectaSinImportarMayusculas() {
		registrar("Cancha 1");

		assertThatThrownBy(() -> registrar("CANCHA 1")).isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje()).isEqualTo("ERR-CAN-03");
		assertThat(jdbc.queryForObject("select count(*) from cancha", Integer.class)).isEqualTo(1);
	}

	// CA5 con dos solicitudes al mismo tiempo: la segunda pasa la revisión del caso de uso, pero
	// el índice único de la V3 la rechaza al guardar
	@Test
	void ca5LaBaseRechazaElNombreRepetidoAunqueSeSalteLaRevision() {
		Cancha primera = registrar("Cancha 1");
		Cancha segunda = Cancha.reconstruir(UUID.randomUUID(), escenarioId, F5, "cancha 1", BigDecimal.TEN,
				EstadoCancha.DISPONIBLE);

		assertThatThrownBy(() -> repositorioCanchas.guardar(segunda)).isInstanceOf(ReglaNegocioException.class)
				.satisfies(e -> {
					ReglaNegocioException error = (ReglaNegocioException) e;
					assertThat(error.getCodigoMensaje()).isEqualTo("ERR-CAN-03");
					assertThat(error.getParametros()).containsExactly("cancha 1");
				});
		assertThat(jdbc.queryForList("select id from cancha", UUID.class)).containsExactly(primera.getId());
	}

	@Test
	void ca6UnTipoQueNoEstaEnElCatalogoNoSeGuarda() {
		DatosRegistroCancha datos = new DatosRegistroCancha(escenarioId, UUID.randomUUID(), "Cancha 1",
				BigDecimal.TEN);

		assertThatThrownBy(() -> registrarCancha.ejecutar(datos, propietario))
				.isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje()).isEqualTo("ERR-CAN-04");
	}

	@Test
	void ca10ListaLosTiposCargadosPorLaV2() {
		assertThat(listarTipos.ejecutar()).extracting(TipoCancha::getCodigo, TipoCancha::getNombre,
				TipoCancha::getCapacidadJugadores)
				.containsExactly(
						tuple("F11", "Fútbol 11", 22),
						tuple("F5", "Fútbol 5", 10),
						tuple("F7", "Fútbol 7", 14));
	}

}
