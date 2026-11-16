package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ListarHorarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioHorarios;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

@Service
public class ListarHorariosCasoUso implements ListarHorarios {

	private final RepositorioEscenarios escenarios;
	private final RepositorioHorarios horarios;

	public ListarHorariosCasoUso(RepositorioEscenarios escenarios, RepositorioHorarios horarios) {
		this.escenarios = escenarios;
		this.horarios = horarios;
	}

	// Mismas reglas de acceso que al definir: el escenario debe existir y ser del solicitante
	@Override
	public List<HorarioRecurrente> ejecutar(UUID escenarioId, Solicitante solicitante) {
		Escenario escenario = escenarios.buscarPorId(escenarioId)
				.orElseThrow(() -> new ReglaNegocioException("ERR-HOR-01"));
		HorarioRecurrente.validarPropietario(escenario, solicitante);
		return horarios.buscarPorEscenario(escenarioId);
	}

}
