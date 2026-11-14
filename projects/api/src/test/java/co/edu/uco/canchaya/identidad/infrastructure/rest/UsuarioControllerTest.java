package co.edu.uco.canchaya.identidad.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import co.edu.uco.canchaya.identidad.application.port.in.ActualizarPerfil;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultarCorreoDisponible;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultarPerfil;
import co.edu.uco.canchaya.identidad.application.port.in.RegistrarUsuario;
import co.edu.uco.canchaya.identidad.application.port.in.RegistrarUsuario.DatosRegistro;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

@WebMvcTest(UsuarioController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class UsuarioControllerTest {

	private static final String VALIDO = """
			{
			  "nombre": "Ana Pérez",
			  "correoElectronico": "ana@correo.com",
			  "contrasena": "Clave2026",
			  "tipoDocumento": "CC",
			  "numeroDocumento": "1036123456",
			  "numeroCelular": "+573001234567",
			  "rol": "JUGADOR",
			  "autorizacionAcudiente": false
			}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RegistrarUsuario registrarUsuario;

	@MockitoBean
	private ConsultarCorreoDisponible consultarCorreoDisponible;

	// HU3: el controlador también los necesita; sus pruebas están en PerfilControllerTest
	@MockitoBean
	private ConsultarPerfil consultarPerfil;

	@MockitoBean
	private ActualizarPerfil actualizarPerfil;

	private ResultActions registrar(String json) throws Exception {
		return mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static Usuario usuarioRegistrado() {
		return Usuario.registrar("Ana Pérez", "ana@correo.com", "$2a$10$hash", TipoDocumento.CC, "1036123456",
				"+573001234567", Rol.JUGADOR, false, OffsetDateTime.parse("2026-11-06T09:00:00-05:00"));
	}

	@Test
	void ca1RegistraYRespondeConElMensajeDeExito() throws Exception {
		when(registrarUsuario.ejecutar(any())).thenReturn(usuarioRegistrado());

		registrar(VALIDO)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.codigo").value("EXI-USU-01"))
				.andExpect(jsonPath("$.mensaje").value("Su cuenta se creó correctamente. Inicie sesión para continuar."))
				.andExpect(jsonPath("$.usuario.correoElectronico").value("ana@correo.com"))
				.andExpect(jsonPath("$.usuario.rol").value("JUGADOR"))
				.andExpect(jsonPath("$.usuario.id").exists());
	}

	@Test
	void ca1LaRespuestaNoTraeLaContrasenaNiDatosSensibles() throws Exception {
		when(registrarUsuario.ejecutar(any())).thenReturn(usuarioRegistrado());

		registrar(VALIDO)
				.andExpect(content().string(not(containsString("Clave2026"))))
				.andExpect(content().string(not(containsString("hash"))))
				.andExpect(content().string(not(containsString("1036123456"))))
				.andExpect(content().string(not(containsString("3001234567"))));
	}

	@Test
	void ca1PasaLosDatosAlCasoDeUsoSinEspaciosSobrantes() throws Exception {
		when(registrarUsuario.ejecutar(any())).thenReturn(usuarioRegistrado());

		registrar(VALIDO.replace("\"Ana Pérez\"", "\"  Ana Pérez  \""));

		ArgumentCaptor<DatosRegistro> datos = ArgumentCaptor.forClass(DatosRegistro.class);
		verify(registrarUsuario).ejecutar(datos.capture());
		assertThat(datos.getValue().nombre()).isEqualTo("Ana Pérez");
		assertThat(datos.getValue().tipoDocumento()).isEqualTo(TipoDocumento.CC);
		assertThat(datos.getValue().rol()).isEqualTo(Rol.JUGADOR);
	}

	@Test
	void ca13SiNoLlegaLaAutorizacionDelAcudienteSeTomaComoNoAceptada() throws Exception {
		when(registrarUsuario.ejecutar(any())).thenReturn(usuarioRegistrado());

		registrar(VALIDO.replace(",\n  \"autorizacionAcudiente\": false", ""));

		ArgumentCaptor<DatosRegistro> datos = ArgumentCaptor.forClass(DatosRegistro.class);
		verify(registrarUsuario).ejecutar(datos.capture());
		assertThat(datos.getValue().autorizacionAcudiente()).isFalse();
	}

	@Test
	void ca2SinDatosDevuelveUnErrorDeObligatoriedadPorCadaCampo() throws Exception {
		registrar("{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(7)))
				.andExpect(jsonPath("$.errores[*].codigo", everyItem(is("ERR-GEN-01"))))
				.andExpect(jsonPath("$.errores[*].mensaje", hasItem("El campo correo electrónico es obligatorio.")));

		verify(registrarUsuario, never()).ejecutar(any());
	}

	@Test
	void ca2UnCampoSoloConEspaciosCuentaComoVacio() throws Exception {
		registrar(VALIDO.replace("\"Ana Pérez\"", "\"   \""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(1)))
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-01"))
				.andExpect(jsonPath("$.errores[0].campo").value("nombre"));
	}

	@Test
	void ca2CorreoSinFormatoDevuelveErrGen02ConElEjemplo() throws Exception {
		registrar(VALIDO.replace("ana@correo.com", "ana.correo.com"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-02"))
				.andExpect(jsonPath("$.errores[0].campo").value("correoElectronico"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("El campo correo electrónico no tiene un formato válido. Ejemplo: nombre@correo.com"));
	}

	@Test
	void ca2NombreMuyLargoDevuelveErrGen03ConLosLimites() throws Exception {
		registrar(VALIDO.replace("Ana Pérez", "A".repeat(101)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-03"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("El campo nombre debe tener entre 1 y 100 caracteres."));
	}

	@Test
	void ca2CelularSinMinimoDiceSoloElMaximo() throws Exception {
		registrar(VALIDO.replace("+573001234567", "+5730012345678901"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("El campo número de celular debe tener máximo 15 caracteres."));
	}

	@Test
	void ca2DevuelveTodosLosErroresALaVez() throws Exception {
		String json = VALIDO.replace("Ana Pérez", "Ana 123")
				.replace("ana@correo.com", "ana")
				.replace("+573001234567", "300-123");

		registrar(json)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(3)))
				.andExpect(jsonPath("$.errores[*].campo", hasItem("nombre")))
				.andExpect(jsonPath("$.errores[*].campo", hasItem("correoElectronico")))
				.andExpect(jsonPath("$.errores[*].campo", hasItem("numeroCelular")));

		verify(registrarUsuario, never()).ejecutar(any());
	}

	@Test
	void ca3CorreoRepetidoDevuelveElTextoDelCatalogo() throws Exception {
		when(registrarUsuario.ejecutar(any())).thenThrow(new ReglaNegocioException("ERR-USU-01"));

		registrar(VALIDO)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-USU-01"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("Ya existe una cuenta registrada con este correo electrónico."));
	}

	@Test
	void ca5UnRolQueNoExisteLlegaAlDominioComoNulo() throws Exception {
		when(registrarUsuario.ejecutar(any())).thenThrow(new ReglaNegocioException("ERR-USU-03"));

		registrar(VALIDO.replace("JUGADOR", "SUPERUSUARIO"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-USU-03"));

		ArgumentCaptor<DatosRegistro> datos = ArgumentCaptor.forClass(DatosRegistro.class);
		verify(registrarUsuario).ejecutar(datos.capture());
		assertThat(datos.getValue().rol()).isNull();
	}

	@Test
	void ca10ErrUsu13LlevaElTipoYElFormatoEsperado() throws Exception {
		when(registrarUsuario.ejecutar(any()))
				.thenThrow(new ReglaNegocioException("ERR-USU-13", "CC", "de 6 a 10 dígitos"));

		registrar(VALIDO)
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("El número de documento no es válido para el tipo CC: de 6 a 10 dígitos."));
	}

	@Test
	void ag10UnErrorInesperadoNoMuestraLaTraza() throws Exception {
		when(registrarUsuario.ejecutar(any())).thenThrow(new IllegalStateException("conexión rechazada"));

		registrar(VALIDO)
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"))
				.andExpect(content().string(not(containsString("conexión rechazada"))));
	}

	@Test
	void unJsonMalArmadoNoLlegaAlCasoDeUso() throws Exception {
		registrar("{ \"nombre\": ")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));

		verify(registrarUsuario, never()).ejecutar(any());
	}

	@Test
	void ca8CorreoYaRegistradoNoEstaDisponible() throws Exception {
		when(consultarCorreoDisponible.ejecutar("ana@correo.com")).thenReturn(false);

		mvc.perform(get("/api/usuarios/correo-disponible").param("correoElectronico", "ana@correo.com"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.disponible").value(false))
				.andExpect(jsonPath("$.codigo").value("ERR-USU-01"))
				.andExpect(jsonPath("$.mensaje").value("Ya existe una cuenta registrada con este correo electrónico."));
	}

	@Test
	void ca8CorreoLibreSoloDiceQueEstaDisponible() throws Exception {
		when(consultarCorreoDisponible.ejecutar("nuevo@correo.com")).thenReturn(true);

		mvc.perform(get("/api/usuarios/correo-disponible").param("correoElectronico", "nuevo@correo.com"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.disponible").value(true))
				.andExpect(jsonPath("$.codigo").doesNotExist());
	}

	@Test
	void ca8SinCorreoDevuelveErrGen01() throws Exception {
		mvc.perform(get("/api/usuarios/correo-disponible"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-01"))
				.andExpect(jsonPath("$.errores[0].campo").value("correoElectronico"));
	}

}
