package co.edu.uco.canchaya.escenarios.infrastructure.fotos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AlmacenamientoFotosTest {

	private static final byte[] FOTO = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1 };

	@Nested
	class Supabase {

		private static final String PROYECTO = "https://proyecto.supabase.co";
		private static final String OBJETO = PROYECTO + "/storage/v1/object/fotos-escenarios/abc.jpg";

		private final RestClient.Builder builder = RestClient.builder();
		private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
		private final AlmacenamientoFotosSupabase almacenamiento =
				new AlmacenamientoFotosSupabase(builder, PROYECTO + "/", "clave-servicio", "fotos-escenarios");

		@Test
		void subeConLaClaveDeServicioYDevuelveLaDireccionPublica() {
			servidor.expect(requestTo(OBJETO))
					.andExpect(method(HttpMethod.POST))
					.andExpect(header("Authorization", "Bearer clave-servicio"))
					.andExpect(header("apikey", "clave-servicio"))
					.andExpect(header("Content-Type", "image/jpeg"))
					.andExpect(content().bytes(FOTO))
					.andRespond(withSuccess());

			String url = almacenamiento.subir("abc.jpg", FOTO, "image/jpeg");

			assertThat(url).isEqualTo(PROYECTO + "/storage/v1/object/public/fotos-escenarios/abc.jpg");
			servidor.verify();
		}

		@Test
		void borraLaFotoPorSuNombre() {
			servidor.expect(requestTo(OBJETO)).andExpect(method(HttpMethod.DELETE)).andRespond(withSuccess());

			almacenamiento.borrar(PROYECTO + "/storage/v1/object/public/fotos-escenarios/abc.jpg");

			servidor.verify();
		}

		@Test
		void siBorrarFallaNoLanzaError() {
			servidor.expect(requestTo(OBJETO)).andRespond(withServerError());

			assertThatCode(() -> almacenamiento.borrar(OBJETO)).doesNotThrowAnyException();
		}

	}

	@Nested
	class Local {

		@TempDir
		Path carpeta;

		@Test
		void guardaYBorraLaFotoEnLaCarpeta() {
			AlmacenamientoFotosLocal almacenamiento = new AlmacenamientoFotosLocal(carpeta,
					"http://localhost:8080/fotos-locales");

			String url = almacenamiento.subir("abc.jpg", FOTO, "image/jpeg");

			assertThat(url).isEqualTo("http://localhost:8080/fotos-locales/abc.jpg");
			assertThat(carpeta.resolve("abc.jpg")).hasBinaryContent(FOTO);

			almacenamiento.borrar(url);
			assertThat(Files.exists(carpeta.resolve("abc.jpg"))).isFalse();
		}

	}

}
