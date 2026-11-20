package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarNombreCanchaDisponible;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarCanchasEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarTiposCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.ModificarCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.ModificarCancha.DatosModificacionCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarCancha;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/** HU9: lo que entra y sale por HTTP. El caso de uso es un doble: sus reglas se prueban aparte. */
@WebMvcTest(CanchaController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class ModificarCanchaControllerTest {

	private static final UUID USUARIO = UUID.randomUUID();
	private static final Solicitante PROPIETARIO = new Solicitante(USUARIO, Rol.ADMINISTRADOR_CANCHA);
	private static final UUID CANCHA = UUID.randomUUID();
	private static final UUID F7 = UUID.randomUUID();

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ModificarCancha modificarCancha;

	@MockitoBean
	private RegistrarCancha registrarCancha;

	@MockitoBean
	private ConsultarNombreCanchaDisponible consultarNombreDisponible;

	@MockitoBean
	private ListarCanchasEscenario listarCanchas;

	@MockitoBean
	private ListarTiposCancha listarTipos;

	private static MockHttpServletRequestBuilder modificar(Object id, String cuerpo) {
		return put("/api/canchas/{id}", id).contentType(MediaType.APPLICATION_JSON).content(cuerpo)
				.with(jwt().jwt(token -> token.subject(USUARIO.toString())
						.claim("rol", Rol.ADMINISTRADOR_CANCHA.name())));
	}

	private static String cuerpo(String nombre, String tipo, String precio) {
		return """
				{"nombre": %s, "tipoCanchaId": %s, "precioHora": %s}""".formatted(nombre, tipo, precio);
	}

	private static String valido() {
		return cuerpo("\"Cancha Principal\"", "\"" + F7 + "\"", "95000");
	}

	@Test
	void ca1ModificarCancha_200_EXI_CAN_02_conLaCanchaGuardada() throws Exception {
		Cancha guardada = Cancha.reconstruir(CANCHA, UUID.randomUUID(), F7, "Cancha Principal",
				new BigDecimal("95000.00"), EstadoCancha.DISPONIBLE);
		when(modificarCancha.ejecutar(any(), eq(PROPIETARIO))).thenReturn(guardada);

		mvc.perform(modificar(CANCHA, cuerpo("\"  Cancha Principal \"", "\"" + F7 + "\"", "95000")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value("EXI-CAN-02"))
				.andExpect(jsonPath("$.mensaje").value("La cancha se modificó correctamente."))
				// AG-04: la cancha como quedó, con la forma de la fila de la lista (RespuestaCancha)
				.andExpect(jsonPath("$.cancha.id").value(CANCHA.toString()))
				.andExpect(jsonPath("$.cancha.nombre").value("Cancha Principal"))
				.andExpect(jsonPath("$.cancha.tipoCanchaId").value(F7.toString()))
				.andExpect(jsonPath("$.cancha.precioHora").value(95000.00))
				.andExpect(jsonPath("$.cancha.estado").value("DISPONIBLE"));

		// El id sale de la URL y el nombre llega sin espacios en los extremos (AG-06)
		verify(modificarCancha).ejecutar(
				new DatosModificacionCancha(CANCHA, F7, "Cancha Principal", new BigDecimal("95000")), PROPIETARIO);
	}

	// CA2: obligatoriedad (01), formato (02) y longitud (03). El precio en cero es CA7, no CA2
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"null       | F7   | 95000      | nombre       | ERR-GEN-01 | El campo nombre es obligatorio.",
			"'\"   \"'  | F7   | 95000      | nombre       | ERR-GEN-01 | El campo nombre es obligatorio.",
			"LARGO      | F7   | 95000      | nombre       | ERR-GEN-03 | El campo nombre debe tener entre 1 y 50 caracteres.",
			"'\"C1\"'   | null | 95000      | tipoCanchaId | ERR-GEN-01 | El campo tipo de cancha es obligatorio.",
			"'\"C1\"'   | F7   | null       | precioHora   | ERR-GEN-01 | El campo precio por hora es obligatorio.",
			"'\"C1\"'   | F7   | 95000.555  | precioHora   | ERR-GEN-02 | El campo precio por hora no tiene un formato válido. Escriba solo números, con máximo 8 cifras enteras y 2 decimales. Ejemplo: 80000" })
	void ca2DatosInvalidos_400_noLleganAlCasoDeUso(String nombre, String tipo, String precio, String campo,
			String codigo, String mensaje) throws Exception {
		String valorNombre = "LARGO".equals(nombre) ? "\"" + "a".repeat(51) + "\"" : nombre;
		String valorTipo = "F7".equals(tipo) ? "\"" + F7 + "\"" : tipo;

		mvc.perform(modificar(CANCHA, cuerpo(valorNombre, valorTipo, precio)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(1)))
				.andExpect(jsonPath("$.errores[0].codigo").value(codigo))
				.andExpect(jsonPath("$.errores[0].campo").value(campo))
				.andExpect(jsonPath("$.errores[0].mensaje").value(mensaje));
		verifyNoInteractions(modificarCancha);
	}

	// CA2 / AG-03: todos los campos con error a la vez
	@Test
	void ca2DatosInvalidos_400_conTodosLosErrores() throws Exception {
		mvc.perform(modificar(CANCHA, "{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(3)))
				.andExpect(jsonPath("$.errores[*].campo").value(containsInAnyOrder("nombre", "tipoCanchaId",
						"precioHora")));
		verifyNoInteractions(modificarCancha);
	}

	@Test
	void idQueNoEsUuid_400_ERR_GEN_07() throws Exception {
		mvc.perform(modificar("no-es-uuid", valido()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
		verifyNoInteractions(modificarCancha);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"ERR-CAN-07 | 404 | La cancha no existe.", // CA3
			"ERR-CAN-02 | 403 | No tiene permiso para gestionar las canchas de este escenario.", // CA4
			"ERR-CAN-04 | 422 | El tipo de cancha seleccionado no existe. Elija un tipo de la lista.", // CA6
			"ERR-CAN-05 | 422 | El precio por hora debe ser mayor que cero." }) // CA7
	void ca3Ca4Ca6Ca7ReglasDelCasoDeUso(String codigo, int estado, String mensaje) throws Exception {
		when(modificarCancha.ejecutar(any(), any())).thenThrow(new ReglaNegocioException(codigo));

		mvc.perform(modificar(CANCHA, valido()))
				.andExpect(status().is(estado))
				.andExpect(jsonPath("$.errores[0].codigo").value(codigo))
				.andExpect(jsonPath("$.errores[0].mensaje").value(mensaje));
	}

	@Test
	void ca5NombreDeOtraCancha_422_ERR_CAN_08_conElNombre() throws Exception {
		when(modificarCancha.ejecutar(any(), any())).thenThrow(new ReglaNegocioException("ERR-CAN-08", "Cancha 2"));

		mvc.perform(modificar(CANCHA, valido()))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-CAN-08"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("Ya existe otra cancha llamada Cancha 2 en este escenario."));
	}

	@Test
	void sinToken_401() throws Exception {
		mvc.perform(put("/api/canchas/{id}", CANCHA).contentType(MediaType.APPLICATION_JSON).content(valido()))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(modificarCancha);
	}

}
