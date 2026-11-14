package co.edu.uco.canchaya.transversal.infrastructure.correo;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import co.edu.uco.canchaya.transversal.application.port.out.EnvioCorreo;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Arma el correo con la plantilla del catálogo (COR-*.asunto y COR-*.cuerpo) y lo envía por
 * SMTP. Si algo falla, lo deja en el log con ERR-GEN-08 y sigue: sin reintentos y sin lanzar
 * el error, para que la operación ya guardada no se revierta (D15, PAR-GEN-02, ESC-DIS-02).
 */
@Component
class EnvioCorreoSmtp implements EnvioCorreo {

	private static final Logger LOG = LoggerFactory.getLogger(EnvioCorreoSmtp.class);

	private final JavaMailSender servidor;
	private final CatalogoMensajes catalogo;
	private final String remitente;

	EnvioCorreoSmtp(JavaMailSender servidor, CatalogoMensajes catalogo,
			@Value("${canchaya.correo.remitente}") String remitente) {
		this.servidor = servidor;
		this.catalogo = catalogo;
		this.remitente = remitente;
	}

	@Override
	public void enviar(Correo correo) {
		try {
			MimeMessage mensaje = servidor.createMimeMessage();
			MimeMessageHelper datos = new MimeMessageHelper(mensaje, "UTF-8");
			datos.setFrom(remitente);
			datos.setTo(correo.destinatario());
			datos.setSubject(catalogo.texto(correo.codigo() + ".asunto", correo.parametrosAsunto()));
			datos.setText(catalogo.texto(correo.codigo() + ".cuerpo", seguros(correo.parametrosCuerpo())), true);
			servidor.send(mensaje);
		} catch (MessagingException | RuntimeException e) {
			LOG.error("ERR-GEN-08 {}", catalogo.texto("ERR-GEN-08",
					List.of(correo.codigo(), correo.destinatario(), String.valueOf(e.getMessage()))));
		}
	}

	// El cuerpo es HTML: un nombre como «<b>Golazo</b>» se muestra tal cual y no cambia el correo
	private static List<Object> seguros(List<Object> parametros) {
		return parametros.stream()
				.map(valor -> valor instanceof String texto ? (Object) HtmlUtils.htmlEscape(texto, "UTF-8") : valor)
				.toList();
	}

}
