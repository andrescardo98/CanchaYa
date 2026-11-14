package co.edu.uco.canchaya.escenarios.infrastructure.fotos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.edu.uco.canchaya.escenarios.application.port.out.AlmacenamientoFotos;

/**
 * Solo para desarrollo local: guarda las fotos en una carpeta del computador. No sirve en
 * Render, porque el disco del contenedor se borra al reiniciar (D13).
 */
class AlmacenamientoFotosLocal implements AlmacenamientoFotos {

	private static final Logger LOG = LoggerFactory.getLogger(AlmacenamientoFotosLocal.class);

	private final Path carpeta;
	private final String urlBase;

	AlmacenamientoFotosLocal(Path carpeta, String urlBase) {
		this.carpeta = carpeta;
		this.urlBase = urlBase.endsWith("/") ? urlBase : urlBase + "/";
	}

	@Override
	public String subir(String nombre, byte[] contenido, String tipoContenido) {
		try {
			Files.createDirectories(carpeta);
			Files.write(carpeta.resolve(nombre), contenido);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return urlBase + nombre;
	}

	@Override
	public void borrar(String url) {
		String nombre = url.substring(url.lastIndexOf('/') + 1);
		try {
			Files.deleteIfExists(carpeta.resolve(nombre));
		} catch (IOException e) {
			LOG.error("No se pudo borrar la foto local {}", nombre, e);
		}
	}

}
