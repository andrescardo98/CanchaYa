package co.edu.uco.canchaya.escenarios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * HU29, CA1: las franjas de la agenda (política 11 de «calcularFranjas»), sin las políticas 10 y
 * 12, que son para el jugador (P6 de la especificación).
 */
class HorarioRecurrenteFranjasDeLaFechaTest {

	private static final UUID ESCENARIO_ID = UUID.randomUUID();
	/** Miércoles 25 de noviembre de 2026. */
	private static final LocalDate MIERCOLES = LocalDate.of(2026, 11, 25);

	private static LocalTime hora(String texto) {
		return LocalTime.parse(texto);
	}

	private static HorarioRecurrente horario(DiaSemana dia, String inicio, String fin) {
		return HorarioRecurrente.reconstruir(UUID.randomUUID(), ESCENARIO_ID, dia, hora(inicio), hora(fin));
	}

	@Test
	void laFechaDePruebaEsMiercoles() {
		assertThat(MIERCOLES.getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
	}

	@Test
	void ca1FranjasDeUnaHoraDeLosHorariosDelDiaPorHoraDeInicio() {
		List<HorarioRecurrente> horarios = List.of(horario(DiaSemana.MIERCOLES, "18:00", "20:00"),
				horario(DiaSemana.MIERCOLES, "06:00", "08:00"), horario(DiaSemana.JUEVES, "10:00", "11:00"));

		assertThat(HorarioRecurrente.franjasDeLaFecha(horarios, MIERCOLES))
				.extracting(Franja::fecha, Franja::horaInicio, Franja::horaFin)
				.containsExactly(tuple(MIERCOLES, hora("06:00"), hora("07:00")),
						tuple(MIERCOLES, hora("07:00"), hora("08:00")),
						tuple(MIERCOLES, hora("18:00"), hora("19:00")),
						tuple(MIERCOLES, hora("19:00"), hora("20:00")));
	}

	// P6: el administrador ve todo el día, también lo que ya empezó, y cualquier fecha (sin la ventana de PAR-RES-01)
	@Test
	void ca1NoQuitaLasFranjasPasadasNiMiraLaVentana() {
		LocalDate haceUnAnio = MIERCOLES.minusWeeks(52);
		LocalDate dentroDeUnAnio = MIERCOLES.plusWeeks(52);
		List<HorarioRecurrente> horarios = List.of(horario(DiaSemana.MIERCOLES, "06:00", "07:00"));

		assertThat(HorarioRecurrente.franjasDeLaFecha(horarios, haceUnAnio)).hasSize(1);
		assertThat(HorarioRecurrente.franjasDeLaFecha(horarios, dentroDeUnAnio)).hasSize(1);
	}

	@Test
	void ca1SinHorariosEseDiaNoHayFranjas() {
		assertThat(HorarioRecurrente.franjasDeLaFecha(List.of(horario(DiaSemana.LUNES, "06:00", "10:00")),
				MIERCOLES)).isEmpty();
	}

}
