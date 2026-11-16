package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ListarTiposCancha;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioTiposCancha;
import co.edu.uco.canchaya.escenarios.domain.TipoCancha;

@Service
public class ListarTiposCanchaCasoUso implements ListarTiposCancha {

	private final RepositorioTiposCancha repositorio;

	public ListarTiposCanchaCasoUso(RepositorioTiposCancha repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	public List<TipoCancha> ejecutar() {
		return repositorio.listar();
	}

}
