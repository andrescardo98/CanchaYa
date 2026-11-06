package co.edu.uco.canchaya.identidad.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * Reglas del dominio de HU1 (registrarse), sin Spring ni base de datos.
 * Cada prueba cita el criterio de aceptación (CAn) y la política de Usuario que revisa.
 */
class UsuarioTest {

	private static final String HASH = "$2a$10$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz01234";
	private static final OffsetDateTime AHORA = OffsetDateTime.now(ZoneId.of("America/Bogota"));
	private static final int LONGITUD_MINIMA = 8; // PAR-USU-01

	private static Usuario registrar(TipoDocumento tipo, String numero, Rol rol, boolean autorizacion) {
		return Usuario.registrar("  Ana Pérez ", "  Ana.Perez@Correo.COM ", HASH, tipo, numero,
				"+573001234567", rol, autorizacion, AHORA);
	}

	private static void esperarError(Runnable accion, String codigo) {
		assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje())
				.isEqualTo(codigo);
	}

	@Nested
	@DisplayName("CA1: datos correctos")
	class DatosCorrectos {

		@ParameterizedTest
		@ValueSource(strings = { "JUGADOR", "ADMINISTRADOR_CANCHA" })
		void creaElUsuarioConElRolElegidoSuFechaYElHash(Rol rol) {
			Usuario usuario = registrar(TipoDocumento.CC, "1036123456", rol, false);

			assertThat(usuario.getId()).isNotNull();
			assertThat(usuario.getRol()).isEqualTo(rol);
			assertThat(usuario.getFechaRegistro()).isEqualTo(AHORA);
			// Política 5: solo se guarda el hash que llega del cifrador.
			assertThat(usuario.getContrasenaHash()).isEqualTo(HASH);
		}

		@Test
		void guardaElCorreoEnMinusculaYSinEspacios() {
			// Conversación de HU1: la unicidad del correo no distingue mayúsculas (política 1).
			Usuario usuario = registrar(TipoDocumento.CC, "1036123456", Rol.JUGADOR, false);
			assertThat(usuario.getCorreoElectronico()).isEqualTo("ana.perez@correo.com");
			assertThat(usuario.getNombre()).isEqualTo("Ana Pérez");
		}

		@Test
		void cadaRegistroRecibeUnIdentificadorDistinto() {
			// Política 4: si el id ya existe, el caso de uso pide otro con conNuevoId().
			Usuario usuario = registrar(TipoDocumento.CC, "1036123456", Rol.JUGADOR, false);
			Usuario otro = usuario.conNuevoId();
			assertThat(otro.getId()).isNotEqualTo(usuario.getId());
			assertThat(otro.getCorreoElectronico()).isEqualTo(usuario.getCorreoElectronico());
		}

	}

	@Nested
	@DisplayName("CA5: rol no permitido (política 3)")
	class RolNoPermitido {

		@Test
		void rechazaAlAdministradorDelSistema() {
			esperarError(() -> registrar(TipoDocumento.CC, "1036123456", Rol.ADMINISTRADOR_SISTEMA, false),
					"ERR-USU-03");
		}

		@Test
		void rechazaUnRolVacio() {
			esperarError(() -> registrar(TipoDocumento.CC, "1036123456", null, false), "ERR-USU-03");
		}

	}

	@Nested
	@DisplayName("CA9: contraseña (política 12)")
	class Contrasena {

		@ParameterizedTest
		@ValueSource(strings = { "clave123", "Contraseña2026", "12345abc", "ñandú2026" })
		void aceptaLasQueCumplen(String contrasena) {
			assertThatCode(() -> Usuario.validarContrasena(contrasena, LONGITUD_MINIMA)).doesNotThrowAnyException();
		}

		@ParameterizedTest
		@ValueSource(strings = { "clave12", "soloLetras", "12345678", "" })
		void rechazaCortasOSinLetraOSinNumero(String contrasena) {
			esperarError(() -> Usuario.validarContrasena(contrasena, LONGITUD_MINIMA), "ERR-USU-12");
		}

		@Test
		void rechazaUnaContrasenaVacia() {
			esperarError(() -> Usuario.validarContrasena(null, LONGITUD_MINIMA), "ERR-USU-12");
		}

	}

	@Nested
	@DisplayName("CA10: formato del documento (política 13)")
	class FormatoDocumento {

		@ParameterizedTest(name = "{0} {1} es válido")
		@CsvSource({
				"CC, 123456", "CC, 1036123456",
				"CE, AB1234", "CE, 1234567890",
				"TI, 1001234567", "TI, 10012345678",
				"PASAPORTE, AB123456", "PASAPORTE, A1B2C3D4E5F6",
				"NIT, 900123456-7" })
		void aceptaLosNumerosConElFormatoDelTipo(TipoDocumento tipo, String numero) {
			// La TI exige rol JUGADOR y autorización; se envían para probar solo el formato.
			assertThatCode(() -> registrar(tipo, numero, Rol.JUGADOR, true)).doesNotThrowAnyException();
		}

		@ParameterizedTest(name = "{0} {1} no es válido")
		@CsvSource({
				"CC, 12345", "CC, 12345678901", "CC, 12345A",
				"CE, AB123", "CE, AB12345678X", "CE, AB-1234",
				"TI, 123456789", "TI, 123456789012", "TI, 10012345A7",
				"PASAPORTE, AB123", "PASAPORTE, AB1234567890X", "PASAPORTE, AB 12345",
				"NIT, 900123456", "NIT, 90012345-6", "NIT, 900123456-78" })
		void rechazaLosNumerosSinElFormatoDelTipo(TipoDocumento tipo, String numero) {
			assertThatThrownBy(() -> registrar(tipo, numero, Rol.JUGADOR, true))
					.isInstanceOf(ReglaNegocioException.class)
					.satisfies(e -> {
						ReglaNegocioException error = (ReglaNegocioException) e;
						assertThat(error.getCodigoMensaje()).isEqualTo("ERR-USU-13");
						// Parámetros del mensaje: {tipoDocumento} y {formatoEsperado}.
						assertThat(error.getParametros()).containsExactly(tipo.name(), tipo.getFormatoEsperado());
					});
		}

		@Test
		void rechazaUnTipoDeDocumentoVacio() {
			esperarError(() -> registrar(null, "1036123456", Rol.JUGADOR, false), "ERR-USU-13");
		}

	}

	@Nested
	@DisplayName("CA12 y CA13: tarjeta de identidad (políticas 14 y 15)")
	class TarjetaDeIdentidad {

		@Test
		void ca12RechazaLaTiParaUnAdministradorDeCancha() {
			esperarError(() -> registrar(TipoDocumento.TI, "1001234567", Rol.ADMINISTRADOR_CANCHA, true),
					"ERR-USU-14");
		}

		@Test
		void ca13RechazaLaTiSinLaAutorizacionDelAcudiente() {
			esperarError(() -> registrar(TipoDocumento.TI, "1001234567", Rol.JUGADOR, false), "ERR-USU-15");
		}

		@Test
		void aceptaLaTiDeUnJugadorConAutorizacion() {
			assertThat(registrar(TipoDocumento.TI, "1001234567", Rol.JUGADOR, true).getTipoDocumento())
					.isEqualTo(TipoDocumento.TI);
		}

		@Test
		void laAutorizacionSoloSeExigeConTi() {
			assertThatCode(() -> registrar(TipoDocumento.CC, "1036123456", Rol.JUGADOR, false))
					.doesNotThrowAnyException();
		}

	}

}
