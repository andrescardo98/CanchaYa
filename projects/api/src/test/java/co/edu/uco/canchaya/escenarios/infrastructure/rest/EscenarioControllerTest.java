package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario.DatosRegistroEscenario;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

@WebMvcTest(EscenarioController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class EscenarioControllerTest {

	private static final UUID USUARIO = UUID.randomUUID();
	private static final UUID CIUDAD = UUID.randomUUID();
	private static final String VALIDO = """
			{
			  "nombre": " Cancha La 80 ",
			  "descripcion": "  ",
			  "direccion": "Calle 80 # 45-10",
			  "ciudadId": "%s",
			  "latitud": 6.155123,
			  "longitud": -75.3737,
			  "telefonoContacto": "+573001234567",
			  "correoContacto": "contacto@la80.com"
			}
			""".formatted(CIUDAD);

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RegistrarEscenario registrarEscenario;

	private static MockMultipartFile datos(String json) {
		return new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes());
	}

	private static MockMultipartHttpServletRequestBuilder peticion(String json) {
		return multipart("/api/escenarios").file(datos(json));
	}

	private ResultActions enviarComo(MockMultipartHttpServletRequestBuilder peticion, Rol rol) throws Exception {
		return mvc.perform(peticion.with(jwt().jwt(token -> token.subject(USUARIO.toString()).claim("rol", rol.name()))));
	}

	private static Escenario registrado() {
		return Escenario.registrar("Cancha La 80", null, "Calle 80 # 45-10", CIUDAD, new BigDecimal("6.155123"),
				new BigDecimal("-75.3737"), "+573001234567", null, List.of(),
				new Solicitante(USUARIO, Rol.ADMINISTRADOR_CANCHA), 5);
	}

	@Test
	void ca1RegistraConLasFotosYElSolicitanteDelTokenYRespondeExiEsc01() throws Exception {
		Escenario escenario = registrado();
		when(registrarEscenario.ejecutar(any(), any())).thenReturn(escenario);
		var foto = new MockMultipartFile("fotos", "cancha.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

		enviarComo(peticion(VALIDO).file(foto), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.codigo").value("EXI-ESC-01"))
				.andExpect(jsonPath("$.mensaje").value("El escenario se registró correctamente. Será visible para "
						+ "los jugadores cuando el administrador del sistema lo active."))
				.andExpect(jsonPath("$.id").value(escenario.getId().toString()))
				// Campos que el usuario no ve
				.andExpect(jsonPath("$.estado").doesNotExist())
				.andExpect(jsonPath("$.administradorId").doesNotExist());

		var datos = ArgumentCaptor.forClass(DatosRegistroEscenario.class);
		var solicitante = ArgumentCaptor.forClass(Solicitante.class);
		verify(registrarEscenario).ejecutar(datos.capture(), solicitante.capture());
		assertThat(solicitante.getValue()).isEqualTo(new Solicitante(USUARIO, Rol.ADMINISTRADOR_CANCHA));
		assertThat(datos.getValue().nombre()).isEqualTo("Cancha La 80");
		assertThat(datos.getValue().descripcion()).isNull();
		assertThat(datos.getValue().ciudadId()).isEqualTo(CIUDAD);
		assertThat(datos.getValue().fotos()).singleElement()
				.satisfies(f -> {
					assertThat(f.nombreArchivo()).isEqualTo("cancha.jpg");
					assertThat(f.contenido()).containsExactly(1, 2, 3);
				});
	}

	@Test
	void ca1RegistraSinFotosNiOpcionales() throws Exception {
		when(registrarEscenario.ejecutar(any(), any())).thenReturn(registrado());
		String minimo = """
				{"nombre": "La 80", "direccion": "Calle 80", "ciudadId": "%s", "latitud": 6.1,
				 "longitud": -75.3, "telefonoContacto": "+573001234567"}
				""".formatted(CIUDAD);

		enviarComo(peticion(minimo), Rol.ADMINISTRADOR_CANCHA).andExpect(status().isCreated());

		var datos = ArgumentCaptor.forClass(DatosRegistroEscenario.class);
		verify(registrarEscenario).ejecutar(datos.capture(), any());
		assertThat(datos.getValue().fotos()).isEmpty();
		assertThat(datos.getValue().correoContacto()).isNull();
	}

	@Test
	void ca2SinLosObligatoriosRespondeErrGen01PorCadaCampo() throws Exception {
		enviarComo(peticion("{}"), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(6)))
				.andExpect(jsonPath("$.errores[?(@.campo == 'telefonoContacto')].mensaje")
						.value("El campo teléfono de contacto es obligatorio."))
				.andExpect(jsonPath("$.errores[?(@.campo == 'ciudadId')].codigo").value("ERR-GEN-01"));
		verify(registrarEscenario, never()).ejecutar(any(), any());
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"nombre           | \"%s\"                | ERR-GEN-03 | El campo nombre debe tener entre 1 y 100 caracteres.",
			"descripcion      | \"%s\"                | ERR-GEN-03 | El campo descripción debe tener máximo 500 caracteres.",
			"telefonoContacto | \"300-123\"           | ERR-GEN-02 | El campo teléfono de contacto no tiene un formato válido. Escriba el indicativo y el número, solo con dígitos. Ejemplo: +573001234567",
			"telefonoContacto | \"+5730012345678901\" | ERR-GEN-03 | El campo teléfono de contacto debe tener máximo 15 caracteres.",
			"correoContacto   | \"contacto@la80\"     | ERR-GEN-02 | El campo correo de contacto no tiene un formato válido. Ejemplo: escenario@dominio.com" })
	void ca2ConUnDatoIncorrectoRespondeElErrorDelCampo(String campo, String valor, String codigo, String mensaje)
			throws Exception {
		String largo = campo.equals("nombre") ? "x".repeat(101) : "x".repeat(501);
		String json = VALIDO.replaceFirst("\"" + campo + "\": [^,\\n]+", "\"" + campo + "\": " + valor.formatted(largo));

		enviarComo(peticion(json), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(1)))
				.andExpect(jsonPath("$.errores[0].campo").value(campo))
				.andExpect(jsonPath("$.errores[0].codigo").value(codigo))
				.andExpect(jsonPath("$.errores[0].mensaje").value(mensaje));
		verify(registrarEscenario, never()).ejecutar(any(), any());
	}

	@Test
	void ca3UnJugadorRecibeErrEsc01() throws Exception {
		when(registrarEscenario.ejecutar(any(), any())).thenThrow(new ReglaNegocioException("ERR-ESC-01"));

		enviarComo(peticion(VALIDO), Rol.JUGADOR)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-ESC-01"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("Solo un administrador de cancha puede registrar escenarios."));

		var solicitante = ArgumentCaptor.forClass(Solicitante.class);
		verify(registrarEscenario).ejecutar(any(), solicitante.capture());
		assertThat(solicitante.getValue().rol()).isEqualTo(Rol.JUGADOR);
	}

	@Test
	void ca10ElMensajeDeFotosDiceQueReglaSeIncumplio() throws Exception {
		when(registrarEscenario.ejecutar(any(), any())).thenThrow(
				new ReglaNegocioException("ERR-ESC-13", new TextoCatalogo("ERR-ESC-13.formato", "plano.gif")));

		enviarComo(peticion(VALIDO), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-ESC-13"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("Las fotos no cumplen las condiciones: la foto "
						+ "plano.gif no es JPG ni PNG. Se permiten máximo 5 fotos por escenario, en formato JPG o PNG "
						+ "y de hasta 2 MB cada una."));
	}

	@Test
	void sinTokenRespondeErrGen05() throws Exception {
		mvc.perform(peticion(VALIDO))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-05"));
		verify(registrarEscenario, never()).ejecutar(any(), any());
	}

}
