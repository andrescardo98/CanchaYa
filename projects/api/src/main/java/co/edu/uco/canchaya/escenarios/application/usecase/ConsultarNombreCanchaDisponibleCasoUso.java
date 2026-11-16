package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarNombreCanchaDisponible;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

@Service
public class ConsultarNombreCanchaDisponibleCasoUso implements ConsultarNombreCanchaDisponible {

	private final RepositorioEscenarios repositorioEscenarios;
	private final RepositorioCanchas repositorioCanchas;

	public ConsultarNombreCanchaDisponibleCasoUso(RepositorioEscenarios repositorioEscenarios,
			RepositorioCanchas repositorioCanchas) {
		this.repositorioEscenarios = repositorioEscenarios;
		this.repositorioCanchas = repositorioCanchas;
	}

	@Override
	public boolean ejecutar(UUID escenarioId, String nombre, Solicitante solicitante) {
		Escenario escenario = repositorioEscenarios.buscarPorId(escenarioId)
				.orElseThrow(() -> new ReglaNegocioException("ERR-CAN-01"));
		// Así nadie averigua los nombres de las canchas de otro escenario
		Cancha.validarPropietario(escenario, solicitante);

		return !repositorioCanchas.existeNombreEnEscenario(escenarioId, nombre.strip());
	}

}
