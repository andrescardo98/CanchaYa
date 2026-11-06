package co.edu.uco.canchaya;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prueba que la aplicación arranca contra una base real y que Flyway aplicó la migración V1.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CanchayaBackendApplicationTests {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void arrancaYAplicaLaMigracionV1() {
		Integer aplicadas = jdbc.queryForObject(
				"select count(*) from flyway_schema_history where version = '1' and success", Integer.class);
		assertThat(aplicadas).isEqualTo(1);
	}

	@Test
	void activaLasExtensionesQueNecesitanLaBusquedaYLasReservas() {
		List<String> extensiones = jdbc.queryForList("select extname from pg_extension", String.class);
		// postgis: búsqueda por radio (AC-05.1). btree_gist: regla de reservas que no se cruzan (D29).
		assertThat(extensiones).contains("postgis", "btree_gist");
	}

	@Test
	void creaLasTablasPrincipales() {
		List<String> tablas = jdbc.queryForList(
				"select table_name from information_schema.tables where table_schema = 'public'", String.class);
		assertThat(tablas).contains("usuario", "escenario", "cancha", "reserva");
	}

	@Test
	void usaLaZonaHorariaDeColombia() {
		// D27: la JVM de las pruebas se configura con America/Bogota en el pom.xml (surefire).
		assertThat(TimeZone.getDefault().getID()).isEqualTo(CanchayaBackendApplication.ZONA_HORARIA);
	}

}
