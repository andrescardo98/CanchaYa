package co.edu.uco.canchaya.identidad.domain;

import java.util.regex.Pattern;

/**
 * Tipos de documento y el formato de su número (D12, PAR-USU-04 a PAR-USU-08). Van fijos en el
 * código porque el catálogo los marca como no configurables.
 */
public enum TipoDocumento {

	CC("^[0-9]{6,10}$", "de 6 a 10 dígitos"),

	CE("^[A-Za-z0-9]{6,10}$", "de 6 a 10 caracteres alfanuméricos"),

	TI("^[0-9]{10,11}$", "10 u 11 dígitos"),

	PASAPORTE("^[A-Za-z0-9]{6,12}$", "de 6 a 12 caracteres alfanuméricos"),

	NIT("^[0-9]{9}-[0-9]$", "9 dígitos, guion y dígito de verificación (ejemplo: 900123456-7)");

	private final Pattern patron;
	private final String formatoEsperado;

	TipoDocumento(String patron, String formatoEsperado) {
		this.patron = Pattern.compile(patron);
		this.formatoEsperado = formatoEsperado;
	}

	public Pattern getPatron() {
		return patron;
	}

	/** Texto para el parámetro {formatoEsperado} de ERR-USU-13. */
	public String getFormatoEsperado() {
		return formatoEsperado;
	}

	public boolean validarNumero(String numeroDocumento) {
		return numeroDocumento != null && patron.matcher(numeroDocumento).matches();
	}

}
