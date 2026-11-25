package co.edu.uco.canchaya.escenarios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU12: responsabilidad «eliminar» de HorarioRecurrente (políticas 2 y 15). */
class HorarioRecurrenteEliminarTest {

	private static final Escenario ESCENARIO = Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null,
			"Calle 80 # 45-10", UUID.randomUUID(), new BigDecimal("6.155123"), new BigDecimal("-75.373700"),
			"+573001234567", null, List.of(), UUID.randomUUID(), EstadoEscenario.ACTIVO);

	private static final Solicitante PROPIETARIO =
			new Solicitante(ESCENARIO.getAdministradorId(), Rol.ADMINISTRADOR_CANCHA);

	private static final HorarioRecurrente HORARIO = HorarioRecurrente.reconstruir(UUID.randomUUID(),
			ESCENARIO.getId(), DiaSemana.LUNES, LocalTime.parse("06:00"), LocalTime.parse("12:00"));

	private static void esperarError(Runnable accion, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
	}

	@Test
	void ca1ValidarEliminacion_propietarioSinReservas_noLanza() {
		assertThatCode(() -> HORARIO.validarEliminacion(ESCENARIO, PROPIETARIO, false)).doesNotThrowAnyException();
	}

	// CA5 / política 2: con cualquier rol, si no es el dueño no puede. El encargado de sede es
	// ADMINISTRADOR_CANCHA y el administrador del sistema tampoco es el dueño
	@ParameterizedTest
	@EnumSource(Rol.class)
	void ca5NoPropietario_ERR_HOR_02(Rol rol) {
		Solicitante otro = new Solicitante(UUID.randomUUID(), rol);

		esperarError(() -> HORARIO.validarEliminacion(ESCENARIO, otro, false), "ERR-HOR-02");
	}

	@Test
	void ca5SinSolicitante_ERR_HOR_02() {
		esperarError(() -> HORARIO.validarEliminacion(ESCENARIO, null, false), "ERR-HOR-02");
	}

	// CA5: el permiso va primero, así quien no es el dueño no se entera de si hay reservas
	@Test
	void ca5NoPropietarioConReservas_ERR_HOR_02() {
		Solicitante otro = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(() -> HORARIO.validarEliminacion(ESCENARIO, otro, true), "ERR-HOR-02");
	}

	// CA6 / política 15 (RN-17, D08)
	@Test
	void ca6ConReservasFuturas_ERR_HOR_15() {
		esperarError(() -> HORARIO.validarEliminacion(ESCENARIO, PROPIETARIO, true), "ERR-HOR-15");
	}

	// Error de programación: el caso de uso debe pasar el escenario del horario
	@Test
	void validarEliminacion_conOtroEscenario_lanzaIllegalArgument() {
		Escenario otro = Escenario.reconstruir(UUID.randomUUID(), "Otra cancha", null, "Calle 10 # 1-1",
				UUID.randomUUID(), new BigDecimal("6.1"), new BigDecimal("-75.3"), "+573001234567", null, List.of(),
				ESCENARIO.getAdministradorId(), EstadoEscenario.ACTIVO);

		assertThatThrownBy(() -> HORARIO.validarEliminacion(otro, PROPIETARIO, false))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// Para pasarle el día a Reservas sin que dependa de DiaSemana (spec de HU12, diseño por capas)
	@ParameterizedTest
	@EnumSource(DayOfWeek.class)
	void aDayOfWeek_esElInversoDeDe(DayOfWeek dia) {
		assertThat(DiaSemana.de(dia).aDayOfWeek()).isEqualTo(dia);
	}

}
