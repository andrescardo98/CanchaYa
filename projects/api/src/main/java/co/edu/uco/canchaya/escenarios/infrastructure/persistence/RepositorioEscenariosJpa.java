package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.domain.Escenario;

@Repository
class RepositorioEscenariosJpa implements RepositorioEscenarios {

	private final EscenarioJpa jpa;

	RepositorioEscenariosJpa(EscenarioJpa jpa) {
		this.jpa = jpa;
	}

	@Override
	public boolean existePorId(UUID id) {
		return jpa.existsById(id);
	}

	// El escenario y sus fotos entran juntos o no entra nada
	@Override
	@Transactional
	public void guardar(Escenario escenario) {
		jpa.saveAndFlush(EscenarioEntidad.desde(escenario));
	}

}
