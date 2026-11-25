package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU11: caso de uso «modificar» de HorarioRecurrente (política 6 y el orden de las demás). */
class ModificarHorarioCasoUsoTest {

	private static final LocalTime SEIS = LocalTime.of(6, 0);
	private static final LocalTime DOCE = LocalTime.of(12, 0);

	private HorariosEnMemoria.Escenarios escenarios;
	private HorariosEnMemoria horarios;
	private ModificarHorarioCasoUso casoUso;
	private Escenario escenario;
	private Solicitante propietario;
	private HorarioRecurrente lunes;

	@BeforeEach
	void preparar() {
		escenarios = new HorariosEnMemoria.Escenarios();
		horarios = new HorariosEnMemoria();
		casoUso = new ModificarHorarioCasoUso(escenarios, horarios);

		escenario = Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null, "Calle 80 # 45-10",
				UUID.randomUUID(), new BigDecimal("6.155123"), new BigDecimal("-75.373700"), "+573001234567", null,
				List.of(), UUID.randomUUID(), EstadoEscenario.ACTIVO);
		escenarios.escenarios.add(escenario);
		propietario = new Solicitante(escenario.getAdministradorId(), Rol.ADMINISTRADOR_CANCHA);

		lunes = guardar(DiaSemana.LUNES, SEIS, DOCE);
	}

	private HorarioRecurrente guardar(DiaSemana dia, LocalTime inicio, LocalTime fin) {
		HorarioRecurrente horario = HorarioRecurrente.reconstruir(UUID.randomUUID(), escenario.getId(), dia, inicio,
				fin);
		horarios.guardar(horario);
		return horario;
	}

	private HorarioRecurrente guardado(UUID id) {
		return horarios.guardados.stream().filter(h -> h.getId().equals(id)).findFirst().orElseThrow();
	}

	// Si hay error, el horario guardado sigue de lunes de 6 a 12
	private void esperarError(Runnable accion, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
		HorarioRecurrente sinCambios = guardado(lunes.getId());
		assertThat(sinCambios.getDiaSemana()).isEqualTo(DiaSemana.LUNES);
		assertThat(sinCambios.getHoraInicio()).isEqualTo(SEIS);
		assertThat(sinCambios.getHoraFin()).isEqualTo(DOCE);
	}

	@Test
	void ca1Modificar_guardaLosCambios() {
		HorarioRecurrente modificado = casoUso.ejecutar(lunes.getId(), DiaSemana.MARTES, LocalTime.of(7, 0),
				LocalTime.of(13, 0), propietario);

		assertThat(modificado.getId()).isEqualTo(lunes.getId());
		HorarioRecurrente enRepositorio = guardado(lunes.getId());
		assertThat(enRepositorio.getDiaSemana()).isEqualTo(DiaSemana.MARTES);
		assertThat(enRepositorio.getHoraInicio()).isEqualTo(LocalTime.of(7, 0));
		assertThat(enRepositorio.getHoraFin()).isEqualTo(LocalTime.of(13, 0));
		assertThat(horarios.guardados).hasSize(1);
	}

	// Política 7: se compara con los horarios del día nuevo, no con los del día que tenía
	@Test
	void ca1CambiarDeDia_revisaLosHorariosDelDiaNuevo() {
		guardar(DiaSemana.MARTES, LocalTime.of(10, 0), LocalTime.of(14, 0));

		esperarError(() -> casoUso.ejecutar(lunes.getId(), DiaSemana.MARTES, SEIS, DOCE, propietario), "ERR-HOR-07");
	}

	// Ampliar el horario sobre sus propias horas no es cruzarse
	@Test
	void ca1AmpliarElMismoHorario_seGuarda() {
		casoUso.ejecutar(lunes.getId(), DiaSemana.LUNES, SEIS, LocalTime.of(14, 0), propietario);

		assertThat(guardado(lunes.getId()).getHoraFin()).isEqualTo(LocalTime.of(14, 0));
	}

	// CA3 / política 6
	@Test
	void ca3HorarioInexistente_ERR_HOR_06() {
		esperarError(() -> casoUso.ejecutar(UUID.randomUUID(), DiaSemana.MARTES, SEIS, DOCE, propietario),
				"ERR-HOR-06");
	}

	// CA4: el encargado de sede tiene el mismo rol, pero no es el propietario
	@Test
	void ca4EncargadoDeSede_ERR_HOR_02() {
		Solicitante encargado = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(() -> casoUso.ejecutar(lunes.getId(), DiaSemana.MARTES, SEIS, DOCE, encargado), "ERR-HOR-02");
	}

	@Test
	void ca4AdministradorDelSistema_ERR_HOR_02() {
		Solicitante administrador = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_SISTEMA);

		esperarError(() -> casoUso.ejecutar(lunes.getId(), DiaSemana.MARTES, SEIS, DOCE, administrador),
				"ERR-HOR-02");
	}

	// CA6: un horario del mismo día de otro escenario no cuenta
	@Test
	void ca6UnHorarioDeOtroEscenarioNoCuenta() {
		horarios.guardar(HorarioRecurrente.reconstruir(UUID.randomUUID(), UUID.randomUUID(), DiaSemana.MARTES, SEIS,
				DOCE));

		casoUso.ejecutar(lunes.getId(), DiaSemana.MARTES, SEIS, DOCE, propietario);

		assertThat(guardado(lunes.getId()).getDiaSemana()).isEqualTo(DiaSemana.MARTES);
	}

}
