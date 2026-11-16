package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarCancha.DatosRegistroCancha;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.TipoCancha;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU6: nombre disponible (CA8), lista de canchas del escenario (CA1) y tipos de cancha (CA10). */
class ConsultasCanchasCasoUsoTest {

	private static final Solicitante PROPIETARIO = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);
	private static final Solicitante OTRO = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

	private CanchasEnMemoria memoria;
	private Escenario escenario;
	private TipoCancha f5;
	private ConsultarNombreCanchaDisponibleCasoUso nombreDisponible;
	private ListarCanchasEscenarioCasoUso listarCanchas;

	@BeforeEach
	void preparar() {
		memoria = new CanchasEnMemoria();
		escenario = memoria.escenarioDe(PROPIETARIO.usuarioId());
		f5 = memoria.tipo("F5", "Fútbol 5", 10);
		nombreDisponible = new ConsultarNombreCanchaDisponibleCasoUso(memoria.repositorioEscenarios(),
				memoria.repositorioCanchas());
		listarCanchas = new ListarCanchasEscenarioCasoUso(memoria.repositorioEscenarios(),
				memoria.repositorioCanchas());
	}

	private Cancha registrar(String nombre) {
		return new RegistrarCanchaCasoUso(memoria.repositorioEscenarios(), memoria.repositorioCanchas(),
				memoria.repositorioTipos())
				.ejecutar(new DatosRegistroCancha(escenario.getId(), f5.getId(), nombre, BigDecimal.TEN), PROPIETARIO);
	}

	private static void esperarError(Runnable accion, String codigo) {
		assertThatThrownBy(accion::run).isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje()).isEqualTo(codigo);
	}

	@Test
	void ca8ElNombreEstaLibre() {
		registrar("Cancha 1");

		assertThat(nombreDisponible.ejecutar(escenario.getId(), "Cancha 2", PROPIETARIO)).isTrue();
	}

	@Test
	void ca8ElNombreYaExisteSinImportarMayusculas() {
		registrar("Cancha 1");

		assertThat(nombreDisponible.ejecutar(escenario.getId(), " CANCHA 1 ", PROPIETARIO)).isFalse();
	}

	@Test
	void ca8SoloElPropietarioPreguntaPorLosNombres() {
		esperarError(() -> nombreDisponible.ejecutar(escenario.getId(), "Cancha 1", OTRO), "ERR-CAN-02");
		esperarError(() -> nombreDisponible.ejecutar(UUID.randomUUID(), "Cancha 1", PROPIETARIO), "ERR-CAN-01");
	}

	@Test
	void ca1LaListaTraeLasCanchasDelEscenarioOrdenadasPorNombre() {
		Cancha b = registrar("Cancha B");
		Cancha a = registrar("Cancha A");
		Escenario otro = memoria.escenarioDe(PROPIETARIO.usuarioId());
		new RegistrarCanchaCasoUso(memoria.repositorioEscenarios(), memoria.repositorioCanchas(),
				memoria.repositorioTipos())
				.ejecutar(new DatosRegistroCancha(otro.getId(), f5.getId(), "De otro", BigDecimal.TEN), PROPIETARIO);

		assertThat(listarCanchas.ejecutar(escenario.getId(), PROPIETARIO)).containsExactly(a, b);
	}

	@Test
	void ca1SoloElPropietarioVeLaLista() {
		esperarError(() -> listarCanchas.ejecutar(escenario.getId(), OTRO), "ERR-CAN-02");
		esperarError(() -> listarCanchas.ejecutar(UUID.randomUUID(), PROPIETARIO), "ERR-CAN-01");
	}

	@Test
	void ca10ListaLosTiposOrdenadosPorCodigo() {
		TipoCancha f11 = memoria.tipo("F11", "Fútbol 11", 22);
		TipoCancha f7 = memoria.tipo("F7", "Fútbol 7", 14);

		// Orden de texto, como en la base: "F11" va antes que "F5"
		assertThat(new ListarTiposCanchaCasoUso(memoria.repositorioTipos()).ejecutar())
				.containsExactly(f11, f5, f7);
	}

}
