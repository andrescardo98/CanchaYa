package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaReservasFuturas;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU10: caso de uso «cambiar el estado de una cancha» (políticas 7, 2, 10 y 12, y P2 de la especificación). */
class CambiarEstadoCanchaCasoUsoTest {

	private static final Solicitante PROPIETARIO = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

	private CanchasEnMemoria memoria;
	private ReservasFalsas reservas;
	private CambiarEstadoCanchaCasoUso casoUso;
	private Escenario escenario;

	@BeforeEach
	void preparar() {
		memoria = new CanchasEnMemoria();
		reservas = new ReservasFalsas();
		casoUso = new CambiarEstadoCanchaCasoUso(memoria.repositorioEscenarios(), memoria.repositorioCanchas(),
				reservas);
		escenario = memoria.escenarioDe(PROPIETARIO.usuarioId(), EstadoEscenario.ACTIVO);
	}

	private void esperarError(Cancha cancha, EstadoCancha nuevoEstado, Solicitante solicitante, String codigo) {
		esperarError(cancha.getId(), nuevoEstado, solicitante, codigo);
		// Ningún error cambia la cancha guardada
		assertThat(memoria.guardada(cancha.getId()).getEstado()).isEqualTo(cancha.getEstado());
		assertThat(memoria.estadosActualizados).isEmpty();
	}

	private void esperarError(UUID canchaId, EstadoCancha nuevoEstado, Solicitante solicitante, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(
				() -> casoUso.ejecutar(canchaId, nuevoEstado, solicitante))
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
	}

	@Test
	void ca1Inactivar_guardaElEstado() {
		Cancha cancha = memoria.cancha(escenario, "Cancha 1", EstadoCancha.DISPONIBLE);

		Cancha resultado = casoUso.ejecutar(cancha.getId(), EstadoCancha.INACTIVA, PROPIETARIO);

		assertThat(resultado.getEstado()).isEqualTo(EstadoCancha.INACTIVA);
		assertThat(memoria.guardada(cancha.getId()).getEstado()).isEqualTo(EstadoCancha.INACTIVA);
		// Solo se pregunta por esta cancha, no por las demás del escenario
		assertThat(reservas.consultas).containsExactly(List.of(cancha.getId()));
	}

	// P2 (D29 punto 2, D36): con el turno tomado, una reserva de HU21 no puede entrar entre la
	// consulta de reservas futuras y el cambio de estado
	@Test
	void ca1Inactivar_tomaElTurnoAntesDeConsultarReservas() {
		Cancha cancha = memoria.cancha(escenario, "Cancha 1", EstadoCancha.DISPONIBLE);

		casoUso.ejecutar(cancha.getId(), EstadoCancha.INACTIVA, PROPIETARIO);

		assertThat(memoria.turnosTomados).containsExactly(cancha.getId());
		assertThat(reservas.turnoTomadoAlConsultar).isTrue();
	}

	@Test
	void ca2Activar_guardaElEstado() {
		Cancha cancha = memoria.cancha(escenario, "Cancha 1", EstadoCancha.INACTIVA);

		Cancha resultado = casoUso.ejecutar(cancha.getId(), EstadoCancha.DISPONIBLE, PROPIETARIO);

		assertThat(resultado.getEstado()).isEqualTo(EstadoCancha.DISPONIBLE);
		assertThat(memoria.guardada(cancha.getId()).getEstado()).isEqualTo(EstadoCancha.DISPONIBLE);
	}

	// Activar no puede romper D08: no hace falta preguntar a Reservas ni esperar el turno
	@Test
	void ca2Activar_noConsultaReservasNiTomaTurno() {
		Cancha cancha = memoria.cancha(escenario, "Cancha 1", EstadoCancha.INACTIVA);
		reservas.hayFuturas = true;

		casoUso.ejecutar(cancha.getId(), EstadoCancha.DISPONIBLE, PROPIETARIO);

		assertThat(reservas.consultas).isEmpty();
		assertThat(memoria.turnosTomados).isEmpty();
	}

	// CA3 / política 7
	@Test
	void ca3CanchaInexistente_ERR_CAN_07() {
		memoria.cancha(escenario, "Cancha 1", EstadoCancha.DISPONIBLE);

		esperarError(UUID.randomUUID(), EstadoCancha.INACTIVA, PROPIETARIO, "ERR-CAN-07");
		assertThat(memoria.estadosActualizados).isEmpty();
		assertThat(reservas.consultas).isEmpty();
	}

	// CA4 / política 2: otro administrador de cancha o un encargado de sede (mismo rol, otro usuario)
	@Test
	void ca4NoPropietario_ERR_CAN_02() {
		Cancha cancha = memoria.cancha(escenario, "Cancha 1", EstadoCancha.DISPONIBLE);
		Solicitante otro = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(cancha, EstadoCancha.INACTIVA, otro, "ERR-CAN-02");
		// No alcanza a tomar el turno ni a consultar las reservas de la cancha
		assertThat(memoria.turnosTomados).isEmpty();
		assertThat(reservas.consultas).isEmpty();
	}

	// CA4 y P3: el administrador del sistema tampoco
	@Test
	void ca4AdministradorDelSistema_ERR_CAN_02() {
		Cancha cancha = memoria.cancha(escenario, "Cancha 1", EstadoCancha.DISPONIBLE);
		Solicitante administrador = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_SISTEMA);

		esperarError(cancha, EstadoCancha.INACTIVA, administrador, "ERR-CAN-02");
	}

	// CA5 / política 10
	@ParameterizedTest
	@EnumSource(EstadoCancha.class)
	void ca5MismoEstado_ERR_CAN_10(EstadoCancha estado) {
		Cancha cancha = memoria.cancha(escenario, "Cancha 1", estado);

		esperarError(cancha, estado, PROPIETARIO, "ERR-CAN-10");
	}

	// CA6 / política 12
	@Test
	void ca6ReservasFuturas_noGuarda() {
		Cancha cancha = memoria.cancha(escenario, "Cancha 1", EstadoCancha.DISPONIBLE);
		reservas.hayFuturas = true;

		esperarError(cancha, EstadoCancha.INACTIVA, PROPIETARIO, "ERR-CAN-12");
	}

	private final class ReservasFalsas implements ConsultaReservasFuturas {

		final List<List<UUID>> consultas = new ArrayList<>();
		boolean hayFuturas;
		boolean turnoTomadoAlConsultar;

		@Override
		public boolean existenConfirmadasFuturas(Collection<UUID> canchaIds) {
			consultas.add(List.copyOf(canchaIds));
			turnoTomadoAlConsultar = memoria.turnosTomados.containsAll(canchaIds);
			return hayFuturas;
		}

	}

}
