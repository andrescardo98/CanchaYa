package co.edu.uco.canchaya.identidad.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.identidad.application.port.in.RegistrarUsuario.DatosRegistro;
import co.edu.uco.canchaya.identidad.application.port.out.RepositorioUsuarios;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

class RegistrarUsuarioCasoUsoTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	private static final Instant INSTANTE = Instant.parse("2026-11-06T14:00:00Z"); // 9:00 a. m. en Bogotá

	private RepositorioEnMemoria repositorio;
	private RegistrarUsuarioCasoUso casoUso;

	@BeforeEach
	void preparar() {
		repositorio = new RepositorioEnMemoria();
		casoUso = new RegistrarUsuarioCasoUso(repositorio, new CifradorFalso(), Clock.fixed(INSTANTE, BOGOTA), 8);
	}

	private static DatosRegistro datos(String correo, TipoDocumento tipo, String numero) {
		return new DatosRegistro("Ana Pérez", correo, "clave2026", tipo, numero, "+573001234567",
				Rol.JUGADOR, false);
	}

	@Test
	void ca1GuardaElUsuarioConSuFechaDeRegistroYElHash() {
		Usuario usuario = casoUso.ejecutar(datos("Ana@Correo.com", TipoDocumento.CC, "1036123456"));

		assertThat(repositorio.guardados).containsExactly(usuario);
		assertThat(usuario.getFechaRegistro()).isEqualTo(OffsetDateTime.ofInstant(INSTANTE, BOGOTA));
		// Política 5
		assertThat(usuario.getContrasenaHash()).isEqualTo("hash-de-clave2026");
		assertThat(usuario.getCorreoElectronico()).isEqualTo("ana@correo.com");
	}

	@Test
	void ca3RechazaUnCorreoYaRegistradoSinDistinguirMayusculas() {
		casoUso.ejecutar(datos("ana@correo.com", TipoDocumento.CC, "1036123456"));

		assertThatThrownBy(() -> casoUso.ejecutar(datos("ANA@Correo.COM", TipoDocumento.CC, "98765432")))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("ERR-USU-01");
		assertThat(repositorio.guardados).hasSize(1);
	}

	@Test
	void ca4RechazaUnTipoYNumeroDeDocumentoYaRegistrados() {
		casoUso.ejecutar(datos("ana@correo.com", TipoDocumento.CC, "1036123456"));

		assertThatThrownBy(() -> casoUso.ejecutar(datos("otra@correo.com", TipoDocumento.CC, "1036123456")))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("ERR-USU-02");
		assertThat(repositorio.guardados).hasSize(1);
	}

	@Test
	void ca4AceptaElMismoNumeroConOtroTipoDeDocumento() {
		casoUso.ejecutar(datos("ana@correo.com", TipoDocumento.CC, "1036123456"));
		casoUso.ejecutar(datos("otra@correo.com", TipoDocumento.CE, "1036123456"));
		assertThat(repositorio.guardados).hasSize(2);
	}

	@Test
	void ca9RechazaUnaContrasenaDebilAntesDeCifrarla() {
		DatosRegistro debil = new DatosRegistro("Ana Pérez", "ana@correo.com", "corta1", TipoDocumento.CC,
				"1036123456", "+573001234567", Rol.JUGADOR, false);

		assertThatThrownBy(() -> casoUso.ejecutar(debil)).hasMessage("ERR-USU-12");
		assertThat(repositorio.guardados).isEmpty();
	}

	@Test
	void politica4ReintentaConOtroIdentificadorSiYaExiste() {
		repositorio.idsQueChocan = 2; // los dos primeros identificadores "ya existen"

		Usuario usuario = casoUso.ejecutar(datos("ana@correo.com", TipoDocumento.CC, "1036123456"));

		assertThat(repositorio.idsConsultados).hasSize(3).doesNotHaveDuplicates();
		assertThat(usuario.getId()).isEqualTo(repositorio.idsConsultados.get(2));
	}

	private static final class RepositorioEnMemoria implements RepositorioUsuarios {

		final List<Usuario> guardados = new ArrayList<>();
		final List<UUID> idsConsultados = new ArrayList<>();
		final Set<String> documentos = new HashSet<>();
		int idsQueChocan;

		@Override
		public boolean existePorCorreo(String correoElectronico) {
			return guardados.stream().anyMatch(u -> u.getCorreoElectronico().equals(correoElectronico));
		}

		@Override
		public boolean existePorDocumento(TipoDocumento tipoDocumento, String numeroDocumento) {
			return documentos.contains(tipoDocumento + ":" + numeroDocumento);
		}

		@Override
		public boolean existePorId(UUID id) {
			idsConsultados.add(id);
			return idsQueChocan-- > 0;
		}

		@Override
		public Optional<Usuario> buscarPorCorreo(String correoElectronico) {
			return guardados.stream().filter(u -> u.getCorreoElectronico().equals(correoElectronico)).findFirst();
		}

		@Override
		public void guardar(Usuario usuario) {
			guardados.add(usuario);
			documentos.add(usuario.getTipoDocumento() + ":" + usuario.getNumeroDocumento());
		}

	}

}
