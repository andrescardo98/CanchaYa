package co.edu.uco.canchaya.escenarios.application.port.out;

import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.TipoCancha;

public interface RepositorioTiposCancha {

	boolean existePorId(UUID id);

	/** Ordenados por código. */
	List<TipoCancha> listar();

}
