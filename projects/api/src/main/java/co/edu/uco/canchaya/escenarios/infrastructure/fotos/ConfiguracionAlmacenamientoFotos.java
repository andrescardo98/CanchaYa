package co.edu.uco.canchaya.escenarios.infrastructure.fotos;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import co.edu.uco.canchaya.escenarios.application.port.out.AlmacenamientoFotos;

/**
 * Elige dónde se guardan las fotos con canchaya.fotos.almacenamiento: «supabase» en Render
 * (D31) o «local» en el computador del desarrollador (valor por defecto).
 */
@Configuration(proxyBeanMethods = false)
class ConfiguracionAlmacenamientoFotos {

	static final String PROPIEDAD = "canchaya.fotos.almacenamiento";

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(name = PROPIEDAD, havingValue = "supabase")
	static class Supabase {

		// Si falta alguno en Render, la aplicación no arranca, en vez de fallar al subir la primera foto
		@Bean
		AlmacenamientoFotos almacenamientoFotos(@Value("${canchaya.fotos.supabase.url}") String url,
				@Value("${canchaya.fotos.supabase.clave-servicio}") String claveServicio,
				@Value("${canchaya.fotos.supabase.bucket}") String bucket) {
			if (url.isBlank() || claveServicio.isBlank() || bucket.isBlank()) {
				throw new IllegalStateException(
						"Faltan SUPABASE_URL, SUPABASE_CLAVE_SERVICIO o SUPABASE_BUCKET_FOTOS para guardar las fotos");
			}
			return new AlmacenamientoFotosSupabase(RestClient.builder(), url, claveServicio, bucket);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(name = PROPIEDAD, havingValue = "local", matchIfMissing = true)
	static class Local implements WebMvcConfigurer {

		static final String RUTA = "/fotos-locales/";

		private final Path carpeta;

		Local(@Value("${canchaya.fotos.local.carpeta}") Path carpeta) {
			this.carpeta = carpeta.toAbsolutePath();
		}

		@Bean
		AlmacenamientoFotos almacenamientoFotos(@Value("${canchaya.fotos.local.url-base}") String urlBase) {
			return new AlmacenamientoFotosLocal(carpeta, urlBase + RUTA);
		}

		// El backend muestra las fotos de la carpeta, como lo haría el bucket público de Supabase
		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registro) {
			// Spring exige la barra final para tratar la ubicación como carpeta
			String ubicacion = carpeta.toUri().toString();
			registro.addResourceHandler(RUTA + "**")
					.addResourceLocations(ubicacion.endsWith("/") ? ubicacion : ubicacion + "/");
		}

	}

}
