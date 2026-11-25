package co.edu.uco.canchaya.reservas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;

/** HU12, CA6: lo que Reservas responde a Escenarios cuando se quiere eliminar un horario (D08). */
class ConsultaReservasFuturasCasoUsoTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	private static final LocalDateTime AHORA = LocalDateTime.of(2030, 1, 15, 10, 0);
	private static final Clock RELOJ = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
	private static final LocalTime SEIS = LocalTime.of(6, 0);
	private static final LocalTime DOCE = LocalTime.of(12, 0);

	private RepositorioReservas repositorio;
	private ConsultaReservasFuturasCasoUso consulta;

	@BeforeEach
	void preparar() {
		repositorio = mock(RepositorioReservas.class);
		consulta = new ConsultaReservasFuturasCasoUso(repositorio, RELOJ);
	}

	// D27: «futura» se mide con la hora de Bogotá
	@Test
	void ca6EnHorario_preguntaConElDiaLasHorasYLaHoraDeBogota() {
		List<UUID> canchas = List.of(UUID.randomUUID());
		when(repositorio.existenConfirmadasEnHorarioQueEmpiezanDespuesDe(canchas, DayOfWeek.TUESDAY, SEIS, DOCE,
				AHORA)).thenReturn(true);

		assertThat(consulta.existenConfirmadasFuturasEnHorario(canchas, DayOfWeek.TUESDAY, SEIS, DOCE)).isTrue();
		verify(repositorio).existenConfirmadasEnHorarioQueEmpiezanDespuesDe(canchas, DayOfWeek.TUESDAY, SEIS, DOCE,
				AHORA);
	}

	@Test
	void ca6EnHorario_sinReservasResponde_false() {
		when(repositorio.existenConfirmadasEnHorarioQueEmpiezanDespuesDe(any(), any(), any(), any(), any()))
				.thenReturn(false);

		assertThat(consulta.existenConfirmadasFuturasEnHorario(List.of(UUID.randomUUID()), DayOfWeek.TUESDAY, SEIS,
				DOCE)).isFalse();
	}

	// Un escenario sin canchas no puede tener reservas; un "in ()" vacío además falla en SQL
	@Test
	void ca6EnHorario_sinCanchasNoVaALaBase() {
		assertThat(consulta.existenConfirmadasFuturasEnHorario(List.of(), DayOfWeek.TUESDAY, SEIS, DOCE)).isFalse();
		verifyNoInteractions(repositorio);
	}

}
