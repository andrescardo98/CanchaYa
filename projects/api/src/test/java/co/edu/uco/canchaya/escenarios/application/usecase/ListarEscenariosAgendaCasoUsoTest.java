package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import co.edu.uco.canchaya.escenarios.application.port.in.ListarEscenariosAgenda.EscenarioAgenda;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU29: los escenarios que el usuario puede elegir para ver la agenda (CA4, CA5, P7 y P8). */
class ListarEscenariosAgendaCasoUsoTest {

	private static final UUID ANA = UUID.randomUUID();
	private static final UUID LUIS = UUID.randomUUID();
	private static final UUID ENCARGADA = UUID.randomUUID();

	private CanchasEnMemoria datos;
	private EncargadosEnMemoria encargados;
	private ListarEscenariosAgendaCasoUso casoUso;
	private Escenario sedeNorte;
	private Escenario la70;
	private Escenario golazo;

	@BeforeEach
	void preparar() {
		datos = new CanchasEnMemoria();
		encargados = new EncargadosEnMemoria();
		sedeNorte = datos.escenarioDe(ANA, "Sede Norte", EstadoEscenario.ACTIVO);
		la70 = datos.escenarioDe(ANA, "Canchas La 70", EstadoEscenario.INACTIVO);
		golazo = datos.escenarioDe(LUIS, "Golazo", EstadoEscenario.ACTIVO);
		casoUso = new ListarEscenariosAgendaCasoUso(datos.repositorioEscenarios(), encargados);
	}

	private static Solicitante administrador(UUID id) {
		return new Solicitante(id, Rol.ADMINISTRADOR_CANCHA);
	}

	// CA4 y P8: todos los del propietario, por nombre, también los INACTIVOS; los de otros no
	@Test
	void ca4ElPropietarioVeTodosSusEscenarios() {
		assertThat(casoUso.ejecutar(administrador(ANA)))
				.extracting(EscenarioAgenda::id, EscenarioAgenda::nombre, EscenarioAgenda::estado,
						EscenarioAgenda::propietario)
				.containsExactly(tuple(la70.getId(), "Canchas La 70", EstadoEscenario.INACTIVO, true),
						tuple(sedeNorte.getId(), "Sede Norte", EstadoEscenario.ACTIVO, true));
	}

	// CA5: el encargado solo ve los que tiene asignados, y no como propietario
	@Test
	void ca5ElEncargadoSoloVeLosAsignados() {
		encargados.asignar(sedeNorte.getId(), ENCARGADA);

		assertThat(casoUso.ejecutar(administrador(ENCARGADA)))
				.extracting(EscenarioAgenda::nombre, EscenarioAgenda::propietario)
				.containsExactly(tuple("Sede Norte", false));
	}

	// CA4 y CA5: quien es propietario de uno y encargado de otro ve los dos, en orden
	@Test
	void ca4Ca5PropietarioDeUnoYEncargadoDeOtro() {
		encargados.asignar(sedeNorte.getId(), LUIS);

		assertThat(casoUso.ejecutar(administrador(LUIS)))
				.extracting(EscenarioAgenda::nombre, EscenarioAgenda::propietario)
				.containsExactly(tuple("Golazo", true), tuple("Sede Norte", false));
	}

	// P8: sin escenarios, lista vacía (el INF que falta queda reportado al Excel)
	@Test
	void sinEscenariosLaListaVaVacia() {
		assertThat(casoUso.ejecutar(administrador(UUID.randomUUID()))).isEmpty();
	}

	// P7: solo el rol ADMINISTRADOR_CANCHA (propietario o encargado, D07)
	@ParameterizedTest
	@EnumSource(value = Rol.class, names = "ADMINISTRADOR_CANCHA", mode = EnumSource.Mode.EXCLUDE)
	void p7OtroRolRecibeErrGen06(Rol rol) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(
				() -> casoUso.ejecutar(new Solicitante(ANA, rol))).isInstanceOf(ReglaNegocioException.class).actual();

		assertThat(error.getCodigoMensaje()).isEqualTo("ERR-GEN-06");
	}

	@Test
	void p7SinSolicitanteRecibeErrGen06() {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(() -> casoUso.ejecutar(null))
				.isInstanceOf(ReglaNegocioException.class).actual();

		assertThat(error.getCodigoMensaje()).isEqualTo("ERR-GEN-06");
	}

}
