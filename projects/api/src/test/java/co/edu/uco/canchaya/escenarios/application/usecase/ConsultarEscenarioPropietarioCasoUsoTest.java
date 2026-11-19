package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU8: consulta del escenario para llenar el formulario (P2 de la especificación; políticas 6 y 7). */
class ConsultarEscenarioPropietarioCasoUsoTest {

	private static final UUID PROPIETARIO_ID = UUID.randomUUID();
	private static final Solicitante PROPIETARIO = new Solicitante(PROPIETARIO_ID, Rol.ADMINISTRADOR_CANCHA);

	private CanchasEnMemoria datos;
	private ConsultarEscenarioPropietarioCasoUso casoUso;

	@BeforeEach
	void preparar() {
		datos = new CanchasEnMemoria();
		casoUso = new ConsultarEscenarioPropietarioCasoUso(datos.repositorioEscenarios());
	}

	private static void esperarError(Runnable accion, String codigo) {
		assertThatThrownBy(accion::run).isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje()).isEqualTo(codigo);
	}

	@Test
	void ca1ElPropietarioVeLosDatosActualesAunqueEsteInactivo() {
		// Un escenario recién registrado está INACTIVO y /perfil no lo muestra; el formulario sí
		Escenario guardado = datos.escenarioDe(PROPIETARIO_ID);

		Escenario escenario = casoUso.ejecutar(guardado.getId(), PROPIETARIO);

		assertThat(escenario).isSameAs(guardado);
		assertThat(escenario.getEstado()).isEqualTo(EstadoEscenario.INACTIVO);
	}

	@Test
	void ca3ElEscenarioNoExiste() {
		esperarError(() -> casoUso.ejecutar(UUID.randomUUID(), PROPIETARIO), "ERR-ESC-06");
	}

	@Test
	void ca4OtroAdministradorDeCanchaOUnEncargadoNoLoVen() {
		Escenario guardado = datos.escenarioDe(PROPIETARIO_ID);
		Solicitante otro = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(() -> casoUso.ejecutar(guardado.getId(), otro), "ERR-ESC-07");
	}

	@ParameterizedTest
	@EnumSource(value = Rol.class, names = { "JUGADOR", "ADMINISTRADOR_SISTEMA" })
	void ca4OtrosRolesNoLoVen(Rol rol) {
		Escenario guardado = datos.escenarioDe(PROPIETARIO_ID);

		esperarError(() -> casoUso.ejecutar(guardado.getId(), new Solicitante(UUID.randomUUID(), rol)),
				"ERR-ESC-07");
	}

}
