package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * Formatos de foto que se pueden admitir (PAR-ESC-03). Se reconocen por los primeros bytes
 * del archivo y no por la extensión, que el usuario puede cambiar con solo renombrarlo.
 */
enum FormatoFoto {

	JPG("jpg", "image/jpeg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }),
	PNG("png", "image/png", new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' });

	private final String extension;
	private final String tipoContenido;
	private final byte[] firma;

	FormatoFoto(String extension, String tipoContenido, byte[] firma) {
		this.extension = extension;
		this.tipoContenido = tipoContenido;
		this.firma = firma;
	}

	/** El formato del archivo, si es uno de los admitidos. */
	static Optional<FormatoFoto> detectar(byte[] contenido, Collection<FormatoFoto> admitidos) {
		return admitidos.stream().filter(formato -> formato.coincide(contenido)).findFirst();
	}

	private boolean coincide(byte[] contenido) {
		return contenido != null && contenido.length >= firma.length
				&& Arrays.equals(contenido, 0, firma.length, firma, 0, firma.length);
	}

	String getExtension() {
		return extension;
	}

	String getTipoContenido() {
		return tipoContenido;
	}

}
