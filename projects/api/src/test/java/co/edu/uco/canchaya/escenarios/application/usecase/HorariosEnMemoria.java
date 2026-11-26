package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioHorarios;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;

/** Repositorios falsos para las pruebas de HU7, HU11 y HU12. Solo hacen lo que usan los casos de uso de horarios. */
final class HorariosEnMemoria implements RepositorioHorarios {

	final List<HorarioRecurrente> guardados = new ArrayList<>();
	// Cuántas veces existePorId responde que el id ya está, para probar la política 5
	int idRepetidos;
	int consultasDeId;

	@Override
	public boolean existePorId(UUID id) {
		consultasDeId++;
		if (idRepetidos > 0) {
			idRepetidos--;
			return true;
		}
		return guardados.stream().anyMatch(h -> h.getId().equals(id));
	}

	@Override
	public void guardar(HorarioRecurrente horario) {
		guardados.add(horario);
	}

	// HU11. Devuelve una copia, como la base: cambiar el objeto no cambia lo guardado hasta actualizar
	@Override
	public Optional<HorarioRecurrente> buscarPorId(UUID id) {
		return guardados.stream().filter(h -> h.getId().equals(id)).findFirst().map(HorariosEnMemoria::copia);
	}

	// HU11
	@Override
	public void actualizar(HorarioRecurrente horario) {
		guardados.replaceAll(h -> h.getId().equals(horario.getId()) ? copia(horario) : h);
	}

	// HU12
	@Override
	public void eliminar(UUID id) {
		guardados.removeIf(h -> h.getId().equals(id));
	}

	private static HorarioRecurrente copia(HorarioRecurrente h) {
		return HorarioRecurrente.reconstruir(h.getId(), h.getEscenarioId(), h.getDiaSemana(), h.getHoraInicio(),
				h.getHoraFin());
	}

	@Override
	public List<HorarioRecurrente> buscarPorEscenarioYDia(UUID escenarioId, DiaSemana diaSemana) {
		return guardados.stream()
				.filter(h -> h.getEscenarioId().equals(escenarioId) && h.getDiaSemana() == diaSemana)
				.toList();
	}

	@Override
	public List<HorarioRecurrente> buscarPorEscenario(UUID escenarioId) {
		return guardados.stream()
				.filter(h -> h.getEscenarioId().equals(escenarioId))
				.sorted(Comparator.comparing(HorarioRecurrente::getDiaSemana)
						.thenComparing(HorarioRecurrente::getHoraInicio))
				.toList();
	}

	@Override
	public List<HorarioRecurrente> buscarPorEscenarios(Collection<UUID> escenarioIds) {
		return guardados.stream().filter(h -> escenarioIds.contains(h.getEscenarioId())).toList();
	}

	static final class Escenarios implements RepositorioEscenarios {

		final List<Escenario> escenarios = new ArrayList<>();
		// HU12: las canchas de cada escenario, para tomar sus turnos
		final Map<UUID, List<UUID>> canchas = new HashMap<>();

		@Override
		public Optional<Escenario> buscarPorId(UUID id) {
			return escenarios.stream().filter(e -> e.getId().equals(id)).findFirst();
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

		@Override
		public List<ResumenEscenario> listarPorAdministrador(UUID administradorId) {
			throw new UnsupportedOperationException(); // HU29
		}

		@Override
		public List<ResumenEscenario> listarPorIds(Collection<UUID> ids) {
			throw new UnsupportedOperationException(); // HU29
		}

		@Override
		public List<UUID> idsCanchas(UUID escenarioId) {
			return canchas.getOrDefault(escenarioId, List.of());
		}

	}

}
