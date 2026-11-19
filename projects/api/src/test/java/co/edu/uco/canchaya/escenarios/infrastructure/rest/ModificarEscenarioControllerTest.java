package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import co.edu.uco.canchaya.escenarios.application.port.in.CambiarEstadoEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarEscenarioPropietario;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarPerfilEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarEscenariosAdministracion;
import co.edu.uco.canchaya.escenarios.application.port.in.ModificarEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ModificarEscenario.DatosModificacionEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/** HU8: GET y PUT /api/escenarios/{id}. */
@WebMvcTest(EscenarioController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class ModificarEscenarioControllerTest {

	private static final UUID USUARIO = UUID.randomUUID();
	private static final UUID ESCENARIO = UUID.randomUUID();
	private static final UUID CIUDAD = UUID.randomUUID();
	private static final String FOTO_1 = "https://fotos.test/1.jpg";
	private static final String FOTO_2 = "https://fotos.test/2.jpg";
	private static final String VALIDO = """
			{
			  "nombre": " Cancha El Templo ",
			  "descripcion": "  ",
			  "direccion": "Carrera 70 # 30-20",
			  "ciudadId": "%s",
			  "latitud": 6.244203,
			  "longitud": -75.581215,
			  "telefonoContacto": "+573109876543",
			  "correoContacto": "contacto@templo.com",
			  "fotosConservadas": ["%s", "%s"]
			}
			""".formatted(CIUDAD, FOTO_2, FOTO_1);

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ConsultarEscenarioPropietario consultarEscenario;

	@MockitoBean
	private ModificarEscenario modificarEscenario;

	// Del mismo controlador, de HU5, HU13 y HU19; sus pruebas están en las otras clases
	@MockitoBean
	private RegistrarEscenario registrarEscenario;

	@MockitoBean
	private ListarEscenariosAdministracion listarEscenarios;

	@MockitoBean
	private CambiarEstadoEscenario cambiarEstado;

	@MockitoBean
	private ConsultarPerfilEscenario consultarPerfil;

	private static MockMultipartHttpServletRequestBuilder peticion(String json) {
		return multipart(HttpMethod.PUT, "/api/escenarios/{id}", ESCENARIO)
				.file(new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes()));
	}

	private ResultActions enviarComo(AbstractMockHttpServletRequestBuilder<?> peticion, Rol rol) throws Exception {
		return mvc.perform(peticion.with(jwt().jwt(token -> token.subject(USUARIO.toString()).claim("rol", rol.name()))));
	}

	private static Escenario guardado() {
		return Escenario.reconstruir(ESCENARIO, "Cancha El Templo", null, "Carrera 70 # 30-20", CIUDAD,
				new BigDecimal("6.244203"), new BigDecimal("-75.581215"), "+573109876543", "contacto@templo.com",
				List.of(FOTO_2, FOTO_1), USUARIO, EstadoEscenario.INACTIVO);
	}

	@Test
	void ca1ConsultarParaEditarDevuelveLosDatosActuales() throws Exception {
		when(consultarEscenario.ejecutar(ESCENARIO, new Solicitante(USUARIO, Rol.ADMINISTRADOR_CANCHA)))
				.thenReturn(guardado());

		enviarComo(get("/api/escenarios/{id}", ESCENARIO), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ESCENARIO.toString()))
				.andExpect(jsonPath("$.nombre").value("Cancha El Templo"))
				.andExpect(jsonPath("$.descripcion").isEmpty())
				.andExpect(jsonPath("$.ciudadId").value(CIUDAD.toString()))
				.andExpect(jsonPath("$.latitud").value(6.244203))
				.andExpect(jsonPath("$.longitud").value(-75.581215))
				.andExpect(jsonPath("$.telefonoContacto").value("+573109876543"))
				.andExpect(jsonPath("$.correoContacto").value("contacto@templo.com"))
				.andExpect(jsonPath("$.fotos[0]").value(FOTO_2))
				.andExpect(jsonPath("$.fotos[1]").value(FOTO_1))
				.andExpect(jsonPath("$.estado").value("INACTIVO"))
				.andExpect(jsonPath("$.administradorId").doesNotExist());
	}

	@Test
	void ca1ModificaConLasFotosConservadasYLasNuevasYRespondeExiEsc02() throws Exception {
		when(modificarEscenario.ejecutar(eq(ESCENARIO), any(), any())).thenReturn(guardado());
		var foto = new MockMultipartFile("fotos", "nueva.png", "image/png", new byte[] { 1, 2, 3 });

		enviarComo(peticion(VALIDO).file(foto), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value("EXI-ESC-02"))
				.andExpect(jsonPath("$.mensaje").value("El escenario se modificó correctamente."))
				// AG-04: la pantalla muestra lo que quedó guardado
				.andExpect(jsonPath("$.escenario.id").value(ESCENARIO.toString()))
				.andExpect(jsonPath("$.escenario.nombre").value("Cancha El Templo"))
				.andExpect(jsonPath("$.escenario.fotos", hasSize(2)))
				.andExpect(jsonPath("$.escenario.administradorId").doesNotExist());

		var datos = ArgumentCaptor.forClass(DatosModificacionEscenario.class);
		var solicitante = ArgumentCaptor.forClass(Solicitante.class);
		verify(modificarEscenario).ejecutar(eq(ESCENARIO), datos.capture(), solicitante.capture());
		assertThat(solicitante.getValue()).isEqualTo(new Solicitante(USUARIO, Rol.ADMINISTRADOR_CANCHA));
		// AG-06: sin espacios en los extremos; el opcional en blanco no se envía
		assertThat(datos.getValue().nombre()).isEqualTo("Cancha El Templo");
		assertThat(datos.getValue().descripcion()).isNull();
		assertThat(datos.getValue().ciudadId()).isEqualTo(CIUDAD);
		assertThat(datos.getValue().fotosConservadas()).containsExactly(FOTO_2, FOTO_1);
		assertThat(datos.getValue().fotosNuevas()).singleElement().satisfies(f -> {
			assertThat(f.nombreArchivo()).isEqualTo("nueva.png");
			assertThat(f.contenido()).containsExactly(1, 2, 3);
		});
	}

	@Test
	void ca1ConFotosConservadasVaciaYSinNuevasQuitaTodasLasFotos() throws Exception {
		when(modificarEscenario.ejecutar(eq(ESCENARIO), any(), any())).thenReturn(guardado());
		String sinFotos = VALIDO.replaceFirst("\"fotosConservadas\": \\[[^]]*]", "\"fotosConservadas\": []");

		enviarComo(peticion(sinFotos), Rol.ADMINISTRADOR_CANCHA).andExpect(status().isOk());

		var datos = ArgumentCaptor.forClass(DatosModificacionEscenario.class);
		verify(modificarEscenario).ejecutar(eq(ESCENARIO), datos.capture(), any());
		assertThat(datos.getValue().fotosConservadas()).isEmpty();
		assertThat(datos.getValue().fotosNuevas()).isEmpty();
	}

	@Test
	void ca2SinFotosConservadasRespondeErrGen01YNoModifica() throws Exception {
		// Un descuido del cliente no debe borrar las fotos actuales
		String sinCampo = VALIDO.replaceFirst(",\\s*\"fotosConservadas\": \\[[^]]*]", "");

		enviarComo(peticion(sinCampo), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(1)))
				.andExpect(jsonPath("$.errores[0].campo").value("fotosConservadas"))
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-01"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("El campo fotos conservadas es obligatorio."));
		verify(modificarEscenario, never()).ejecutar(any(), any(), any());
	}

	@Test
	void ca2DatosInvalidosRespondenTodosLosErroresYNoModifica() throws Exception {
		String invalido = """
				{"nombre": "", "direccion": "%s", "ciudadId": "%s", "latitud": 6.2, "longitud": -75.5,
				 "telefonoContacto": "310-987", "correoContacto": "contacto@templo", "fotosConservadas": []}
				""".formatted("x".repeat(201), CIUDAD);

		enviarComo(peticion(invalido), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(4)))
				.andExpect(jsonPath("$.errores[?(@.campo == 'nombre')].codigo").value("ERR-GEN-01"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'direccion')].mensaje")
						.value("El campo dirección debe tener entre 1 y 200 caracteres."))
				.andExpect(jsonPath("$.errores[?(@.campo == 'telefonoContacto')].codigo").value("ERR-GEN-02"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'correoContacto')].codigo").value("ERR-GEN-02"));
		verify(modificarEscenario, never()).ejecutar(any(), any(), any());
	}

	@Test
	void ca3ElEscenarioNoExisteRespondeErrEsc06() throws Exception {
		when(modificarEscenario.ejecutar(any(), any(), any())).thenThrow(new ReglaNegocioException("ERR-ESC-06"));
		when(consultarEscenario.ejecutar(any(), any())).thenThrow(new ReglaNegocioException("ERR-ESC-06"));

		enviarComo(peticion(VALIDO), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-ESC-06"));
		enviarComo(get("/api/escenarios/{id}", ESCENARIO), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-ESC-06"));
	}

	@Test
	void ca4ElQueNoEsPropietarioRecibeErrEsc07() throws Exception {
		// Otro administrador de cancha o un encargado de sede, que usa ese mismo rol (D07)
		when(modificarEscenario.ejecutar(any(), any(), any())).thenThrow(new ReglaNegocioException("ERR-ESC-07"));

		enviarComo(peticion(VALIDO), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-ESC-07"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("No tiene permiso para modificar este escenario."));

		var solicitante = ArgumentCaptor.forClass(Solicitante.class);
		verify(modificarEscenario).ejecutar(any(), any(), solicitante.capture());
		assertThat(solicitante.getValue()).isEqualTo(new Solicitante(USUARIO, Rol.ADMINISTRADOR_CANCHA));
	}

	@Test
	void ca4ConsultarParaEditarElDeOtroUsuarioRespondeErrEsc07() throws Exception {
		when(consultarEscenario.ejecutar(any(), any())).thenThrow(new ReglaNegocioException("ERR-ESC-07"));

		enviarComo(get("/api/escenarios/{id}", ESCENARIO), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-ESC-07"));
	}

	@Test
	void ca5LaCiudadNoExisteRespondeErrEsc02() throws Exception {
		when(modificarEscenario.ejecutar(any(), any(), any())).thenThrow(new ReglaNegocioException("ERR-ESC-02"));

		enviarComo(peticion(VALIDO), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-ESC-02"));
	}

	@Test
	void ca6LaUbicacionFueraDeRangoRespondeErrEsc03() throws Exception {
		when(modificarEscenario.ejecutar(any(), any(), any())).thenThrow(new ReglaNegocioException("ERR-ESC-03"));

		enviarComo(peticion(VALIDO.replace("6.244203", "91")), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-ESC-03"));
	}

	@Test
	void ca8MasDeCincoFotosEnTotalRespondeErrEsc13() throws Exception {
		when(modificarEscenario.ejecutar(any(), any(), any())).thenThrow(
				new ReglaNegocioException("ERR-ESC-13", new TextoCatalogo("ERR-ESC-13.cantidad", 5)));

		enviarComo(peticion(VALIDO), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].mensaje").value("Las fotos no cumplen las condiciones: se "
						+ "adjuntaron más de 5 fotos. Se permiten máximo 5 fotos por escenario, en formato JPG o PNG "
						+ "y de hasta 2 MB cada una."));
	}

	@Test
	void ca9FotoNuevaQueNoEsJpgNiPngRespondeErrEsc13ConElArchivo() throws Exception {
		when(modificarEscenario.ejecutar(any(), any(), any())).thenThrow(
				new ReglaNegocioException("ERR-ESC-13", new TextoCatalogo("ERR-ESC-13.formato", "plano.gif")));

		enviarComo(peticion(VALIDO), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].mensaje").value("Las fotos no cumplen las condiciones: la foto "
						+ "plano.gif no es JPG ni PNG. Se permiten máximo 5 fotos por escenario, en formato JPG o PNG "
						+ "y de hasta 2 MB cada una."));
	}

	@Test
	void ca10FotoNuevaDeMasDe2MbRespondeErrEsc13ConElArchivo() throws Exception {
		when(modificarEscenario.ejecutar(any(), any(), any())).thenThrow(
				new ReglaNegocioException("ERR-ESC-13", new TextoCatalogo("ERR-ESC-13.tamano", "grande.jpg", 2)));

		enviarComo(peticion(VALIDO), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].mensaje").value("Las fotos no cumplen las condiciones: la foto "
						+ "grande.jpg pesa más de 2 MB. Se permiten máximo 5 fotos por escenario, en formato JPG o PNG "
						+ "y de hasta 2 MB cada una."));
	}

	@Test
	void p1UnaFotoConservadaManipuladaRespondeErrGen07Con400() throws Exception {
		when(modificarEscenario.ejecutar(any(), any(), any())).thenThrow(new ReglaNegocioException("ERR-GEN-07"));

		enviarComo(peticion(VALIDO), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
	}

	@Test
	void unIdentificadorQueNoEsUuidRespondeErrGen07() throws Exception {
		enviarComo(get("/api/escenarios/no-es-uuid"), Rol.ADMINISTRADOR_CANCHA)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
	}

	@Test
	void sinTokenRespondeErrGen05() throws Exception {
		mvc.perform(peticion(VALIDO))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-05"));
		mvc.perform(get("/api/escenarios/{id}", ESCENARIO))
				.andExpect(status().isUnauthorized());
		verify(modificarEscenario, never()).ejecutar(any(), any(), any());
		verify(consultarEscenario, never()).ejecutar(any(), any());
	}

}
