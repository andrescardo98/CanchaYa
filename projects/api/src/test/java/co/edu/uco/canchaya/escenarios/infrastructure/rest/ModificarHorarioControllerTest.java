package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;
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

import co.edu.uco.canchaya.escenarios.application.port.in.DefinirHorario;
import co.edu.uco.canchaya.escenarios.application.port.in.EliminarHorario;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarHorarios;
import co.edu.uco.canchaya.escenarios.application.port.in.ModificarHorario;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/** HU11: lo que entra y sale por HTTP. El caso de uso es un doble: sus reglas se prueban aparte. */
@WebMvcTest(HorarioController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class ModificarHorarioControllerTest {

	private static final UUID USUARIO = UUID.randomUUID();
	private static final Solicitante PROPIETARIO = new Solicitante(USUARIO, Rol.ADMINISTRADOR_CANCHA);
	private static final UUID HORARIO = UUID.randomUUID();

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ModificarHorario modificarHorario;

	// HU7: los pide HorarioController; sus pruebas están en HorarioControllerTest
	@MockitoBean
	private DefinirHorario definirHorario;

	@MockitoBean
	private ListarHorarios listarHorarios;

	// HU12: lo pide HorarioController; sus pruebas están en EliminarHorarioControllerTest
	@MockitoBean
	private EliminarHorario eliminarHorario;

	private static MockHttpServletRequestBuilder modificar(Object id, String cuerpo) {
		return put("/api/horarios/{id}", id).contentType(MediaType.APPLICATION_JSON).content(cuerpo)
				.with(jwt().jwt(token -> token.subject(USUARIO.toString())
						.claim("rol", Rol.ADMINISTRADOR_CANCHA.name())));
	}

	private static final String VALIDO = """
			{"diaSemana": "MARTES", "horaInicio": "07:00", "horaFin": "13:00"}""";

	@Test
	void ca1ModificarHorario_200_EXI_HOR_02_conElHorarioGuardado() throws Exception {
		HorarioRecurrente modificado = HorarioRecurrente.reconstruir(HORARIO, UUID.randomUUID(), DiaSemana.MARTES,
				LocalTime.of(7, 0), LocalTime.of(13, 0));
		when(modificarHorario.ejecutar(HORARIO, DiaSemana.MARTES, LocalTime.of(7, 0), LocalTime.of(13, 0),
				PROPIETARIO)).thenReturn(modificado);

		mvc.perform(modificar(HORARIO, VALIDO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value("EXI-HOR-02"))
				.andExpect(jsonPath("$.mensaje").value("El horario de atención se modificó correctamente."))
				.andExpect(jsonPath("$.horario.id").value(HORARIO.toString()))
				.andExpect(jsonPath("$.horario.diaSemana").value("MARTES"))
				.andExpect(jsonPath("$.horario.horaInicio").value("07:00"))
				.andExpect(jsonPath("$.horario.horaFin").value("13:00"));
	}

	// CA2: los tres campos vacíos se devuelven a la vez (AG-03)
	@Test
	void ca2SinDatos_400_ERR_GEN_01PorCadaCampo() throws Exception {
		mvc.perform(modificar(HORARIO, "{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(3)))
				.andExpect(jsonPath("$.errores[?(@.campo == 'diaSemana')].codigo").value("ERR-GEN-01"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'horaInicio')].codigo").value("ERR-GEN-01"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'horaFin')].codigo").value("ERR-GEN-01"));
		verifyNoInteractions(modificarHorario);
	}

	// CA2: formato, todos los errores a la vez
	@Test
	void ca2DatosInvalidos_400_conTodosLosErrores() throws Exception {
		mvc.perform(modificar(HORARIO, """
				{"diaSemana": "FERIADO", "horaInicio": "7:00", "horaFin": "25:00"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(3)))
				.andExpect(jsonPath("$.errores[?(@.campo == 'diaSemana')].codigo").value("ERR-GEN-02"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'horaInicio')].codigo").value("ERR-GEN-02"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'horaFin')].codigo").value("ERR-GEN-02"));
		verifyNoInteractions(modificarHorario);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"ERR-HOR-06 | 404 | El horario de atención no existe.", // CA3
			"ERR-HOR-02 | 403 | No tiene permiso para gestionar los horarios de este escenario.", // CA4
			"ERR-HOR-03 | 422 | La hora de fin debe ser mayor que la hora de inicio.", // CA5
			"ERR-HOR-13 | 422 | Los horarios se definen en horas en punto (por ejemplo, 18:00)." // CA7
	})
	void ca3Ca4Ca5Ca7LasReglasIncumplidasRespondenSuMensaje(String codigo, int estadoHttp, String texto)
			throws Exception {
		when(modificarHorario.ejecutar(any(), any(), any(), any(), any())).thenThrow(new ReglaNegocioException(codigo));

		mvc.perform(modificar(HORARIO, VALIDO))
				.andExpect(status().is(estadoHttp))
				.andExpect(jsonPath("$.errores[0].codigo").value(codigo))
				.andExpect(jsonPath("$.errores[0].mensaje").value(texto));
	}

	@Test
	void ca6SeCruza_422_ERR_HOR_07ConElDia() throws Exception {
		when(modificarHorario.ejecutar(any(), any(), any(), any(), any()))
				.thenThrow(new ReglaNegocioException("ERR-HOR-07", new TextoCatalogo("dia.MIERCOLES")));

		mvc.perform(modificar(HORARIO, VALIDO))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-HOR-07"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("El horario se cruza con otro horario ya definido para el miércoles."));
	}

	@Test
	void ca7UnaHoraQueNoEsEnPuntoLlegaAlCasoDeUso() throws Exception {
		when(modificarHorario.ejecutar(any(), any(), any(), any(), any()))
				.thenThrow(new ReglaNegocioException("ERR-HOR-13"));

		mvc.perform(modificar(HORARIO, """
				{"diaSemana": "MARTES", "horaInicio": "07:30", "horaFin": "13:00"}"""))
				.andExpect(status().isUnprocessableContent());

		// El formato hh:mm es válido: los minutos los revisa la política 13 y no el formulario
		verify(modificarHorario).ejecutar(HORARIO, DiaSemana.MARTES, LocalTime.of(7, 30), LocalTime.of(13, 0),
				PROPIETARIO);
	}

	@Test
	void sinSesion_401_ERR_GEN_05() throws Exception {
		mvc.perform(put("/api/horarios/{id}", HORARIO).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-05"));
		verifyNoInteractions(modificarHorario);
	}

	@Test
	void unIdentificadorQueNoEsUuid_400_ERR_GEN_07() throws Exception {
		mvc.perform(modificar("no-es-uuid", VALIDO))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
		verifyNoInteractions(modificarHorario);
	}

}
