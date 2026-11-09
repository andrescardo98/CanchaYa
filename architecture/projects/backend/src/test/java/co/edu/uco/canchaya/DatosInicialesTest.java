package co.edu.uco.canchaya;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prueba que la migración V2 carga los datos que necesitan las HU del Sprint 1:
 * ciudades para el escenario (HU5), tipos de cancha (HU6) y el administrador del sistema (HU13).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DatosInicialesTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void cargaAntioquiaConSus125Municipios() {
		// D16, RNG-07: el ámbito geográfico es Antioquia (código DANE 05).
		Integer ciudades = jdbc.queryForObject("""
				select count(*) from ciudad c join departamento d on d.id = c.departamento_id
				where d.codigo = '05' and d.nombre = 'Antioquia'""", Integer.class);
		assertThat(ciudades).isEqualTo(125);
	}

	@Test
	void incluyeLasCiudadesDeValidacion() {
		// Los administradores que validan el proyecto están en Rionegro y Marinilla.
		List<String> nombres = jdbc.queryForList(
				"select nombre from ciudad where codigo in ('05615', '05440') order by nombre", String.class);
		assertThat(nombres).containsExactly("Marinilla", "Rionegro");
	}

	@Test
	void cargaLosTiposDeCanchaF5F7YF11() {
		// D04: en el Release 1 el catálogo es de solo lectura.
		List<Map<String, Object>> tipos = jdbc.queryForList(
				"select codigo, capacidad_jugadores from tipo_cancha order by capacidad_jugadores");
		assertThat(tipos).extracting(t -> t.get("codigo")).containsExactly("F5", "F7", "F11");
		assertThat(tipos).extracting(t -> ((Number) t.get("capacidad_jugadores")).intValue())
				.containsExactly(10, 14, 22);
	}

	@Test
	void creaUnSoloAdministradorDelSistemaConSuContrasenaEnHash() {
		// RN-16: el administrador del sistema no se registra por la aplicación.
		List<Map<String, Object>> admins = jdbc.queryForList(
				"select correo_electronico, contrasena_hash from usuario where rol = 'ADMINISTRADOR_SISTEMA'");
		assertThat(admins).hasSize(1);
		assertThat((String) admins.get(0).get("correo_electronico")).isEqualTo("admin@canchaya.local");
		// Política 5 de Usuario: solo se guarda el hash bcrypt (60 caracteres que empiezan por $2).
		assertThat((String) admins.get(0).get("contrasena_hash")).hasSize(60).startsWith("$2");
	}

}
