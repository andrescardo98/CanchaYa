package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarNombreCanchaDisponible;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarCanchasEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarTiposCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarCancha.DatosRegistroCancha;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.escenarios.domain.TipoCancha;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/** HU6: lo que entra y sale por HTTP. Los casos de uso son dobles: sus reglas se prueban aparte. */
@WebMvcTest(CanchaController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class CanchaControllerTest {

	private static final UUID USUARIO = UUID.randomUUID();
	private static final Solicitante PROPIETARIO = new Solicitante(USUARIO, Rol.ADMINISTRADOR_CANCHA);
	private static final UUID ESCENARIO = UUID.randomUUID();
	private static final UUID F5 = UUID.randomUUID();

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RegistrarCancha registrarCancha;

	@MockitoBean
	private ConsultarNombreCanchaDisponible consultarNombreDisponible;

	@MockitoBean
	private ListarCanchasEscenario listarCanchas;

	@MockitoBean
	private ListarTiposCancha listarTipos;

	private static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder peticion) {
		return peticion.with(jwt().jwt(token -> token.subject(USUARIO.toString())
				.claim("rol", Rol.ADMINISTRADOR_CANCHA.name())));
	}

	private static MockHttpServletRequestBuilder registrar(String cuerpo) {
		return conToken(post("/api/escenarios/{id}/canchas", ESCENARIO).contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo));
	}

	private static String cuerpo(String nombre, String tipo, String precio) {
		return """
				{"nombre": %s, "tipoCanchaId": %s, "precioHora": %s}""".formatted(nombre, tipo, precio);
	}

	private static Cancha cancha(String nombre) {
		return Cancha.reconstruir(UUID.randomUUID(), ESCENARIO, F5, nombre, new BigDecimal("80000.00"),
				EstadoCancha.DISPONIBLE);
	}

	@Test
	void ca1RegistraYRespondeConElMensajeDeExito() throws Exception {
		Cancha cancha = cancha("Cancha 1");
		when(registrarCancha.ejecutar(any(), eq(PROPIETARIO))).thenReturn(cancha);

		mvc.perform(registrar(cuerpo("\"  Cancha 1 \"", "\"" + F5 + "\"", "80000")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.codigo").value("EXI-CAN-01"))
				.andExpect(jsonPath("$.mensaje").value("La cancha se registró correctamente."))
				.andExpect(jsonPath("$.id").value(cancha.getId().toString()))
				// El estado no se le muestra al usuario
				.andExpect(jsonPath("$.estado").doesNotExist());

		// El escenario sale de la URL y el nombre llega sin espacios en los extremos (AG-06)
		verify(registrarCancha).ejecutar(
				new DatosRegistroCancha(ESCENARIO, F5, "Cancha 1", new BigDecimal("80000")), PROPIETARIO);
	}

	// CA2: obligatoriedad (01), formato (02) y longitud (03). El precio en cero es CA7, no CA2
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"null       | F5   | 80000      | nombre       | ERR-GEN-01 | El campo nombre es obligatorio.",
			"'\"   \"'  | F5   | 80000      | nombre       | ERR-GEN-01 | El campo nombre es obligatorio.",
			"LARGO      | F5   | 80000      | nombre       | ERR-GEN-03 | El campo nombre debe tener entre 1 y 50 caracteres.",
			"'\"C1\"'   | null | 80000      | tipoCanchaId | ERR-GEN-01 | El campo tipo de cancha es obligatorio.",
			"'\"C1\"'   | F5   | null       | precioHora   | ERR-GEN-01 | El campo precio por hora es obligatorio.",
			"'\"C1\"'   | F5   | 80000.555  | precioHora   | ERR-GEN-02 | El campo precio por hora no tiene un formato válido. Escriba solo números, con máximo 8 cifras enteras y 2 decimales. Ejemplo: 80000",
			"'\"C1\"'   | F5   | 123456789  | precioHora   | ERR-GEN-02 | El campo precio por hora no tiene un formato válido. Escriba solo números, con máximo 8 cifras enteras y 2 decimales. Ejemplo: 80000" })
	void ca2DatosInvalidosNoLleganAlCasoDeUso(String nombre, String tipo, String precio, String campo, String codigo,
			String mensaje) throws Exception {
		String valorNombre = "LARGO".equals(nombre) ? "\"" + "a".repeat(51) + "\"" : nombre;
		String valorTipo = "F5".equals(tipo) ? "\"" + F5 + "\"" : tipo;

		mvc.perform(registrar(cuerpo(valorNombre, valorTipo, precio)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(1)))
				.andExpect(jsonPath("$.errores[0].codigo").value(codigo))
				.andExpect(jsonPath("$.errores[0].campo").value(campo))
				.andExpect(jsonPath("$.errores[0].mensaje").value(mensaje));
		verifyNoInteractions(registrarCancha);
	}

	// CA2 / AG-03: todos los campos con error a la vez
	@Test
	void ca2DevuelveTodosLosCamposConError() throws Exception {
		mvc.perform(registrar("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasSize(3)));
		verifyNoInteractions(registrarCancha);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"ERR-CAN-01 | 404 | El escenario al que quiere agregar la cancha no existe.", // CA3
			"ERR-CAN-02 | 403 | No tiene permiso para gestionar las canchas de este escenario.", // CA4
			"ERR-CAN-04 | 422 | El tipo de cancha seleccionado no existe. Elija un tipo de la lista.", // CA6
			"ERR-CAN-05 | 422 | El precio por hora debe ser mayor que cero." }) // CA7
	void ca3Ca4Ca6Ca7ReglasDelCasoDeUso(String codigo, int estado, String mensaje) throws Exception {
		when(registrarCancha.ejecutar(any(), any())).thenThrow(new ReglaNegocioException(codigo));

		mvc.perform(registrar(cuerpo("\"Cancha 1\"", "\"" + F5 + "\"", "0")))
				.andExpect(status().is(estado))
				.andExpect(jsonPath("$.errores[0].codigo").value(codigo))
				.andExpect(jsonPath("$.errores[0].mensaje").value(mensaje));
	}

	@Test
	void ca5ElNombreRepetidoLlevaElNombreEnElMensaje() throws Exception {
		when(registrarCancha.ejecutar(any(), any())).thenThrow(new ReglaNegocioException("ERR-CAN-03", "Cancha 1"));

		mvc.perform(registrar(cuerpo("\"Cancha 1\"", "\"" + F5 + "\"", "80000")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-CAN-03"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("Ya existe una cancha llamada Cancha 1 en este escenario."));
	}

	@Test
	void sinTokenNoSePuedeRegistrar() throws Exception {
		mvc.perform(post("/api/escenarios/{id}/canchas", ESCENARIO).contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo("\"Cancha 1\"", "\"" + F5 + "\"", "80000")))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(registrarCancha);
	}

	@Test
	void ca1ListaLasCanchasDelEscenario() throws Exception {
		Cancha cancha = cancha("Cancha 1");
		when(listarCanchas.ejecutar(ESCENARIO, PROPIETARIO)).thenReturn(List.of(cancha));

		mvc.perform(conToken(get("/api/escenarios/{id}/canchas", ESCENARIO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(cancha.getId().toString()))
				.andExpect(jsonPath("$[0].nombre").value("Cancha 1"))
				.andExpect(jsonPath("$[0].tipoCanchaId").value(F5.toString()))
				.andExpect(jsonPath("$[0].precioHora").value(80000.00))
				.andExpect(jsonPath("$[0].estado").value("DISPONIBLE"));
	}

	@Test
	void ca8ElNombreEstaLibre() throws Exception {
		when(consultarNombreDisponible.ejecutar(ESCENARIO, "Cancha 2", PROPIETARIO)).thenReturn(true);

		mvc.perform(conToken(get("/api/escenarios/{id}/canchas/nombre-disponible", ESCENARIO)
				.param("nombre", "Cancha 2")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.disponible").value(true))
				.andExpect(jsonPath("$.codigo").doesNotExist());
	}

	@Test
	void ca8ElNombreYaExisteYSeMuestraErrCan03() throws Exception {
		when(consultarNombreDisponible.ejecutar(ESCENARIO, " Cancha 1 ", PROPIETARIO)).thenReturn(false);

		mvc.perform(conToken(get("/api/escenarios/{id}/canchas/nombre-disponible", ESCENARIO)
				.param("nombre", " Cancha 1 ")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.disponible").value(false))
				.andExpect(jsonPath("$.codigo").value("ERR-CAN-03"))
				.andExpect(jsonPath("$.mensaje").value("Ya existe una cancha llamada Cancha 1 en este escenario."));
	}

	@Test
	void ca8SinNombreEsObligatorio() throws Exception {
		mvc.perform(conToken(get("/api/escenarios/{id}/canchas/nombre-disponible", ESCENARIO)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-01"));
		verifyNoInteractions(consultarNombreDisponible);
	}

	@Test
	void ca10ListaLosTiposDeCancha() throws Exception {
		when(listarTipos.ejecutar()).thenReturn(List.of(TipoCancha.reconstruir(F5, "F5", "Fútbol 5", 10)));

		mvc.perform(conToken(get("/api/tipos-cancha")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(F5.toString()))
				.andExpect(jsonPath("$[0].codigo").value("F5"))
				.andExpect(jsonPath("$[0].nombre").value("Fútbol 5"))
				.andExpect(jsonPath("$[0].capacidadJugadores").value(10));
	}

}
