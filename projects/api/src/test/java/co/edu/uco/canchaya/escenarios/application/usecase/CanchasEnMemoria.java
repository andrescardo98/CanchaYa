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
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.escenarios.domain.TipoCancha;

/** Repositorios falsos que comparten las pruebas de los casos de uso de canchas (HU6). */
final class CanchasEnMemoria {

	final Map<UUID, Escenario> escenarios = new HashMap<>();
	final List<Cancha> canchas = new ArrayList<>();
	final List<TipoCancha> tipos = new ArrayList<>();

	Escenario escenarioDe(UUID propietarioId) {
		Escenario escenario = Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null, "Calle 80 # 45-10",
				UUID.randomUUID(), new BigDecimal("6.155123"), new BigDecimal("-75.373700"), "+573001234567", null,
				List.of(), propietarioId, EstadoEscenario.INACTIVO);
		escenarios.put(escenario.getId(), escenario);
		return escenario;
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
		};
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
