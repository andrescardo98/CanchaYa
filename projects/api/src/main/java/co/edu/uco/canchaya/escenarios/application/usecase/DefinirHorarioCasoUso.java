package co.edu.uco.canchaya.escenarios.application.usecase;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.DefinirHorario;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioHorarios;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

@Service
public class DefinirHorarioCasoUso implements DefinirHorario {

	private static final Logger LOG = LoggerFactory.getLogger(DefinirHorarioCasoUso.class);

	private final RepositorioEscenarios escenarios;
	private final RepositorioHorarios horarios;

	public DefinirHorarioCasoUso(RepositorioEscenarios escenarios, RepositorioHorarios horarios) {
		this.escenarios = escenarios;
		this.horarios = horarios;
	}

	@Override
	public HorarioRecurrente ejecutar(UUID escenarioId, DiaSemana diaSemana, LocalTime horaInicio,
			LocalTime horaFin, Solicitante solicitante) {
		// Política 1. Va antes que la 2 porque para saber quién es el dueño hay que tener el escenario
		Escenario escenario = escenarios.buscarPorId(escenarioId)
				.orElseThrow(() -> new ReglaNegocioException("ERR-HOR-01"));

		List<HorarioRecurrente> delMismoDia = horarios.buscarPorEscenarioYDia(escenarioId, diaSemana);
		HorarioRecurrente horario = HorarioRecurrente.definir(escenario, diaSemana, horaInicio, horaFin,
				delMismoDia, solicitante);

		// Política 5. ERR-HOR-05 solo va al log; el usuario no lo ve
		while (horarios.existePorId(horario.getId())) {
			LOG.warn("ERR-HOR-05 objeto=HorarioRecurrente id={}", horario.getId());
			horario = horario.conNuevoId();
		}

		horarios.guardar(horario);
		return horario;
	}

}
