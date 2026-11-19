package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import co.edu.uco.canchaya.escenarios.application.port.in.ListarEscenariosAdministracion.EscenarioAdministracion;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.ubicacion.application.port.in.ConsultaUbicacion;
import co.edu.uco.canchaya.ubicacion.domain.Ciudad;
import co.edu.uco.canchaya.ubicacion.domain.Departamento;

class ListarEscenariosAdministracionCasoUsoTest {

	private static final Solicitante ADMINISTRADOR_SISTEMA =
			new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_SISTEMA);
	private static final UUID RIONEGRO = UUID.randomUUID();
	private static final UUID MARINILLA = UUID.randomUUID();
	private static final UUID ANA = UUID.randomUUID();
	private static final UUID LUIS = UUID.randomUUID();

	private RepositorioFalso repositorio;
	private ListarEscenariosAdministracionCasoUso casoUso;

	@BeforeEach
	void preparar() {
		repositorio = new RepositorioFalso();
		repositorio.resumenes.add(new RepositorioEscenarios.ResumenEscenario(UUID.randomUUID(), "Cancha La 80",
				RIONEGRO, ANA, EstadoEscenario.INACTIVO));
		repositorio.resumenes.add(new RepositorioEscenarios.ResumenEscenario(UUID.randomUUID(), "Golazo",
				MARINILLA, LUIS, EstadoEscenario.INACTIVO));
		casoUso = new ListarEscenariosAdministracionCasoUso(repositorio, new UbicacionFalsa(), new UsuariosFalsos());
	}

	@Test
	void ca1FiltraPorEstadoYMuestraCiudadYPropietario() {
		List<EscenarioAdministracion> lista = casoUso.ejecutar(EstadoEscenario.INACTIVO, ADMINISTRADOR_SISTEMA);

		assertThat(repositorio.filtros).containsExactly(EstadoEscenario.INACTIVO);
		assertThat(lista).extracting(EscenarioAdministracion::nombre, EscenarioAdministracion::ciudad,
				EscenarioAdministracion::propietario, EscenarioAdministracion::estado)
				.containsExactly(
						tuple("Cancha La 80", "Rionegro", "Ana Pérez",
								EstadoEscenario.INACTIVO),
						tuple("Golazo", "Marinilla", "Luis Gómez",
								EstadoEscenario.INACTIVO));
	}

	@Test
	void ca1SinFiltroTraeTodos() {
		casoUso.ejecutar(null, ADMINISTRADOR_SISTEMA);

		assertThat(repositorio.filtros).containsExactly((EstadoEscenario) null);
	}

	// Restricción de diseño de la HU13: solo el administrador del sistema (ERR-GEN-06)
	@ParameterizedTest
	@EnumSource(value = Rol.class, names = "ADMINISTRADOR_SISTEMA", mode = EnumSource.Mode.EXCLUDE)
	void soloElAdministradorDelSistemaVeLaLista(Rol rol) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(
				() -> casoUso.ejecutar(EstadoEscenario.INACTIVO, new Solicitante(UUID.randomUUID(), rol)))
				.isInstanceOf(ReglaNegocioException.class).actual();

		assertThat(error.getCodigoMensaje()).isEqualTo("ERR-GEN-06");
		assertThat(repositorio.filtros).isEmpty();
	}

	private static final class RepositorioFalso implements RepositorioEscenarios {

		final List<ResumenEscenario> resumenes = new ArrayList<>();
		final List<EstadoEscenario> filtros = new ArrayList<>();

		@Override
		public List<ResumenEscenario> listar(EstadoEscenario estado) {
			filtros.add(estado);
			return resumenes;
		}

		@Override
		public boolean existePorId(UUID id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void guardar(Escenario escenario) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<Escenario> buscarPorId(UUID id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void actualizar(Escenario escenario) {
			throw new UnsupportedOperationException(); // HU8
		}

		@Override
		public void actualizarEstado(Escenario escenario) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<UUID> idsCanchas(UUID escenarioId) {
			throw new UnsupportedOperationException();
		}

	}

	private static final class UbicacionFalsa implements ConsultaUbicacion {

		@Override
		public List<Ciudad> buscarCiudades(Collection<UUID> ciudadIds) {
			return List.of(Ciudad.reconstruir(RIONEGRO, "05615", UUID.randomUUID(), "Rionegro"),
					Ciudad.reconstruir(MARINILLA, "05440", UUID.randomUUID(), "Marinilla"));
		}

		@Override
		public boolean existeCiudad(UUID ciudadId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Departamento> listarDepartamentos() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Ciudad> listarCiudades(UUID departamentoId) {
			throw new UnsupportedOperationException();
		}

	}

	private static final class UsuariosFalsos implements ConsultaUsuarios {

		@Override
		public List<ContactoUsuario> buscarContactos(Collection<UUID> usuarioIds) {
			return List.of(new ContactoUsuario(ANA, "Ana Pérez", "ana@correo.com"),
					new ContactoUsuario(LUIS, "Luis Gómez", "luis@correo.com"));
		}

	}

}
