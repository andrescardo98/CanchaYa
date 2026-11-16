package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioTiposCancha;
import co.edu.uco.canchaya.escenarios.domain.TipoCancha;

@Repository
class RepositorioTiposCanchaJpa implements RepositorioTiposCancha {

	private final TipoCanchaJpa jpa;

	RepositorioTiposCanchaJpa(TipoCanchaJpa jpa) {
		this.jpa = jpa;
	}

	@Override
	public boolean existePorId(UUID id) {
		return jpa.existsById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TipoCancha> listar() {
		return jpa.findAllByOrderByCodigoAsc().stream().map(TipoCanchaEntidad::aDominio).toList();
	}

}
