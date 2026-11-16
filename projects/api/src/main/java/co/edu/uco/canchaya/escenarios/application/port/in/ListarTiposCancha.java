package co.edu.uco.canchaya.escenarios.application.port.in;

import java.util.List;

import co.edu.uco.canchaya.escenarios.domain.TipoCancha;

// HU6, CA10: lista de solo lectura para elegir el tipo de la cancha
public interface ListarTiposCancha {

	/** @return los tipos del catálogo ordenados por código */
	List<TipoCancha> ejecutar();

}
