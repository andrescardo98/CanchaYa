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

/** HU10: responsabilidad «cambiarEstado» de Cancha (políticas 2, 10 y 12) y su efecto en la 11. */
class CanchaCambiarEstadoTest {

	private static final UUID PROPIETARIO_ID = UUID.randomUUID();
	private static final Solicitante PROPIETARIO = new Solicitante(PROPIETARIO_ID, Rol.ADMINISTRADOR_CANCHA);

	private static Escenario escenario(EstadoEscenario estado) {
		return Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null, "Calle 80 # 45-10", UUID.randomUUID(),
				new BigDecimal("6.155123"), new BigDecimal("-75.373700"), "+573001234567", null, List.of(),
				PROPIETARIO_ID, estado);
	}

	private final Escenario escenario = escenario(EstadoEscenario.ACTIVO);

	private static Cancha cancha(Escenario escenario, EstadoCancha estado) {
		return Cancha.reconstruir(UUID.randomUUID(), escenario.getId(), UUID.randomUUID(), "Cancha 1",
				new BigDecimal("80000.00"), estado);
	}

	private static ReglaNegocioException esperarError(Runnable accion, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
		return error;
	}

	// CA1: deja de estar disponible y de publicarse (política 11, RN-04)
	@Test
	void ca1Inactivar_quedaInactivaYNoSePublica() {
		Cancha cancha = cancha(escenario, EstadoCancha.DISPONIBLE);

		cancha.cambiarEstado(escenario, EstadoCancha.INACTIVA, PROPIETARIO, false);

		assertThat(cancha.getEstado()).isEqualTo(EstadoCancha.INACTIVA);
		assertThat(cancha.estaDisponible()).isFalse();
		assertThat(cancha.publicarEnBusqueda(escenario, true)).isFalse();
	}

	// CA2: vuelve a publicarse porque cumple las demás condiciones de RN-04
	@Test
	void ca2Activar_vuelveAPublicarseSiCumpleRN04() {
		Cancha cancha = cancha(escenario, EstadoCancha.INACTIVA);

		cancha.cambiarEstado(escenario, EstadoCancha.DISPONIBLE, PROPIETARIO, false);

		assertThat(cancha.getEstado()).isEqualTo(EstadoCancha.DISPONIBLE);
		assertThat(cancha.estaDisponible()).isTrue();
		assertThat(cancha.publicarEnBusqueda(escenario, true)).isTrue();
	}

	// CA2: «si cumple las demás condiciones de RN-04». Con el escenario INACTIVO se activa, pero no se publica
	@Test
	void ca2Activar_escenarioInactivo_seActivaPeroNoSePublica() {
		Escenario inactivo = escenario(EstadoEscenario.INACTIVO);
		Cancha cancha = cancha(inactivo, EstadoCancha.INACTIVA);

		cancha.cambiarEstado(inactivo, EstadoCancha.DISPONIBLE, PROPIETARIO, false);

		assertThat(cancha.getEstado()).isEqualTo(EstadoCancha.DISPONIBLE);
		assertThat(cancha.publicarEnBusqueda(inactivo, true)).isFalse();
	}

	// CA4 / política 2: otro administrador de cancha
	@Test
	void ca4NoPropietario_ERR_CAN_02() {
		Cancha cancha = cancha(escenario, EstadoCancha.DISPONIBLE);
		Solicitante otro = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(() -> cancha.cambiarEstado(escenario, EstadoCancha.INACTIVA, otro, false), "ERR-CAN-02");
		assertThat(cancha.getEstado()).isEqualTo(EstadoCancha.DISPONIBLE);
	}

	// CA4: el encargado de sede tiene rol ADMINISTRADOR_CANCHA (D07), pero no es el propietario (RN-15)
	@Test
	void ca4EncargadoDeSede_ERR_CAN_02() {
		Cancha cancha = cancha(escenario, EstadoCancha.DISPONIBLE);
		Solicitante encargado = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(() -> cancha.cambiarEstado(escenario, EstadoCancha.INACTIVA, encargado, false), "ERR-CAN-02");
		assertThat(cancha.getEstado()).isEqualTo(EstadoCancha.DISPONIBLE);
	}

	// CA4 y P3 de la especificación: tampoco el administrador del sistema, aunque el id coincida
	@ParameterizedTest
	@EnumSource(value = Rol.class, names = { "JUGADOR", "ADMINISTRADOR_SISTEMA" })
	void ca4AdministradorDelSistema_ERR_CAN_02(Rol rol) {
		Cancha cancha = cancha(escenario, EstadoCancha.DISPONIBLE);
		Solicitante solicitante = new Solicitante(PROPIETARIO_ID, rol);

		esperarError(() -> cancha.cambiarEstado(escenario, EstadoCancha.INACTIVA, solicitante, false), "ERR-CAN-02");
		assertThat(cancha.getEstado()).isEqualTo(EstadoCancha.DISPONIBLE);
	}

	@Test
	void ca4SinSolicitante_ERR_CAN_02() {
		Cancha cancha = cancha(escenario, EstadoCancha.DISPONIBLE);

		esperarError(() -> cancha.cambiarEstado(escenario, EstadoCancha.INACTIVA, null, false), "ERR-CAN-02");
	}

	// CA5 / política 10, en los dos sentidos
	@ParameterizedTest
	@EnumSource(EstadoCancha.class)
	void ca5MismoEstado_ERR_CAN_10(EstadoCancha estado) {
		Cancha cancha = cancha(escenario, estado);

		ReglaNegocioException error = esperarError(
				() -> cancha.cambiarEstado(escenario, estado, PROPIETARIO, false), "ERR-CAN-10");

		// {estado} sale del catálogo: «Disponible» o «Inactiva»
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("estado." + estado.name()));
		assertThat(cancha.getEstado()).isEqualTo(estado);
	}

	// CA6 / política 12 (RN-17, D08)
	@Test
	void ca6ReservasFuturas_ERR_CAN_12() {
		Cancha cancha = cancha(escenario, EstadoCancha.DISPONIBLE);

		esperarError(() -> cancha.cambiarEstado(escenario, EstadoCancha.INACTIVA, PROPIETARIO, true), "ERR-CAN-12");
		assertThat(cancha.getEstado()).isEqualTo(EstadoCancha.DISPONIBLE);
	}

	// La política 12 solo impide inactivar: activar con reservas futuras sí se puede
	@Test
	void ca6ActivarConReservasFuturas_seActiva() {
		Cancha cancha = cancha(escenario, EstadoCancha.INACTIVA);

		cancha.cambiarEstado(escenario, EstadoCancha.DISPONIBLE, PROPIETARIO, true);

		assertThat(cancha.getEstado()).isEqualTo(EstadoCancha.DISPONIBLE);
	}

	// No es una regla de negocio: el caso de uso siempre pasa el escenario de la cancha
	@Test
	void conOtroEscenario_esErrorDeProgramacion() {
		Cancha cancha = cancha(escenario, EstadoCancha.DISPONIBLE);
		Escenario otro = escenario(EstadoEscenario.ACTIVO);

		assertThatThrownBy(() -> cancha.cambiarEstado(otro, EstadoCancha.INACTIVA, PROPIETARIO, false))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(cancha.getEstado()).isEqualTo(EstadoCancha.DISPONIBLE);
	}

	@Test
	void sinNuevoEstado_esErrorDeProgramacion() {
		Cancha cancha = cancha(escenario, EstadoCancha.DISPONIBLE);

		assertThatThrownBy(() -> cancha.cambiarEstado(escenario, null, PROPIETARIO, false))
				.isInstanceOf(NullPointerException.class);
	}

}
