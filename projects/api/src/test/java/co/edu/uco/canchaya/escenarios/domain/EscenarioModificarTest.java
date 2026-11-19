package co.edu.uco.canchaya.escenarios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

/** HU8: responsabilidad «modificar» de Escenario (políticas 3, 7 y 13). */
class EscenarioModificarTest {

	private static final int MAXIMO_FOTOS = 5; // PAR-ESC-01
	private static final UUID PROPIETARIO_ID = UUID.randomUUID();
	private static final Solicitante PROPIETARIO = new Solicitante(PROPIETARIO_ID, Rol.ADMINISTRADOR_CANCHA);
	private static final UUID CIUDAD_NUEVA = UUID.randomUUID();

	private static Escenario escenarioGuardado(EstadoEscenario estado) {
		return Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", "Descripción vieja", "Calle 80 # 45-10",
				UUID.randomUUID(), new BigDecimal("6.155123"), new BigDecimal("-75.373700"), "+573001234567",
				"viejo@la80.com", List.of("https://fotos/1.jpg"), PROPIETARIO_ID, estado);
	}

	private static void modificar(Escenario escenario, String latitud, String longitud, List<String> fotos,
			Solicitante solicitante) {
		escenario.modificar("  Canchas La 70 ", "  Nueva descripción ", " Cra 70 # 10-20 ", CIUDAD_NUEVA,
				new BigDecimal(latitud), new BigDecimal(longitud), " +576045551234 ", "  ", fotos, solicitante,
				MAXIMO_FOTOS);
	}

	private static ReglaNegocioException esperarError(Runnable accion, String codigo) {
		var error = assertThatThrownBy(accion::run).isInstanceOf(ReglaNegocioException.class).actual();
		ReglaNegocioException regla = (ReglaNegocioException) error;
		assertThat(regla.getCodigoMensaje()).isEqualTo(codigo);
		return regla;
	}

	@Test
	void ca1CambiaLosDatosYQuitaEspacios() {
		Escenario escenario = escenarioGuardado(EstadoEscenario.ACTIVO);

		modificar(escenario, "6.2518401", "-75.5635901", List.of("https://fotos/2.jpg"), PROPIETARIO);

		assertThat(escenario.getNombre()).isEqualTo("Canchas La 70");
		assertThat(escenario.getDescripcion()).isEqualTo("Nueva descripción");
		assertThat(escenario.getDireccion()).isEqualTo("Cra 70 # 10-20");
		assertThat(escenario.getCiudadId()).isEqualTo(CIUDAD_NUEVA);
		assertThat(escenario.getTelefonoContacto()).isEqualTo("+576045551234");
		// Un opcional en blanco queda vacío, igual que al registrar (AG-06)
		assertThat(escenario.getCorreoContacto()).isNull();
		assertThat(escenario.getFotos()).containsExactly("https://fotos/2.jpg");
	}

	@Test
	void ca1GuardaLasCoordenadasConSeisDecimales() {
		Escenario escenario = escenarioGuardado(EstadoEscenario.ACTIVO);

		modificar(escenario, "6.2518401", "-75.5635901", List.of(), PROPIETARIO);

		assertThat(escenario.getLatitud()).isEqualByComparingTo("6.251840");
		assertThat(escenario.getLatitud().scale()).isEqualTo(6);
		assertThat(escenario.getLongitud()).isEqualByComparingTo("-75.563590");
	}

	@ParameterizedTest
	@EnumSource(EstadoEscenario.class)
	void ca1NoCambiaElIdElPropietarioNiElEstado(EstadoEscenario estado) {
		Escenario escenario = escenarioGuardado(estado);
		UUID id = escenario.getId();

		modificar(escenario, "6", "-75", List.of(), PROPIETARIO);

		assertThat(escenario.getId()).isEqualTo(id);
		assertThat(escenario.getAdministradorId()).isEqualTo(PROPIETARIO_ID);
		assertThat(escenario.getEstado()).isEqualTo(estado);
	}

	@Test
	void ca4OtroAdministradorDeCanchaNoPuedeModificar() {
		Escenario escenario = escenarioGuardado(EstadoEscenario.ACTIVO);
		Solicitante otro = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(() -> modificar(escenario, "6", "-75", List.of(), otro), "ERR-ESC-07");
		assertThat(escenario.getNombre()).isEqualTo("Cancha La 80");
	}

	@Test
	void ca4UnEncargadoDeSedeNoPuedeModificar() {
		// El encargado de sede tiene rol ADMINISTRADOR_CANCHA (D07), pero no es el propietario (RN-15)
		Escenario escenario = escenarioGuardado(EstadoEscenario.ACTIVO);
		Solicitante encargado = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(() -> escenario.validarPropietario(encargado), "ERR-ESC-07");
	}

	@ParameterizedTest
	@EnumSource(value = Rol.class, names = { "JUGADOR", "ADMINISTRADOR_SISTEMA" })
	void ca4OtroRolConElMismoIdNoPuedeModificar(Rol rol) {
		// Política 7 y P3 de la especificación: tampoco el administrador del sistema
		Escenario escenario = escenarioGuardado(EstadoEscenario.ACTIVO);

		esperarError(() -> modificar(escenario, "6", "-75", List.of(), new Solicitante(PROPIETARIO_ID, rol)),
				"ERR-ESC-07");
	}

	@Test
	void ca4SinSolicitanteEsErrEsc07() {
		Escenario escenario = escenarioGuardado(EstadoEscenario.ACTIVO);

		esperarError(() -> modificar(escenario, "6", "-75", List.of(), null), "ERR-ESC-07");
	}

	@Test
	void ca4ElPropietarioPasaLaValidacion() {
		Escenario escenario = escenarioGuardado(EstadoEscenario.INACTIVO);

		assertThatCode(() -> escenario.validarPropietario(PROPIETARIO)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@CsvSource({ "90.000001, 0", "-90.5, 0", "0, 180.000001", "0, -181" })
	void ca6UbicacionFueraDeRangoEsErrEsc03(String latitud, String longitud) {
		Escenario escenario = escenarioGuardado(EstadoEscenario.ACTIVO);

		esperarError(() -> modificar(escenario, latitud, longitud, List.of(), PROPIETARIO), "ERR-ESC-03");
		assertThat(escenario.getLatitud()).isEqualByComparingTo("6.155123");
	}

	@Test
	void ca8MasDeCincoFotosEsErrEsc13() {
		Escenario escenario = escenarioGuardado(EstadoEscenario.ACTIVO);
		List<String> seis = List.of("1", "2", "3", "4", "5", "6");

		ReglaNegocioException error = esperarError(() -> modificar(escenario, "6", "-75", seis, PROPIETARIO),
				"ERR-ESC-13");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("ERR-ESC-13.cantidad", MAXIMO_FOTOS));
		assertThat(escenario.getFotos()).containsExactly("https://fotos/1.jpg");
	}

	@Test
	void ca8CincoFotosSePermiten() {
		Escenario escenario = escenarioGuardado(EstadoEscenario.ACTIVO);

		modificar(escenario, "6", "-75", List.of("1", "2", "3", "4", "5"), PROPIETARIO);

		assertThat(escenario.getFotos()).hasSize(5);
	}

}
