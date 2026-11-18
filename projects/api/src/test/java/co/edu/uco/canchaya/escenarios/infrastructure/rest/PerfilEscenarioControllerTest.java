package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.uco.canchaya.escenarios.application.port.in.CambiarEstadoEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarPerfilEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarPerfilEscenario.CanchaPerfil;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarPerfilEscenario.PerfilEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarEscenariosAdministracion;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/** HU19: GET /api/escenarios/{id}/perfil. Las peticiones van sin token: la ficha es pública (AG-11). */
@WebMvcTest(EscenarioController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class PerfilEscenarioControllerTest {

	private static final UUID ESCENARIO = UUID.randomUUID();
	private static final UUID CANCHA = UUID.randomUUID();

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ConsultarPerfilEscenario consultarPerfil;

	// Del mismo controlador (HU5 y HU13); sus pruebas están en EscenarioControllerTest y EstadoEscenarioControllerTest
	@MockitoBean
	private RegistrarEscenario registrarEscenario;

	@MockitoBean
	private ListarEscenariosAdministracion listarEscenarios;

	@MockitoBean
	private CambiarEstadoEscenario cambiarEstado;

	private static PerfilEscenario perfil(List<String> fotos, BigDecimal promedio) {
		return new PerfilEscenario(ESCENARIO, "Cancha La 80", "Grama sintética nueva", "Calle 80 # 45-10",
				"Rionegro", "Antioquia", new BigDecimal("6.155123"), new BigDecimal("-75.373700"), "+576045551234",
				"contacto@la80.com", fotos,
				List.of(new CanchaPerfil(CANCHA, "Cancha 1", "Fútbol 5", new BigDecimal("80000.00"))), promedio);
	}

	@Test
	void ca1Y_ca6SinSesionDevuelveElPerfilCompleto() throws Exception {
		when(consultarPerfil.ejecutar(ESCENARIO))
				.thenReturn(perfil(List.of("https://fotos/1.jpg"), new BigDecimal("4.3")));

		mvc.perform(get("/api/escenarios/{id}/perfil", ESCENARIO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ESCENARIO.toString()))
				.andExpect(jsonPath("$.nombre").value("Cancha La 80"))
				.andExpect(jsonPath("$.descripcion").value("Grama sintética nueva"))
				.andExpect(jsonPath("$.direccion").value("Calle 80 # 45-10"))
				.andExpect(jsonPath("$.ciudad").value("Rionegro"))
				.andExpect(jsonPath("$.departamento").value("Antioquia"))
				.andExpect(jsonPath("$.latitud").value(6.155123))
				.andExpect(jsonPath("$.longitud").value(-75.3737))
				.andExpect(jsonPath("$.telefonoContacto").value("+576045551234"))
				.andExpect(jsonPath("$.correoContacto").value("contacto@la80.com"))
				.andExpect(jsonPath("$.fotos[0]").value("https://fotos/1.jpg"))
				// CA5: cada cancha trae su id para ir a su disponibilidad (HU20)
				.andExpect(jsonPath("$.canchas[0].id").value(CANCHA.toString()))
				.andExpect(jsonPath("$.canchas[0].nombre").value("Cancha 1"))
				.andExpect(jsonPath("$.canchas[0].tipoCancha").value("Fútbol 5"))
				.andExpect(jsonPath("$.canchas[0].precioHora").value(80000.00))
				.andExpect(jsonPath("$.calificacionPromedio").value(4.3))
				// Con promedio no hay mensaje informativo
				.andExpect(jsonPath("$.codigo").isEmpty())
				.andExpect(jsonPath("$.mensaje").isEmpty());
	}

	@Test
	void ca1ElPerfilNoExponeDatosDelAdministradorNiElEstado() throws Exception {
		when(consultarPerfil.ejecutar(ESCENARIO)).thenReturn(perfil(List.of(), null));

		mvc.perform(get("/api/escenarios/{id}/perfil", ESCENARIO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.administradorId").doesNotExist())
				.andExpect(jsonPath("$.administrador").doesNotExist())
				.andExpect(jsonPath("$.numeroCelular").doesNotExist())
				.andExpect(jsonPath("$.estado").doesNotExist());
	}

	@Test
	void ca2SinCalificacionesNoHayPromedioYSeMuestraInfRse01() throws Exception {
		when(consultarPerfil.ejecutar(ESCENARIO)).thenReturn(perfil(List.of(), null));

		mvc.perform(get("/api/escenarios/{id}/perfil", ESCENARIO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.calificacionPromedio").isEmpty())
				.andExpect(jsonPath("$.codigo").value("INF-RSE-01"))
				.andExpect(jsonPath("$.mensaje").value("Este escenario aún no tiene reseñas."));
	}

	@Test
	void ca3SinFotosLaListaVieneVacia() throws Exception {
		when(consultarPerfil.ejecutar(ESCENARIO)).thenReturn(perfil(List.of(), null));

		mvc.perform(get("/api/escenarios/{id}/perfil", ESCENARIO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fotos").isArray())
				.andExpect(jsonPath("$.fotos").isEmpty());
	}

	@Test
	void ca4UnEscenarioQueNoExisteOEstaInactivoResponde404ConErrEsc06() throws Exception {
		when(consultarPerfil.ejecutar(ESCENARIO)).thenThrow(new ReglaNegocioException("ERR-ESC-06"));

		mvc.perform(get("/api/escenarios/{id}/perfil", ESCENARIO))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-ESC-06"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("El escenario no existe o no está disponible."));
	}

	@Test
	void unIdQueNoEsUuidResponde400ConErrGen07() throws Exception {
		mvc.perform(get("/api/escenarios/{id}/perfil", "no-es-un-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
	}

}
