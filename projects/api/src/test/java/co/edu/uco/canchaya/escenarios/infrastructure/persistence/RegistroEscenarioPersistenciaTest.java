package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario.DatosRegistroEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario.FotoAdjunta;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU5 con la base real (PostGIS) y las fotos en una carpeta temporal. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RegistroEscenarioPersistenciaTest {

	@TempDir
	static Path carpetaFotos;

	@DynamicPropertySource
	static void fotosEnCarpetaTemporal(DynamicPropertyRegistry registro) {
		registro.add("canchaya.fotos.almacenamiento", () -> "local");
		registro.add("canchaya.fotos.local.carpeta", carpetaFotos::toString);
	}

	@Autowired
	private RegistrarEscenario registrarEscenario;

	@Autowired
	private JdbcTemplate jdbc;

	private Solicitante propietario;
	private UUID rionegro;

	@BeforeEach
	void preparar() {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Dueño', 'dueno@correo.com', ?, 'CC', '1000000002', '+573001234567',
				  'ADMINISTRADOR_CANCHA', now())""", id, "x".repeat(60));
		propietario = new Solicitante(id, Rol.ADMINISTRADOR_CANCHA);
		rionegro = jdbc.queryForObject("select id from ciudad where nombre = 'Rionegro'", UUID.class);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from foto_escenario");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private DatosRegistroEscenario datos(UUID ciudad, List<FotoAdjunta> fotos) {
		return new DatosRegistroEscenario("Cancha La 80", null, "Calle 80 # 45-10", ciudad,
				new BigDecimal("6.1551234"), new BigDecimal("-75.3737"), "+573001234567", null, fotos);
	}

	private static FotoAdjunta jpg(String nombre) {
		return new FotoAdjunta(nombre, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3 });
	}

	@Test
	void ca1GuardaElEscenarioInactivoConSuPuntoYSusFotosEnOrden() throws Exception {
		Escenario escenario = registrarEscenario.ejecutar(datos(rionegro, List.of(jpg("a.jpg"), jpg("b.jpg"))),
				propietario);

		Map<String, Object> fila = jdbc.queryForMap("""
				select estado, administrador_id, latitud, descripcion,
				  st_y(ubicacion::geometry) as punto_latitud, st_x(ubicacion::geometry) as punto_longitud
				from escenario where id = ?""", escenario.getId());
		assertThat(fila.get("estado")).isEqualTo("INACTIVO");
		assertThat(fila.get("administrador_id")).isEqualTo(propietario.usuarioId());
		assertThat((BigDecimal) fila.get("latitud")).isEqualByComparingTo("6.155123");
		assertThat(fila.get("descripcion")).isNull();
		// La base calcula el punto geográfico sola (restricción de diseño de la HU)
		assertThat((Double) fila.get("punto_latitud")).isEqualTo(6.155123);
		assertThat((Double) fila.get("punto_longitud")).isEqualTo(-75.3737);

		List<String> urls = jdbc.queryForList(
				"select url from foto_escenario where escenario_id = ? order by orden", String.class, escenario.getId());
		assertThat(urls).containsExactlyElementsOf(escenario.getFotos()).hasSize(2);
		for (String url : urls) {
			assertThat(Files.exists(carpetaFotos.resolve(url.substring(url.lastIndexOf('/') + 1)))).isTrue();
		}
	}

	@Test
	void ca4RechazaUnaCiudadQueNoEstaEnElCatalogoSinGuardarNiSubirFotos() throws Exception {
		long fotosAntes;
		try (var archivos = Files.list(carpetaFotos)) {
			fotosAntes = archivos.count();
		}

		assertThatThrownBy(() -> registrarEscenario.ejecutar(datos(UUID.randomUUID(), List.of(jpg("a.jpg"))),
				propietario))
				.isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje())
				.isEqualTo("ERR-ESC-02");

		assertThat(jdbc.queryForObject("select count(*) from escenario", Integer.class)).isZero();
		try (var archivos = Files.list(carpetaFotos)) {
			assertThat(archivos.count()).isEqualTo(fotosAntes);
		}
	}

}
