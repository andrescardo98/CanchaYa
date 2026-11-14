package co.edu.uco.canchaya.escenarios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

/** HU13: responsabilidad «cambiarEstado» de Escenario (políticas 8, 9, 10 y 14). */
class EscenarioCambiarEstadoTest {

	private static final Solicitante ADMINISTRADOR_SISTEMA =
			new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_SISTEMA);

	private static Escenario escenario(EstadoEscenario estado) {
		return Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null, "Calle 80 # 45-10", UUID.randomUUID(),
				new BigDecimal("6.155123"), new BigDecimal("-75.373700"), "+573001234567", null, List.of(),
				UUID.randomUUID(), estado);
	}

	private static ReglaNegocioException esperarError(Runnable accion, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
		return error;
	}

	@Test
	void ca2ElAdministradorDelSistemaActivaUnEscenarioInactivo() {
		Escenario escenario = escenario(EstadoEscenario.INACTIVO);

		escenario.cambiarEstado(EstadoEscenario.ACTIVO, ADMINISTRADOR_SISTEMA, false);

		assertThat(escenario.getEstado()).isEqualTo(EstadoEscenario.ACTIVO);
		// Política 10: ya puede salir en la búsqueda
		assertThat(escenario.estaActivo()).isTrue();
	}

	@Test
	void ca3ElAdministradorDelSistemaInactivaUnEscenarioSinReservasFuturas() {
		Escenario escenario = escenario(EstadoEscenario.ACTIVO);

		escenario.cambiarEstado(EstadoEscenario.INACTIVO, ADMINISTRADOR_SISTEMA, false);

		assertThat(escenario.getEstado()).isEqualTo(EstadoEscenario.INACTIVO);
		assertThat(escenario.estaActivo()).isFalse();
	}

	// CA5 / política 8: el propietario tampoco puede, ni un jugador, ni alguien sin sesión
	@ParameterizedTest
	@EnumSource(value = Rol.class, names = "ADMINISTRADOR_SISTEMA", mode = EnumSource.Mode.EXCLUDE)
	void ca5SoloElAdministradorDelSistemaCambiaElEstado(Rol rol) {
		Escenario escenario = escenario(EstadoEscenario.INACTIVO);
		Solicitante solicitante = new Solicitante(escenario.getAdministradorId(), rol);

		esperarError(() -> escenario.cambiarEstado(EstadoEscenario.ACTIVO, solicitante, false), "ERR-ESC-08");
		assertThat(escenario.getEstado()).isEqualTo(EstadoEscenario.INACTIVO);
	}

	@Test
	void ca5SinSolicitanteTambienEsErrEsc08() {
		esperarError(() -> Escenario.validarAdministradorSistema(null), "ERR-ESC-08");
	}

	// CA6 / política 9
	@ParameterizedTest
	@EnumSource(EstadoEscenario.class)
	void ca6ElEstadoEnviadoEsIgualAlActual(EstadoEscenario estado) {
		Escenario escenario = escenario(estado);

		ReglaNegocioException error = esperarError(
				() -> escenario.cambiarEstado(estado, ADMINISTRADOR_SISTEMA, false), "ERR-ESC-09");

		// {estado} sale del catálogo: «Activo» o «Inactivo»
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("estado." + estado.name()));
		assertThat(escenario.getEstado()).isEqualTo(estado);
	}

	// CA7 / política 14 (RN-17, D08)
	@Test
	void ca7NoSeInactivaConReservasConfirmadasFuturas() {
		Escenario escenario = escenario(EstadoEscenario.ACTIVO);

		esperarError(() -> escenario.cambiarEstado(EstadoEscenario.INACTIVO, ADMINISTRADOR_SISTEMA, true),
				"ERR-ESC-14");
		assertThat(escenario.getEstado()).isEqualTo(EstadoEscenario.ACTIVO);
	}

	// Las reservas futuras solo impiden inactivar, no activar
	@Test
	void ca7LasReservasFuturasNoImpidenActivar() {
		Escenario escenario = escenario(EstadoEscenario.INACTIVO);

		escenario.cambiarEstado(EstadoEscenario.ACTIVO, ADMINISTRADOR_SISTEMA, true);

		assertThat(escenario.getEstado()).isEqualTo(EstadoEscenario.ACTIVO);
	}

}
