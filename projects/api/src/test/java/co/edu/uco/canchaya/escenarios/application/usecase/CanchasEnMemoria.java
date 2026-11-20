package co.edu.uco.canchaya.escenarios.application.usecase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioTiposCancha;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.escenarios.domain.TipoCancha;

/** Repositorios falsos que comparten las pruebas de los casos de uso de canchas (HU6, HU9 y HU20). */
final class CanchasEnMemoria {

	final Map<UUID, Escenario> escenarios = new HashMap<>();
	final List<Cancha> canchas = new ArrayList<>();
	final List<TipoCancha> tipos = new ArrayList<>();
	// Canchas de las que se tomó el turno (HU20, D36)
	final List<UUID> turnosTomados = new ArrayList<>();

	Escenario escenarioDe(UUID propietarioId) {
		return escenarioDe(propietarioId, EstadoEscenario.INACTIVO);
	}

	Escenario escenarioDe(UUID propietarioId, EstadoEscenario estado) {
		Escenario escenario = Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null, "Calle 80 # 45-10",
				UUID.randomUUID(), new BigDecimal("6.155123"), new BigDecimal("-75.373700"), "+573001234567", null,
				List.of(), propietarioId, estado);
		escenarios.put(escenario.getId(), escenario);
		return escenario;
	}

	/** Una cancha ya guardada, con el estado que pida la prueba (HU20). */
	Cancha cancha(Escenario escenario, String nombre, EstadoCancha estado) {
		Cancha cancha = Cancha.reconstruir(UUID.randomUUID(), escenario.getId(), UUID.randomUUID(), nombre,
				new BigDecimal("80000.00"), estado);
		canchas.add(cancha);
		return cancha;
	}

	TipoCancha tipo(String codigo, String nombre, int capacidad) {
		TipoCancha tipo = TipoCancha.reconstruir(UUID.randomUUID(), codigo, nombre, capacidad);
		tipos.add(tipo);
		return tipo;
	}

	RepositorioEscenarios repositorioEscenarios() {
		return new RepositorioEscenarios() {

			@Override
			public Optional<Escenario> buscarPorId(UUID id) {
				return Optional.ofNullable(escenarios.get(id));
			}

			@Override
			public boolean existePorId(UUID id) {
				return escenarios.containsKey(id);
			}

			@Override
			public void guardar(Escenario escenario) {
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
			public List<ResumenEscenario> listar(EstadoEscenario estado) {
				throw new UnsupportedOperationException();
			}

			// HU19: todas las canchas del escenario, también las INACTIVAS
			@Override
			public List<UUID> idsCanchas(UUID escenarioId) {
				return canchas.stream().filter(c -> c.getEscenarioId().equals(escenarioId)).map(Cancha::getId)
						.toList();
			}
		};
	}

	RepositorioCanchas repositorioCanchas() {
		return new RepositorioCanchas() {

			@Override
			public boolean existePorId(UUID id) {
				return canchas.stream().anyMatch(c -> c.getId().equals(id));
			}

			@Override
			public boolean existeNombreEnEscenario(UUID escenarioId, String nombre) {
				String buscado = nombre.toLowerCase(Locale.ROOT);
				return canchas.stream().anyMatch(c -> c.getEscenarioId().equals(escenarioId)
						&& c.getNombre().toLowerCase(Locale.ROOT).equals(buscado));
			}

			@Override
			public void guardar(Cancha cancha) {
				canchas.add(cancha);
			}

			@Override
			public List<Cancha> listarPorEscenario(UUID escenarioId) {
				return canchas.stream().filter(c -> c.getEscenarioId().equals(escenarioId))
						.sorted(Comparator.comparing(Cancha::getNombre)).toList();
			}

			@Override
			public boolean existeNombreEnOtraCancha(UUID escenarioId, String nombre, UUID canchaId) {
				String buscado = nombre.toLowerCase(Locale.ROOT);
				return canchas.stream().anyMatch(c -> c.getEscenarioId().equals(escenarioId)
						&& !c.getId().equals(canchaId) && c.getNombre().toLowerCase(Locale.ROOT).equals(buscado));
			}

			// HU9: reemplaza la cancha guardada por la que se modificó
			@Override
			public void actualizar(Cancha cancha) {
				canchas.replaceAll(c -> c.getId().equals(cancha.getId()) ? copia(cancha) : c);
			}

			// Una copia, como la que arma la base: si el caso de uso la modifica y luego falla, la
			// guardada no cambia (HU9)
			@Override
			public Optional<Cancha> buscarPorId(UUID id) {
				return canchas.stream().filter(c -> c.getId().equals(id)).findFirst().map(CanchasEnMemoria::copia);
			}

			@Override
			public void tomarTurno(UUID id) {
				turnosTomados.add(id);
			}
		};
	}

	private static Cancha copia(Cancha cancha) {
		return Cancha.reconstruir(cancha.getId(), cancha.getEscenarioId(), cancha.getTipoCanchaId(),
				cancha.getNombre(), cancha.getPrecioHora(), cancha.getEstado());
	}

	/** La cancha guardada con ese identificador (HU9). */
	Cancha guardada(UUID id) {
		return canchas.stream().filter(c -> c.getId().equals(id)).findFirst().orElseThrow();
	}

	RepositorioTiposCancha repositorioTipos() {
		return new RepositorioTiposCancha() {

			@Override
			public boolean existePorId(UUID id) {
				return tipos.stream().anyMatch(t -> t.getId().equals(id));
			}

			@Override
			public List<TipoCancha> listar() {
				return tipos.stream().sorted(Comparator.comparing(TipoCancha::getCodigo)).toList();
			}
		};
	}

}
