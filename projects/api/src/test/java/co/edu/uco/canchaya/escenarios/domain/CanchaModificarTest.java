package co.edu.uco.canchaya.escenarios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU9: responsabilidad «modificar» de Cancha (políticas 2 y 5). */
class CanchaModificarTest {

	private static final UUID PROPIETARIO_ID = UUID.randomUUID();
	private static final Solicitante PROPIETARIO = new Solicitante(PROPIETARIO_ID, Rol.ADMINISTRADOR_CANCHA);
	private static final UUID TIPO_F5 = UUID.randomUUID();
	private static final UUID TIPO_F8 = UUID.randomUUID();

	private final Escenario escenario = Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null,
			"Calle 80 # 45-10", UUID.randomUUID(), new BigDecimal("6.155123"), new BigDecimal("-75.373700"),
			"+573001234567", null, List.of(), PROPIETARIO_ID, EstadoEscenario.ACTIVO);

	private Cancha canchaGuardada(EstadoCancha estado) {
		return Cancha.reconstruir(UUID.randomUUID(), escenario.getId(), TIPO_F5, "Cancha 1",
				new BigDecimal("80000.00"), estado);
	}

	private static void esperarError(Runnable accion, String codigo) {
		assertThatThrownBy(accion::run).isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje()).isEqualTo(codigo);
	}

	private static void sinCambios(Cancha cancha) {
		assertThat(cancha.getNombre()).isEqualTo("Cancha 1");
		assertThat(cancha.getTipoCanchaId()).isEqualTo(TIPO_F5);
		assertThat(cancha.getPrecioHora()).isEqualByComparingTo("80000.00");
	}

	@ParameterizedTest
	@EnumSource(EstadoCancha.class)
	void ca1Modificar_cambiaLosDatosYConservaEscenarioYEstado(EstadoCancha estado) {
		Cancha cancha = canchaGuardada(estado);
		UUID id = cancha.getId();

		cancha.modificar(escenario, TIPO_F8, "  Cancha Principal ", new BigDecimal("95000.5"), PROPIETARIO);

		assertThat(cancha.getNombre()).isEqualTo("Cancha Principal");
		assertThat(cancha.getTipoCanchaId()).isEqualTo(TIPO_F8);
		assertThat(cancha.getPrecioHora()).isEqualByComparingTo("95000.50");
		assertThat(cancha.getPrecioHora().scale()).isEqualTo(2);
		assertThat(cancha.getId()).isEqualTo(id);
		assertThat(cancha.getEscenarioId()).isEqualTo(escenario.getId());
		assertThat(cancha.getEstado()).isEqualTo(estado);
	}

	// CA4: otro administrador de cancha
	@Test
	void ca4NoPropietario_ERR_CAN_02() {
		Cancha cancha = canchaGuardada(EstadoCancha.DISPONIBLE);
		Solicitante otro = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(() -> cancha.modificar(escenario, TIPO_F8, "Otra", BigDecimal.TEN, otro), "ERR-CAN-02");
		sinCambios(cancha);
	}

	// CA4: el encargado de sede tiene rol ADMINISTRADOR_CANCHA (D07), pero no es el propietario (RN-15)
	@Test
	void ca4EncargadoDeSede_ERR_CAN_02() {
		Cancha cancha = canchaGuardada(EstadoCancha.DISPONIBLE);
		Solicitante encargado = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(() -> cancha.modificar(escenario, TIPO_F8, "Otra", BigDecimal.TEN, encargado), "ERR-CAN-02");
		sinCambios(cancha);
	}

	// CA4 y P3 de la especificación: tampoco el administrador del sistema, aunque el id coincida
	@ParameterizedTest
	@EnumSource(value = Rol.class, names = { "JUGADOR", "ADMINISTRADOR_SISTEMA" })
	void ca4AdministradorDelSistema_ERR_CAN_02(Rol rol) {
		Cancha cancha = canchaGuardada(EstadoCancha.DISPONIBLE);
		Solicitante solicitante = new Solicitante(PROPIETARIO_ID, rol);

		esperarError(() -> cancha.modificar(escenario, TIPO_F8, "Otra", BigDecimal.TEN, solicitante), "ERR-CAN-02");
		sinCambios(cancha);
	}

	@Test
	void ca4SinSolicitante_ERR_CAN_02() {
		Cancha cancha = canchaGuardada(EstadoCancha.DISPONIBLE);

		esperarError(() -> cancha.modificar(escenario, TIPO_F8, "Otra", BigDecimal.TEN, null), "ERR-CAN-02");
	}

	// CA7 / política 5. 0.004 queda en 0.00 al redondear a 2 decimales
	@ParameterizedTest
	@ValueSource(strings = { "0", "0.00", "-1", "0.004" })
	void ca7PrecioMenorOIgualACero_ERR_CAN_05(String precio) {
		Cancha cancha = canchaGuardada(EstadoCancha.DISPONIBLE);

		esperarError(() -> cancha.modificar(escenario, TIPO_F8, "Otra", new BigDecimal(precio), PROPIETARIO),
				"ERR-CAN-05");
		sinCambios(cancha);
	}

	@Test
	void ca7SinPrecio_ERR_CAN_05() {
		Cancha cancha = canchaGuardada(EstadoCancha.DISPONIBLE);

		esperarError(() -> cancha.modificar(escenario, TIPO_F8, "Otra", null, PROPIETARIO), "ERR-CAN-05");
	}

	// No es una regla de negocio: el caso de uso siempre pasa el escenario de la cancha
	@Test
	void conOtroEscenario_esErrorDeProgramacion() {
		Cancha cancha = canchaGuardada(EstadoCancha.DISPONIBLE);
		Escenario otro = Escenario.reconstruir(UUID.randomUUID(), "Otro", null, "Calle 1", UUID.randomUUID(),
				BigDecimal.ONE, BigDecimal.ONE, "+573001234567", null, List.of(), PROPIETARIO_ID,
				EstadoEscenario.ACTIVO);

		assertThatThrownBy(() -> cancha.modificar(otro, TIPO_F8, "Otra", BigDecimal.TEN, PROPIETARIO))
				.isInstanceOf(IllegalArgumentException.class);
		sinCambios(cancha);
	}

}
