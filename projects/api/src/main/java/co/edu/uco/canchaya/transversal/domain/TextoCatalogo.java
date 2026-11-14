package co.edu.uco.canchaya.transversal.domain;

import java.util.List;

/**
 * Parámetro de un mensaje que también sale del catálogo. Por ejemplo, {reglaIncumplida} de
 * ERR-ESC-13. Así el dominio no escribe textos (regla 6 del AGENTS.md).
 */
public record TextoCatalogo(String clave, List<Object> parametros) {

	public TextoCatalogo(String clave, Object... parametros) {
		this(clave, List.of(parametros));
	}

}
