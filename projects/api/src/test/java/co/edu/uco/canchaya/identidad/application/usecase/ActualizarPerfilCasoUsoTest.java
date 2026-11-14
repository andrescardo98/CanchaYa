package co.edu.uco.canchaya.identidad.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.uco.canchaya.identidad.application.port.in.ActualizarPerfil.DatosPerfil;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU3: caso de uso «actualizar perfil». */
class ActualizarPerfilCasoUsoTest {

	private static final String HASH_ACTUAL = "hash-de-clave2026";
	private static final OffsetDateTime REGISTRO = OffsetDateTime.now(ZoneId.of("America/Bogota"));

	private final UUID idAna = UUID.randomUUID();
	private final UUID idLuis = UUID.randomUUID();
	private final Solicitante ana = new Solicitante(idAna, Rol.JUGADOR);

	private RepositorioPerfilesEnMemoria repositorio;
	private ActualizarPerfilCasoUso casoUso;

	@BeforeEach
	void preparar() {
		repositorio = new RepositorioPerfilesEnMemoria();
		repositorio.agregar(Usuario.reconstruir(idAna, "Ana Pérez", "ana@correo.com", HASH_ACTUAL,
				TipoDocumento.CC, "1036123456", "+573001234567", Rol.JUGADOR, REGISTRO));
		repositorio.agregar(Usuario.reconstruir(idLuis, "Luis Gómez", "luis@correo.com", "hash-de-luis2026",
				TipoDocumento.CC, "98765432", "+573107654321", Rol.ADMINISTRADOR_CANCHA, REGISTRO));
		casoUso = new ActualizarPerfilCasoUso(repositorio, new CifradorFalso(), 8);
	}

	/** Lo que manda el formulario: los datos nuevos, sin documento ni rol. */
	private DatosPerfil cambios(String correo, String nuevaContrasena) {
		return new DatosPerfil(idAna, "Ana María Pérez", correo, "+573109876543", nuevaContrasena, null, null, null);
	}

	private static void esperarError(Runnable accion, String codigo) {
		assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje())
				.isEqualTo(codigo);
	}

	private void esperarQueNoSeGuardo() {
		assertThat(repositorio.actualizaciones).isZero();
		Usuario guardado = repositorio.guardado(idAna);
		assertThat(guardado.getNombre()).isEqualTo("Ana Pérez");
		assertThat(guardado.getContrasenaHash()).isEqualTo(HASH_ACTUAL);
	}

	@Test
	@DisplayName("CA1: guarda los datos nuevos y cifra la nueva contraseña")
	void ca1GuardaLosCambios() {
		Usuario actualizado = casoUso.ejecutar(ana, cambios("Ana.Maria@Correo.com", "nueva2026"));

		Usuario guardado = repositorio.guardado(idAna);
		assertThat(guardado.getNombre()).isEqualTo("Ana María Pérez");
		assertThat(guardado.getCorreoElectronico()).isEqualTo("ana.maria@correo.com");
		assertThat(guardado.getNumeroCelular()).isEqualTo("+573109876543");
		// Política 5
		assertThat(guardado.getContrasenaHash()).isEqualTo("hash-de-nueva2026");
		assertThat(actualizado.getCorreoElectronico()).isEqualTo("ana.maria@correo.com");
	}

	@Test
	@DisplayName("CA1: se acepta que el formulario envíe el mismo documento y el mismo rol")
	void ca1AceptaElDocumentoYElRolSinCambios() {
		DatosPerfil conLosMismos = new DatosPerfil(idAna, "Ana María Pérez", "ana@correo.com", "+573001234567",
				null, "CC", "1036123456", "JUGADOR");

		casoUso.ejecutar(ana, conLosMismos);

		assertThat(repositorio.guardado(idAna).getNombre()).isEqualTo("Ana María Pérez");
	}

	@Test
	@DisplayName("CA3: rechaza un correo que ya es de otro usuario, sin distinguir mayúsculas (política 10)")
	void ca3RechazaElCorreoDeOtroUsuario() {
		esperarError(() -> casoUso.ejecutar(ana, cambios("LUIS@Correo.COM", null)), "ERR-USU-10");
		esperarQueNoSeGuardo();
	}

	@Test
	@DisplayName("CA3: conservar su propio correo, aunque lo escriba con mayúsculas, no es un duplicado")
	void ca3SuPropioCorreoNoEsDuplicado() {
		casoUso.ejecutar(ana, cambios("ANA@Correo.com", null));

		assertThat(repositorio.guardado(idAna).getCorreoElectronico()).isEqualTo("ana@correo.com");
	}

	@ParameterizedTest(name = "nueva contraseña «{0}»")
	@ValueSource(strings = { "" })
	@NullSource
	@DisplayName("CA4: sin nueva contraseña se guardan los demás datos y se conserva la actual")
	void ca4SinNuevaContrasenaConservaLaActual(String nuevaContrasena) {
		casoUso.ejecutar(ana, cambios("ana@correo.com", nuevaContrasena));

		Usuario guardado = repositorio.guardado(idAna);
		assertThat(guardado.getContrasenaHash()).isEqualTo(HASH_ACTUAL);
		assertThat(guardado.getNombre()).isEqualTo("Ana María Pérez");
	}

	@ParameterizedTest(name = "{0} = «{1}»")
	@CsvSource({
			"tipoDocumento, CE",
			"tipoDocumento, XX",
			"numeroDocumento, 1036999999",
			"rol, ADMINISTRADOR_CANCHA",
			"rol, ADMINISTRADOR_SISTEMA" })
	@DisplayName("CA5: rechaza un tipo de documento, un número o un rol distinto al registrado (política 11)")
	void ca5RechazaCambiarDocumentoORol(String campo, String valor) {
		DatosPerfil datos = new DatosPerfil(idAna, "Ana María Pérez", "ana@correo.com", "+573001234567", null,
				campo.equals("tipoDocumento") ? valor : null,
				campo.equals("numeroDocumento") ? valor : null,
				campo.equals("rol") ? valor : null);

		esperarError(() -> casoUso.ejecutar(ana, datos), "ERR-USU-11");
		esperarQueNoSeGuardo();
	}

	@Test
	@DisplayName("CA6: rechaza modificar el perfil de otro usuario (política 9)")
	void ca6RechazaElPerfilDeOtroUsuario() {
		Solicitante luis = new Solicitante(idLuis, Rol.ADMINISTRADOR_CANCHA);

		esperarError(() -> casoUso.ejecutar(luis, cambios("ana@correo.com", "nueva2026")), "ERR-USU-09");
		esperarQueNoSeGuardo();
	}

	@Test
	@DisplayName("CA6: la política 9 va antes que la 11, para no revelar el documento de otro usuario")
	void ca6NoRevelaElDocumentoDeOtroUsuario() {
		Solicitante luis = new Solicitante(idLuis, Rol.ADMINISTRADOR_CANCHA);
		DatosPerfil adivinando = new DatosPerfil(idAna, "Ana Pérez", "ana@correo.com", "+573001234567", null,
				"CC", "1111111111", null);

		esperarError(() -> casoUso.ejecutar(luis, adivinando), "ERR-USU-09");
	}

	@ParameterizedTest(name = "«{0}»")
	@ValueSource(strings = { "corta1", "soloLetras", "12345678", "        " })
	@DisplayName("CA8: rechaza una nueva contraseña corta, sin letra o sin número (política 12)")
	void ca8RechazaUnaContrasenaDebil(String nuevaContrasena) {
		esperarError(() -> casoUso.ejecutar(ana, cambios("ana@correo.com", nuevaContrasena)), "ERR-USU-12");
		esperarQueNoSeGuardo();
	}

	@Test
	@DisplayName("Política 8: rechaza un identificador que no existe")
	void politica8RechazaUnUsuarioQueNoExiste() {
		UUID inexistente = UUID.randomUUID();
		DatosPerfil datos = new DatosPerfil(inexistente, "Ana", "ana@correo.com", "+573001234567", null, null,
				null, null);

		esperarError(() -> casoUso.ejecutar(new Solicitante(inexistente, Rol.JUGADOR), datos), "ERR-USU-08");
	}

}
