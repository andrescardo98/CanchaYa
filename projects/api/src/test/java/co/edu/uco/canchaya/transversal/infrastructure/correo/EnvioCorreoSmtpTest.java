package co.edu.uco.canchaya.transversal.infrastructure.correo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import co.edu.uco.canchaya.transversal.application.port.out.EnvioCorreo.Correo;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/** HU13, CA8: cómo se arma COR-ESC-01 y qué pasa si el servidor de correo falla (D15). */
@ExtendWith(OutputCaptureExtension.class)
class EnvioCorreoSmtpTest {

	private static final String REMITENTE = "CanchaYa <no-responder@canchaya.local>";

	private static Correo corEsc01(String nombreEscenario) {
		return new Correo("COR-ESC-01", "ana@correo.com",
				List.of(nombreEscenario, new TextoCatalogo("estado.ACTIVO")),
				List.of("Ana Pérez", nombreEscenario, new TextoCatalogo("estado.ACTIVO"), "13/11/2026 15:30",
						new TextoCatalogo("COR-ESC-01.textoSegunEstado.ACTIVO")));
	}

	@Test
	void ca8ArmaElCorreoConLaPlantillaDelCatalogo() throws Exception {
		ServidorEnMemoria servidor = new ServidorEnMemoria();

		new EnvioCorreoSmtp(servidor, new CatalogoMensajes(), REMITENTE).enviar(corEsc01("Cancha La 80"));

		MimeMessage mensaje = servidor.enviados.getFirst();
		mensaje.saveChanges();
		assertThat(((InternetAddress) mensaje.getFrom()[0]).getAddress()).isEqualTo("no-responder@canchaya.local");
		assertThat(mensaje.getAllRecipients()).extracting(Object::toString).containsExactly("ana@correo.com");
		assertThat(mensaje.getSubject()).isEqualTo("Su escenario Cancha La 80 ahora está Activo");
		assertThat((String) mensaje.getContent())
				.startsWith("Hola, Ana Pérez:<br><br>")
				.contains("cambió el estado de su escenario Cancha La 80 a Activo el 13/11/2026 15:30.")
				.contains("ya aparecen en la búsqueda de los jugadores.");
	}

	@Test
	void ca8UnNombreConEtiquetasNoCambiaElHtmlDelCorreo() throws Exception {
		ServidorEnMemoria servidor = new ServidorEnMemoria();

		new EnvioCorreoSmtp(servidor, new CatalogoMensajes(), REMITENTE).enviar(corEsc01("<b>Golazo</b>"));

		assertThat((String) servidor.enviados.getFirst().getContent())
				.contains("&lt;b&gt;Golazo&lt;/b&gt;").doesNotContain("<b>");
	}

	@Test
	void ca8SiElServidorNoRespondeSeRegistraErrGen08SinLanzarError(CapturedOutput salida) {
		// Puerto 1: nadie escucha ahí, así que la conexión falla de verdad
		JavaMailSenderImpl servidor = new JavaMailSenderImpl();
		servidor.setHost("localhost");
		servidor.setPort(1);

		assertThatCode(() -> new EnvioCorreoSmtp(servidor, new CatalogoMensajes(), REMITENTE)
				.enviar(corEsc01("Cancha La 80")))
				.doesNotThrowAnyException();
		assertThat(salida.getAll())
				.contains("ERR-GEN-08 No se pudo enviar el correo COR-ESC-01 a ana@correo.com")
				.contains("La operación no se revierte y no se reintenta.");
	}

	@Test
	void ca8NoSeReintenta() {
		ServidorEnMemoria servidor = new ServidorEnMemoria();
		servidor.fallar = true;

		new EnvioCorreoSmtp(servidor, new CatalogoMensajes(), REMITENTE).enviar(corEsc01("Cancha La 80"));

		assertThat(servidor.intentos).isEqualTo(1); // PAR-GEN-02 = 0 reintentos
	}

	private static final class ServidorEnMemoria extends JavaMailSenderImpl {

		final List<MimeMessage> enviados = new ArrayList<>();
		boolean fallar;
		int intentos;

		@Override
		public void send(MimeMessage mensaje) {
			intentos++;
			if (fallar) {
				throw new MailSendException("servidor caído");
			}
			enviados.add(mensaje);
		}

	}

}
