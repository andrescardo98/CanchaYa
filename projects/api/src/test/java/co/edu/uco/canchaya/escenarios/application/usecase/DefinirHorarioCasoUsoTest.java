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

class DefinirHorarioCasoUsoTest {

	private static final LocalTime SEIS = LocalTime.of(6, 0);
	private static final LocalTime DOCE = LocalTime.of(12, 0);

	private HorariosEnMemoria.Escenarios escenarios;
	private HorariosEnMemoria horarios;
	private DefinirHorarioCasoUso casoUso;
	private Escenario escenario;
	private Solicitante propietario;

	@BeforeEach
	void preparar() {
		escenarios = new HorariosEnMemoria.Escenarios();
		horarios = new HorariosEnMemoria();
		casoUso = new DefinirHorarioCasoUso(escenarios, horarios);

		escenario = Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null, "Calle 80 # 45-10",
				UUID.randomUUID(), new BigDecimal("6.155123"), new BigDecimal("-75.373700"), "+573001234567", null,
				List.of(), UUID.randomUUID(), EstadoEscenario.ACTIVO);
		escenarios.escenarios.add(escenario);
		propietario = new Solicitante(escenario.getAdministradorId(), Rol.ADMINISTRADOR_CANCHA);
	}

	private void esperarError(Runnable accion, String codigo) {
		int antes = horarios.guardados.size();
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
		// Si hay error no se guarda nada
		assertThat(horarios.guardados).hasSize(antes);
	}

	@Test
	void ca1GuardaElHorario() {
		HorarioRecurrente horario = casoUso.ejecutar(escenario.getId(), DiaSemana.LUNES, SEIS, DOCE, propietario);

		assertThat(horarios.guardados).containsExactly(horario);
		assertThat(horario.getEscenarioId()).isEqualTo(escenario.getId());
	}

	@Test
	void ca3ElEscenarioNoExiste() {
		esperarError(() -> casoUso.ejecutar(UUID.randomUUID(), DiaSemana.LUNES, SEIS, DOCE, propietario),
				"ERR-HOR-01");
	}

	@Test
	void ca4ElEncargadoDeSedeNoDefineHorarios() {
		Solicitante encargado = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(() -> casoUso.ejecutar(escenario.getId(), DiaSemana.LUNES, SEIS, DOCE, encargado),
				"ERR-HOR-02");
	}

	@Test
	void ca6SeCruzaConUnHorarioGuardadoDelMismoDia() {
		casoUso.ejecutar(escenario.getId(), DiaSemana.LUNES, SEIS, DOCE, propietario);

		esperarError(() -> casoUso.ejecutar(escenario.getId(), DiaSemana.LUNES, LocalTime.of(10, 0),
				LocalTime.of(14, 0), propietario), "ERR-HOR-04");
	}

	@Test
	void ca6SoloComparaConLosHorariosDelMismoDia() {
		casoUso.ejecutar(escenario.getId(), DiaSemana.LUNES, SEIS, DOCE, propietario);

		casoUso.ejecutar(escenario.getId(), DiaSemana.MARTES, SEIS, DOCE, propietario);

		assertThat(horarios.guardados).hasSize(2);
	}

	@Test
	void ca6UnHorarioDeOtroEscenarioNoCuenta() {
		horarios.guardar(HorarioRecurrente.reconstruir(UUID.randomUUID(), UUID.randomUUID(), DiaSemana.LUNES, SEIS,
				DOCE));

		casoUso.ejecutar(escenario.getId(), DiaSemana.LUNES, SEIS, DOCE, propietario);

		assertThat(horarios.guardados).hasSize(2);
	}

	@Test
	void ca7DosHorariosQueNoSeCruzanQuedanVigentes() {
		casoUso.ejecutar(escenario.getId(), DiaSemana.LUNES, SEIS, DOCE, propietario);
		casoUso.ejecutar(escenario.getId(), DiaSemana.LUNES, LocalTime.of(14, 0), LocalTime.of(23, 0),
				propietario);

		assertThat(horarios.buscarPorEscenarioYDia(escenario.getId(), DiaSemana.LUNES)).hasSize(2);
	}

	@Test
	void politica5SiElIdYaExisteSeGeneraOtro() {
		horarios.idRepetidos = 2;

		HorarioRecurrente horario = casoUso.ejecutar(escenario.getId(), DiaSemana.LUNES, SEIS, DOCE, propietario);

		// Dos ids repetidos y el tercero sí sirve
		assertThat(horarios.consultasDeId).isEqualTo(3);
		assertThat(horarios.guardados).containsExactly(horario);
	}

}
