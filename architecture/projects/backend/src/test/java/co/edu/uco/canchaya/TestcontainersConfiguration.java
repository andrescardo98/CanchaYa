package co.edu.uco.canchaya;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Levanta una base PostgreSQL real con PostGIS en Docker para las pruebas.
 * Es la misma imagen del docker-compose.yml local, así las pruebas revisan
 * las migraciones y las restricciones de la base de verdad (D29).
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	static final DockerImageName IMAGEN_POSTGIS =
			DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres");

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(IMAGEN_POSTGIS);
	}

}
