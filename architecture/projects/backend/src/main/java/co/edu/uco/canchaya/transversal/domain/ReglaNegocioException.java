package co.edu.uco.canchaya.transversal.domain;

import java.util.List;

/**
 * Lleva el código del catálogo de mensajes (por ejemplo, ERR-USU-01) y los valores de sus
 * parámetros, no el texto: el adaptador REST busca el texto en el catálogo.
 */
public final class ReglaNegocioException extends RuntimeException {

	private final String codigoMensaje;
	private final List<Object> parametros;

	public ReglaNegocioException(String codigoMensaje, Object... parametros) {
		super(codigoMensaje);
		if (codigoMensaje == null || codigoMensaje.isBlank()) {
			throw new IllegalArgumentException("El código del mensaje es obligatorio");
		}
		this.codigoMensaje = codigoMensaje;
		this.parametros = List.of(parametros);
	}

	public String getCodigoMensaje() {
		return codigoMensaje;
	}

	public List<Object> getParametros() {
		return parametros;
	}

}
