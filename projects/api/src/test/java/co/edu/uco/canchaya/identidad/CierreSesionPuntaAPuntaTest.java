package co.edu.uco.canchaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import co.edu.uco.canchaya.TestcontainersConfiguration;

/**
 * HU4: la parte del backend en el cierre de sesión.
 *
 * <p>Con JWT el servidor no guarda sesiones (D37): cerrar sesión es borrar el token en el
 * dispositivo. Aquí se revisa que, sin ese token, el backend no entregue datos y que el navegador
 * no guarde copia de las páginas protegidas para mostrarlas con el botón Atrás.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class CierreSesionPuntaAPuntaTest {

	// Una ruta que exige sesión. Todavía no tiene controlador, pero la seguridad responde antes.
	private static final String RUTA_PROTEGIDA = "/api/ruta-sin-controlador";

	@Autowired
	private RestTestClient cliente;

	@Test
	void ca2_despuesDeCerrarSesion_laRutaProtegidaRespondeErrGen05() {
		String token = iniciarSesion("198.51.100.20");
		// Con sesión, la seguridad deja pasar (404 porque la ruta no existe)
		cliente.get().uri(RUTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange()
				.expectStatus().isNotFound();

		// Cerró sesión: el frontend borró el token y la petición llega sin él
		cliente.get().uri(RUTA_PROTEGIDA).exchange()
				.expectStatus().isUnauthorized()
				.expectBody()
				.jsonPath("$.errores[0].codigo").isEqualTo("ERR-GEN-05")
				.jsonPath("$.errores[0].mensaje").isEqualTo("Su sesión no está iniciada o venció. Inicie sesión para continuar.");
	}

	@Test
	void ca2_laRespuestaConSesion_pideAlNavegadorNoGuardarCopia() {
		String token = iniciarSesion("198.51.100.21");

		// Sin copia guardada, el botón Atrás vuelve a pedir la página y recibe ERR-GEN-05
		String cache = cliente.get().uri(RUTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.exchange()
				.returnResult(String.class).getResponseHeaders().getCacheControl();
		assertThat(cache).contains("no-store");
	}

	// Usa el administrador del sistema que crea la V2; cada prueba llega desde su propia IP (D28)
	private String iniciarSesion(String ip) {
		return cliente.post().uri("/api/sesiones")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Forwarded-For", ip)
				.body("""
						{"correoElectronico": "admin@canchaya.local", "contrasena": "Admin12345"}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody(RespuestaToken.class).returnResult().getResponseBody().token();
	}

	private record RespuestaToken(String token) {
	}

}
