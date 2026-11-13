package co.edu.uco.canchaya.identidad.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.identidad.application.port.in.IniciarSesion.Credenciales;
import co.edu.uco.canchaya.identidad.application.port.in.IniciarSesion.Sesion;
import co.edu.uco.canchaya.identidad.application.port.out.ContadorIntentosFallidos;
import co.edu.uco.canchaya.identidad.application.port.out.EmisorToken;
import co.edu.uco.canchaya.identidad.application.port.out.RepositorioUsuarios;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

class IniciarSesionCasoUsoTest {

	private static final String ORIGEN = "203.0.113.7";
	private static final Instant VENCE = Instant.parse("2026-11-12T15:00:00Z");

	private final Usuario ana = Usuario.reconstruir(UUID.randomUUID(), "Ana Pérez", "ana@correo.com",
			"hash-de-Clave2026", TipoDocumento.CC, "1036123456", "+573001234567", Rol.ADMINISTRADOR_CANCHA,
			OffsetDateTime.parse("2026-11-06T09:00:00-05:00"));

	private CifradorFalso cifrador;
	private ContadorFalso contador;
	private IniciarSesionCasoUso casoUso;

	@BeforeEach
	void preparar() {
		RepositorioUsuarios repositorio = new RepositorioDeUnUsuario(ana);
		cifrador = new CifradorFalso();
		contador = new ContadorFalso();
		EmisorToken emisor = usuario -> new EmisorToken.TokenEmitido("token-de-" + usuario.getId(), VENCE);
		casoUso = new IniciarSesionCasoUso(repositorio, cifrador, emisor, contador, 15);
	}

	@Test
	void ca1ConCredencialesCorrectasEntregaElTokenYElRol() {
		// El correo no distingue mayúsculas ni espacios en los extremos
		Sesion sesion = casoUso.ejecutar(new Credenciales(" Ana@Correo.com ", "Clave2026"), ORIGEN);

		assertThat(sesion.token()).isEqualTo("token-de-" + ana.getId());
		assertThat(sesion.rol()).isEqualTo(Rol.ADMINISTRADOR_CANCHA);
		assertThat(sesion.venceEn()).isEqualTo(VENCE);
		assertThat(contador.fallos).isEmpty();
	}

	@Test
	void ca3ConUnCorreoNoRegistradoRespondeErrUsu06YCuentaElFallo() {
		assertThatThrownBy(() -> casoUso.ejecutar(new Credenciales("nadie@correo.com", "Clave2026"), ORIGEN))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("ERR-USU-06");

		assertThat(contador.fallos).containsExactly("nadie@correo.com|" + ORIGEN);
		// ESC-SEG-03: igual se compara la contraseña, para tardar lo mismo que con un correo registrado
		assertThat(cifrador.comparaciones).isEqualTo(1);
	}

	@Test
	void ca4ConUnaContrasenaQueNoCoincideRespondeErrUsu07YCuentaElFallo() {
		assertThatThrownBy(() -> casoUso.ejecutar(new Credenciales("ana@correo.com", "Otra2026"), ORIGEN))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("ERR-USU-07");

		assertThat(contador.fallos).containsExactly("ana@correo.com|" + ORIGEN);
	}

	@Test
	void ca7ConLaCuentaBloqueadaRechazaAunqueLaContrasenaSeaCorrecta() {
		contador.bloqueados.add("ana@correo.com");

		assertThatThrownBy(() -> casoUso.ejecutar(new Credenciales("ana@correo.com", "Clave2026"), ORIGEN))
				.isInstanceOfSatisfying(ReglaNegocioException.class, e -> {
					assertThat(e.getCodigoMensaje()).isEqualTo("ERR-USU-16");
					assertThat(e.getParametros()).containsExactly(15); // PAR-USU-12
				});
		// Bloqueado no se revisa la contraseña ni se cuenta otro fallo
		assertThat(cifrador.comparaciones).isZero();
		assertThat(contador.fallos).isEmpty();
	}

	@Test
	void ca7ConElOrigenBloqueadoRechazaTambienOtrasCuentas() {
		contador.bloqueados.add(ORIGEN);

		assertThatThrownBy(() -> casoUso.ejecutar(new Credenciales("ana@correo.com", "Clave2026"), ORIGEN))
				.hasMessage("ERR-USU-16");
	}

	@Test
	void ca7ConUnCorreoNoRegistradoYBloqueadoRespondeElMismoErrUsu16() {
		contador.bloqueados.add("nadie@correo.com");

		assertThatThrownBy(() -> casoUso.ejecutar(new Credenciales("nadie@correo.com", "Clave2026"), ORIGEN))
				.hasMessage("ERR-USU-16");
	}

	/** El conteo real por tiempo se prueba en ContadorIntentosEnMemoriaTest. */
	private static final class ContadorFalso implements ContadorIntentosFallidos {

		final Set<String> bloqueados = new HashSet<>();
		final List<String> fallos = new ArrayList<>();

		@Override
		public boolean estaBloqueado(String correoElectronico, String origen) {
			return bloqueados.contains(correoElectronico) || bloqueados.contains(origen);
		}

		@Override
		public void registrarFallo(String correoElectronico, String origen) {
			fallos.add(correoElectronico + "|" + origen);
		}

	}

	private record RepositorioDeUnUsuario(Usuario usuario) implements RepositorioUsuarios {

		@Override
		public Optional<Usuario> buscarPorCorreo(String correoElectronico) {
			return usuario.getCorreoElectronico().equals(correoElectronico) ? Optional.of(usuario) : Optional.empty();
		}

		@Override
		public boolean existePorCorreo(String correoElectronico) {
			return buscarPorCorreo(correoElectronico).isPresent();
		}

		@Override
		public boolean existePorDocumento(TipoDocumento tipoDocumento, String numeroDocumento) {
			return false;
		}

		@Override
		public boolean existePorId(UUID id) {
			return false;
		}

		@Override
		public void guardar(Usuario usuario) {
			throw new UnsupportedOperationException();
		}

	}

}
