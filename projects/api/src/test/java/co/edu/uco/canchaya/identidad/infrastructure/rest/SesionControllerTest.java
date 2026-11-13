package co.edu.uco.canchaya.identidad.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import co.edu.uco.canchaya.identidad.application.port.in.IniciarSesion;
import co.edu.uco.canchaya.identidad.application.port.in.IniciarSesion.Credenciales;
import co.edu.uco.canchaya.identidad.application.port.in.IniciarSesion.Sesion;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

@WebMvcTest(SesionController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class SesionControllerTest {

	private static final String INCORRECTOS = "El correo electrónico o la contraseña son incorrectos.";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private IniciarSesion iniciarSesion;

	private ResultActions iniciar(String json) throws Exception {
		return mvc.perform(post("/api/sesiones").contentType(MediaType.APPLICATION_JSON).content(json)
				.with(peticion -> {
					peticion.setRemoteAddr("203.0.113.7");
					return peticion;
				}));
	}

	@Test
	void ca1ConCredencialesCorrectasRespondeElTokenElRolYElVencimiento() throws Exception {
		when(iniciarSesion.ejecutar(any(), any()))
				.thenReturn(new Sesion("un.token.jwt", Rol.JUGADOR, Instant.parse("2026-11-12T15:00:00Z")));

		iniciar("""
				{"correoElectronico": " ana@correo.com ", "contrasena": "Clave2026"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("un.token.jwt"))
				.andExpect(jsonPath("$.rol").value("JUGADOR"))
				.andExpect(jsonPath("$.venceEn").value("2026-11-12T15:00:00Z"));

		// AG-06 y D28: el correo llega sin espacios y el origen es la IP del cliente
		verify(iniciarSesion).ejecutar(eq(new Credenciales("ana@correo.com", "Clave2026")), eq("203.0.113.7"));
	}

	@Test
	void ca2ConLosDatosVaciosRespondeErrGen01YNoIntentaAutenticar() throws Exception {
		iniciar("""
				{"correoElectronico": "", "contrasena": ""}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.length()").value(2))
				.andExpect(jsonPath("$.errores[?(@.campo == 'correoElectronico')].codigo").value("ERR-GEN-01"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'contrasena')].mensaje")
						.value("El campo contraseña es obligatorio."));

		verifyNoInteractions(iniciarSesion);
	}

	@Test
	void ca2ConUnCorreoSinFormatoValidoRespondeErrGen02YNoIntentaAutenticar() throws Exception {
		iniciar("""
				{"correoElectronico": "ana-correo.com", "contrasena": "Clave2026"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-02"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("El campo correo electrónico no tiene un formato válido. Ejemplo: nombre@correo.com"));

		verifyNoInteractions(iniciarSesion);
	}

	@Test
	void ca3ConUnCorreoNoRegistradoRespondeErrUsu06() throws Exception {
		when(iniciarSesion.ejecutar(any(), any())).thenThrow(new ReglaNegocioException("ERR-USU-06"));

		iniciar("""
				{"correoElectronico": "nadie@correo.com", "contrasena": "Clave2026"}
				""")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-USU-06"))
				.andExpect(jsonPath("$.errores[0].mensaje").value(INCORRECTOS));
	}

	@Test
	void ca4ConUnaContrasenaQueNoCoincideRespondeErrUsu07ConElMismoTextoYEstado() throws Exception {
		when(iniciarSesion.ejecutar(any(), any())).thenThrow(new ReglaNegocioException("ERR-USU-07"));

		iniciar("""
				{"correoElectronico": "ana@correo.com", "contrasena": "Otra2026"}
				""")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-USU-07"))
				.andExpect(jsonPath("$.errores[0].mensaje").value(INCORRECTOS));
	}

	@Test
	void ca7ConLaCuentaOElOrigenBloqueadoRespondeErrUsu16ConLosMinutos() throws Exception {
		when(iniciarSesion.ejecutar(any(), any())).thenThrow(new ReglaNegocioException("ERR-USU-16", 15));

		iniciar("""
				{"correoElectronico": "ana@correo.com", "contrasena": "Clave2026"}
				""")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-USU-16"))
				.andExpect(jsonPath("$.errores[0].mensaje").value(
						"Superó el número de intentos permitidos para iniciar sesión. Intente de nuevo en 15 minutos."));
	}

}
