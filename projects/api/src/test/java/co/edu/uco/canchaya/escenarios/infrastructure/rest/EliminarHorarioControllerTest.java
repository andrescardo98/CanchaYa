package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import co.edu.uco.canchaya.escenarios.application.port.in.DefinirHorario;
import co.edu.uco.canchaya.escenarios.application.port.in.EliminarHorario;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarHorarios;
import co.edu.uco.canchaya.escenarios.application.port.in.ModificarHorario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/** HU12: lo que entra y sale por HTTP. El caso de uso es un doble: sus reglas se prueban aparte. */
@WebMvcTest(HorarioController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class EliminarHorarioControllerTest {

	private static final UUID USUARIO = UUID.randomUUID();
	private static final Solicitante PROPIETARIO = new Solicitante(USUARIO, Rol.ADMINISTRADOR_CANCHA);
	private static final UUID HORARIO = UUID.randomUUID();

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private EliminarHorario eliminarHorario;

	// HU7 y HU11: los pide HorarioController; sus pruebas están en sus propias clases
	@MockitoBean
	private DefinirHorario definirHorario;

	@MockitoBean
	private ListarHorarios listarHorarios;

	@MockitoBean
	private ModificarHorario modificarHorario;

	private static MockHttpServletRequestBuilder eliminar(Object id) {
		return delete("/api/horarios/{id}", id).with(jwt().jwt(token -> token.subject(USUARIO.toString())
				.claim("rol", Rol.ADMINISTRADOR_CANCHA.name())));
	}

	// CA1 y P3: 200 con el código y el texto del catálogo
	@Test
	void ca1EliminarHorario_200_EXI_HOR_03() throws Exception {
		mvc.perform(eliminar(HORARIO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value("EXI-HOR-03"))
				.andExpect(jsonPath("$.mensaje").value("El horario de atención se eliminó correctamente."));

		verify(eliminarHorario).ejecutar(HORARIO, PROPIETARIO);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"ERR-HOR-06 | 404 | El horario de atención no existe.", // CA4
			"ERR-HOR-02 | 403 | No tiene permiso para gestionar los horarios de este escenario.", // CA5
			"ERR-HOR-15 | 422 | No se puede eliminar el horario porque hay reservas confirmadas futuras dentro de él." // CA6
	})
	void ca4Ca5Ca6LasReglasIncumplidasRespondenSuMensaje(String codigo, int estadoHttp, String texto)
			throws Exception {
		doThrow(new ReglaNegocioException(codigo)).when(eliminarHorario).ejecutar(any(), any());

		mvc.perform(eliminar(HORARIO))
				.andExpect(status().is(estadoHttp))
				.andExpect(jsonPath("$.errores[0].codigo").value(codigo))
				.andExpect(jsonPath("$.errores[0].mensaje").value(texto));
	}

	@Test
	void sinSesion_401_ERR_GEN_05() throws Exception {
		mvc.perform(delete("/api/horarios/{id}", HORARIO))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-05"));
		verifyNoInteractions(eliminarHorario);
	}

	@Test
	void unIdentificadorQueNoEsUuid_400_ERR_GEN_07() throws Exception {
		mvc.perform(eliminar("no-es-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
		verifyNoInteractions(eliminarHorario);
	}

}
