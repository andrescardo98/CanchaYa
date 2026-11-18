package co.edu.uco.canchaya.escenarios.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** HU19: responsabilidad «calcularCalificacionPromedio» de Escenario (políticas 11 y 12, D05). */
class EscenarioCalificacionPromedioTest {

	@Test
	void ca1ElPromedioQuedaConUnDecimal() {
		// (4 + 5 + 4) / 3 = 4,333…
		assertThat(Escenario.calcularCalificacionPromedio(List.of((short) 4, (short) 5, (short) 4)))
				.contains(new BigDecimal("4.3"));
	}

	@Test
	void ca1ElPromedioRedondeaHaciaArribaDesdeLaMitad() {
		// (4 + 5) / 2 = 4,5 y (4 + 4 + 5 + 5 + 5 + 5) / 6 = 4,666… → 4,7
		assertThat(Escenario.calcularCalificacionPromedio(List.of((short) 4, (short) 5)))
				.contains(new BigDecimal("4.5"));
		assertThat(Escenario.calcularCalificacionPromedio(
				List.of((short) 4, (short) 4, (short) 5, (short) 5, (short) 5, (short) 5)))
				.contains(new BigDecimal("4.7"));
	}

	@Test
	void ca1UnaSolaCalificacionEsElPromedio() {
		assertThat(Escenario.calcularCalificacionPromedio(List.of((short) 5))).contains(new BigDecimal("5.0"));
	}

	@Test
	void ca2SinCalificacionesNoHayPromedioNiCero() {
		// Política 12: sin reseñas no se presenta calificación; nunca un cero artificial
		assertThat(Escenario.calcularCalificacionPromedio(List.of())).isEmpty();
		assertThat(Escenario.calcularCalificacionPromedio(null)).isEmpty();
	}

	@Test
	void politica11SoloCuentanLasCalificacionesDe1a5() {
		List<Short> calificaciones = new ArrayList<>();
		calificaciones.add((short) 3);
		calificaciones.add(null);
		calificaciones.add((short) 0);
		calificaciones.add((short) 6);

		assertThat(Escenario.calcularCalificacionPromedio(calificaciones)).contains(new BigDecimal("3.0"));
	}

	@Test
	void politica12SiNingunaCalificacionEsValidaNoHayPromedio() {
		assertThat(Escenario.calcularCalificacionPromedio(List.of((short) 0, (short) 7))).isEmpty();
	}

}
