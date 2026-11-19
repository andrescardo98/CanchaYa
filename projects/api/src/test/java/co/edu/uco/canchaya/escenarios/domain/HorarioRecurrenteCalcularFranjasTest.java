package co.edu.uco.canchaya.escenarios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU20: responsabilidad «calcularFranjas» de HorarioRecurrente (políticas 10, 11 y 12). */
class HorarioRecurrenteCalcularFranjasTest {

	/** PAR-RES-01. Se pasa como parámetro: el valor real lo lee la configuración. */
	private static final int VENTANA_DIAS = 30;
	private static final UUID ESCENARIO_ID = UUID.randomUUID();
	/** Lunes 23 de noviembre de 2026, a las 05:00 en Bogotá: antes de cualquier franja del día. */
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 5, 0);
	private static final LocalDate HOY = AHORA.toLocalDate();

	private static LocalTime hora(String texto) {
		return LocalTime.parse(texto);
	}

	private static HorarioRecurrente horario(DiaSemana dia, String inicio, String fin) {
		return HorarioRecurrente.reconstruir(UUID.randomUUID(), ESCENARIO_ID, dia, hora(inicio), hora(fin));
	}

	private static List<Franja> calcular(List<HorarioRecurrente> horarios, LocalDate fecha, LocalDateTime ahora) {
		return HorarioRecurrente.calcularFranjas(horarios, fecha, ahora, VENTANA_DIAS);
	}

	private static void esperarErrHor10(Runnable accion) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo("ERR-HOR-10");
	}

	@Test
	void laFechaDePruebaEsLunes() {
		assertThat(HOY.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
	}

	// CA1 / política 11: franjas de una hora en punto a partir del horario
	@Test
	void ca1UnHorarioDeTresHorasDaTresFranjasDeUnaHora() {
		List<Franja> franjas = calcular(List.of(horario(DiaSemana.LUNES, "06:00", "09:00")), HOY, AHORA);

		assertThat(franjas).extracting(Franja::fecha, Franja::horaInicio, Franja::horaFin).containsExactly(
				tuple(HOY, hora("06:00"), hora("07:00")),
				tuple(HOY, hora("07:00"), hora("08:00")),
				tuple(HOY, hora("08:00"), hora("09:00")));
	}

	// CA1: solo cuentan los horarios del día de la semana de la fecha, y salen en orden aunque lleguen desordenados
	@Test
	void ca1VariosHorariosSoloLosDelDiaDeLaFechaYEnOrden() {
		List<HorarioRecurrente> horarios = List.of(
				horario(DiaSemana.LUNES, "14:00", "16:00"),
				horario(DiaSemana.MARTES, "10:00", "12:00"),
				horario(DiaSemana.LUNES, "06:00", "09:00"));

		List<Franja> franjas = calcular(horarios, HOY, AHORA);

		assertThat(franjas).extracting(Franja::horaInicio).containsExactly(
				hora("06:00"), hora("07:00"), hora("08:00"), hora("14:00"), hora("15:00"));
	}

	// CA1 y P3 de HU21: dos horarios contiguos dan franjas seguidas
	@Test
	void ca1HorariosContiguosDanFranjasSeguidas() {
		List<HorarioRecurrente> horarios = List.of(
				horario(DiaSemana.LUNES, "12:00", "14:00"),
				horario(DiaSemana.LUNES, "06:00", "12:00"));

		List<Franja> franjas = calcular(horarios, HOY, AHORA);

		assertThat(franjas).hasSize(8);
		for (int i = 1; i < franjas.size(); i++) {
			assertThat(franjas.get(i).horaInicio()).isEqualTo(franjas.get(i - 1).horaFin());
		}
	}

	// CA4: sin horario para ese día de la semana no hay franjas (el mensaje INF-HOR-01 lo pone el caso de uso)
	@Test
	void ca4SinHorarioParaEseDiaNoHayFranjas() {
		List<HorarioRecurrente> horarios = List.of(horario(DiaSemana.MARTES, "06:00", "22:00"));

		assertThat(calcular(horarios, HOY, AHORA)).isEmpty();
		assertThat(calcular(List.of(), HOY, AHORA)).isEmpty();
	}

	// CA3 / política 12: hoy a las 07:30 ya empezaron 06–07 y 07–08
	@Test
	void ca3HoyNoOfreceLasFranjasQueYaEmpezaron() {
		List<HorarioRecurrente> horarios = List.of(horario(DiaSemana.LUNES, "06:00", "10:00"));

		List<Franja> franjas = calcular(horarios, HOY, HOY.atTime(7, 30));

		assertThat(franjas).extracting(Franja::horaInicio).containsExactly(hora("08:00"), hora("09:00"));
	}

	// CA3: a las 08:00 en punto la franja de las 08:00 ya empezó
	@Test
	void ca3UnaFranjaQueEmpiezaJustoAhoraYaNoSeOfrece() {
		List<HorarioRecurrente> horarios = List.of(horario(DiaSemana.LUNES, "06:00", "10:00"));

		List<Franja> franjas = calcular(horarios, HOY, HOY.atTime(8, 0));

		assertThat(franjas).extracting(Franja::horaInicio).containsExactly(hora("09:00"));
	}

	// CA3: en otra fecha la hora actual no quita franjas
	@Test
	void ca3OtraFechaNoFiltraPorLaHoraActual() {
		LocalDate proximoLunes = HOY.plusWeeks(1);
		List<HorarioRecurrente> horarios = List.of(horario(DiaSemana.LUNES, "06:00", "10:00"));

		List<Franja> franjas = calcular(horarios, proximoLunes, HOY.atTime(22, 0));

		assertThat(franjas).hasSize(4).allMatch(franja -> franja.fecha().equals(proximoLunes));
	}

	// CA5 / política 10: antes de hoy o después de hoy + 30 no se calcula
	@ParameterizedTest
	@ValueSource(ints = { -1, -30, 31, 60 })
	void ca5UnaFechaFueraDeLaVentanaEsErrHor10(int dias) {
		List<HorarioRecurrente> horarios = List.of(horario(DiaSemana.LUNES, "06:00", "10:00"));

		esperarErrHor10(() -> calcular(horarios, HOY.plusDays(dias), AHORA));
	}

	// CA5 y P6: la ventana incluye hoy y hoy + 30
	@ParameterizedTest
	@ValueSource(ints = { 0, 1, 29, 30 })
	void ca5LosBordesDeLaVentanaSeCalculan(int dias) {
		LocalDate fecha = HOY.plusDays(dias);
		List<HorarioRecurrente> horarios = List.of(horario(DiaSemana.de(fecha.getDayOfWeek()), "06:00", "07:00"));

		assertThat(calcular(horarios, fecha, AHORA)).hasSize(1);
	}

	// CA5: el tamaño de la ventana es el parámetro, no un 30 escrito en el código (regla 7)
	@Test
	void ca5LaVentanaSaleDelParametro() {
		List<HorarioRecurrente> horarios = List.of(horario(DiaSemana.MARTES, "06:00", "07:00"));

		assertThat(HorarioRecurrente.calcularFranjas(horarios, HOY.plusDays(1), AHORA, 1)).hasSize(1);
		esperarErrHor10(() -> HorarioRecurrente.calcularFranjas(horarios, HOY.plusDays(8), AHORA, 7));
	}

	// La fecha de hoy sale de «ahora» (hora de Bogotá, D27), no del reloj del computador
	@Test
	void ca5HoyEsLaFechaDeAhora() {
		LocalDateTime ultimoMinutoDelDomingo = LocalDateTime.of(2026, 11, 22, 23, 59);
		List<HorarioRecurrente> horarios = List.of(horario(DiaSemana.DOMINGO, "06:00", "07:00"));

		esperarErrHor10(() -> calcular(horarios, LocalDate.of(2026, 11, 21), ultimoMinutoDelDomingo));
	}

	@ParameterizedTest
	@CsvSource({ "MONDAY, LUNES", "TUESDAY, MARTES", "WEDNESDAY, MIERCOLES", "THURSDAY, JUEVES", "FRIDAY, VIERNES",
			"SATURDAY, SABADO", "SUNDAY, DOMINGO" })
	void cadaDiaDeLaSemanaTieneSuDiaSemana(DayOfWeek dia, DiaSemana esperado) {
		assertThat(DiaSemana.de(dia)).isEqualTo(esperado);
	}

}
