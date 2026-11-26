package co.edu.uco.canchaya.transversal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** AG-18 y AG-19: así se ven la fecha, la hora y el precio en EXI-RES-01 y en los correos (HU21, HU22). */
class FormatoTextoTest {

	// AG-19: separador de miles con punto, sin decimales. Venía de RespuestaReserva (HU21 CA1, P3 de HU22)
	@ParameterizedTest
	@CsvSource({ "80000.00, $ 80.000", "1500000.00, $ 1.500.000", "950.00, $ 950", "80000.50, $ 80.001" })
	void CA1_precioConFormato(BigDecimal precio, String esperado) {
		assertThat(FormatoTexto.precio(precio)).isEqualTo(esperado);
	}

	// AG-18: dd/mm/aaaa
	@Test
	void CA1_fechaConFormato() {
		assertThat(FormatoTexto.fecha(LocalDate.of(2026, 11, 3))).isEqualTo("03/11/2026");
	}

	// AG-18: hh:mm en 24 horas
	@Test
	void CA1_horaConFormato() {
		assertThat(FormatoTexto.hora(LocalTime.of(7, 0))).isEqualTo("07:00");
		assertThat(FormatoTexto.hora(LocalTime.of(18, 0))).isEqualTo("18:00");
	}

	// HU25: {fechaCancelacion} en dd/mm/aaaa hh:mm y en la hora de Bogotá (D27, AG-18), aunque llegue en UTC
	@Test
	void HU25_fechaHora_enBogota() {
		OffsetDateTime enBogota = OffsetDateTime.of(2026, 11, 20, 14, 5, 0, 0, ZoneOffset.ofHours(-5));
		OffsetDateTime enUtc = OffsetDateTime.of(2026, 11, 21, 2, 30, 0, 0, ZoneOffset.UTC);

		assertThat(FormatoTexto.fechaHora(enBogota)).isEqualTo("20/11/2026 14:05");
		assertThat(FormatoTexto.fechaHora(enUtc)).isEqualTo("20/11/2026 21:30");
	}

}
