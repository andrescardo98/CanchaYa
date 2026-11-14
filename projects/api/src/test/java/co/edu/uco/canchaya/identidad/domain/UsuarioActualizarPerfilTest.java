package co.edu.uco.canchaya.identidad.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU3: responsabilidad «actualizarPerfil» de Usuario. */
class UsuarioActualizarPerfilTest {

	private static final String HASH_ACTUAL = "$2a$10$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz01234";
	private static final String HASH_NUEVO = "$2a$10$zyxwvutsrqponmlkjihgfezyxwvutsrqponmlkjihgfedcba98765";
	private static final OffsetDateTime REGISTRO = OffsetDateTime.now(ZoneId.of("America/Bogota"));

	private final UUID id = UUID.randomUUID();
	private final Usuario usuario = Usuario.reconstruir(id, "Ana Pérez", "ana@correo.com", HASH_ACTUAL,
			TipoDocumento.CC, "1036123456", "+573001234567", Rol.JUGADOR, REGISTRO);
	private final Solicitante elMismo = new Solicitante(id, Rol.JUGADOR);

	@Test
	@DisplayName("CA1: cambia nombre, correo, celular y contraseña")
	void ca1CambiaLosDatosDelPerfil() {
		usuario.actualizarPerfil(elMismo, "Ana María Pérez", "ana.maria@correo.com", "+573109876543", HASH_NUEVO);

		assertThat(usuario.getNombre()).isEqualTo("Ana María Pérez");
		assertThat(usuario.getCorreoElectronico()).isEqualTo("ana.maria@correo.com");
		assertThat(usuario.getNumeroCelular()).isEqualTo("+573109876543");
		// Política 5: llega y se guarda ya cifrada
		assertThat(usuario.getContrasenaHash()).isEqualTo(HASH_NUEVO);
	}

	@Test
	@DisplayName("CA1: el correo se guarda en minúscula y sin espacios en los extremos")
	void ca1NormalizaElCorreoYElNombre() {
		usuario.actualizarPerfil(elMismo, "  Ana Pérez ", "  Ana.Maria@Correo.COM ", "+573001234567", null);

		assertThat(usuario.getCorreoElectronico()).isEqualTo("ana.maria@correo.com");
		assertThat(usuario.getNombre()).isEqualTo("Ana Pérez");
	}

	@Test
	@DisplayName("CA1 y política 11: no cambian el id, el documento, el rol ni la fecha de registro")
	void ca1ConservaLosDatosQueNoSeModifican() {
		usuario.actualizarPerfil(elMismo, "Ana María Pérez", "ana.maria@correo.com", "+573109876543", HASH_NUEVO);

		assertThat(usuario.getId()).isEqualTo(id);
		assertThat(usuario.getTipoDocumento()).isEqualTo(TipoDocumento.CC);
		assertThat(usuario.getNumeroDocumento()).isEqualTo("1036123456");
		assertThat(usuario.getRol()).isEqualTo(Rol.JUGADOR);
		assertThat(usuario.getFechaRegistro()).isEqualTo(REGISTRO);
	}

	@Test
	@DisplayName("CA4: sin nueva contraseña se conserva la actual")
	void ca4SinNuevaContrasenaConservaLaActual() {
		usuario.actualizarPerfil(elMismo, "Ana María Pérez", "ana@correo.com", "+573001234567", null);

		assertThat(usuario.getContrasenaHash()).isEqualTo(HASH_ACTUAL);
		assertThat(usuario.getNombre()).isEqualTo("Ana María Pérez");
	}

	@Test
	@DisplayName("CA6: otro usuario no puede modificar el perfil (política 9)")
	void ca6OtroUsuarioRecibeErrUsu09YNadaCambia() {
		Solicitante otro = new Solicitante(UUID.randomUUID(), Rol.JUGADOR);

		assertThatThrownBy(() -> usuario.actualizarPerfil(otro, "Intruso", "intruso@correo.com",
				"+573000000000", HASH_NUEVO))
				.isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje())
				.isEqualTo("ERR-USU-09");

		assertThat(usuario.getNombre()).isEqualTo("Ana Pérez");
		assertThat(usuario.getCorreoElectronico()).isEqualTo("ana@correo.com");
		assertThat(usuario.getContrasenaHash()).isEqualTo(HASH_ACTUAL);
	}

	@Test
	@DisplayName("CA6: ni el administrador del sistema puede modificar el perfil de otro (política 9)")
	void ca6ElAdministradorDelSistemaTampocoPuede() {
		Solicitante administrador = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_SISTEMA);

		assertThatThrownBy(() -> usuario.actualizarPerfil(administrador, "Otro", "otro@correo.com",
				"+573000000000", null))
				.isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje())
				.isEqualTo("ERR-USU-09");
	}

}
