package co.edu.uco.canchaya.escenarios.infrastructure.calificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** HU19, CA2: mientras Reputación no exista, no hay calificaciones (P1 y P6 de la especificación). */
class ConsultaCalificacionesSinResenasTest {

	private final ConsultaCalificacionesSinResenas consulta = new ConsultaCalificacionesSinResenas();

	@Test
	void ca2SinReputacionNoHayCalificaciones() {
		assertThat(consulta.calificacionesDeCanchas(List.of(UUID.randomUUID(), UUID.randomUUID()))).isEmpty();
	}

	@Test
	void ca2UnEscenarioSinCanchasTampocoTieneCalificaciones() {
		assertThat(consulta.calificacionesDeCanchas(List.of())).isEmpty();
	}

}
