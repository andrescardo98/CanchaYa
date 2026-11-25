package co.edu.uco.canchaya.escenarios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

/** HU11: responsabilidad «modificar» de HorarioRecurrente (políticas 2, 13, 3 y 7). */
class HorarioRecurrenteModificarTest {

	private static final Escenario ESCENARIO = Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null,
			"Calle 80 # 45-10", UUID.randomUUID(), new BigDecimal("6.155123"), new BigDecimal("-75.373700"),
			"+573001234567", null, List.of(), UUID.randomUUID(), EstadoEscenario.ACTIVO);

	private static final Solicitante PROPIETARIO =
			new Solicitante(ESCENARIO.getAdministradorId(), Rol.ADMINISTRADOR_CANCHA);

	private static LocalTime hora(String texto) {
		return LocalTime.parse(texto);
	}

	private static HorarioRecurrente guardado(DiaSemana dia, String inicio, String fin) {
		return HorarioRecurrente.reconstruir(UUID.randomUUID(), ESCENARIO.getId(), dia, hora(inicio), hora(fin));
	}

	private static ReglaNegocioException esperarError(Runnable accion, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
		return error;
	}

	// Un error no debe dejar el horario a medias
	private static void sigueIgual(HorarioRecurrente horario) {
		assertThat(horario.getDiaSemana()).isEqualTo(DiaSemana.LUNES);
		assertThat(horario.getHoraInicio()).isEqualTo(hora("06:00"));
		assertThat(horario.getHoraFin()).isEqualTo(hora("12:00"));
	}

	@Test
	void ca1Modificar_cambiaDiaYHorasYConservaIdYEscenario() {
		HorarioRecurrente horario = guardado(DiaSemana.LUNES, "06:00", "12:00");
		UUID id = horario.getId();

		horario.modificar(ESCENARIO, DiaSemana.MARTES, hora("07:00"), hora("13:00"), List.of(), PROPIETARIO);

		assertThat(horario.getId()).isEqualTo(id);
		assertThat(horario.getEscenarioId()).isEqualTo(ESCENARIO.getId());
		assertThat(horario.getDiaSemana()).isEqualTo(DiaSemana.MARTES);
		assertThat(horario.getHoraInicio()).isEqualTo(hora("07:00"));
		assertThat(horario.getHoraFin()).isEqualTo(hora("13:00"));
	}

	// Política 7: el caso de uso le pasa los horarios del día, que incluyen al que se modifica
	@Test
	void ca1Modificar_noSeCruzaConsigoMismo() {
		HorarioRecurrente horario = guardado(DiaSemana.LUNES, "06:00", "12:00");

		horario.modificar(ESCENARIO, DiaSemana.LUNES, hora("06:00"), hora("14:00"), List.of(horario), PROPIETARIO);

		assertThat(horario.getHoraFin()).isEqualTo(hora("14:00"));
	}

	// CA4 / política 2: con cualquier rol, si no es el dueño no puede (el encargado de sede es ADMINISTRADOR_CANCHA)
	@ParameterizedTest
	@EnumSource(Rol.class)
	void ca4NoPropietario_ERR_HOR_02(Rol rol) {
		HorarioRecurrente horario = guardado(DiaSemana.LUNES, "06:00", "12:00");
		Solicitante otro = new Solicitante(UUID.randomUUID(), rol);

		esperarError(() -> horario.modificar(ESCENARIO, DiaSemana.MARTES, hora("07:00"), hora("13:00"), List.of(),
				otro), "ERR-HOR-02");
		sigueIgual(horario);
	}

	// CA5 / política 3
	@ParameterizedTest
	@CsvSource({ "12:00, 12:00", "14:00, 12:00" })
	void ca5FinNoMayorQueInicio_ERR_HOR_03(String inicio, String fin) {
		HorarioRecurrente horario = guardado(DiaSemana.LUNES, "06:00", "12:00");

		esperarError(() -> horario.modificar(ESCENARIO, DiaSemana.LUNES, hora(inicio), hora(fin), List.of(),
				PROPIETARIO), "ERR-HOR-03");
		sigueIgual(horario);
	}

	// CA6 / política 7: cruce parcial por cada lado, uno dentro del otro y el mismo rango
	@ParameterizedTest
	@CsvSource({ "13:00, 16:00", "17:00, 20:00", "15:00, 17:00", "13:00, 20:00", "14:00, 18:00" })
	void ca6SeCruzaConOtro_ERR_HOR_07(String inicio, String fin) {
		HorarioRecurrente horario = guardado(DiaSemana.LUNES, "06:00", "12:00");
		List<HorarioRecurrente> delDia = List.of(horario, guardado(DiaSemana.LUNES, "14:00", "18:00"));

		ReglaNegocioException error = esperarError(() -> horario.modificar(ESCENARIO, DiaSemana.LUNES, hora(inicio),
				hora(fin), delDia, PROPIETARIO), "ERR-HOR-07");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("dia.LUNES"));
		sigueIgual(horario);
	}

	// CA6: el día del mensaje es el día nuevo
	@Test
	void ca6SeCruzaConOtroDelDiaNuevo_ERR_HOR_07ConEseDia() {
		HorarioRecurrente horario = guardado(DiaSemana.LUNES, "06:00", "12:00");
		List<HorarioRecurrente> delMartes = List.of(guardado(DiaSemana.MARTES, "08:00", "10:00"));

		ReglaNegocioException error = esperarError(() -> horario.modificar(ESCENARIO, DiaSemana.MARTES,
				hora("06:00"), hora("12:00"), delMartes, PROPIETARIO), "ERR-HOR-07");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("dia.MARTES"));
	}

	// CA6: quedar pegado a otro horario no es cruzarse (la hora de fin es abierta)
	@ParameterizedTest
	@CsvSource({ "12:00, 14:00", "18:00, 23:00" })
	void ca6HorarioPegado_seGuarda(String inicio, String fin) {
		HorarioRecurrente horario = guardado(DiaSemana.LUNES, "06:00", "12:00");
		List<HorarioRecurrente> delDia = List.of(horario, guardado(DiaSemana.LUNES, "14:00", "18:00"));

		horario.modificar(ESCENARIO, DiaSemana.LUNES, hora(inicio), hora(fin), delDia, PROPIETARIO);

		assertThat(horario.getHoraInicio()).isEqualTo(hora(inicio));
	}

	// CA7 / política 13
	@ParameterizedTest
	@CsvSource({ "07:30, 13:00", "07:00, 13:15", "07:00:30, 13:00" })
	void ca7HoraNoEnPunto_ERR_HOR_13(String inicio, String fin) {
		HorarioRecurrente horario = guardado(DiaSemana.LUNES, "06:00", "12:00");

		esperarError(() -> horario.modificar(ESCENARIO, DiaSemana.LUNES, hora(inicio), hora(fin), List.of(),
				PROPIETARIO), "ERR-HOR-13");
		sigueIgual(horario);
	}

	@Test
	void escenarioQueNoEsElDelHorario_esErrorDeProgramacion() {
		HorarioRecurrente horario = HorarioRecurrente.reconstruir(UUID.randomUUID(), UUID.randomUUID(),
				DiaSemana.LUNES, hora("06:00"), hora("12:00"));

		assertThatThrownBy(() -> horario.modificar(ESCENARIO, DiaSemana.LUNES, hora("07:00"), hora("13:00"),
				List.of(), PROPIETARIO)).isInstanceOf(IllegalArgumentException.class);
	}

}
