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

/** HU29: quién puede ver la agenda del escenario (RN-15, AG-12). */
class EscenarioAccesoAgendaTest {

	private static final Escenario ESCENARIO = Escenario.reconstruir(UUID.randomUUID(), "Canchas La 70", null,
			"Calle 70 # 45-10", UUID.randomUUID(), new BigDecimal("6.155123"), new BigDecimal("-75.373700"),
			"+573001234567", null, List.of(), UUID.randomUUID(), EstadoEscenario.ACTIVO);

	private static final Solicitante PROPIETARIO =
			new Solicitante(ESCENARIO.getAdministradorId(), Rol.ADMINISTRADOR_CANCHA);

	private static final Solicitante OTRO_ADMINISTRADOR =
			new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

	private static void esperarErrGen06(Runnable accion) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo("ERR-GEN-06");
	}

	@Test
	void elPropietarioPuedeVerLaAgendaYEsPropietario() {
		assertThat(ESCENARIO.validarAccesoAgenda(PROPIETARIO, false)).isTrue();
	}

	@Test
	void unEncargadoAsignadoPuedeVerLaAgendaPeroNoEsPropietario() {
		assertThat(ESCENARIO.validarAccesoAgenda(OTRO_ADMINISTRADOR, true)).isFalse();
	}

	@Test
	void ca6UnAdministradorQueNoEsPropietarioNiEncargadoNoPuede() {
		esperarErrGen06(() -> ESCENARIO.validarAccesoAgenda(OTRO_ADMINISTRADOR, false));
	}

	// CA6: el encargado de sede usa el rol ADMINISTRADOR_CANCHA (D07); con otro rol no entra aunque figure asignado
	@ParameterizedTest
	@EnumSource(value = Rol.class, names = "ADMINISTRADOR_CANCHA", mode = EnumSource.Mode.EXCLUDE)
	void ca6OtroRolNoPuedeAunqueSeaElIdDelPropietario(Rol rol) {
		Solicitante conOtroRol = new Solicitante(ESCENARIO.getAdministradorId(), rol);

		esperarErrGen06(() -> ESCENARIO.validarAccesoAgenda(conOtroRol, false));
		esperarErrGen06(() -> ESCENARIO.validarAccesoAgenda(conOtroRol, true));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void ca6SinSolicitanteNoPuede(boolean esEncargadoAsignado) {
		esperarErrGen06(() -> ESCENARIO.validarAccesoAgenda(null, esEncargadoAsignado));
	}

}
