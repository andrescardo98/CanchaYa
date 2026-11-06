package co.edu.uco.canchaya.transversal.domain;

import java.util.List;

/**
 * Excepción que lanza el dominio cuando una operación incumple una política del modelo de
 * dominio enriquecido. No lleva el texto que ve el usuario, solo el código del catálogo de
 * mensajes (por ejemplo, {@code ERR-USU-01}) y, si el mensaje los tiene, los valores de sus
 * parámetros ({@code {tipoDocumento}}, {@code {formatoEsperado}}…). El adaptador REST busca el
 * texto en el catálogo con ese código.
 */
public final class ReglaNegocioException extends RuntimeException {

	private final String codigoMensaje;
	private final List<Object> parametros;

	/**
	 * @param codigoMensaje código del catálogo de mensajes (ERR-&lt;OBJ&gt;-nn)
	 * @param parametros    valores de los parámetros del mensaje, en el orden del catálogo
	 * @throws IllegalArgumentException si el código viene vacío
	 */
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
