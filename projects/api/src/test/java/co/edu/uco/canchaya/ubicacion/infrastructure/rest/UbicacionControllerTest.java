package co.edu.uco.canchaya.ubicacion.infrastructure.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;
import co.edu.uco.canchaya.ubicacion.application.port.in.ConsultaUbicacion;
import co.edu.uco.canchaya.ubicacion.domain.Ciudad;
import co.edu.uco.canchaya.ubicacion.domain.Departamento;

@WebMvcTest(UbicacionController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class UbicacionControllerTest {

	private static final UUID ANTIOQUIA = UUID.randomUUID();

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ConsultaUbicacion consultaUbicacion;

	@Test
	void listaLosDepartamentos() throws Exception {
		when(consultaUbicacion.listarDepartamentos())
				.thenReturn(List.of(Departamento.reconstruir(ANTIOQUIA, "05", "Antioquia")));

		mvc.perform(get("/api/departamentos").with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(ANTIOQUIA.toString()))
				.andExpect(jsonPath("$[0].nombre").value("Antioquia"))
				.andExpect(jsonPath("$[0].codigo").doesNotExist());
	}

	@Test
	void ca6ListaLasCiudadesDelDepartamentoElegido() throws Exception {
		when(consultaUbicacion.listarCiudades(ANTIOQUIA)).thenReturn(List.of(
				Ciudad.reconstruir(UUID.randomUUID(), "05440", ANTIOQUIA, "Marinilla"),
				Ciudad.reconstruir(UUID.randomUUID(), "05615", ANTIOQUIA, "Rionegro")));

		mvc.perform(get("/api/departamentos/{id}/ciudades", ANTIOQUIA).with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].nombre").value(org.hamcrest.Matchers.contains("Marinilla", "Rionegro")));
	}

	@Test
	void sinTokenRespondeErrGen05() throws Exception {
		mvc.perform(get("/api/departamentos"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-05"));
	}

}
