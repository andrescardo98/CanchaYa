package co.edu.uco.canchaya.escenarios.infrastructure.fotos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import co.edu.uco.canchaya.escenarios.application.port.out.AlmacenamientoFotos;

/**
 * Supabase Storage por su interfaz HTTP (D31). Solo el backend tiene la clave de servicio:
 * el navegador ve las fotos porque el bucket es público, pero no puede subir ni borrar (RT-10).
 */
class AlmacenamientoFotosSupabase implements AlmacenamientoFotos {

	private static final Logger LOG = LoggerFactory.getLogger(AlmacenamientoFotosSupabase.class);

	private final RestClient cliente;
	private final String urlProyecto;
	private final String bucket;

	AlmacenamientoFotosSupabase(RestClient.Builder builder, String urlProyecto, String claveServicio, String bucket) {
		this.urlProyecto = sinBarraFinal(urlProyecto);
		this.bucket = bucket;
		this.cliente = builder
				.baseUrl(this.urlProyecto + "/storage/v1/object/" + bucket + "/")
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + claveServicio)
				.defaultHeader("apikey", claveServicio)
				.build();
	}

	@Override
	public String subir(String nombre, byte[] contenido, String tipoContenido) {
		cliente.post()
				.uri(nombre)
				.contentType(MediaType.parseMediaType(tipoContenido))
				.body(contenido)
				.retrieve()
				.toBodilessEntity();
		return urlProyecto + "/storage/v1/object/public/" + bucket + "/" + nombre;
	}

	@Override
	public void borrar(String url) {
		String nombre = url.substring(url.lastIndexOf('/') + 1);
		try {
			cliente.delete().uri(nombre).retrieve().toBodilessEntity();
		} catch (RestClientException e) {
			// Es limpieza: si falla queda una foto sin escenario, pero no se daña ningún dato
			LOG.error("No se pudo borrar la foto {} de Supabase Storage", nombre, e);
		}
	}

	private static String sinBarraFinal(String url) {
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

}
