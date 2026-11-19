package co.edu.uco.canchaya.reservas.infrastructure.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarDisponibilidad;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarDisponibilidad.Disponibilidad;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarDisponibilidad.FranjaDisponible;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/** HU20: GET /api/canchas/{canchaId}/disponibilidad. Las peticiones van sin token: es pública (AG-11). */
@WebMvcTest(DisponibilidadController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class DisponibilidadControllerTest {

	private static final UUID CANCHA = UUID.randomUUID();
	private static final LocalDate HOY = LocalDate.of(2026, 11, 23);
	private static final LocalDate FECHA = HOY.plusDays(2);
	private static final String RUTA = "/api/canchas/{canchaId}/disponibilidad";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ConsultarDisponibilidad consultarDisponibilidad;

	private static Disponibilidad disponibilidad(LocalDate fecha, List<FranjaDisponible> franjas) {
		return new Disponibilidad(CANCHA, fecha, HOY, HOY.plusDays(30), 2, franjas);
	}

	private static FranjaDisponible franja(int hora, boolean libre) {
		return new FranjaDisponible(LocalTime.of(hora, 0), LocalTime.of(hora + 1, 0), libre);
	}

	@Test
	void CA1_canchaDisponible_devuelveFranjasLibresYOcupadas() throws Exception {
		when(consultarDisponibilidad.ejecutar(CANCHA, FECHA))
				.thenReturn(disponibilidad(FECHA, List.of(franja(6, true), franja(7, false))));

		mvc.perform(get(RUTA, CANCHA).param("fecha", "2026-11-25"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.canchaId").value(CANCHA.toString()))
				.andExpect(jsonPath("$.fecha").value("2026-11-25"))
				.andExpect(jsonPath("$.fechaMinima").value("2026-11-23"))
				.andExpect(jsonPath("$.fechaMaxima").value("2026-12-23"))
				.andExpect(jsonPath("$.franjas[0].horaInicio").value("06:00"))
				.andExpect(jsonPath("$.franjas[0].horaFin").value("07:00"))
				.andExpect(jsonPath("$.franjas[0].libre").value(true))
				.andExpect(jsonPath("$.franjas[1].libre").value(false))
				// Con franjas no hay mensaje informativo
				.andExpect(jsonPath("$.codigo").isEmpty())
				.andExpect(jsonPath("$.mensaje").isEmpty());
	}

	@Test
	void CA1_sinFecha_elCasoDeUsoRecibeVacioYUsaHoy() throws Exception {
		when(consultarDisponibilidad.ejecutar(eq(CANCHA), isNull()))
				.thenReturn(disponibilidad(HOY, List.of(franja(20, true))));

		mvc.perform(get(RUTA, CANCHA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fecha").value("2026-11-23"));
	}

	@Test
	void CA2_respuesta_noExponeQuienNiPorQue() throws Exception {
		when(consultarDisponibilidad.ejecutar(CANCHA, FECHA)).thenReturn(disponibilidad(FECHA, List.of(franja(7, false))));

		mvc.perform(get(RUTA, CANCHA).param("fecha", "2026-11-25"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.franjas[0].jugadorId").doesNotExist())
				.andExpect(jsonPath("$.franjas[0].reservaId").doesNotExist())
				.andExpect(jsonPath("$.franjas[0].tipoBloqueo").doesNotExist())
				.andExpect(jsonPath("$.franjas[0].motivo").doesNotExist());
	}

	@Test
	void CA4_sinFranjas_INF_HOR_01() throws Exception {
		when(consultarDisponibilidad.ejecutar(CANCHA, FECHA)).thenReturn(disponibilidad(FECHA, List.of()));

		mvc.perform(get(RUTA, CANCHA).param("fecha", "2026-11-25"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.franjas").isArray())
				.andExpect(jsonPath("$.franjas").isEmpty())
				.andExpect(jsonPath("$.codigo").value("INF-HOR-01"))
				.andExpect(jsonPath("$.mensaje").value("No hay franjas disponibles para esta fecha."));
	}

	@Test
	void CA5_fechaFueraDeVentana_422_ERR_HOR_10() throws Exception {
		LocalDate lejana = HOY.plusDays(31);
		when(consultarDisponibilidad.ejecutar(CANCHA, lejana)).thenThrow(new ReglaNegocioException("ERR-HOR-10"));

		mvc.perform(get(RUTA, CANCHA).param("fecha", lejana.toString()))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-HOR-10"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("Solo se pueden consultar fechas entre hoy y los próximos 30 días."));
	}

	@Test
	void CA6_canchaInexistenteOInactiva_404_ERR_RES_01() throws Exception {
		when(consultarDisponibilidad.ejecutar(CANCHA, FECHA)).thenThrow(new ReglaNegocioException("ERR-RES-01"));

		mvc.perform(get(RUTA, CANCHA).param("fecha", "2026-11-25"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-RES-01"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("La cancha no existe o no está disponible para reservas."));
	}

	@Test
	void CA7_respuesta_traeFranjasMaximasYFranjasSeguidas() throws Exception {
		when(consultarDisponibilidad.ejecutar(CANCHA, FECHA))
				.thenReturn(disponibilidad(FECHA, List.of(franja(18, true), franja(19, true))));

		mvc.perform(get(RUTA, CANCHA).param("fecha", "2026-11-25"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.franjasMaximasPorReserva").value(2))
				.andExpect(jsonPath("$.franjas[0].horaFin").value("19:00"))
				.andExpect(jsonPath("$.franjas[1].horaInicio").value("19:00"));
	}

	@Test
	void identificadorOFechaIlegibles_400_ERR_GEN_07() throws Exception {
		mvc.perform(get(RUTA, "no-es-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
		mvc.perform(get(RUTA, CANCHA).param("fecha", "25/11/2026"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
		verifyNoInteractions(consultarDisponibilidad);
	}

	@Test
	void CA8_soloElGetEsPublico() throws Exception {
		// La ruta pública es solo de lectura: otro método sigue pidiendo sesión
		mvc.perform(post(RUTA, CANCHA)).andExpect(status().isUnauthorized());
	}

}
