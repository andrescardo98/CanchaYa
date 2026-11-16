package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ListarCanchasEscenario;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

@Service
public class ListarCanchasEscenarioCasoUso implements ListarCanchasEscenario {

	private final RepositorioEscenarios repositorioEscenarios;
	private final RepositorioCanchas repositorioCanchas;

	public ListarCanchasEscenarioCasoUso(RepositorioEscenarios repositorioEscenarios,
			RepositorioCanchas repositorioCanchas) {
		this.repositorioEscenarios = repositorioEscenarios;
		this.repositorioCanchas = repositorioCanchas;
	}

	@Override
	public List<Cancha> ejecutar(UUID escenarioId, Solicitante solicitante) {
		Escenario escenario = repositorioEscenarios.buscarPorId(escenarioId)
				.orElseThrow(() -> new ReglaNegocioException("ERR-CAN-01"));
		Cancha.validarPropietario(escenario, solicitante);

		return repositorioCanchas.listarPorEscenario(escenarioId);
	}

}
