package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU8: lo que el caso de uso le pide a la base al modificar un escenario, con PostGIS real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ModificacionEscenarioPersistenciaTest {

	private static final String URL_1 = "https://fotos.test/1.jpg";
	private static final String URL_2 = "https://fotos.test/2.jpg";
	private static final String URL_3 = "https://fotos.test/3.jpg";
	private static final String URL_NUEVA = "https://fotos.test/nueva.jpg";

	@Autowired
	private RepositorioEscenarios repositorio;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID propietario;
	private UUID rionegro;
	private UUID medellin;

	@BeforeEach
	void preparar() {
		propietario = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Ana Pérez', 'ana@correo.com', ?, 'CC', '1000000081', '+573001234567',
				  'ADMINISTRADOR_CANCHA', now())""", propietario, "x".repeat(60));
		rionegro = jdbc.queryForObject("select id from ciudad where nombre = 'Rionegro'", UUID.class);
		medellin = jdbc.queryForObject("select id from ciudad where nombre = 'Medellín'", UUID.class);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from foto_escenario");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private UUID escenarioConFotos(String estado, String... urls) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, descripcion, direccion, ciudad_id, latitud, longitud,
				  telefono_contacto, correo_contacto, administrador_id, estado)
				values (?, 'Cancha La 80', 'Grama sintética', 'Calle 80 # 45-10', ?, 6.155123, -75.3737,
				  '+573001234567', 'contacto@la80.com', ?, ?)""", id, rionegro, propietario, estado);
		for (int i = 0; i < urls.length; i++) {
			jdbc.update("insert into foto_escenario (id, escenario_id, url, orden) values (?, ?, ?, ?)",
					UUID.randomUUID(), id, urls[i], i + 1);
		}
		return id;
	}

	private List<String> fotosGuardadas(UUID id) {
		return jdbc.queryForList("select url from foto_escenario where escenario_id = ? order by orden",
				String.class, id);
	}

	private void modificar(UUID id, List<String> fotos) {
		Escenario escenario = repositorio.buscarPorId(id).orElseThrow();
		escenario.modificar("Cancha El Templo", null, "Carrera 70 # 30-20", medellin, new BigDecimal("6.244203"),
				new BigDecimal("-75.581215"), "+573109876543", null, fotos,
				new Solicitante(propietario, Rol.ADMINISTRADOR_CANCHA), 5);
		repositorio.actualizar(escenario);
	}

	@Test
	void ca1GuardaLosDatosNuevosYConservaElPropietarioYElEstado() {
		UUID id = escenarioConFotos("ACTIVO", URL_1);

		modificar(id, List.of(URL_1));

		Map<String, Object> fila = jdbc.queryForMap("""
				select nombre, descripcion, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  correo_contacto, administrador_id, estado,
				  st_y(ubicacion::geometry) as punto_latitud, st_x(ubicacion::geometry) as punto_longitud
				from escenario where id = ?""", id);
		assertThat(fila.get("nombre")).isEqualTo("Cancha El Templo");
		assertThat(fila.get("descripcion")).isNull();
		assertThat(fila.get("direccion")).isEqualTo("Carrera 70 # 30-20");
		assertThat(fila.get("ciudad_id")).isEqualTo(medellin);
		assertThat((BigDecimal) fila.get("latitud")).isEqualByComparingTo("6.244203");
		assertThat((BigDecimal) fila.get("longitud")).isEqualByComparingTo("-75.581215");
		assertThat(fila.get("telefono_contacto")).isEqualTo("+573109876543");
		assertThat(fila.get("correo_contacto")).isNull();
		assertThat(fila.get("administrador_id")).isEqualTo(propietario);
		assertThat(fila.get("estado")).isEqualTo("ACTIVO");
		// La base vuelve a calcular el punto geográfico con la nueva ubicación
		assertThat((Double) fila.get("punto_latitud")).isEqualTo(6.244203);
		assertThat((Double) fila.get("punto_longitud")).isEqualTo(-75.581215);
		assertThat(fotosGuardadas(id)).containsExactly(URL_1);
	}

	@Test
	void ca1ReemplazaLasFotosEnElNuevoOrdenSinChocarLasPosiciones() {
		// La foto 3 pasa a la posición 1 y la 1 a la 2: sin borrar primero, chocaría uq_foto_escenario_orden
		UUID id = escenarioConFotos("INACTIVO", URL_1, URL_2, URL_3);

		modificar(id, List.of(URL_3, URL_1, URL_NUEVA));

		assertThat(fotosGuardadas(id)).containsExactly(URL_3, URL_1, URL_NUEVA);
		assertThat(repositorio.buscarPorId(id).orElseThrow().getFotos()).containsExactly(URL_3, URL_1, URL_NUEVA);
	}

	@Test
	void ca1QuitaTodasLasFotos() {
		UUID id = escenarioConFotos("INACTIVO", URL_1, URL_2);

		modificar(id, List.of());

		assertThat(fotosGuardadas(id)).isEmpty();
	}

}
