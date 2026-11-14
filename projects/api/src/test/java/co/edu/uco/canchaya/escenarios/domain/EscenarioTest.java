package co.edu.uco.canchaya.escenarios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

class EscenarioTest {

	private static final int MAXIMO_FOTOS = 5; // PAR-ESC-01
	private static final UUID CIUDAD = UUID.randomUUID();
	private static final Solicitante PROPIETARIO = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

	private static Escenario registrar(String latitud, String longitud, List<String> fotos, Solicitante solicitante) {
		return Escenario.registrar("  Cancha La 80 ", "  ", " Calle 80 # 45-10 ", CIUDAD,
				latitud == null ? null : new BigDecimal(latitud), longitud == null ? null : new BigDecimal(longitud),
				"+573001234567", "", fotos, solicitante, MAXIMO_FOTOS);
	}

	private static Escenario registrarValido(List<String> fotos) {
		return registrar("6.1551234", "-75.3737", fotos, PROPIETARIO);
	}

	private static ReglaNegocioException esperarError(Runnable accion, String codigo) {
		var error = assertThatThrownBy(accion::run).isInstanceOf(ReglaNegocioException.class).actual();
		ReglaNegocioException regla = (ReglaNegocioException) error;
		assertThat(regla.getCodigoMensaje()).isEqualTo(codigo);
		return regla;
	}

	@Nested
	@DisplayName("CA1: datos correctos")
	class DatosCorrectos {

		@Test
		void quedaInactivoYDelSolicitante() {
			Escenario escenario = registrarValido(List.of());

			assertThat(escenario.getId()).isNotNull();
			// Política 5
			assertThat(escenario.getEstado()).isEqualTo(EstadoEscenario.INACTIVO);
			assertThat(escenario.estaActivo()).isFalse();
			// Política 1
			assertThat(escenario.getAdministradorId()).isEqualTo(PROPIETARIO.usuarioId());
			assertThat(escenario.esPropietario(PROPIETARIO.usuarioId())).isTrue();
		}

		@Test
		void quitaEspaciosYDejaVaciosLosOpcionalesEnBlanco() {
			Escenario escenario = registrarValido(List.of());

			assertThat(escenario.getNombre()).isEqualTo("Cancha La 80");
			assertThat(escenario.getDireccion()).isEqualTo("Calle 80 # 45-10");
			assertThat(escenario.getDescripcion()).isNull();
			assertThat(escenario.getCorreoContacto()).isNull();
		}

		@Test
		void guardaLasCoordenadasConSeisDecimales() {
			Escenario escenario = registrarValido(List.of());

			assertThat(escenario.getLatitud()).isEqualByComparingTo("6.155123");
			assertThat(escenario.getLatitud().scale()).isEqualTo(6);
			assertThat(escenario.getLongitud()).isEqualByComparingTo("-75.373700");
		}

		@Test
		void aceptaHastaElMaximoDeFotos() {
			List<String> cinco = List.of("f1", "f2", "f3", "f4", "f5");
			assertThat(registrarValido(cinco).getFotos()).containsExactlyElementsOf(cinco);
		}

		@Test
		void cambiaElIdentificadorSiSeRepite() {
			// Política 4
			Escenario escenario = registrarValido(List.of());
			Escenario otro = escenario.conNuevoId();
			assertThat(otro.getId()).isNotEqualTo(escenario.getId());
			assertThat(otro.getNombre()).isEqualTo(escenario.getNombre());
		}

	}

	@Nested
	@DisplayName("CA3: solo un administrador de cancha registra (política 1)")
	class SinRol {

		@ParameterizedTest
		@EnumSource(value = Rol.class, names = { "JUGADOR", "ADMINISTRADOR_SISTEMA" })
		void rechazaOtrosRoles(Rol rol) {
			esperarError(() -> registrar("6", "-75", List.of(), new Solicitante(UUID.randomUUID(), rol)), "ERR-ESC-01");
		}

		@Test
		void rechazaSinSolicitante() {
			esperarError(() -> registrar("6", "-75", List.of(), null), "ERR-ESC-01");
		}

	}

	@Nested
	@DisplayName("CA5: ubicación por fuera del rango (política 3)")
	class UbicacionInvalida {

		@ParameterizedTest
		@CsvSource({ "90.000001, 0", "-90.000001, 0", "0, 180.000001", "0, -180.000001", ", 0", "0, " })
		void rechazaCoordenadasInvalidas(String latitud, String longitud) {
			esperarError(() -> registrar(latitud, longitud, List.of(), PROPIETARIO), "ERR-ESC-03");
		}

		@ParameterizedTest
		@CsvSource({ "90, 180", "-90, -180" })
		void aceptaLosLimites(String latitud, String longitud) {
			assertThat(registrar(latitud, longitud, List.of(), PROPIETARIO).getId()).isNotNull();
		}

	}

	@Test
	@DisplayName("CA9: más de 5 fotos (política 13)")
	void rechazaMasFotosQueElMaximo() {
		ReglaNegocioException error = esperarError(
				() -> registrarValido(List.of("f1", "f2", "f3", "f4", "f5", "f6")), "ERR-ESC-13");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("ERR-ESC-13.cantidad", MAXIMO_FOTOS));
	}

}
