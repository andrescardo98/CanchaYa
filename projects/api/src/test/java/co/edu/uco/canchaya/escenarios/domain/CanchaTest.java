package co.edu.uco.canchaya.escenarios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU6: responsabilidad «registrar» de Cancha (políticas 2 y 5). */
class CanchaTest {

	private static final UUID PROPIETARIO_ID = UUID.randomUUID();
	private static final Solicitante PROPIETARIO = new Solicitante(PROPIETARIO_ID, Rol.ADMINISTRADOR_CANCHA);
	private static final UUID TIPO_F5 = UUID.randomUUID();

	private final Escenario escenario = Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null,
			"Calle 80 # 45-10", UUID.randomUUID(), new BigDecimal("6.155123"), new BigDecimal("-75.373700"),
			"+573001234567", null, List.of(), PROPIETARIO_ID, EstadoEscenario.INACTIVO);

	private static void esperarError(Runnable accion, String codigo) {
		assertThatThrownBy(accion::run).isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje()).isEqualTo(codigo);
	}

	@Test
	void ca1ElPropietarioRegistraUnaCanchaQueNaceDisponible() {
		Cancha cancha = Cancha.registrar(escenario, TIPO_F5, "  Cancha 1 ", new BigDecimal("80000"), PROPIETARIO);

		assertThat(cancha.getId()).isNotNull();
		assertThat(cancha.getEscenarioId()).isEqualTo(escenario.getId());
		assertThat(cancha.getTipoCanchaId()).isEqualTo(TIPO_F5);
		assertThat(cancha.getNombre()).isEqualTo("Cancha 1");
		assertThat(cancha.getPrecioHora()).isEqualByComparingTo("80000.00");
		assertThat(cancha.getEstado()).isEqualTo(EstadoCancha.DISPONIBLE);
		assertThat(cancha.estaDisponible()).isTrue();
	}

	// CA4: otro administrador de cancha, que es como queda un encargado de sede
	@Test
	void ca4OtroAdministradorDeCanchaNoPuedeRegistrar() {
		Solicitante otro = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(() -> Cancha.registrar(escenario, TIPO_F5, "Cancha 1", BigDecimal.TEN, otro), "ERR-CAN-02");
	}

	// CA4: aunque el identificador coincida, si el rol no es administrador de cancha no puede
	@ParameterizedTest
	@EnumSource(value = Rol.class, names = "ADMINISTRADOR_CANCHA", mode = EnumSource.Mode.EXCLUDE)
	void ca4OtrosRolesNoPuedenRegistrar(Rol rol) {
		Solicitante solicitante = new Solicitante(PROPIETARIO_ID, rol);

		esperarError(() -> Cancha.registrar(escenario, TIPO_F5, "Cancha 1", BigDecimal.TEN, solicitante),
				"ERR-CAN-02");
	}

	@Test
	void ca4SinSolicitanteEsErrCan02() {
		esperarError(() -> Cancha.registrar(escenario, TIPO_F5, "Cancha 1", BigDecimal.TEN, null), "ERR-CAN-02");
	}

	// CA7 / política 5. 0.004 queda en 0.00 al redondear a 2 decimales
	@ParameterizedTest
	@ValueSource(strings = { "0", "0.00", "-1", "-50000", "0.004" })
	void ca7ElPrecioDebeSerMayorQueCero(String precio) {
		esperarError(() -> Cancha.registrar(escenario, TIPO_F5, "Cancha 1", new BigDecimal(precio), PROPIETARIO),
				"ERR-CAN-05");
	}

	@Test
	void ca7SinPrecioTambienEsErrCan05() {
		esperarError(() -> Cancha.registrar(escenario, TIPO_F5, "Cancha 1", null, PROPIETARIO), "ERR-CAN-05");
	}

	@Test
	void ca7ElPrecioMinimoEsUnCentavo() {
		Cancha cancha = Cancha.registrar(escenario, TIPO_F5, "Cancha 1", new BigDecimal("0.01"), PROPIETARIO);

		assertThat(cancha.getPrecioHora()).isEqualByComparingTo("0.01");
	}

	// Política 6: si el identificador ya existe se genera otro, con los mismos datos
	@Test
	void conNuevoIdConservaLosDatos() {
		Cancha cancha = Cancha.registrar(escenario, TIPO_F5, "Cancha 1", BigDecimal.TEN, PROPIETARIO);

		Cancha otra = cancha.conNuevoId();

		assertThat(otra.getId()).isNotEqualTo(cancha.getId());
		assertThat(otra).usingRecursiveComparison().ignoringFields("id").isEqualTo(cancha);
	}

}
