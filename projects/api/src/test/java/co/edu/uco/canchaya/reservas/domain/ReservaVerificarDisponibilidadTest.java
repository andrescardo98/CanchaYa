package co.edu.uco.canchaya.reservas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU20: responsabilidad «verificarDisponibilidad» de Reserva (políticas 1 a 4). */
class ReservaVerificarDisponibilidadTest {

	/** Lunes 23 de noviembre de 2026, a las 05:00 en Bogotá. */
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 5, 0);
	private static final LocalDate HOY = AHORA.toLocalDate();
	private static final LocalDate MANANA = HOY.plusDays(1);

	static final ParametrosReserva PARAMETROS = new ParametrosReserva(30, 2, 2, 5, 3, 5, LocalTime.NOON, 3, 24,
			250, false);

	private static LocalTime hora(String texto) {
		return LocalTime.parse(texto);
	}

	/** Franjas seguidas de la fecha, una por cada hora de inicio. */
	private static List<Franja> franjas(LocalDate fecha, String... inicios) {
		return Arrays.stream(inicios)
				.map(inicio -> new Franja(fecha, hora(inicio), hora(inicio).plusHours(1)))
				.toList();
	}

	private static boolean verificar(LocalDate fecha, String inicio, String fin, List<Franja> libres) {
		return Reserva.verificarDisponibilidad(fecha, hora(inicio), hora(fin), true, libres, AHORA, PARAMETROS);
	}

	private static void esperarError(Runnable accion, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
	}

	// CA1 y CA2 / política 3: una o dos franjas libres y seguidas están disponibles
	@ParameterizedTest
	@CsvSource({ "18:00, 19:00", "18:00, 20:00", "19:00, 21:00" })
	void ca2UnaODosFranjasLibresEstanDisponibles(String inicio, String fin) {
		List<Franja> libres = franjas(MANANA, "18:00", "19:00", "20:00");

		assertThat(verificar(MANANA, inicio, fin, libres)).isTrue();
	}

	// CA2 / política 3: si una franja del rango tiene reserva o bloqueo, no viene en las libres
	@Test
	void ca2UnRangoConUnaFranjaOcupadaNoEstaDisponible() {
		List<Franja> libres = franjas(MANANA, "18:00", "20:00");

		assertThat(verificar(MANANA, "18:00", "19:00", libres)).isTrue();
		assertThat(verificar(MANANA, "19:00", "20:00", libres)).isFalse();
		assertThat(verificar(MANANA, "18:00", "20:00", libres)).isFalse();
		assertThat(verificar(MANANA, "19:00", "21:00", libres)).isFalse();
	}

	// CA2 / política 3: fuera del horario del escenario no hay franja
	@Test
	void ca2FueraDelHorarioNoEstaDisponible() {
		List<Franja> libres = franjas(MANANA, "18:00", "19:00");

		assertThat(verificar(MANANA, "19:00", "21:00", libres)).isFalse();
		assertThat(verificar(MANANA, "06:00", "07:00", libres)).isFalse();
		assertThat(verificar(MANANA, "18:00", "19:00", List.of())).isFalse();
	}

	// CA2: una franja libre de otra fecha no sirve
	@Test
	void ca2UnaFranjaDeOtraFechaNoCuenta() {
		assertThat(verificar(MANANA, "18:00", "19:00", franjas(HOY, "18:00"))).isFalse();
	}

	// CA7 y PAR-RES-02: más de 2 franjas, menos de una hora o fuera de orden no es un rango que se pueda reservar
	@ParameterizedTest
	@CsvSource({ "18:00, 21:00", "18:00, 18:00", "19:00, 18:00", "18:30, 19:30", "18:00, 18:30" })
	void ca7UnRangoQueNoEsDeUnaODosFranjasNoEstaDisponible(String inicio, String fin) {
		List<Franja> libres = franjas(MANANA, "18:00", "19:00", "20:00");

		assertThat(verificar(MANANA, inicio, fin, libres)).isFalse();
	}

	// CA7: el máximo de franjas sale de PAR-RES-02, no de un 2 escrito en el código
	@Test
	void ca7ElMaximoDeFranjasSaleDelParametro() {
		List<Franja> libres = franjas(MANANA, "18:00", "19:00", "20:00");
		ParametrosReserva tresFranjas = new ParametrosReserva(30, 3, 3, 5, 3, 5, LocalTime.NOON, 3, 24, 250, false);

		assertThat(Reserva.verificarDisponibilidad(MANANA, hora("18:00"), hora("21:00"), true, libres, AHORA,
				tresFranjas)).isTrue();
	}

	// CA3 / política 4: una franja que ya empezó no se ofrece, aunque alguien la pase como libre
	@Test
	void ca3UnaFranjaQueYaEmpezoNoEstaDisponible() {
		List<Franja> libres = franjas(HOY, "07:00", "08:00", "09:00");
		LocalDateTime alas0730 = HOY.atTime(7, 30);

		assertThat(Reserva.verificarDisponibilidad(HOY, hora("07:00"), hora("08:00"), true, libres, alas0730,
				PARAMETROS)).isFalse();
		assertThat(Reserva.verificarDisponibilidad(HOY, hora("08:00"), hora("10:00"), true, libres, alas0730,
				PARAMETROS)).isTrue();
	}

	// CA6 / política 1
	@Test
	void ca6UnaCanchaQueNoEstaDisponibleEsErrRes01() {
		esperarError(() -> Reserva.verificarDisponibilidad(MANANA, hora("18:00"), hora("19:00"), false,
				franjas(MANANA, "18:00"), AHORA, PARAMETROS), "ERR-RES-01");
	}

	// Política 2 (RN-11): antes de hoy o después de hoy + 30
	@ParameterizedTest
	@ValueSource(ints = { -1, 31 })
	void politica2UnaFechaFueraDeLaVentanaEsErrRes02(int dias) {
		LocalDate fecha = HOY.plusDays(dias);

		esperarError(() -> verificar(fecha, "18:00", "19:00", franjas(fecha, "18:00")), "ERR-RES-02");
	}

	// Política 2 y P6 de HU20: hoy y hoy + 30 están dentro
	@ParameterizedTest
	@ValueSource(ints = { 0, 30 })
	void politica2LosBordesDeLaVentanaSeAceptan(int dias) {
		LocalDate fecha = HOY.plusDays(dias);

		assertThat(verificar(fecha, "18:00", "19:00", franjas(fecha, "18:00"))).isTrue();
	}

	// La cancha se revisa antes que la fecha, como en la columna de excepciones de clases.md
	@Test
	void politica1VaAntesQueLaPolitica2() {
		LocalDate ayer = HOY.minusDays(1);

		esperarError(() -> Reserva.verificarDisponibilidad(ayer, hora("18:00"), hora("19:00"), false, List.of(),
				AHORA, PARAMETROS), "ERR-RES-01");
	}

}
