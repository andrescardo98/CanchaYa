package co.edu.uco.canchaya.reservas.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** HU29: qué días trae la agenda en cada vista (CA1 por día, CA2 por semana, P5). */
class VistaAgendaTest {

	private static final LocalDate LUNES = LocalDate.of(2026, 11, 23);
	private static final LocalDate DOMINGO = LocalDate.of(2026, 11, 29);

	// CA1: la vista por día es solo esa fecha
	@ParameterizedTest
	@ValueSource(ints = { 0, 2, 6 })
	void CA1_dia_esLaMismaFecha(int dias) {
		LocalDate fecha = LUNES.plusDays(dias);
		assertThat(VistaAgenda.DIA.desde(fecha)).isEqualTo(fecha);
		assertThat(VistaAgenda.DIA.hasta(fecha)).isEqualTo(fecha);
	}

	// CA2 y P5: la semana va de lunes a domingo de la semana de la fecha, sea cual sea el día elegido
	@ParameterizedTest
	@ValueSource(ints = { 0, 2, 6 })
	void CA2_semana_deLunesADomingo(int dias) {
		LocalDate fecha = LUNES.plusDays(dias);
		assertThat(VistaAgenda.SEMANA.desde(fecha)).isEqualTo(LUNES);
		assertThat(VistaAgenda.SEMANA.hasta(fecha)).isEqualTo(DOMINGO);
		assertThat(LUNES.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
	}

	// CA2: una semana que cruza de mes y de año
	@ParameterizedTest
	@ValueSource(strings = { "2026-12-28", "2027-01-01", "2027-01-03" })
	void CA2_semana_queCruzaElAnio(String texto) {
		LocalDate fecha = LocalDate.parse(texto);
		assertThat(VistaAgenda.SEMANA.desde(fecha)).isEqualTo(LocalDate.of(2026, 12, 28));
		assertThat(VistaAgenda.SEMANA.hasta(fecha)).isEqualTo(LocalDate.of(2027, 1, 3));
	}

}
