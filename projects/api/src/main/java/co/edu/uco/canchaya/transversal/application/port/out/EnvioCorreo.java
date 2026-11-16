package co.edu.uco.canchaya.transversal.application.port.out;

import java.util.List;

/**
 * Enviar un correo con una plantilla del catálogo de mensajes (COR-*). Lo usan Escenarios y
 * Reservas; el caso de uso no sabe qué servidor de correo hay detrás.
 */
public interface EnvioCorreo {

	/**
	 * Se llama después de guardar la operación. Nunca lanza error: si el envío falla, lo
	 * registra en el log con ERR-GEN-08 y no lo reintenta (D15, PAR-GEN-02), así la operación
	 * que ya quedó guardada no se revierte.
	 */
	void enviar(Correo correo);

	/**
	 * @param codigo            código de la plantilla, por ejemplo COR-ESC-01
	 * @param parametrosAsunto  valores de los parámetros del asunto, en el orden en que aparecen
	 * @param parametrosCuerpo  valores de los parámetros del cuerpo, en el orden en que aparecen
	 */
	record Correo(String codigo, String destinatario, List<Object> parametrosAsunto,
			List<Object> parametrosCuerpo) {

		public Correo {
			parametrosAsunto = List.copyOf(parametrosAsunto);
			parametrosCuerpo = List.copyOf(parametrosCuerpo);
		}

	}

}
