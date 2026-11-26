package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEncargados;

/** Repositorio falso de encargados (HU29): la prueba dice quién está asignado a qué escenario. */
final class EncargadosEnMemoria implements RepositorioEncargados {

	private record Asignacion(UUID escenarioId, UUID usuarioId) {
	}

	private final List<Asignacion> asignaciones = new ArrayList<>();

	void asignar(UUID escenarioId, UUID usuarioId) {
		asignaciones.add(new Asignacion(escenarioId, usuarioId));
	}

	@Override
	public boolean estaAsignado(UUID escenarioId, UUID usuarioId) {
		return asignaciones.contains(new Asignacion(escenarioId, usuarioId));
	}

	@Override
	public List<UUID> escenariosAsignados(UUID usuarioId) {
		return asignaciones.stream().filter(a -> a.usuarioId().equals(usuarioId)).map(Asignacion::escenarioId)
				.toList();
	}

}
