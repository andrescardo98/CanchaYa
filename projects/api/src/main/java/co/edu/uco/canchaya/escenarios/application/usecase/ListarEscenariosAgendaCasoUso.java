package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ListarEscenariosAgenda;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEncargados;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios.ResumenEscenario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

@Service
public class ListarEscenariosAgendaCasoUso implements ListarEscenariosAgenda {

	private final RepositorioEscenarios repositorioEscenarios;
	private final RepositorioEncargados repositorioEncargados;

	public ListarEscenariosAgendaCasoUso(RepositorioEscenarios repositorioEscenarios,
			RepositorioEncargados repositorioEncargados) {
		this.repositorioEscenarios = repositorioEscenarios;
		this.repositorioEncargados = repositorioEncargados;
	}

	@Override
	public List<EscenarioAgenda> ejecutar(Solicitante solicitante) {
		// P7: propietarios y encargados usan el rol ADMINISTRADOR_CANCHA (D07); nadie más tiene agenda
		if (solicitante == null || !solicitante.tieneRol(Rol.ADMINISTRADOR_CANCHA)) {
			throw new ReglaNegocioException("ERR-GEN-06");
		}
		UUID usuarioId = solicitante.usuarioId();

		// CA4: los suyos
		List<EscenarioAgenda> escenarios = new ArrayList<>();
		for (ResumenEscenario propio : repositorioEscenarios.listarPorAdministrador(usuarioId)) {
			escenarios.add(new EscenarioAgenda(propio.id(), propio.nombre(), propio.estado(), true));
		}
		// CA5: los asignados como encargado. D22 impide que sea encargado de uno propio; si pasara, queda como propietario
		Set<UUID> propios = escenarios.stream().map(EscenarioAgenda::id).collect(Collectors.toSet());
		List<UUID> asignados = repositorioEncargados.escenariosAsignados(usuarioId).stream()
				.filter(id -> !propios.contains(id)).toList();
		if (!asignados.isEmpty()) {
			for (ResumenEscenario asignado : repositorioEscenarios.listarPorIds(asignados)) {
				escenarios.add(new EscenarioAgenda(asignado.id(), asignado.nombre(), asignado.estado(), false));
			}
		}

		escenarios.sort(Comparator.comparing(EscenarioAgenda::nombre, String.CASE_INSENSITIVE_ORDER));
		return List.copyOf(escenarios);
	}

}
