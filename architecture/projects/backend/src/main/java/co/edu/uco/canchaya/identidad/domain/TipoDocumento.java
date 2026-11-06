package co.edu.uco.canchaya.identidad.domain;

import java.util.regex.Pattern;

/**
 * Tipos de documento admitidos, cada uno con el formato de su número (D12, PAR-USU-03 a
 * PAR-USU-08). Son reglas fijas del catálogo de parámetros («Configurable: No»), por eso
 * se escriben aquí y no en la configuración.
 */
public enum TipoDocumento {

	/** Cédula de ciudadanía: de 6 a 10 dígitos (PAR-USU-04). */
	CC("^[0-9]{6,10}$", "de 6 a 10 dígitos"),

	/** Cédula de extranjería: de 6 a 10 caracteres alfanuméricos (PAR-USU-05). */
	CE("^[A-Za-z0-9]{6,10}$", "de 6 a 10 caracteres alfanuméricos"),

	/** Tarjeta de identidad: 10 u 11 dígitos (PAR-USU-06). Solo con rol JUGADOR. */
	TI("^[0-9]{10,11}$", "10 u 11 dígitos"),

	/** Pasaporte: de 6 a 12 caracteres alfanuméricos (PAR-USU-07). */
	PASAPORTE("^[A-Za-z0-9]{6,12}$", "de 6 a 12 caracteres alfanuméricos"),

	/** NIT: 9 dígitos, guion y dígito de verificación, por ejemplo 900123456-7 (PAR-USU-08). */
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

	/** Descripción del formato para el parámetro {formatoEsperado} de ERR-USU-13. */
	public String getFormatoEsperado() {
		return formatoEsperado;
	}

	/** Indica si el número de documento cumple el formato de este tipo (D12). */
	public boolean validarNumero(String numeroDocumento) {
		return numeroDocumento != null && patron.matcher(numeroDocumento).matches();
	}

}
