package co.edu.uco.canchaya.escenarios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** HU20: la franja es un bloque de una hora en punto (PAR-HOR-01, D10). */
class FranjaTest {

	private static final LocalDate FECHA = LocalDate.of(2026, 11, 23);

	private static LocalTime hora(String texto) {
		return LocalTime.parse(texto);
	}

	@Test
	void ca1UnaFranjaDeUnaHoraEnPuntoSeCrea() {
		Franja franja = new Franja(FECHA, hora("18:00"), hora("19:00"));

		assertThat(franja.fecha()).isEqualTo(FECHA);
		assertThat(franja.horaInicio()).isEqualTo(hora("18:00"));
		assertThat(franja.horaFin()).isEqualTo(hora("19:00"));
		assertThat(franja.inicio()).isEqualTo(LocalDateTime.of(2026, 11, 23, 18, 0));
	}

	// PAR-HOR-01 y PAR-HOR-02: una hora exacta, en punto. 23:00–00:00 no cabe en un horario (fin > inicio)
	@ParameterizedTest
	@CsvSource({ "18:30, 19:30", "18:00, 20:00", "18:00, 18:30", "19:00, 18:00", "18:00, 18:00", "23:00, 00:00",
			"18:00:30, 19:00:30" })
	void ca1UnaFranjaQueNoEsDeUnaHoraEnPuntoSeRechaza(String inicio, String fin) {
		assertThatThrownBy(() -> new Franja(FECHA, hora(inicio), hora(fin)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void ca1SinFechaNiHorasSeRechaza() {
		assertThatThrownBy(() -> new Franja(null, hora("18:00"), hora("19:00")))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new Franja(FECHA, null, hora("19:00"))).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new Franja(FECHA, hora("18:00"), null)).isInstanceOf(NullPointerException.class);
	}

	// CA2: una reserva o un bloqueo ocupa la franja si se cruzan; el fin es abierto, como el tsrange '[)'
	@ParameterizedTest
	@CsvSource({ "18:00, 19:00, true", "17:00, 19:00, true", "18:00, 20:00, true", "16:00, 22:00, true",
			"17:00, 18:00, false", "19:00, 20:00, false", "08:00, 10:00, false" })
	void ca2SeCruzaConUnRangoDeLaMismaFecha(String inicio, String fin, boolean seCruza) {
		Franja franja = new Franja(FECHA, hora("18:00"), hora("19:00"));

		assertThat(franja.seCruzaCon(FECHA, hora(inicio), hora(fin))).isEqualTo(seCruza);
	}

	@Test
	void ca2UnRangoDeOtraFechaNoLaOcupa() {
		Franja franja = new Franja(FECHA, hora("18:00"), hora("19:00"));

		assertThat(franja.seCruzaCon(FECHA.plusDays(1), hora("18:00"), hora("19:00"))).isFalse();
	}

}
