package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.uco.canchaya.escenarios.application.port.in.ModificarCancha.DatosModificacionCancha;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.escenarios.domain.TipoCancha;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU9: caso de uso «modificar cancha» (políticas 7, 2, 8, 4 y 5). */
class ModificarCanchaCasoUsoTest {

	private static final Solicitante PROPIETARIO = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

	private CanchasEnMemoria memoria;
	private ModificarCanchaCasoUso casoUso;
	private Escenario escenario;
	private TipoCancha f8;
	private Cancha cancha;

	@BeforeEach
	void preparar() {
		memoria = new CanchasEnMemoria();
		casoUso = new ModificarCanchaCasoUso(memoria.repositorioEscenarios(), memoria.repositorioCanchas(),
				memoria.repositorioTipos());
		escenario = memoria.escenarioDe(PROPIETARIO.usuarioId());
		f8 = memoria.tipo("F8", "Fútbol 8", 16);
		cancha = memoria.cancha(escenario, "Cancha 1", EstadoCancha.DISPONIBLE);
	}

	private DatosModificacionCancha datos(String nombre) {
		return new DatosModificacionCancha(cancha.getId(), f8.getId(), nombre, new BigDecimal("95000"));
	}

	private ReglaNegocioException esperarError(DatosModificacionCancha datos, Solicitante solicitante, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(
				() -> casoUso.ejecutar(datos, solicitante))
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
		// Ningún error cambia la cancha guardada
		Cancha guardada = memoria.guardada(cancha.getId());
		assertThat(guardada.getNombre()).isEqualTo("Cancha 1");
		assertThat(guardada.getTipoCanchaId()).isEqualTo(cancha.getTipoCanchaId());
		assertThat(guardada.getPrecioHora()).isEqualByComparingTo("80000.00");
		return error;
	}

	@Test
	void ca1Modificar_guardaLosCambios() {
		Cancha resultado = casoUso.ejecutar(datos("  Cancha Principal "), PROPIETARIO);

		Cancha guardada = memoria.guardada(cancha.getId());
		assertThat(guardada.getNombre()).isEqualTo("Cancha Principal");
		assertThat(guardada.getTipoCanchaId()).isEqualTo(f8.getId());
		assertThat(guardada.getPrecioHora()).isEqualByComparingTo("95000.00");
		assertThat(guardada.getEscenarioId()).isEqualTo(escenario.getId());
		assertThat(guardada.getEstado()).isEqualTo(EstadoCancha.DISPONIBLE);
		assertThat(resultado).usingRecursiveComparison().isEqualTo(guardada);
		assertThat(memoria.canchas).hasSize(1);
	}

	// CA1 y política 8: la propia cancha no cuenta como repetida
	@ParameterizedTest
	@ValueSource(strings = { "Cancha 1", "CANCHA 1", "cancha 1" })
	void ca1Modificar_mismoNombreConOtrasMayusculas_seGuarda(String nombre) {
		casoUso.ejecutar(datos(nombre), PROPIETARIO);

		assertThat(memoria.guardada(cancha.getId()).getNombre()).isEqualTo(nombre);
	}

	@Test
	void ca3CanchaInexistente_ERR_CAN_07() {
		DatosModificacionCancha inexistente = new DatosModificacionCancha(UUID.randomUUID(), f8.getId(), "Otra",
				BigDecimal.TEN);

		esperarError(inexistente, PROPIETARIO, "ERR-CAN-07");
	}

	@Test
	void ca4NoPropietario_ERR_CAN_02() {
		esperarError(datos("Otra"), new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA), "ERR-CAN-02");
	}

	// El encargado de sede tiene rol ADMINISTRADOR_CANCHA (D07), pero no es el propietario (RN-15)
	@Test
	void ca4EncargadoDeSede_ERR_CAN_02() {
		Solicitante encargado = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(datos("Otra"), encargado, "ERR-CAN-02");
	}

	// P3 de la especificación
	@Test
	void ca4AdministradorDelSistema_ERR_CAN_02() {
		Solicitante administrador = new Solicitante(PROPIETARIO.usuarioId(), Rol.ADMINISTRADOR_SISTEMA);

		esperarError(datos("Otra"), administrador, "ERR-CAN-02");
	}

	@ParameterizedTest
	@ValueSource(strings = { "Cancha 2", "CANCHA 2", "  cancha 2 " })
	void ca5NombreDeOtraCancha_ERR_CAN_08(String nombre) {
		memoria.cancha(escenario, "Cancha 2", EstadoCancha.DISPONIBLE);

		ReglaNegocioException error = esperarError(datos(nombre), PROPIETARIO, "ERR-CAN-08");
		// El mensaje cita el nombre que se intentó usar, ya recortado
		assertThat(error.getParametros()).containsExactly(nombre.strip());
	}

	@Test
	void ca5MismoNombreEnOtroEscenario_seGuarda() {
		Escenario otro = memoria.escenarioDe(PROPIETARIO.usuarioId());
		memoria.cancha(otro, "Cancha 2", EstadoCancha.DISPONIBLE);

		casoUso.ejecutar(datos("Cancha 2"), PROPIETARIO);

		assertThat(memoria.guardada(cancha.getId()).getNombre()).isEqualTo("Cancha 2");
	}

	@Test
	void ca6TipoInexistente_ERR_CAN_04() {
		DatosModificacionCancha conTipoInexistente = new DatosModificacionCancha(cancha.getId(), UUID.randomUUID(),
				"Otra", BigDecimal.TEN);

		esperarError(conTipoInexistente, PROPIETARIO, "ERR-CAN-04");
	}

	@Test
	void ca7PrecioMenorOIgualACero_ERR_CAN_05() {
		DatosModificacionCancha sinPrecio = new DatosModificacionCancha(cancha.getId(), f8.getId(), "Otra",
				BigDecimal.ZERO);

		esperarError(sinPrecio, PROPIETARIO, "ERR-CAN-05");
	}

}
