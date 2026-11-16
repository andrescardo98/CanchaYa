package co.edu.uco.canchaya.transversal.infrastructure.correo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;

import co.edu.uco.canchaya.TestcontainersConfiguration;

/**
 * D15: una falla del correo no tumba la aplicación. Si el servidor de correo no responde, la
 * verificación de estado sigue en UP; si no, Render reiniciaría el contenedor y UptimeRobot avisaría.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"spring.mail.host=localhost", "spring.mail.port=1" }) // nadie escucha en el puerto 1
@AutoConfigureRestTestClient
class SaludSinServidorCorreoTest {

	@Autowired
	private RestTestClient cliente;

	@Test
	void laAplicacionSigueEnPieAunqueElCorreoNoResponda() {
		cliente.get().uri("/actuator/health").exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.status").isEqualTo("UP");
	}

}
