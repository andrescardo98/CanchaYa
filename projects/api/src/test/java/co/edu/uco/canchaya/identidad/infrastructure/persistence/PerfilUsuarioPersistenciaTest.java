package co.edu.uco.canchaya.identidad.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.identidad.application.port.in.ActualizarPerfil;
import co.edu.uco.canchaya.identidad.application.port.in.ActualizarPerfil.DatosPerfil;
import co.edu.uco.canchaya.identidad.application.port.in.RegistrarUsuario;
import co.edu.uco.canchaya.identidad.application.port.in.RegistrarUsuario.DatosRegistro;
import co.edu.uco.canchaya.identidad.application.port.out.RepositorioUsuarios;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU3 contra PostgreSQL real: lo que queda guardado al actualizar el perfil. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PerfilUsuarioPersistenciaTest {

	private static final String CONTRASENA_ACTUAL = "Clave2026";

	@Autowired
	private RegistrarUsuario registrarUsuario;

	@Autowired
	private ActualizarPerfil actualizarPerfil;

	@Autowired
	private RepositorioUsuarios repositorio;

	@Autowired
	private JdbcTemplate jdbc;

	private Usuario ana;
	private Usuario luis;

	@BeforeEach
	void preparar() {
		ana = registrarUsuario.ejecutar(new DatosRegistro("Ana Pérez", "ana@correo.com", CONTRASENA_ACTUAL,
				TipoDocumento.CC, "1000000001", "+573001234567", Rol.JUGADOR, false));
		luis = registrarUsuario.ejecutar(new DatosRegistro("Luis Gómez", "luis@correo.com", CONTRASENA_ACTUAL,
				TipoDocumento.CC, "1000000002", "+573107654321", Rol.ADMINISTRADOR_CANCHA, false));
	}

	@AfterEach
	void limpiar() {
		// el admin del sistema lo carga la V2, ese no se borra
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private Map<String, Object> filaDe(UUID id) {
		return jdbc.queryForMap("select * from usuario where id = ?", id);
	}

	private void actualizarAna(String correo, String nuevaContrasena) {
		actualizarPerfil.ejecutar(new Solicitante(ana.getId(), Rol.JUGADOR), new DatosPerfil(ana.getId(),
				"Ana María Pérez", correo, "+573109876543", nuevaContrasena, null, null, null));
	}

	@Test
	void ca1_guardaLosDatosNuevosYNoTocaElDocumentoNiElRol() {
		Map<String, Object> antes = filaDe(ana.getId());

		actualizarAna("Ana.Maria@Correo.COM", null);

		Map<String, Object> fila = filaDe(ana.getId());
		assertThat(fila.get("nombre")).isEqualTo("Ana María Pérez");
		assertThat(fila.get("correo_electronico")).isEqualTo("ana.maria@correo.com");
		assertThat(fila.get("numero_celular")).isEqualTo("+573109876543");
		// Política 11
		assertThat(fila.get("tipo_documento")).isEqualTo("CC");
		assertThat(fila.get("numero_documento")).isEqualTo("1000000001");
		assertThat(fila.get("rol")).isEqualTo("JUGADOR");
		assertThat(fila.get("fecha_registro")).isEqualTo(antes.get("fecha_registro"));
	}

	@Test
	void ca1_politica5_laNuevaContrasenaSeGuardaSoloComoHashBcrypt() {
		actualizarAna("ana@correo.com", "Nueva2026");

		String hash = (String) filaDe(ana.getId()).get("contrasena_hash");
		assertThat(hash).hasSize(60).startsWith("$2").doesNotContain("Nueva2026");
		assertThat(new BCryptPasswordEncoder().matches("Nueva2026", hash)).isTrue();
		assertThat(new BCryptPasswordEncoder().matches(CONTRASENA_ACTUAL, hash)).isFalse();
	}

	@Test
	void ca4_sinNuevaContrasenaElHashGuardadoNoCambia() {
		Object hashAntes = filaDe(ana.getId()).get("contrasena_hash");

		actualizarAna("ana@correo.com", "");

		Map<String, Object> fila = filaDe(ana.getId());
		assertThat(fila.get("contrasena_hash")).isEqualTo(hashAntes);
		assertThat(fila.get("nombre")).isEqualTo("Ana María Pérez");
	}

	@Test
	void ca3_correoDeOtroUsuario_noSeGuardaNada() {
		assertThatThrownBy(() -> actualizarAna("LUIS@correo.com", "Nueva2026"))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("ERR-USU-10");

		Map<String, Object> fila = filaDe(ana.getId());
		assertThat(fila.get("nombre")).isEqualTo("Ana Pérez");
		assertThat(fila.get("correo_electronico")).isEqualTo("ana@correo.com");
	}

	@Test
	void politica10_laConsultaExcluyeAlPropioUsuario() {
		assertThat(repositorio.existeOtroConCorreo("ana@correo.com", ana.getId())).isFalse();
		assertThat(repositorio.existeOtroConCorreo("ana@correo.com", luis.getId())).isTrue();
		assertThat(repositorio.existeOtroConCorreo("nadie@correo.com", ana.getId())).isFalse();
	}

	/**
	 * Simula que Luis cambia su correo al de Ana al mismo tiempo que ella lo revisa: se guarda
	 * directamente, sin pasar por la política 10. La restricción única de la base lo rechaza.
	 */
	@Test
	void ca3_correoRepetidoAlMismoTiempo_laBaseLoRechazaYSeTraduceAErrUsu10() {
		Usuario cambiado = repositorio.buscarPorId(luis.getId()).orElseThrow();
		cambiado.actualizarPerfil(new Solicitante(luis.getId(), Rol.ADMINISTRADOR_CANCHA), "Luis Gómez",
				"ana@correo.com", "+573107654321", null);

		assertThatThrownBy(() -> repositorio.actualizar(cambiado))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("ERR-USU-10");
		assertThat(filaDe(luis.getId()).get("correo_electronico")).isEqualTo("luis@correo.com");
	}

	@Test
	void politica8_buscarPorIdDevuelveElUsuarioGuardadoOVacio() {
		Usuario encontrado = repositorio.buscarPorId(ana.getId()).orElseThrow();

		assertThat(encontrado.getCorreoElectronico()).isEqualTo("ana@correo.com");
		assertThat(encontrado.getTipoDocumento()).isEqualTo(TipoDocumento.CC);
		assertThat(encontrado.getRol()).isEqualTo(Rol.JUGADOR);
		assertThat(repositorio.buscarPorId(UUID.randomUUID())).isEmpty();
	}

}
