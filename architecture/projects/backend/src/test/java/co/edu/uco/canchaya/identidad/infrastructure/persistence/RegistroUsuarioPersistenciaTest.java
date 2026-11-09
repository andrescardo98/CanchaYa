package co.edu.uco.canchaya.identidad.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.identidad.application.port.in.RegistrarUsuario;
import co.edu.uco.canchaya.identidad.application.port.in.RegistrarUsuario.DatosRegistro;
import co.edu.uco.canchaya.identidad.application.port.out.RepositorioUsuarios;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RegistroUsuarioPersistenciaTest {

	private static final String CONTRASENA = "Clave2026";

	@Autowired
	private RegistrarUsuario registrarUsuario;

	@Autowired
	private RepositorioUsuarios repositorio;

	@Autowired
	private JdbcTemplate jdbc;

	@AfterEach
	void limpiar() {
		// el admin del sistema lo carga la V2, ese no se borra
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	@Test
	void ca1_guardaElUsuarioEnLaTablaConElCorreoEnMinuscula() {
		Usuario registrado = registrarUsuario.ejecutar(datos("Ana@Correo.COM", "1000000001"));

		Map<String, Object> fila = jdbc.queryForMap("select * from usuario where id = ?", registrado.getId());
		assertThat(fila.get("correo_electronico")).isEqualTo("ana@correo.com");
		assertThat(fila.get("nombre")).isEqualTo("Ana Pérez");
		assertThat(fila.get("tipo_documento")).isEqualTo("CC");
		assertThat(fila.get("numero_documento")).isEqualTo("1000000001");
		assertThat(fila.get("rol")).isEqualTo("JUGADOR");
		assertThat(fila.get("fecha_registro")).isNotNull();
	}

	@Test
	void politica5_soloSeGuardaElHashBcryptNuncaLaContrasena() {
		Usuario registrado = registrarUsuario.ejecutar(datos("ana@correo.com", "1000000001"));

		String hash = jdbc.queryForObject("select contrasena_hash from usuario where id = ?", String.class,
				registrado.getId());
		assertThat(hash).hasSize(60).startsWith("$2").doesNotContain(CONTRASENA);
		assertThat(new BCryptPasswordEncoder().matches(CONTRASENA, hash)).isTrue();
	}

	@Test
	void politicas1y2y4_lasConsultasDeExistenciaVenLoQueSeGuardo() {
		Usuario registrado = registrarUsuario.ejecutar(datos("ana@correo.com", "1000000001"));

		assertThat(repositorio.existePorCorreo("ana@correo.com")).isTrue();
		assertThat(repositorio.existePorCorreo("otra@correo.com")).isFalse();
		assertThat(repositorio.existePorDocumento(TipoDocumento.CC, "1000000001")).isTrue();
		assertThat(repositorio.existePorDocumento(TipoDocumento.CE, "1000000001")).isFalse();
		assertThat(repositorio.existePorId(registrado.getId())).isTrue();
	}

	/** Simula un registro simultáneo: se guarda directamente, sin pasar por las validaciones previas. */
	@Test
	void ca3_correoRepetidoAlMismoTiempo_laBaseLoRechazaYSeTraduceAErrUsu01() {
		registrarUsuario.ejecutar(datos("ana@correo.com", "1000000001"));
		Usuario segundo = usuario("ana@correo.com", "1000000002");

		assertThatThrownBy(() -> repositorio.guardar(segundo))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("ERR-USU-01");
		assertThat(jdbc.queryForObject("select count(*) from usuario where rol <> 'ADMINISTRADOR_SISTEMA'", Integer.class)).isEqualTo(1);
	}

	@Test
	void ca4_documentoRepetidoAlMismoTiempo_laBaseLoRechazaYSeTraduceAErrUsu02() {
		registrarUsuario.ejecutar(datos("ana@correo.com", "1000000001"));
		Usuario segundo = usuario("luis@correo.com", "1000000001");

		assertThatThrownBy(() -> repositorio.guardar(segundo))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("ERR-USU-02");
		assertThat(jdbc.queryForObject("select count(*) from usuario where rol <> 'ADMINISTRADOR_SISTEMA'", Integer.class)).isEqualTo(1);
	}

	private static DatosRegistro datos(String correo, String documento) {
		return new DatosRegistro("Ana Pérez", correo, CONTRASENA, TipoDocumento.CC, documento,
				"3001234567", Rol.JUGADOR, false);
	}

	private static Usuario usuario(String correo, String documento) {
		return Usuario.registrar("Luis Gómez", correo, new BCryptPasswordEncoder().encode(CONTRASENA),
				TipoDocumento.CC, documento, "3007654321", Rol.JUGADOR, false, OffsetDateTime.now());
	}

}
