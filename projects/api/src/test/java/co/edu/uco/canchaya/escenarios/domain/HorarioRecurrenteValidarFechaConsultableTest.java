package co.edu.uco.canchaya.escenarios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * HU18: política 10 de HorarioRecurrente (RN-11) por separado, para que la búsqueda rechace la
 * fecha aunque no haya ninguna cancha a la que calcularle franjas (CA6).
 */
class HorarioRecurrenteValidarFechaConsultableTest {

	/** PAR-RES-01. Se pasa como parámetro: el valor real lo lee la configuración. */
	private static final int VENTANA_DIAS = 30;
	/** Martes 24 de noviembre de 2026, a las 23:30 en Bogotá: tarde, para ver que hoy sigue siendo hoy. */
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 24, 23, 30);
	private static final LocalDate HOY = AHORA.toLocalDate();

	private static void esperarErrHor10(Runnable accion) {
		assertThatThrownBy(accion::run)
				.isInstanceOfSatisfying(ReglaNegocioException.class,
						error -> assertThat(error.getCodigoMensaje()).isEqualTo("ERR-HOR-10"));
	}

	// CA6: ayer y hoy + 31 (o más) están fuera de la ventana
	@ParameterizedTest
	@ValueSource(ints = { -1, -30, 31, 60 })
	void ca6FechaFueraDeLaVentana_ErrHor10(int dias) {
		esperarErrHor10(() -> HorarioRecurrente.validarFechaConsultable(HOY.plusDays(dias), AHORA, VENTANA_DIAS));
	}

	// CA6 y P6 de HU20: la ventana incluye hoy y hoy + 30
	@ParameterizedTest
	@ValueSource(ints = { 0, 1, 29, 30 })
	void ca6BordesDeLaVentana_seAceptan(int dias) {
		assertThatCode(() -> HorarioRecurrente.validarFechaConsultable(HOY.plusDays(dias), AHORA, VENTANA_DIAS))
				.doesNotThrowAnyException();
	}

	// Regla 7: el tamaño de la ventana sale del parámetro, no de un valor fijo
	@Test
	void ca6LaVentanaSaleDelParametro() {
		assertThatCode(() -> HorarioRecurrente.validarFechaConsultable(HOY.plusDays(45), AHORA, 45))
				.doesNotThrowAnyException();
		esperarErrHor10(() -> HorarioRecurrente.validarFechaConsultable(HOY.plusDays(8), AHORA, 7));
	}

	// calcularFranjas sigue aplicando la misma política 10, incluso sin horarios (lo que la búsqueda no puede usar
	// como validación porque, sin canchas candidatas, nunca la llamaría)
	@Test
	void ca6CalcularFranjasUsaLaMismaValidacion() {
		esperarErrHor10(() -> HorarioRecurrente.calcularFranjas(List.of(), HOY.plusDays(31), AHORA, VENTANA_DIAS));
		assertThat(HorarioRecurrente.calcularFranjas(List.of(), HOY.plusDays(30), AHORA, VENTANA_DIAS)).isEmpty();
	}

	@Test
	void fechaOAhoraVacios_lanzanNullPointer() {
		assertThatThrownBy(() -> HorarioRecurrente.validarFechaConsultable(null, AHORA, VENTANA_DIAS))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> HorarioRecurrente.validarFechaConsultable(HOY, null, VENTANA_DIAS))
				.isInstanceOf(NullPointerException.class);
	}

}
