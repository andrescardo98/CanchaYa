package co.edu.uco.canchaya.transversal.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Cómo se ven una fecha, una hora y un precio dentro de un texto del catálogo (AG-18, AG-19). Lo
 * usan la respuesta de la reserva (EXI-RES-01, HU21) y sus correos (COR-RES-01 y COR-RES-02,
 * HU22), para que muestren lo mismo (P3 de HU22). Java puro: lo puede usar cualquier capa.
 */
public final class FormatoTexto {

	// AG-18
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

	private FormatoTexto() {
	}

	/** dd/mm/aaaa, por ejemplo 20/11/2026. */
	public static String fecha(LocalDate fecha) {
		return fecha.format(FECHA);
	}

	/** hh:mm en 24 horas, por ejemplo 18:00. */
	public static String hora(LocalTime hora) {
		return hora.format(HORA);
	}

	/** AG-19: pesos con signo y punto de miles, sin decimales. Por ejemplo, $ 160.000. */
	public static String precio(BigDecimal precio) {
		// DecimalFormat no es seguro entre hilos: se crea uno en cada llamada
		DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
		simbolos.setGroupingSeparator('.');
		DecimalFormat formato = new DecimalFormat("#,##0", simbolos);
		formato.setRoundingMode(RoundingMode.HALF_UP);
		return "$ " + formato.format(precio);
	}

}
