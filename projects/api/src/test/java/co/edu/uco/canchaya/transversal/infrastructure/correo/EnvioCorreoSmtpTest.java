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

	// HU22: los parámetros en el orden de la hoja «Plantillas de correo»
	private static Correo corRes01(Object avisoConfirmacion) {
		return new Correo("COR-RES-01", "juan@correo.com", List.of("Canchas La 70", "20/11/2026", "18:00"),
				List.of("Juan Gómez", "Canchas La 70", "Calle 70 # 45-10", "Rionegro", "Cancha 1", "20/11/2026",
						"18:00", "20:00", "$ 160.000", avisoConfirmacion));
	}

	@Test
	void CA1_armaCorRes01() throws Exception {
		ServidorEnMemoria servidor = new ServidorEnMemoria();
		EnvioCorreoSmtp envio = new EnvioCorreoSmtp(servidor, new CatalogoMensajes(), REMITENTE);

		envio.enviar(corRes01(new TextoCatalogo("COR-RES-01.avisoConfirmacion")));
		envio.enviar(corRes01(""));

		MimeMessage conAviso = servidor.enviados.get(0);
		assertThat(conAviso.getAllRecipients()).extracting(Object::toString).containsExactly("juan@correo.com");
		assertThat(conAviso.getSubject()).isEqualTo("Reserva confirmada: Canchas La 70, 20/11/2026 a las 18:00");
		assertThat((String) conAviso.getContent())
				.startsWith("Hola, Juan Gómez:<br><br>Su reserva quedó confirmada.")
				.contains("Escenario: Canchas La 70<br>Dirección: Calle 70 # 45-10, Rionegro<br>Cancha: Cancha 1")
				.contains("Fecha: 20/11/2026<br>Hora: 18:00 a 20:00<br>Valor: $ 160.000")
				.contains("Le enviaremos un correo 5 horas antes del partido para que confirme su asistencia");
		// D33: con PAR-RES-12 en «No» el aviso va vacío (P5: queda la línea en blanco de la plantilla)
		assertThat((String) servidor.enviados.get(1).getContent())
				.doesNotContain("confirme su asistencia")
				.contains("Valor: $ 160.000<br><br><br><br>Si no puede asistir");
	}

	@Test
	void CA2_armaCorRes02() throws Exception {
		ServidorEnMemoria servidor = new ServidorEnMemoria();

		new EnvioCorreoSmtp(servidor, new CatalogoMensajes(), REMITENTE).enviar(new Correo("COR-RES-02",
				"ana@correo.com", List.of("Canchas La 70", "Cancha 1", "20/11/2026", "18:00"),
				List.of("Ana Pérez", "Cancha 1", "20/11/2026", "18:00", "20:00", "Juan Gómez", "+573001234567")));

		MimeMessage mensaje = servidor.enviados.getFirst();
		assertThat(mensaje.getSubject()).isEqualTo("Nueva reserva en Canchas La 70: Cancha 1, 20/11/2026 a las 18:00");
		assertThat((String) mensaje.getContent())
				.startsWith("Hola, Ana Pérez:<br><br>Se registró una nueva reserva en su escenario.")
				.contains("Cancha: Cancha 1<br>Fecha: 20/11/2026<br>Hora: 18:00 a 20:00")
				.contains("Jugador: Juan Gómez<br>Celular: +573001234567");
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
