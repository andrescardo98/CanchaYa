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

/** HU7, CA1: la lista de horarios que se actualiza después de definir uno. */
class ListarHorariosCasoUsoTest {

	private HorariosEnMemoria horarios;
	private ListarHorariosCasoUso casoUso;
	private Escenario escenario;
	private Solicitante propietario;

	@BeforeEach
	void preparar() {
		HorariosEnMemoria.Escenarios escenarios = new HorariosEnMemoria.Escenarios();
		horarios = new HorariosEnMemoria();
		casoUso = new ListarHorariosCasoUso(escenarios, horarios);

		escenario = Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null, "Calle 80 # 45-10",
				UUID.randomUUID(), new BigDecimal("6.155123"), new BigDecimal("-75.373700"), "+573001234567", null,
				List.of(), UUID.randomUUID(), EstadoEscenario.ACTIVO);
		escenarios.escenarios.add(escenario);
		propietario = new Solicitante(escenario.getAdministradorId(), Rol.ADMINISTRADOR_CANCHA);
	}

	private HorarioRecurrente guardar(DiaSemana dia, int inicio, int fin) {
		HorarioRecurrente horario = HorarioRecurrente.reconstruir(UUID.randomUUID(), escenario.getId(), dia,
				LocalTime.of(inicio, 0), LocalTime.of(fin, 0));
		horarios.guardar(horario);
		return horario;
	}

	private void esperarError(UUID escenarioId, Solicitante solicitante, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(
				() -> casoUso.ejecutar(escenarioId, solicitante))
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
	}

	@Test
	void ca1ElPropietarioVeSusHorariosOrdenados() {
		HorarioRecurrente martes = guardar(DiaSemana.MARTES, 6, 12);
		HorarioRecurrente lunesTarde = guardar(DiaSemana.LUNES, 14, 23);
		HorarioRecurrente lunesManana = guardar(DiaSemana.LUNES, 6, 12);

		assertThat(casoUso.ejecutar(escenario.getId(), propietario))
				.containsExactly(lunesManana, lunesTarde, martes);
	}

	@Test
	void ca3ElEscenarioNoExiste() {
		esperarError(UUID.randomUUID(), propietario, "ERR-HOR-01");
	}

	@Test
	void ca4QuienNoEsElPropietarioNoVeLosHorarios() {
		esperarError(escenario.getId(), new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA), "ERR-HOR-02");
	}

}
