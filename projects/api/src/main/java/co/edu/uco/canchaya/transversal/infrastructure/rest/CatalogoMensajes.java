package co.edu.uco.canchaya.transversal.infrastructure.rest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

@Component
public class CatalogoMensajes {

	private static final String ARCHIVO = "catalogo-mensajes.properties";
	private static final Pattern PARAMETRO = Pattern.compile("\\{[^}]+\\}");

	private final Properties textos = new Properties();

	public CatalogoMensajes() {
		// Se lee en UTF-8 porque Properties.load por defecto daña las tildes
		try (Reader lector = new InputStreamReader(new ClassPathResource(ARCHIVO).getInputStream(),
				StandardCharsets.UTF_8)) {
			textos.load(lector);
		} catch (IOException e) {
			throw new UncheckedIOException("No se pudo leer " + ARCHIVO, e);
		}
	}

	public String texto(String clave) {
		return texto(clave, List.of());
	}

	// Los parámetros se reemplazan en el orden en que aparecen en el texto
	public String texto(String clave, List<?> parametros) {
		String plantilla = textos.getProperty(clave);
		if (plantilla == null) {
			throw new IllegalStateException("El catálogo no tiene el mensaje " + clave);
		}
		Iterator<?> valores = parametros.iterator();
		return PARAMETRO.matcher(plantilla).replaceAll(parametro -> Matcher.quoteReplacement(
				valores.hasNext() ? valor(valores.next()) : parametro.group()));
	}

	// Un parámetro TextoCatalogo es otro texto del catálogo, por ejemplo {reglaIncumplida} de ERR-ESC-13
	private String valor(Object parametro) {
		if (parametro instanceof TextoCatalogo anidado) {
			return texto(anidado.clave(), anidado.parametros());
		}
		return String.valueOf(parametro);
	}

	public String nombreCampo(String atributo) {
		return texto("campo." + atributo);
	}

	public String formatoEsperado(String atributo) {
		return texto("formato." + atributo);
	}

}
