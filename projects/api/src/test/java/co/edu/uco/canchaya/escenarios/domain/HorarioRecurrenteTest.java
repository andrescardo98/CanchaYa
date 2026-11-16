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

/** HU7: responsabilidad «definir» de HorarioRecurrente (políticas 2, 3, 4 y 13). */
class HorarioRecurrenteTest {

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

	private static HorarioRecurrente definir(String inicio, String fin, List<HorarioRecurrente> otros,
			Solicitante solicitante) {
		return HorarioRecurrente.definir(ESCENARIO, DiaSemana.LUNES, hora(inicio), hora(fin), otros, solicitante);
	}

	private static ReglaNegocioException esperarError(Runnable accion, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
		return error;
	}

	@Test
	void ca1ElPropietarioDefineUnHorarioCorrecto() {
		HorarioRecurrente horario = definir("06:00", "12:00", List.of(), PROPIETARIO);

		assertThat(horario.getId()).isNotNull();
		assertThat(horario.getEscenarioId()).isEqualTo(ESCENARIO.getId());
		assertThat(horario.getDiaSemana()).isEqualTo(DiaSemana.LUNES);
		assertThat(horario.getHoraInicio()).isEqualTo(hora("06:00"));
		assertThat(horario.getHoraFin()).isEqualTo(hora("12:00"));
	}

	// CA4 / política 2: con cualquier rol, si no es el dueño no puede (el encargado de sede es ADMINISTRADOR_CANCHA)
	@ParameterizedTest
	@EnumSource(Rol.class)
	void ca4QuienNoEsElPropietarioNoDefineHorarios(Rol rol) {
		Solicitante otro = new Solicitante(UUID.randomUUID(), rol);

		esperarError(() -> definir("06:00", "12:00", List.of(), otro), "ERR-HOR-02");
	}

	@Test
	void ca4SinSolicitanteTambienEsErrHor02() {
		esperarError(() -> definir("06:00", "12:00", List.of(), null), "ERR-HOR-02");
	}

	// CA5 / política 3
	@ParameterizedTest
	@CsvSource({ "12:00, 12:00", "14:00, 12:00" })
	void ca5LaHoraDeFinNoEsMayorQueLaDeInicio(String inicio, String fin) {
		esperarError(() -> definir(inicio, fin, List.of(), PROPIETARIO), "ERR-HOR-03");
	}

	// CA6 / política 4: cruce parcial por cada lado, uno dentro del otro y el mismo horario
	@ParameterizedTest
	@CsvSource({ "10:00, 14:00", "05:00, 07:00", "08:00, 10:00", "05:00, 13:00", "06:00, 12:00" })
	void ca6ElHorarioSeCruzaConOtroDelMismoDia(String inicio, String fin) {
		List<HorarioRecurrente> otros = List.of(guardado(DiaSemana.LUNES, "06:00", "12:00"));

		ReglaNegocioException error = esperarError(() -> definir(inicio, fin, otros, PROPIETARIO), "ERR-HOR-04");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("dia.LUNES"));
	}

	// CA7: de 6 a 12 y de 14 a 23 (el ejemplo de la HU), y también seguidos de 12 a 14
	@ParameterizedTest
	@CsvSource({ "14:00, 23:00", "12:00, 14:00", "00:00, 06:00" })
	void ca7UnSegundoHorarioQueNoSeCruzaSeDefine(String inicio, String fin) {
		List<HorarioRecurrente> otros = List.of(guardado(DiaSemana.LUNES, "06:00", "12:00"));

		HorarioRecurrente horario = definir(inicio, fin, otros, PROPIETARIO);

		assertThat(horario.getHoraInicio()).isEqualTo(hora(inicio));
	}

	@Test
	void unHorarioDeOtroDiaNoSeCruza() {
		HorarioRecurrente martes = guardado(DiaSemana.MARTES, "06:00", "12:00");

		assertThat(definir("06:00", "12:00", List.of(), PROPIETARIO).seCruzaCon(martes)).isFalse();
	}

	// CA8 / política 13
	@ParameterizedTest
	@CsvSource({ "06:30, 12:00", "06:00, 12:15", "06:00:30, 12:00" })
	void ca8LasHorasDebenSerEnPunto(String inicio, String fin) {
		esperarError(() -> definir(inicio, fin, List.of(), PROPIETARIO), "ERR-HOR-13");
	}

	@Test
	void conNuevoIdConservaLosDatos() {
		HorarioRecurrente horario = definir("06:00", "12:00", List.of(), PROPIETARIO);

		HorarioRecurrente otro = horario.conNuevoId();

		assertThat(otro.getId()).isNotEqualTo(horario.getId());
		assertThat(otro.getDiaSemana()).isEqualTo(horario.getDiaSemana());
		assertThat(otro.getHoraInicio()).isEqualTo(horario.getHoraInicio());
		assertThat(otro.getHoraFin()).isEqualTo(horario.getHoraFin());
	}

}
