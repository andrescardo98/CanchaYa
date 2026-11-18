package co.edu.uco.canchaya.escenarios.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * HU19 (y HU18 más adelante): responsabilidad «publicarEnBusqueda» de Cancha (política 11,
 * RN-04). El caso de uso consulta los horarios y le pasa el resultado como un valor simple.
 */
class CanchaPublicarEnBusquedaTest {

	private static Escenario escenario(EstadoEscenario estado) {
		return Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null, "Calle 80 # 45-10", UUID.randomUUID(),
				new BigDecimal("6.155123"), new BigDecimal("-75.373700"), "+573001234567", null, List.of(),
				UUID.randomUUID(), estado);
	}

	private static Cancha cancha(Escenario escenario, EstadoCancha estado) {
		return Cancha.reconstruir(UUID.randomUUID(), escenario.getId(), UUID.randomUUID(), "Cancha 1",
				new BigDecimal("80000.00"), estado);
	}

	@Test
	void ca1DisponibleEscenarioActivoYConHorariosSePublica() {
		Escenario activo = escenario(EstadoEscenario.ACTIVO);

		assertThat(cancha(activo, EstadoCancha.DISPONIBLE).publicarEnBusqueda(activo, true)).isTrue();
	}

	@Test
	void ca1UnaCanchaInactivaNoSePublica() {
		Escenario activo = escenario(EstadoEscenario.ACTIVO);

		assertThat(cancha(activo, EstadoCancha.INACTIVA).publicarEnBusqueda(activo, true)).isFalse();
	}

	@Test
	void ca1SinHorariosNoSePublica() {
		Escenario activo = escenario(EstadoEscenario.ACTIVO);

		assertThat(cancha(activo, EstadoCancha.DISPONIBLE).publicarEnBusqueda(activo, false)).isFalse();
	}

	@Test
	void ca4UnEscenarioInactivoNoPublicaSusCanchas() {
		Escenario inactivo = escenario(EstadoEscenario.INACTIVO);

		assertThat(cancha(inactivo, EstadoCancha.DISPONIBLE).publicarEnBusqueda(inactivo, true)).isFalse();
	}

}
