package co.edu.uco.canchaya.escenarios.application.port.out;

import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Escenario;

public interface RepositorioEscenarios {

	boolean existePorId(UUID id);

	/** Guarda el escenario con sus fotos en una sola transacción. */
	void guardar(Escenario escenario);

}
