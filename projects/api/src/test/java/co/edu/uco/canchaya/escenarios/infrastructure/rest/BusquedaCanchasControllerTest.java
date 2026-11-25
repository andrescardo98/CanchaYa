package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.uco.canchaya.escenarios.application.port.in.BuscarCanchas;
import co.edu.uco.canchaya.escenarios.application.port.in.BuscarCanchas.CanchaEncontrada;
import co.edu.uco.canchaya.escenarios.application.port.in.BuscarCanchas.CriteriosBusqueda;
import co.edu.uco.canchaya.escenarios.application.port.in.BuscarCanchas.ResultadoBusqueda;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/** HU18: GET /api/canchas/busqueda. Las peticiones van sin token: es pública (AG-11, CA9). */
@WebMvcTest(BusquedaCanchasController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class BusquedaCanchasControllerTest {

	private static final String RUTA = "/api/canchas/busqueda";
	private static final LocalDate HOY = LocalDate.of(2026, 11, 23);
	private static final UUID ESCENARIO = UUID.randomUUID();
	private static final UUID CANCHA = UUID.randomUUID();

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private BuscarCanchas buscarCanchas;

	private static CanchaEncontrada encontrada(BigDecimal distanciaKm) {
		return new CanchaEncontrada(ESCENARIO, "Canchas La 70", new BigDecimal("6.155123"), new BigDecimal("-75.374456"),
				CANCHA, "Cancha 1", "Fútbol 5", new BigDecimal("80000.00"), distanciaKm, null);
	}

	private static ResultadoBusqueda resultado(List<CanchaEncontrada> canchas, boolean conUbicacion) {
		return new ResultadoBusqueda(canchas, conUbicacion, conUbicacion ? 5 : null, 1, 30, HOY, HOY.plusDays(30));
	}

	// CA1, CA8 y P9: cada resultado trae lo que muestra la pantalla, el escenarioId para ir al perfil y las coordenadas
	// para el mapa; la respuesta trae el radio aplicado y los límites para la pantalla
	@Test
	void CA1_conUbicacion_devuelveResultadosConDistancia() throws Exception {
		when(buscarCanchas.ejecutar(any())).thenReturn(resultado(List.of(encontrada(new BigDecimal("2.4"))), true));

		mvc.perform(get(RUTA).param("latitud", "6.1551").param("longitud", "-75.3737"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultados[0].escenarioId").value(ESCENARIO.toString()))
				.andExpect(jsonPath("$.resultados[0].nombreEscenario").value("Canchas La 70"))
				.andExpect(jsonPath("$.resultados[0].latitud").value(6.155123))
				.andExpect(jsonPath("$.resultados[0].longitud").value(-75.374456))
				.andExpect(jsonPath("$.resultados[0].canchaId").value(CANCHA.toString()))
				.andExpect(jsonPath("$.resultados[0].nombreCancha").value("Cancha 1"))
				.andExpect(jsonPath("$.resultados[0].tipoCancha").value("Fútbol 5"))
				.andExpect(jsonPath("$.resultados[0].precioHora").value(80000.00))
				.andExpect(jsonPath("$.resultados[0].distanciaKm").value(2.4))
				.andExpect(jsonPath("$.resultados[0].calificacionPromedio").isEmpty())
				.andExpect(jsonPath("$.radioKm").value(5))
				.andExpect(jsonPath("$.radioMinimoKm").value(1))
				.andExpect(jsonPath("$.radioMaximoKm").value(30))
				.andExpect(jsonPath("$.fechaMinima").value("2026-11-23"))
				.andExpect(jsonPath("$.fechaMaxima").value("2026-12-23"))
				// Con ubicación y con resultados no hay mensaje informativo
				.andExpect(jsonPath("$.codigo").isEmpty())
				.andExpect(jsonPath("$.mensaje").isEmpty());
	}

	// Todos los filtros llegan al caso de uso tal como se enviaron
	@Test
	void losParametrosLleganAlCasoDeUso() throws Exception {
		UUID departamento = UUID.randomUUID();
		UUID ciudad = UUID.randomUUID();
		UUID tipo = UUID.randomUUID();
		when(buscarCanchas.ejecutar(any())).thenReturn(resultado(List.of(), true));

		mvc.perform(get(RUTA).param("latitud", "6.1551").param("longitud", "-75.3737").param("radioKm", "10")
				.param("departamentoId", departamento.toString()).param("ciudadId", ciudad.toString())
				.param("tipoCanchaId", tipo.toString()).param("fecha", "2026-11-30").param("hora", "18:00"))
				.andExpect(status().isOk());

		verify(buscarCanchas).ejecutar(new CriteriosBusqueda(new BigDecimal("6.1551"), new BigDecimal("-75.3737"), 10,
				departamento, ciudad, tipo, LocalDate.of(2026, 11, 30), LocalTime.of(18, 0)));
	}

	// AG-14: la respuesta no expone estados ni datos del administrador
	@Test
	void respuesta_noExponeEstadosNiAdministrador() throws Exception {
		when(buscarCanchas.ejecutar(any())).thenReturn(resultado(List.of(encontrada(null)), false));

		mvc.perform(get(RUTA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultados[0].estado").doesNotExist())
				.andExpect(jsonPath("$.resultados[0].administradorId").doesNotExist())
				.andExpect(jsonPath("$.resultados[0].telefonoContacto").doesNotExist());
	}

	// CA2 y P6: sin ubicación y con resultados se informa con INF-ESC-03; sin distancia ni radio
	@Test
	void CA2_sinUbicacion_INF_ESC_03_sinOrdenarPorDistancia() throws Exception {
		when(buscarCanchas.ejecutar(any())).thenReturn(resultado(List.of(encontrada(null)), false));

		mvc.perform(get(RUTA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultados[0].distanciaKm").isEmpty())
				.andExpect(jsonPath("$.radioKm").isEmpty())
				.andExpect(jsonPath("$.codigo").value("INF-ESC-03"))
				.andExpect(jsonPath("$.mensaje")
						.value("No tenemos acceso a su ubicación. Puede buscar canchas por departamento y ciudad."));
	}

	// CA2: latitud sin longitud → 400 con ERR-GEN-01 (lo lanza el caso de uso)
	@Test
	void CA2_latitudSinLongitud_ERR_GEN_01() throws Exception {
		when(buscarCanchas.ejecutar(any()))
				.thenThrow(new ReglaNegocioException("ERR-GEN-01", new TextoCatalogo("campo.longitud")));

		mvc.perform(get(RUTA).param("latitud", "6.1551"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-01"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("El campo longitud es obligatorio."));
	}

	// CA5: hora sin fecha → 400 con ERR-GEN-01
	@Test
	void CA5_horaSinFecha_ERR_GEN_01() throws Exception {
		when(buscarCanchas.ejecutar(any()))
				.thenThrow(new ReglaNegocioException("ERR-GEN-01", new TextoCatalogo("campo.fecha")));

		mvc.perform(get(RUTA).param("hora", "18:00"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].mensaje").value("El campo fecha es obligatorio."));
	}

	// CA5 y P5: hora que no está en punto → 400 con ERR-GEN-02 y el formato esperado
	@Test
	void CA5_horaNoEnPunto_ERR_GEN_02() throws Exception {
		when(buscarCanchas.ejecutar(any())).thenThrow(new ReglaNegocioException("ERR-GEN-02",
				new TextoCatalogo("campo.hora"), new TextoCatalogo("formato.hora")));

		mvc.perform(get(RUTA).param("fecha", "2026-11-30").param("hora", "14:30"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-02"))
				.andExpect(jsonPath("$.errores[0].mensaje").value(
						"El campo hora no tiene un formato válido. Escriba una hora en punto en formato hh:00. Ejemplo: 18:00"));
	}

	// CA6: fecha fuera de la ventana → 422 con ERR-HOR-10, aunque ninguna cancha cumpla los demás filtros
	@Test
	void CA6_fechaFueraDeVentana_422_ERR_HOR_10() throws Exception {
		when(buscarCanchas.ejecutar(any())).thenThrow(new ReglaNegocioException("ERR-HOR-10"));

		mvc.perform(get(RUTA).param("fecha", "2026-12-24"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-HOR-10"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("Solo se pueden consultar fechas entre hoy y los próximos 30 días."));
	}

	// CA7 y AG-09: sin resultados se informa con INF-ESC-01; gana sobre INF-ESC-03 (P6)
	@Test
	void CA7_sinResultados_INF_ESC_01() throws Exception {
		when(buscarCanchas.ejecutar(any())).thenReturn(resultado(List.of(), false));

		mvc.perform(get(RUTA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultados").isEmpty())
				.andExpect(jsonPath("$.codigo").value("INF-ESC-01"))
				.andExpect(jsonPath("$.mensaje").value(
						"No se encontraron canchas con estos filtros. Pruebe con un radio de búsqueda mayor o cambie los filtros."));
	}

	// CA11: radio fuera de 1 a 30 km → 400 con ERR-GEN-04 y los límites en el texto
	@Test
	void CA11_radioFueraDeRango_ERR_GEN_04() throws Exception {
		when(buscarCanchas.ejecutar(any()))
				.thenThrow(new ReglaNegocioException("ERR-GEN-04", new TextoCatalogo("campo.radioKm"), 1, 30));

		mvc.perform(get(RUTA).param("latitud", "6.1551").param("longitud", "-75.3737").param("radioKm", "31"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-04"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("El valor de radio de búsqueda debe estar entre 1 y 30."));
	}

	// P4: el radio es entero; 2.5 no se puede leer y no se busca (ERR-GEN-07, igual que un UUID o una fecha ilegibles)
	@Test
	void datosIlegibles_400_ERR_GEN_07() throws Exception {
		mvc.perform(get(RUTA).param("radioKm", "2.5")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
		mvc.perform(get(RUTA).param("ciudadId", "no-es-uuid")).andExpect(status().isBadRequest());
		mvc.perform(get(RUTA).param("fecha", "30/11/2026")).andExpect(status().isBadRequest());
		mvc.perform(get(RUTA).param("hora", "6pm")).andExpect(status().isBadRequest());

		verifyNoInteractions(buscarCanchas);
	}

	// CA9 y AG-11: sin token responde 200; solo el GET es público
	@Test
	void CA9_sinSesion_200_soloElGet() throws Exception {
		when(buscarCanchas.ejecutar(any())).thenReturn(resultado(List.of(encontrada(null)), false));

		mvc.perform(get(RUTA)).andExpect(status().isOk());
		mvc.perform(post(RUTA)).andExpect(status().isUnauthorized());
	}

}
