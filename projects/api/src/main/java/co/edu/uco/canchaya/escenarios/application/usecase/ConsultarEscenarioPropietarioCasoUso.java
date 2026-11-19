package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarEscenarioPropietario;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

@Service
public class ConsultarEscenarioPropietarioCasoUso implements ConsultarEscenarioPropietario {

	private final RepositorioEscenarios repositorio;

	public ConsultarEscenarioPropietarioCasoUso(RepositorioEscenarios repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	public Escenario ejecutar(UUID escenarioId, Solicitante solicitante) {
		// Política 6
		Escenario escenario = repositorio.buscarPorId(escenarioId)
				.orElseThrow(() -> new ReglaNegocioException("ERR-ESC-06"));
		// Política 7: el formulario solo lo abre quien puede guardarlo
		escenario.validarPropietario(solicitante);
		return escenario;
	}

}
