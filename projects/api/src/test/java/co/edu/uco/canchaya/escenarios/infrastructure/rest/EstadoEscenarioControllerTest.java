package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import co.edu.uco.canchaya.escenarios.application.port.in.CambiarEstadoEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarPerfilEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarEscenariosAdministracion;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarEscenariosAdministracion.EscenarioAdministracion;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/** HU13: lo que entra y sale por HTTP. Los casos de uso son dobles: sus reglas se prueban aparte. */
@WebMvcTest(EscenarioController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class EstadoEscenarioControllerTest {

	private static final UUID ADMINISTRADOR = UUID.randomUUID();
	private static final Solicitante ADMINISTRADOR_SISTEMA = new Solicitante(ADMINISTRADOR, Rol.ADMINISTRADOR_SISTEMA);
	private static final UUID ESCENARIO = UUID.randomUUID();

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ListarEscenariosAdministracion listarEscenarios;

	@MockitoBean
	private CambiarEstadoEscenario cambiarEstado;

	// Del mismo controlador, de la HU19; sus pruebas están en PerfilEscenarioControllerTest
	@MockitoBean
	private ConsultarPerfilEscenario consultarPerfil;

	@MockitoBean
	private RegistrarEscenario registrarEscenario;

	private static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder peticion) {
		return peticion.with(jwt().jwt(token -> token.subject(ADMINISTRADOR.toString())
				.claim("rol", Rol.ADMINISTRADOR_SISTEMA.name())));
	}

	private static MockHttpServletRequestBuilder cambiar(String cuerpo) {
		return patch("/api/escenarios/{id}/estado", ESCENARIO).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
	}

	private static Escenario escenario(EstadoEscenario estado) {
		return Escenario.reconstruir(ESCENARIO, "Cancha La 80", null, "Calle 80 # 45-10", UUID.randomUUID(),
				new BigDecimal("6.155123"), new BigDecimal("-75.373700"), "+573001234567", null, List.of(),
				UUID.randomUUID(), estado);
	}

	@Test
	void ca1ListaLosInactivosConCiudadYPropietario() throws Exception {
		when(listarEscenarios.ejecutar(EstadoEscenario.INACTIVO, ADMINISTRADOR_SISTEMA)).thenReturn(List.of(
				new EscenarioAdministracion(ESCENARIO, "Cancha La 80", "Rionegro", "Ana Pérez",
						EstadoEscenario.INACTIVO)));

		mvc.perform(conToken(get("/api/escenarios").param("estado", "INACTIVO")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(ESCENARIO.toString()))
				.andExpect(jsonPath("$[0].nombre").value("Cancha La 80"))
				.andExpect(jsonPath("$[0].ciudad").value("Rionegro"))
				.andExpect(jsonPath("$[0].propietario").value("Ana Pérez"))
				.andExpect(jsonPath("$[0].estado").value("INACTIVO"));
	}

	@Test
	void ca1SinFiltroPideTodos() throws Exception {
		when(listarEscenarios.ejecutar(null, ADMINISTRADOR_SISTEMA)).thenReturn(List.of());

		mvc.perform(conToken(get("/api/escenarios"))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
		verify(listarEscenarios).ejecutar(null, ADMINISTRADOR_SISTEMA);
	}

	@Test
	void laListaRespondeErrGen06SiNoEsAdministradorDelSistema() throws Exception {
		when(listarEscenarios.ejecutar(any(), any())).thenThrow(new ReglaNegocioException("ERR-GEN-06"));

		mvc.perform(conToken(get("/api/escenarios")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-06"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("No tiene permiso para realizar esta acción."));
	}

	@ParameterizedTest
	@CsvSource({ "ACTIVO, Activo", "INACTIVO, Inactivo" })
	void ca2Ca3RespondeExiEsc03ConElNuevoEstado(EstadoEscenario estado, String texto) throws Exception {
		when(cambiarEstado.ejecutar(ESCENARIO, estado, ADMINISTRADOR_SISTEMA)).thenReturn(escenario(estado));

		mvc.perform(conToken(cambiar("{\"estado\": \"" + estado + "\"}")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value("EXI-ESC-03"))
				.andExpect(jsonPath("$.mensaje").value("El escenario Cancha La 80 quedó en estado " + texto + "."))
				.andExpect(jsonPath("$.id").value(ESCENARIO.toString()))
				.andExpect(jsonPath("$.estado").value(estado.name()));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"ERR-ESC-06 | 404 | El escenario no existe o no está disponible.", // CA4: no existe
			"ERR-ESC-08 | 403 | Solo el administrador del sistema puede activar o inactivar escenarios.", // CA5: permiso
			"ERR-ESC-14 | 422 | No se puede inactivar el escenario porque tiene reservas confirmadas futuras en sus canchas." // CA7
	})
	void ca4Ca5Ca7LasReglasIncumplidasRespondenSuMensaje(String codigo, int estadoHttp, String texto) throws Exception {
		when(cambiarEstado.ejecutar(any(), any(), any())).thenThrow(new ReglaNegocioException(codigo));

		mvc.perform(conToken(cambiar("{\"estado\": \"INACTIVO\"}")))
				.andExpect(status().is(estadoHttp))
				.andExpect(jsonPath("$.errores[0].codigo").value(codigo))
				.andExpect(jsonPath("$.errores[0].mensaje").value(texto));
	}

	@Test
	void ca6RespondeErrEsc09ConElEstadoActual() throws Exception {
		when(cambiarEstado.ejecutar(any(), any(), any()))
				.thenThrow(new ReglaNegocioException("ERR-ESC-09", new TextoCatalogo("estado.ACTIVO")));

		mvc.perform(conToken(cambiar("{\"estado\": \"ACTIVO\"}")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-ESC-09"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("El escenario ya se encuentra en estado Activo."));
	}

	@Test
	void sinEstadoRespondeErrGen01() throws Exception {
		mvc.perform(conToken(cambiar("{}")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-01"))
				.andExpect(jsonPath("$.errores[0].campo").value("estado"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("El campo estado es obligatorio."));
		verifyNoInteractions(cambiarEstado);
	}

	@Test
	void unEstadoQueNoExisteRespondeErrGen07() throws Exception {
		mvc.perform(conToken(cambiar("{\"estado\": \"BORRADO\"}")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
		verifyNoInteractions(cambiarEstado);
	}

	@Test
	void unaUrlMalEscritaRespondeErrGen07YNo500() throws Exception {
		mvc.perform(conToken(patch("/api/escenarios/no-es-uuid/estado").contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\": \"ACTIVO\"}")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
		mvc.perform(conToken(get("/api/escenarios").param("estado", "PENDIENTE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
	}

	@Test
	void sinSesionRespondeErrGen05() throws Exception {
		mvc.perform(cambiar("{\"estado\": \"ACTIVO\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-05"));
		mvc.perform(get("/api/escenarios"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-05"));
		verifyNoInteractions(cambiarEstado, listarEscenarios);
	}

	@Test
	void elSolicitanteSaleDelToken() throws Exception {
		when(cambiarEstado.ejecutar(any(), any(), any())).thenReturn(escenario(EstadoEscenario.ACTIVO));

		mvc.perform(conToken(cambiar("{\"estado\": \"ACTIVO\"}"))).andExpect(status().isOk());

		verify(cambiarEstado).ejecutar(eq(ESCENARIO), eq(EstadoEscenario.ACTIVO), eq(ADMINISTRADOR_SISTEMA));
	}

}
