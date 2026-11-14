package co.edu.uco.canchaya.identidad.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import co.edu.uco.canchaya.identidad.application.port.in.ActualizarPerfil;
import co.edu.uco.canchaya.identidad.application.port.in.ActualizarPerfil.DatosPerfil;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultarCorreoDisponible;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultarPerfil;
import co.edu.uco.canchaya.identidad.application.port.in.RegistrarUsuario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/** HU3: servicios GET y PUT /api/usuarios/{id}. */
@WebMvcTest(UsuarioController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class PerfilControllerTest {

	private static final UUID ID = UUID.fromString("7f1c2d3e-4a5b-4c6d-8e9f-0a1b2c3d4e5f");
	private static final String RUTA = "/api/usuarios/" + ID;
	private static final String HASH = "$2a$10$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz01234";

	private static final String VALIDO = """
			{
			  "nombre": "Ana María Pérez",
			  "correoElectronico": "ana.maria@correo.com",
			  "numeroCelular": "+573109876543",
			  "nuevaContrasena": "Nueva2026"
			}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ConsultarPerfil consultarPerfil;

	@MockitoBean
	private ActualizarPerfil actualizarPerfil;

	@MockitoBean
	private RegistrarUsuario registrarUsuario;

	@MockitoBean
	private ConsultarCorreoDisponible consultarCorreoDisponible;

	/** Token de Ana, como lo emite el inicio de sesión: el id en sub y el rol (HU2). */
	private static JwtRequestPostProcessor tokenDeAna() {
		return jwt().jwt(token -> token.subject(ID.toString()).claim("rol", "JUGADOR"));
	}

	private static Usuario ana() {
		return Usuario.reconstruir(ID, "Ana María Pérez", "ana.maria@correo.com", HASH, TipoDocumento.CC,
				"1036123456", "+573109876543", Rol.JUGADOR, OffsetDateTime.parse("2026-11-06T09:00:00-05:00"));
	}

	private ResultActions actualizar(String json) throws Exception {
		return mvc.perform(put(RUTA).with(tokenDeAna()).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private DatosPerfil datosEnviados() {
		ArgumentCaptor<DatosPerfil> datos = ArgumentCaptor.forClass(DatosPerfil.class);
		verify(actualizarPerfil).ejecutar(any(), datos.capture());
		return datos.getValue();
	}

	// --- GET: llenar el formulario -------------------------------------------

	@Test
	void consultarDevuelveLosDatosConElDocumentoYElRolSinLaContrasena() throws Exception {
		when(consultarPerfil.ejecutar(any(), any())).thenReturn(ana());

		mvc.perform(get(RUTA).with(tokenDeAna()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.perfil.nombre").value("Ana María Pérez"))
				.andExpect(jsonPath("$.perfil.numeroCelular").value("+573109876543"))
				.andExpect(jsonPath("$.perfil.tipoDocumento").value("CC"))
				.andExpect(jsonPath("$.perfil.numeroDocumento").value("1036123456"))
				.andExpect(jsonPath("$.perfil.rol").value("JUGADOR"))
				.andExpect(jsonPath("$.codigo").doesNotExist())
				.andExpect(jsonPath("$.perfil.fechaRegistro").doesNotExist())
				.andExpect(content().string(not(containsString("$2a$"))));
	}

	@Test
	void consultarTomaAlSolicitanteDelToken() throws Exception {
		when(consultarPerfil.ejecutar(any(), any())).thenReturn(ana());

		mvc.perform(get(RUTA).with(tokenDeAna()));

		verify(consultarPerfil).ejecutar(new Solicitante(ID, Rol.JUGADOR), ID);
	}

	@Test
	void consultarSinTokenRespondeErrGen05() throws Exception {
		mvc.perform(get(RUTA))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-05"));
	}

	// --- PUT: guardar los cambios -------------------------------------------

	@Test
	void ca1GuardaYRespondeConElMensajeDeExitoYLosDatosActualizados() throws Exception {
		when(actualizarPerfil.ejecutar(any(), any())).thenReturn(ana());

		actualizar(VALIDO)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value("EXI-USU-02"))
				.andExpect(jsonPath("$.mensaje").value("Su perfil se actualizó correctamente."))
				.andExpect(jsonPath("$.perfil.correoElectronico").value("ana.maria@correo.com"))
				.andExpect(jsonPath("$.perfil.nombre").value("Ana María Pérez"))
				// AG-13
				.andExpect(content().string(not(containsString("Nueva2026"))))
				.andExpect(content().string(not(containsString("$2a$"))));
	}

	@Test
	void ca1ElIdSaleDeLaRutaYElSolicitanteDelTokenSinEspaciosSobrantes() throws Exception {
		when(actualizarPerfil.ejecutar(any(), any())).thenReturn(ana());

		actualizar(VALIDO.replace("\"Ana María Pérez\"", "\"  Ana María Pérez  \""));

		ArgumentCaptor<Solicitante> solicitante = ArgumentCaptor.forClass(Solicitante.class);
		ArgumentCaptor<DatosPerfil> datos = ArgumentCaptor.forClass(DatosPerfil.class);
		verify(actualizarPerfil).ejecutar(solicitante.capture(), datos.capture());
		assertThat(solicitante.getValue()).isEqualTo(new Solicitante(ID, Rol.JUGADOR));
		assertThat(datos.getValue().id()).isEqualTo(ID);
		assertThat(datos.getValue().nombre()).isEqualTo("Ana María Pérez");
		assertThat(datos.getValue().nuevaContrasena()).isEqualTo("Nueva2026");
	}

	@Test
	void ca4SinNuevaContrasenaLlegaVaciaAlCasoDeUso() throws Exception {
		when(actualizarPerfil.ejecutar(any(), any())).thenReturn(ana());

		actualizar(VALIDO.replace(",\n  \"nuevaContrasena\": \"Nueva2026\"", "")).andExpect(status().isOk());

		assertThat(datosEnviados().nuevaContrasena()).isNull();
	}

	@Test
	void ca2SinDatosDevuelveUnErrorDeObligatoriedadPorCadaCampoRequerido() throws Exception {
		actualizar("{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(3)))
				.andExpect(jsonPath("$.errores[*].codigo", everyItem(is("ERR-GEN-01"))));

		verify(actualizarPerfil, never()).ejecutar(any(), any());
	}

	@Test
	void ca2DevuelveLosErroresDeFormatoYLongitudConElTextoDelCatalogo() throws Exception {
		String json = VALIDO.replace("Ana María Pérez", "A".repeat(101))
				.replace("ana.maria@correo.com", "ana.correo.com")
				.replace("+573109876543", "310-987");

		actualizar(json)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(3)))
				.andExpect(jsonPath("$.errores[?(@.campo == 'nombre')].mensaje")
						.value("El campo nombre debe tener entre 1 y 100 caracteres."))
				.andExpect(jsonPath("$.errores[?(@.campo == 'correoElectronico')].mensaje")
						.value("El campo correo electrónico no tiene un formato válido. Ejemplo: nombre@correo.com"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'numeroCelular')].codigo").value("ERR-GEN-02"));

		verify(actualizarPerfil, never()).ejecutar(any(), any());
	}

	@Test
	void ca3CorreoDeOtroUsuarioDevuelveErrUsu10() throws Exception {
		when(actualizarPerfil.ejecutar(any(), any())).thenThrow(new ReglaNegocioException("ERR-USU-10"));

		actualizar(VALIDO)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-USU-10"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("Ya existe otra cuenta registrada con este correo electrónico."));
	}

	@Test
	void ca5ElDocumentoYElRolEnviadosLleganAlCasoDeUsoQueRespondeErrUsu11() throws Exception {
		when(actualizarPerfil.ejecutar(any(), any())).thenThrow(new ReglaNegocioException("ERR-USU-11"));
		String conDocumento = VALIDO.replace("{", "{\n  \"tipoDocumento\": \" CE \", \"numeroDocumento\": \"999999\", \"rol\": \"ADMINISTRADOR_SISTEMA\",");

		actualizar(conDocumento)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-USU-11"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("El tipo de documento, el número de documento y el rol no se pueden modificar."));

		DatosPerfil datos = datosEnviados();
		assertThat(datos.tipoDocumento()).isEqualTo("CE");
		assertThat(datos.numeroDocumento()).isEqualTo("999999");
		assertThat(datos.rol()).isEqualTo("ADMINISTRADOR_SISTEMA");
	}

	@Test
	void ca6PerfilDeOtroUsuarioRespondeProhibidoConErrUsu09() throws Exception {
		when(actualizarPerfil.ejecutar(any(), any())).thenThrow(new ReglaNegocioException("ERR-USU-09"));

		actualizar(VALIDO)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-USU-09"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("No tiene permiso para modificar el perfil de otro usuario."));
	}

	@Test
	void ca8ContrasenaDebilDevuelveErrUsu12() throws Exception {
		when(actualizarPerfil.ejecutar(any(), any())).thenThrow(new ReglaNegocioException("ERR-USU-12"));

		actualizar(VALIDO.replace("Nueva2026", "corta1"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-USU-12"));
	}

	@Test
	void politica8UsuarioQueNoExisteRespondeNoEncontradoConErrUsu08() throws Exception {
		when(actualizarPerfil.ejecutar(any(), any())).thenThrow(new ReglaNegocioException("ERR-USU-08"));

		actualizar(VALIDO)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-USU-08"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("No se encontró el usuario que se quiere modificar."));
	}

	@Test
	void politica8UnIdQueNoEsUuidRespondeErrUsu08SinLlegarAlCasoDeUso() throws Exception {
		mvc.perform(put("/api/usuarios/no-es-un-uuid").with(tokenDeAna())
				.contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-USU-08"));

		verify(actualizarPerfil, never()).ejecutar(any(), any());
	}

	@Test
	void actualizarSinTokenRespondeErrGen05() throws Exception {
		mvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-05"));

		verify(actualizarPerfil, never()).ejecutar(any(), any());
	}

}
