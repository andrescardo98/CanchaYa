package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarCancha.DatosRegistroCancha;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.escenarios.domain.TipoCancha;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

class RegistrarCanchaCasoUsoTest {

	private static final Solicitante PROPIETARIO = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

	private CanchasEnMemoria memoria;
	private RegistrarCanchaCasoUso casoUso;
	private Escenario escenario;
	private TipoCancha f5;

	@BeforeEach
	void preparar() {
		memoria = new CanchasEnMemoria();
		casoUso = new RegistrarCanchaCasoUso(memoria.repositorioEscenarios(), memoria.repositorioCanchas(),
				memoria.repositorioTipos());
		escenario = memoria.escenarioDe(PROPIETARIO.usuarioId());
		f5 = memoria.tipo("F5", "Fútbol 5", 10);
	}

	private DatosRegistroCancha datos(String nombre) {
		return new DatosRegistroCancha(escenario.getId(), f5.getId(), nombre, new BigDecimal("80000"));
	}

	private ReglaNegocioException esperarError(DatosRegistroCancha datos, Solicitante solicitante, String codigo) {
		int antes = memoria.canchas.size();
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(
				() -> casoUso.ejecutar(datos, solicitante))
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
		// Ningún error guarda la cancha
		assertThat(memoria.canchas).hasSize(antes);
		return error;
	}

	@Test
	void ca1GuardaLaCanchaDisponible() {
		Cancha cancha = casoUso.ejecutar(datos("Cancha 1"), PROPIETARIO);

		assertThat(cancha.getEstado()).isEqualTo(EstadoCancha.DISPONIBLE);
		assertThat(memoria.canchas).containsExactly(cancha);
	}

	@Test
	void ca1OtroEscenarioPuedeUsarElMismoNombre() {
		casoUso.ejecutar(datos("Cancha 1"), PROPIETARIO);
		Escenario otro = memoria.escenarioDe(PROPIETARIO.usuarioId());

		casoUso.ejecutar(new DatosRegistroCancha(otro.getId(), f5.getId(), "Cancha 1", BigDecimal.TEN), PROPIETARIO);

		assertThat(memoria.canchas).hasSize(2);
	}

	@Test
	void ca3ElEscenarioNoExiste() {
		DatosRegistroCancha datos = new DatosRegistroCancha(UUID.randomUUID(), f5.getId(), "Cancha 1",
				BigDecimal.TEN);

		esperarError(datos, PROPIETARIO, "ERR-CAN-01");
	}

	// CA4: otro administrador de cancha, que es como queda un encargado de sede
	@Test
	void ca4ElSolicitanteNoEsElPropietario() {
		Solicitante otro = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(datos("Cancha 1"), otro, "ERR-CAN-02");
	}

	@Test
	void ca4UnJugadorNoPuedeRegistrar() {
		esperarError(datos("Cancha 1"), new Solicitante(UUID.randomUUID(), Rol.JUGADOR), "ERR-CAN-02");
	}

	// CA5: el nombre se compara sin distinguir mayúsculas de minúsculas
	@ParameterizedTest
	@ValueSource(strings = { "Cancha 1", "CANCHA 1", "cancha 1", "  Cancha 1  " })
	void ca5YaExisteUnaCanchaConEseNombre(String nombre) {
		casoUso.ejecutar(datos("Cancha 1"), PROPIETARIO);

		ReglaNegocioException error = esperarError(datos(nombre), PROPIETARIO, "ERR-CAN-03");
		// {nombre} del mensaje, sin los espacios de los extremos
		assertThat(error.getParametros()).containsExactly(nombre.strip());
	}

	@Test
	void ca6ElTipoDeCanchaNoExiste() {
		DatosRegistroCancha datos = new DatosRegistroCancha(escenario.getId(), UUID.randomUUID(), "Cancha 1",
				BigDecimal.TEN);

		esperarError(datos, PROPIETARIO, "ERR-CAN-04");
	}

	@Test
	void ca7ElPrecioEsCero() {
		DatosRegistroCancha datos = new DatosRegistroCancha(escenario.getId(), f5.getId(), "Cancha 1",
				BigDecimal.ZERO);

		esperarError(datos, PROPIETARIO, "ERR-CAN-05");
	}

	// Política 6: si el identificador ya existe, se reintenta con otro
	@Test
	void siElIdentificadorYaExisteSeGeneraOtro() {
		CanchaConIdOcupado ocupado = new CanchaConIdOcupado(memoria);
		RegistrarCanchaCasoUso conColision = new RegistrarCanchaCasoUso(memoria.repositorioEscenarios(), ocupado,
				memoria.repositorioTipos());

		Cancha cancha = conColision.ejecutar(datos("Cancha 1"), PROPIETARIO);

		assertThat(ocupado.primerId).isNotNull();
		assertThat(cancha.getId()).isNotEqualTo(ocupado.primerId);
		assertThat(memoria.canchas).extracting(Cancha::getId).containsExactly(cancha.getId());
	}

	/** Dice que el primer identificador que le preguntan ya existe. */
	private static final class CanchaConIdOcupado implements RepositorioCanchas {

		private final RepositorioCanchas real;
		private UUID primerId;

		CanchaConIdOcupado(CanchasEnMemoria memoria) {
			this.real = memoria.repositorioCanchas();
		}

		@Override
		public boolean existePorId(UUID id) {
			if (primerId == null) {
				primerId = id;
				return true;
			}
			return real.existePorId(id);
		}

		@Override
		public boolean existeNombreEnEscenario(UUID escenarioId, String nombre) {
			return real.existeNombreEnEscenario(escenarioId, nombre);
		}

		@Override
		public void guardar(Cancha cancha) {
			real.guardar(cancha);
		}

		@Override
		public List<Cancha> listarPorEscenario(UUID escenarioId) {
			return real.listarPorEscenario(escenarioId);
		}

		@Override
		public Optional<Cancha> buscarPorId(UUID id) {
			return real.buscarPorId(id);
		}

		@Override
		public void tomarTurno(UUID id) {
			real.tomarTurno(id);
		}
	}

}
