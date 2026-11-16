package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;
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

import co.edu.uco.canchaya.escenarios.application.port.in.DefinirHorario;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarHorarios;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/** HU7: lo que entra y sale por HTTP. Los casos de uso son dobles: sus reglas se prueban aparte. */
@WebMvcTest(HorarioController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class HorarioControllerTest {

	private static final UUID USUARIO = UUID.randomUUID();
	private static final Solicitante PROPIETARIO = new Solicitante(USUARIO, Rol.ADMINISTRADOR_CANCHA);
	private static final UUID ESCENARIO = UUID.randomUUID();

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private DefinirHorario definirHorario;

	@MockitoBean
	private ListarHorarios listarHorarios;

	private static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder peticion) {
		return peticion.with(jwt().jwt(token -> token.subject(USUARIO.toString())
				.claim("rol", Rol.ADMINISTRADOR_CANCHA.name())));
	}

	private static MockHttpServletRequestBuilder definir(String cuerpo) {
		return conToken(post("/api/escenarios/{id}/horarios", ESCENARIO).contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo));
	}

	private static HorarioRecurrente horario(DiaSemana dia, int inicio, int fin) {
		return HorarioRecurrente.reconstruir(UUID.randomUUID(), ESCENARIO, dia, LocalTime.of(inicio, 0),
				LocalTime.of(fin, 0));
	}

	@Test
	void ca1RespondeExiHor01ConElHorarioGuardado() throws Exception {
		HorarioRecurrente guardado = horario(DiaSemana.LUNES, 6, 12);
		when(definirHorario.ejecutar(ESCENARIO, DiaSemana.LUNES, LocalTime.of(6, 0), LocalTime.of(12, 0),
				PROPIETARIO)).thenReturn(guardado);

		mvc.perform(definir("""
				{"diaSemana": "LUNES", "horaInicio": "06:00", "horaFin": "12:00"}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.codigo").value("EXI-HOR-01"))
				.andExpect(jsonPath("$.mensaje").value("El horario de atención se definió correctamente."))
				.andExpect(jsonPath("$.horario.id").value(guardado.getId().toString()))
				.andExpect(jsonPath("$.horario.diaSemana").value("LUNES"))
				.andExpect(jsonPath("$.horario.horaInicio").value("06:00"))
				.andExpect(jsonPath("$.horario.horaFin").value("12:00"));
	}

	@Test
	void ca1LaListaSaleConLasHorasEnHhMm() throws Exception {
		when(listarHorarios.ejecutar(ESCENARIO, PROPIETARIO))
				.thenReturn(List.of(horario(DiaSemana.LUNES, 6, 12), horario(DiaSemana.LUNES, 14, 23)));

		mvc.perform(conToken(get("/api/escenarios/{id}/horarios", ESCENARIO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[1].diaSemana").value("LUNES"))
				.andExpect(jsonPath("$[1].horaInicio").value("14:00"))
				.andExpect(jsonPath("$[1].horaFin").value("23:00"));
	}

	// CA2: los tres campos vacíos se devuelven a la vez (AG-03)
	@Test
	void ca2SinDatosRespondeErrGen01PorCadaCampo() throws Exception {
		mvc.perform(definir("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(3)))
				.andExpect(jsonPath("$.errores[?(@.campo == 'diaSemana')].mensaje")
						.value("El campo día de la semana es obligatorio."))
				.andExpect(jsonPath("$.errores[?(@.campo == 'horaInicio')].codigo").value("ERR-GEN-01"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'horaFin')].codigo").value("ERR-GEN-01"));
		verifyNoInteractions(definirHorario);
	}

	// CA2: formato
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"{\"diaSemana\": \"FERIADO\", \"horaInicio\": \"06:00\", \"horaFin\": \"12:00\"} | diaSemana | El campo día de la semana no tiene un formato válido. Elija un día de lunes a domingo.",
			"{\"diaSemana\": \"LUNES\", \"horaInicio\": \"6am\", \"horaFin\": \"12:00\"} | horaInicio | El campo hora de inicio no tiene un formato válido. Escriba la hora en formato hh:mm. Ejemplo: 06:00",
			"{\"diaSemana\": \"LUNES\", \"horaInicio\": \"06:00\", \"horaFin\": \"24:00\"} | horaFin | El campo hora de fin no tiene un formato válido. Escriba la hora en formato hh:mm. Ejemplo: 23:00"
	})
	void ca2UnFormatoMaloRespondeErrGen02(String cuerpo, String campo, String texto) throws Exception {
		mvc.perform(definir(cuerpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(1)))
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-02"))
				.andExpect(jsonPath("$.errores[0].campo").value(campo))
				.andExpect(jsonPath("$.errores[0].mensaje").value(texto));
		verifyNoInteractions(definirHorario);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"ERR-HOR-01 | 404 | El escenario al que quiere definir el horario no existe.", // CA3
			"ERR-HOR-02 | 403 | No tiene permiso para gestionar los horarios de este escenario.", // CA4
			"ERR-HOR-03 | 422 | La hora de fin debe ser mayor que la hora de inicio.", // CA5
			"ERR-HOR-13 | 422 | Los horarios se definen en horas en punto (por ejemplo, 18:00)." // CA8
	})
	void ca3Ca4Ca5Ca8LasReglasIncumplidasRespondenSuMensaje(String codigo, int estadoHttp, String texto)
			throws Exception {
		when(definirHorario.ejecutar(any(), any(), any(), any(), any())).thenThrow(new ReglaNegocioException(codigo));

		mvc.perform(definir("""
				{"diaSemana": "LUNES", "horaInicio": "06:30", "horaFin": "12:00"}"""))
				.andExpect(status().is(estadoHttp))
				.andExpect(jsonPath("$.errores[0].codigo").value(codigo))
				.andExpect(jsonPath("$.errores[0].mensaje").value(texto));
	}

	@Test
	void ca6RespondeErrHor04ConElDia() throws Exception {
		when(definirHorario.ejecutar(any(), any(), any(), any(), any()))
				.thenThrow(new ReglaNegocioException("ERR-HOR-04", new TextoCatalogo("dia.SABADO")));

		mvc.perform(definir("""
				{"diaSemana": "SABADO", "horaInicio": "10:00", "horaFin": "14:00"}"""))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-HOR-04"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("El horario se cruza con otro horario ya definido para el sábado."));
	}

	@Test
	void ca8UnaHoraQueNoEsEnPuntoLlegaAlCasoDeUso() throws Exception {
		when(definirHorario.ejecutar(any(), any(), any(), any(), any())).thenThrow(new ReglaNegocioException("ERR-HOR-13"));

		mvc.perform(definir("""
				{"diaSemana": "LUNES", "horaInicio": "06:30", "horaFin": "12:00"}"""))
				.andExpect(status().isUnprocessableContent());

		// El formato hh:mm es válido: los minutos los revisa la política 13 y no el formulario
		verify(definirHorario).ejecutar(ESCENARIO, DiaSemana.LUNES, LocalTime.of(6, 30), LocalTime.of(12, 0),
				PROPIETARIO);
	}

	@Test
	void sinSesionRespondeErrGen05() throws Exception {
		mvc.perform(post("/api/escenarios/{id}/horarios", ESCENARIO).contentType(MediaType.APPLICATION_JSON)
				.content("{\"diaSemana\": \"LUNES\", \"horaInicio\": \"06:00\", \"horaFin\": \"12:00\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-05"));
		mvc.perform(get("/api/escenarios/{id}/horarios", ESCENARIO))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(definirHorario, listarHorarios);
	}

	@Test
	void unIdentificadorQueNoEsUuidRespondeErrGen07() throws Exception {
		mvc.perform(conToken(post("/api/escenarios/no-es-uuid/horarios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"diaSemana\": \"LUNES\", \"horaInicio\": \"06:00\", \"horaFin\": \"12:00\"}")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
		verifyNoInteractions(definirHorario);
	}

}
