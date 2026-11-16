package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios.ResumenEscenario;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU13: lo que el caso de uso le pide a la base de Escenarios, con PostGIS real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CambioEstadoEscenarioPersistenciaTest {

	private static final String F5 = "27d013e3-f5b3-5378-a7df-7c80429554fe";

	@Autowired
	private RepositorioEscenarios repositorio;

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
				values (?, 'Ana Pérez', 'ana@correo.com', ?, 'CC', '1000000031', '+573001234567',
				  'ADMINISTRADOR_CANCHA', now())""", propietario, "x".repeat(60));
		rionegro = jdbc.queryForObject("select id from ciudad where nombre = 'Rionegro'", UUID.class);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from cancha");
		jdbc.update("delete from foto_escenario");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private UUID escenario(String nombre, String estado) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, ?, 'Calle 80 # 45-10', ?, 6.155123, -75.3737, '+573001234567', ?, ?)""",
				id, nombre, rionegro, propietario, estado);
		return id;
	}

	private void foto(UUID escenario, String url, int orden) {
		jdbc.update("insert into foto_escenario (id, escenario_id, url, orden) values (?, ?, ?, ?)",
				UUID.randomUUID(), escenario, url, orden);
	}

	private UUID cancha(UUID escenario, String nombre) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora) values (?, ?, ?, ?, 80000)",
				id, escenario, UUID.fromString(F5), nombre);
		return id;
	}

	@Test
	void buscaElEscenarioConSusFotosEnOrden() {
		UUID id = escenario("Cancha La 80", "INACTIVO");
		foto(id, "https://fotos.test/2.jpg", 2);
		foto(id, "https://fotos.test/1.jpg", 1);

		Escenario escenario = repositorio.buscarPorId(id).orElseThrow();

		assertThat(escenario.getEstado()).isEqualTo(EstadoEscenario.INACTIVO);
		assertThat(escenario.getAdministradorId()).isEqualTo(propietario);
		assertThat(escenario.getFotos()).containsExactly("https://fotos.test/1.jpg", "https://fotos.test/2.jpg");
	}

	@Test
	void ca4UnIdentificadorQueNoExisteNoTraeNada() {
		assertThat(repositorio.buscarPorId(UUID.randomUUID())).isEmpty();
	}

	@Test
	void ca2GuardaSoloElNuevoEstadoYConservaLasFotos() {
		UUID id = escenario("Cancha La 80", "INACTIVO");
		foto(id, "https://fotos.test/1.jpg", 1);
		Escenario escenario = repositorio.buscarPorId(id).orElseThrow();
		escenario.cambiarEstado(EstadoEscenario.ACTIVO, new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_SISTEMA),
				false);

		repositorio.actualizarEstado(escenario);

		assertThat(jdbc.queryForObject("select estado from escenario where id = ?", String.class, id))
				.isEqualTo("ACTIVO");
		assertThat(jdbc.queryForObject("select count(*) from foto_escenario where escenario_id = ?", Integer.class, id))
				.isEqualTo(1);
	}

	@Test
	void ca1ListaPorEstadoOrdenadoPorNombre() {
		escenario("Golazo", "INACTIVO");
		escenario("Cancha La 80", "INACTIVO");
		escenario("El Templo", "ACTIVO");

		assertThat(repositorio.listar(EstadoEscenario.INACTIVO))
				.extracting(ResumenEscenario::nombre).containsExactly("Cancha La 80", "Golazo");
		assertThat(repositorio.listar(null))
				.extracting(ResumenEscenario::nombre).containsExactly("Cancha La 80", "El Templo", "Golazo");
		assertThat(repositorio.listar(EstadoEscenario.ACTIVO)).singleElement().satisfies(resumen -> {
			assertThat(resumen.ciudadId()).isEqualTo(rionegro);
			assertThat(resumen.administradorId()).isEqualTo(propietario);
			assertThat(resumen.estado()).isEqualTo(EstadoEscenario.ACTIVO);
		});
	}

	@Test
	void ca7TraeSoloLasCanchasDelEscenario() {
		UUID id = escenario("Cancha La 80", "ACTIVO");
		UUID otro = escenario("Golazo", "ACTIVO");
		UUID cancha1 = cancha(id, "Cancha 1");
		UUID cancha2 = cancha(id, "Cancha 2");
		cancha(otro, "Cancha 1");

		assertThat(repositorio.idsCanchas(id)).containsExactlyInAnyOrder(cancha1, cancha2);
		assertThat(repositorio.idsCanchas(UUID.randomUUID())).isEqualTo(List.of());
	}

}
