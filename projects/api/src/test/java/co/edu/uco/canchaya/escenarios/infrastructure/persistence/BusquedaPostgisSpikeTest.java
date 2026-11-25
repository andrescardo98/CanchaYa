package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
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

/**
 * HU18, T1 (spike): antes de escribir la búsqueda se comprueba con PostGIS real que la columna
 * generada escenario.ubicacion y las funciones ST_DWithin y ST_Distance miden en metros sobre la
 * Tierra, como supone la especificación (restricción de diseño de HU18, D11, D26).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BusquedaPostgisSpikeTest {

	/** Parque principal de Rionegro: el punto del jugador en estas pruebas. */
	private static final double LAT_RIONEGRO = 6.155100;
	private static final double LNG_RIONEGRO = -75.373700;

	/** Parque Berrío de Medellín. */
	private static final double LAT_MEDELLIN = 6.250000;
	private static final double LNG_MEDELLIN = -75.568300;

	/** Un grado de latitud mide unos 111 km: estos dos puntos quedan a unos 945 m y 1056 m al norte. */
	private static final double GRADOS_945_M = 0.0085;
	private static final double GRADOS_1056_M = 0.0095;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID propietario;
	private UUID rionegro;

	@BeforeEach
	void preparar() {
		propietario = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Ana Pérez', 'ana@correo.com', ?, 'CC', '1000000181', '+573001234567',
				  'ADMINISTRADOR_CANCHA', now())""", propietario, "x".repeat(60));
		rionegro = jdbc.queryForObject("select id from ciudad where nombre = 'Rionegro'", UUID.class);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private UUID escenario(String nombre, double latitud, double longitud) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, ?, 'Calle 1 # 2-3', ?, ?, ?, '+573001234567', ?, 'ACTIVO')""",
				id, nombre, rionegro, BigDecimal.valueOf(latitud), BigDecimal.valueOf(longitud), propietario);
		return id;
	}

	/** Los nombres de los escenarios a menos de radioMetros del punto, del más cercano al más lejano. */
	private List<String> dentroDelRadio(double latitud, double longitud, double radioMetros) {
		return jdbc.queryForList("""
				select nombre from escenario
				where ST_DWithin(ubicacion, ST_MakePoint(?, ?)::geography, ?)
				order by ST_Distance(ubicacion, ST_MakePoint(?, ?)::geography), nombre""",
				String.class, longitud, latitud, radioMetros, longitud, latitud);
	}

	private double distanciaMetros(UUID escenarioId, double latitud, double longitud) {
		return jdbc.queryForObject("""
				select ST_Distance(ubicacion, ST_MakePoint(?, ?)::geography) from escenario where id = ?""",
				Double.class, longitud, latitud, escenarioId);
	}

	/** Fórmula del semiverseno sobre una esfera de 6371 km: el cálculo «aparte» contra el que se compara. */
	private static double semiversenoMetros(double lat1, double lng1, double lat2, double lng2) {
		double radioTierra = 6_371_008.8;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.pow(Math.sin(dLat / 2), 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
		return 2 * radioTierra * Math.asin(Math.sqrt(a));
	}

	// Punto 3 del spike: la base llena ubicacion sola con latitud y longitud, y la recalcula al cambiarlas
	@Test
	void spike_columnaGenerada_seLlenaYSeRecalcula() {
		UUID id = escenario("La 70", LAT_RIONEGRO, LNG_RIONEGRO);

		assertThat(jdbc.queryForObject("select ST_Y(ubicacion::geometry) from escenario where id = ?", Double.class, id))
				.isCloseTo(LAT_RIONEGRO, within(1e-9));
		assertThat(jdbc.queryForObject("select ST_X(ubicacion::geometry) from escenario where id = ?", Double.class, id))
				.isCloseTo(LNG_RIONEGRO, within(1e-9));

		jdbc.update("update escenario set latitud = ?, longitud = ? where id = ?",
				BigDecimal.valueOf(LAT_MEDELLIN), BigDecimal.valueOf(LNG_MEDELLIN), id);

		assertThat(jdbc.queryForObject("select ST_Y(ubicacion::geometry) from escenario where id = ?", Double.class, id))
				.isCloseTo(LAT_MEDELLIN, within(1e-9));
	}

	// Punto 2 del spike: Rionegro – Medellín da unos 24 km, igual al cálculo aparte (diferencia menor al 1 %,
	// porque PostGIS mide sobre el elipsoide WGS 84 y el semiverseno sobre una esfera)
	@Test
	void spike_distancia_enMetrosSobreLaTierra() {
		UUID medellin = escenario("Medellín", LAT_MEDELLIN, LNG_MEDELLIN);

		double esperada = semiversenoMetros(LAT_RIONEGRO, LNG_RIONEGRO, LAT_MEDELLIN, LNG_MEDELLIN);
		double medida = distanciaMetros(medellin, LAT_RIONEGRO, LNG_RIONEGRO);

		assertThat(esperada).isBetween(23_000.0, 25_000.0);
		assertThat(medida).isCloseTo(esperada, within(esperada * 0.01));
	}

	// Puntos 1 y 2 del spike: con radio de 1 km sale el de 945 m y no el de 1056 m (borde del radio, PAR-ESC-06)
	@Test
	void spike_radio_respetaElBordeEnMetros() {
		escenario("A 945 m", LAT_RIONEGRO + GRADOS_945_M, LNG_RIONEGRO);
		escenario("A 1056 m", LAT_RIONEGRO + GRADOS_1056_M, LNG_RIONEGRO);

		assertThat(dentroDelRadio(LAT_RIONEGRO, LNG_RIONEGRO, 1_000)).containsExactly("A 945 m");
		assertThat(dentroDelRadio(LAT_RIONEGRO, LNG_RIONEGRO, 1_100)).containsExactly("A 945 m", "A 1056 m");
	}

	// Punto 1 del spike: el radio es en metros y no en grados. Si fuera en grados, 30000 abarcaría toda la
	// Tierra y saldría también el escenario a un grado (unos 111 km)
	@Test
	void spike_radio_noEsEnGrados() {
		escenario("Cerca", LAT_RIONEGRO + GRADOS_945_M, LNG_RIONEGRO);
		escenario("Medellín", LAT_MEDELLIN, LNG_MEDELLIN);
		escenario("A un grado", LAT_RIONEGRO + 1, LNG_RIONEGRO);

		assertThat(dentroDelRadio(LAT_RIONEGRO, LNG_RIONEGRO, 30_000)).containsExactly("Cerca", "Medellín");
	}

}
