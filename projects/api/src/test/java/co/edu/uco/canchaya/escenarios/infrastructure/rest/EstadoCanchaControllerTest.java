package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import co.edu.uco.canchaya.escenarios.application.port.in.CambiarEstadoCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarNombreCanchaDisponible;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarCanchasEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarTiposCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.ModificarCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarCancha;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/** HU10: lo que entra y sale por HTTP. El caso de uso es un doble: sus reglas se prueban aparte. */
@WebMvcTest(CanchaController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class EstadoCanchaControllerTest {

	private static final UUID USUARIO = UUID.randomUUID();
	private static final Solicitante PROPIETARIO = new Solicitante(USUARIO, Rol.ADMINISTRADOR_CANCHA);
	private static final UUID CANCHA = UUID.randomUUID();

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CambiarEstadoCancha cambiarEstado;

	// Del mismo controlador (HU6 y HU9); sus pruebas están en CanchaControllerTest y ModificarCanchaControllerTest
	@MockitoBean
	private RegistrarCancha registrarCancha;

	@MockitoBean
	private ModificarCancha modificarCancha;

	@MockitoBean
	private ConsultarNombreCanchaDisponible consultarNombreDisponible;

	@MockitoBean
	private ListarCanchasEscenario listarCanchas;

	@MockitoBean
	private ListarTiposCancha listarTipos;

	private static MockHttpServletRequestBuilder sinToken(Object id, String cuerpo) {
		return patch("/api/canchas/{id}/estado", id).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
	}

	private static MockHttpServletRequestBuilder cambiar(Object id, String cuerpo) {
		return sinToken(id, cuerpo).with(jwt().jwt(token -> token.subject(USUARIO.toString())
				.claim("rol", Rol.ADMINISTRADOR_CANCHA.name())));
	}

	private static Cancha cancha(EstadoCancha estado) {
		return Cancha.reconstruir(CANCHA, UUID.randomUUID(), UUID.randomUUID(), "Cancha 1",
				new BigDecimal("80000.00"), estado);
	}

	// CA1 y CA2: el mensaje dice el nombre y el nuevo estado, y la respuesta trae el estado para la fila
	@ParameterizedTest
	@CsvSource({ "INACTIVA, Inactiva", "DISPONIBLE, Disponible" })
	void ca1Ca2CambiarEstado_200_EXI_CAN_03(EstadoCancha estado, String texto) throws Exception {
		when(cambiarEstado.ejecutar(CANCHA, estado, PROPIETARIO)).thenReturn(cancha(estado));

		mvc.perform(cambiar(CANCHA, "{\"estado\": \"" + estado + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value("EXI-CAN-03"))
				.andExpect(jsonPath("$.mensaje").value("La cancha Cancha 1 quedó en estado " + texto + "."))
				.andExpect(jsonPath("$.id").value(CANCHA.toString()))
				.andExpect(jsonPath("$.estado").value(estado.name()));
		// El solicitante sale del token, no del cuerpo (AG-12)
		verify(cambiarEstado).ejecutar(CANCHA, estado, PROPIETARIO);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"ERR-CAN-07 | 404 | La cancha no existe.", // CA3
			"ERR-CAN-02 | 403 | No tiene permiso para gestionar las canchas de este escenario.", // CA4
			"ERR-CAN-12 | 422 | No se puede inactivar la cancha porque tiene reservas confirmadas futuras." // CA6
	})
	void ca3Ca4Ca6LasReglasIncumplidasRespondenSuMensaje(String codigo, int estadoHttp, String texto)
			throws Exception {
		when(cambiarEstado.ejecutar(any(), any(), any())).thenThrow(new ReglaNegocioException(codigo));

		mvc.perform(cambiar(CANCHA, "{\"estado\": \"INACTIVA\"}"))
				.andExpect(status().is(estadoHttp))
				.andExpect(jsonPath("$.errores[0].codigo").value(codigo))
				.andExpect(jsonPath("$.errores[0].mensaje").value(texto));
	}

	// CA5
	@ParameterizedTest
	@CsvSource({ "DISPONIBLE, Disponible", "INACTIVA, Inactiva" })
	void ca5MismoEstado_422_ERR_CAN_10(EstadoCancha estado, String texto) throws Exception {
		when(cambiarEstado.ejecutar(any(), any(), any()))
				.thenThrow(new ReglaNegocioException("ERR-CAN-10", new TextoCatalogo("estado." + estado.name())));

		mvc.perform(cambiar(CANCHA, "{\"estado\": \"" + estado + "\"}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-CAN-10"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("La cancha ya se encuentra en estado " + texto + "."));
	}

	@Test
	void estadoInvalido_400_sinEstado_ERR_GEN_01() throws Exception {
		mvc.perform(cambiar(CANCHA, "{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-01"))
				.andExpect(jsonPath("$.errores[0].campo").value("estado"));
		verifyNoInteractions(cambiarEstado);
	}

	// Un estado de escenario (INACTIVO) tampoco sirve para una cancha
	@ParameterizedTest
	@CsvSource({ "BORRADA", "INACTIVO", "ACTIVO" })
	void estadoInvalido_400_estadoQueNoExiste_ERR_GEN_07(String estado) throws Exception {
		mvc.perform(cambiar(CANCHA, "{\"estado\": \"" + estado + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
		verifyNoInteractions(cambiarEstado);
	}

	@Test
	void estadoInvalido_400_idQueNoEsUuid_ERR_GEN_07() throws Exception {
		mvc.perform(cambiar("no-es-uuid", "{\"estado\": \"INACTIVA\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
		verifyNoInteractions(cambiarEstado);
	}

	@Test
	void sinSesion_401_ERR_GEN_05() throws Exception {
		mvc.perform(sinToken(CANCHA, "{\"estado\": \"INACTIVA\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-05"));
		verifyNoInteractions(cambiarEstado);
	}

}
