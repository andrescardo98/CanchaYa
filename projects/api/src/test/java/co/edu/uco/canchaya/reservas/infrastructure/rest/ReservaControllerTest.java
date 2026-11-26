package co.edu.uco.canchaya.reservas.infrastructure.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
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

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.infrastructure.security.SeguridadParaPruebas;
import co.edu.uco.canchaya.reservas.application.port.in.CancelarReserva;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarMisReservas;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarMisReservas.MisReservas;
import co.edu.uco.canchaya.reservas.application.port.in.ConsultarMisReservas.ReservaDelJugador;
import co.edu.uco.canchaya.reservas.application.port.in.ReservarCancha;
import co.edu.uco.canchaya.reservas.application.port.in.ReservarCancha.ReservaConfirmada;
import co.edu.uco.canchaya.reservas.domain.ConfirmacionAsistencia;
import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.OrigenCancelacion;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/**
 * HU21: POST /api/reservas, HU23: GET /api/reservas/mias y HU24: POST /api/reservas/{id}/cancelacion.
 * Los casos de uso son dobles: sus reglas se prueban aparte.
 */
@WebMvcTest(ReservaController.class)
@Import({ CatalogoMensajes.class, SeguridadParaPruebas.class })
class ReservaControllerTest {

	private static final UUID USUARIO = UUID.randomUUID();
	private static final Solicitante JUGADOR = new Solicitante(USUARIO, Rol.JUGADOR);
	private static final UUID CANCHA = UUID.randomUUID();
	private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);
	private static final String CUERPO = """
			{"canchaId": "%s", "fecha": "2026-11-20", "horaInicio": "18:00", "horaFin": "20:00"}""".formatted(CANCHA);

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ReservarCancha reservarCancha;

	@MockitoBean
	private ConsultarMisReservas consultarMisReservas;

	@MockitoBean
	private CancelarReserva cancelarReserva;

	private static MockHttpServletRequestBuilder reservar(Rol rol, String cuerpo) {
		return post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(cuerpo)
				.with(jwt().jwt(token -> token.subject(USUARIO.toString()).claim("rol", rol.name())));
	}

	private void cuandoFalla(String codigo, Object... parametros) {
		when(reservarCancha.ejecutar(any(), any(), any(), any(), any()))
				.thenThrow(new ReglaNegocioException(codigo, parametros));
	}

	@Test
	void CA1_reservaConfirmada_201_EXI_RES_01() throws Exception {
		Reserva reserva = Reserva.reconstruir(UUID.randomUUID(), CANCHA, USUARIO, FECHA, LocalTime.of(18, 0),
				LocalTime.of(20, 0), new BigDecimal("160000.00"), EstadoReserva.CONFIRMADA,
				ConfirmacionAsistencia.NO_REQUERIDA, null, null, OffsetDateTime.now(), null, null);
		when(reservarCancha.ejecutar(CANCHA, FECHA, LocalTime.of(18, 0), LocalTime.of(20, 0), JUGADOR))
				.thenReturn(new ReservaConfirmada(reserva, "Cancha 1", "Canchas La 70"));

		mvc.perform(reservar(Rol.JUGADOR, CUERPO))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(reserva.getId().toString()))
				.andExpect(jsonPath("$.canchaId").value(CANCHA.toString()))
				.andExpect(jsonPath("$.cancha").value("Cancha 1"))
				.andExpect(jsonPath("$.escenario").value("Canchas La 70"))
				.andExpect(jsonPath("$.fecha").value("2026-11-20"))
				.andExpect(jsonPath("$.horaInicio").value("18:00"))
				.andExpect(jsonPath("$.horaFin").value("20:00"))
				.andExpect(jsonPath("$.precio").value(160000.00))
				.andExpect(jsonPath("$.codigo").value("EXI-RES-01"))
				// AG-18 (dd/mm/aaaa) y AG-19 ($ 160.000)
				.andExpect(jsonPath("$.mensaje").value("¡Reserva confirmada! Canchas La 70, Cancha 1, 20/11/2026 de "
						+ "18:00 a 20:00. Valor: $ 160.000. Le enviamos los datos a su correo."))
				// AG-14: datos que el jugador no ve
				.andExpect(jsonPath("$.estado").doesNotExist())
				.andExpect(jsonPath("$.confirmacionAsistencia").doesNotExist())
				.andExpect(jsonPath("$.origenCancelacion").doesNotExist())
				.andExpect(jsonPath("$.fechaCreacion").doesNotExist())
				.andExpect(jsonPath("$.fechaCancelacion").doesNotExist())
				.andExpect(jsonPath("$.jugadorId").doesNotExist());
	}

	@Test
	void CA3_franjaYaReservada_409() throws Exception {
		cuandoFalla("ERR-RES-06");

		mvc.perform(reservar(Rol.JUGADOR, CUERPO))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-RES-06"))
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("El horario elegido ya no está disponible. Elija otro horario."));
	}

	@Test
	void CA9_canchaNoDisponible_404_ERR_RES_01() throws Exception {
		cuandoFalla("ERR-RES-01");

		mvc.perform(reservar(Rol.JUGADOR, CUERPO))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-RES-01"));
	}

	@Test
	void CA10_noJugador_403_ERR_RES_05() throws Exception {
		cuandoFalla("ERR-RES-05");

		mvc.perform(reservar(Rol.ADMINISTRADOR_CANCHA, CUERPO))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-RES-05"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("Solo los jugadores pueden hacer reservas."));
	}

	@Test
	void CA12_duracionInvalida_422_ERR_RES_31() throws Exception {
		cuandoFalla("ERR-RES-31");

		mvc.perform(reservar(Rol.JUGADOR, CUERPO))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-RES-31"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("Una reserva puede durar 1 o 2 horas seguidas."));
	}

	// CA5, CA6, CA7 y CA8: las demás reglas responden 422 con su texto
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"ERR-RES-02 | Solo se puede reservar entre hoy y los próximos 30 días.",
			"ERR-RES-07 | El horario elegido no está disponible. Elija otro horario.",
			"ERR-RES-08 | Ya tiene 5 reservas activas, que es el máximo permitido." })
	void reglasDelJugador_422(String codigo, String texto) throws Exception {
		cuandoFalla(codigo);

		mvc.perform(reservar(Rol.JUGADOR, CUERPO))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].codigo").value(codigo))
				.andExpect(jsonPath("$.errores[0].mensaje").value(texto));
	}

	@Test
	void CA7_ERR_RES_09_llevaLaFecha() throws Exception {
		cuandoFalla("ERR-RES-09", "20/11/2026");

		mvc.perform(reservar(Rol.JUGADOR, CUERPO))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errores[0].mensaje")
						.value("Con esta reserva superaría el máximo de 2 horas de reserva para el 20/11/2026."));
	}

	// AG-03: los cuatro campos que faltan, todos a la vez
	@Test
	void camposVacios_400_ERR_GEN_01() throws Exception {
		mvc.perform(reservar(Rol.JUGADOR, "{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo").value(containsInAnyOrder("canchaId", "fecha", "horaInicio",
						"horaFin")))
				.andExpect(jsonPath("$.errores[*].codigo").value(containsInAnyOrder("ERR-GEN-01", "ERR-GEN-01",
						"ERR-GEN-01", "ERR-GEN-01")));
		verifyNoInteractions(reservarCancha);
	}

	@Test
	void horaIlegible_400_ERR_GEN_07() throws Exception {
		mvc.perform(reservar(Rol.JUGADOR, CUERPO.replace("\"20:00\"", "\"25:00\"")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
		verifyNoInteractions(reservarCancha);
	}

	// CA15: sin sesión. INF-RES-02 y volver al resumen son de la pantalla
	@Test
	void CA15_sinSesion_401() throws Exception {
		mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-05"));
		verifyNoInteractions(reservarCancha);
	}

	// --- HU23: GET /api/reservas/mias --------------------------------------------------------

	private static MockHttpServletRequestBuilder misReservas(Rol rol) {
		return get("/api/reservas/mias")
				.with(jwt().jwt(token -> token.subject(USUARIO.toString()).claim("rol", rol.name())));
	}

	private static Reserva guardada(LocalDate fecha, EstadoReserva estado, OrigenCancelacion origen, String motivo) {
		return Reserva.reconstruir(UUID.randomUUID(), CANCHA, USUARIO, fecha, LocalTime.of(18, 0),
				LocalTime.of(20, 0), new BigDecimal("160000.00"), estado, ConfirmacionAsistencia.NO_REQUERIDA, origen,
				motivo, OffsetDateTime.now(), null, null);
	}

	private static ReservaDelJugador delJugador(Reserva reserva, boolean cancelable) {
		return new ReservaDelJugador(reserva, "Cancha 1", "Canchas La 70", cancelable);
	}

	// HU23, CA1: cada reserva con escenario, cancha, fecha, horas, precio, estado y confirmación de
	// asistencia; y el indicador de CA3. El jugador sale del token (CA5)
	@Test
	void HU23_CA1_devuelveLosDatosDeCadaReserva() throws Exception {
		Reserva vigente = guardada(FECHA, EstadoReserva.CONFIRMADA, null, null);
		Reserva pasada = guardada(FECHA.minusDays(7), EstadoReserva.NO_ASISTIO, null, null);
		when(consultarMisReservas.ejecutar(JUGADOR)).thenReturn(new MisReservas(
				List.of(delJugador(vigente, true)), List.of(delJugador(pasada, false))));

		mvc.perform(misReservas(Rol.JUGADOR))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vigentes.length()").value(1))
				.andExpect(jsonPath("$.vigentes[0].id").value(vigente.getId().toString()))
				.andExpect(jsonPath("$.vigentes[0].escenario").value("Canchas La 70"))
				.andExpect(jsonPath("$.vigentes[0].cancha").value("Cancha 1"))
				.andExpect(jsonPath("$.vigentes[0].fecha").value("2026-11-20"))
				.andExpect(jsonPath("$.vigentes[0].horaInicio").value("18:00"))
				.andExpect(jsonPath("$.vigentes[0].horaFin").value("20:00"))
				.andExpect(jsonPath("$.vigentes[0].precio").value(160000.00))
				.andExpect(jsonPath("$.vigentes[0].estado").value("CONFIRMADA"))
				.andExpect(jsonPath("$.vigentes[0].confirmacionAsistencia").value("NO_REQUERIDA"))
				.andExpect(jsonPath("$.vigentes[0].cancelable").value(true))
				.andExpect(jsonPath("$.vigentes[0].canchaId").doesNotExist())
				.andExpect(jsonPath("$.vigentes[0].jugadorId").doesNotExist())
				.andExpect(jsonPath("$.pasadas[0].id").value(pasada.getId().toString()))
				.andExpect(jsonPath("$.pasadas[0].estado").value("NO_ASISTIO"))
				.andExpect(jsonPath("$.pasadas[0].cancelable").value(false))
				.andExpect(jsonPath("$.codigo").isEmpty())
				.andExpect(jsonPath("$.mensaje").isEmpty());
	}

	// HU23, CA2 (AG-09): sin reservas, listas vacías y INF-RES-01
	@Test
	void HU23_CA2_sinReservas_INF_RES_01() throws Exception {
		when(consultarMisReservas.ejecutar(JUGADOR)).thenReturn(new MisReservas(List.of(), List.of()));

		mvc.perform(misReservas(Rol.JUGADOR))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vigentes").isEmpty())
				.andExpect(jsonPath("$.pasadas").isEmpty())
				.andExpect(jsonPath("$.codigo").value("INF-RES-01"))
				.andExpect(jsonPath("$.mensaje")
						.value("Aún no tiene reservas. Busque una cancha cerca de usted para hacer la primera."));
	}

	// HU23, CA4: quién canceló; la justificación solo si fue el administrador
	@ParameterizedTest
	@CsvSource({ "ADMINISTRADOR, Tormenta eléctrica, Tormenta eléctrica", "JUGADOR, texto guardado,",
			"SISTEMA_SIN_CONFIRMACION, texto guardado," })
	void HU23_CA4_cancelada_muestraOrigenYJustificacion(OrigenCancelacion origen, String guardado, String esperado)
			throws Exception {
		Reserva cancelada = guardada(FECHA, EstadoReserva.CANCELADA, origen, guardado);
		when(consultarMisReservas.ejecutar(JUGADOR))
				.thenReturn(new MisReservas(List.of(), List.of(delJugador(cancelada, false))));

		mvc.perform(misReservas(Rol.JUGADOR))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pasadas[0].estado").value("CANCELADA"))
				.andExpect(jsonPath("$.pasadas[0].origenCancelacion").value(origen.name()))
				.andExpect(jsonPath("$.pasadas[0].motivoCancelacion").value(esperado));
	}

	// HU23, P2: otro rol recibe 403 con ERR-GEN-06
	@Test
	void HU23_P2_noJugador_403_ERR_GEN_06() throws Exception {
		when(consultarMisReservas.ejecutar(any())).thenThrow(new ReglaNegocioException("ERR-GEN-06"));

		mvc.perform(misReservas(Rol.ADMINISTRADOR_CANCHA))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-06"));
	}

	// HU23, AG-11: sin sesión
	@Test
	void HU23_sinSesion_401() throws Exception {
		mvc.perform(get("/api/reservas/mias"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-05"));
		verifyNoInteractions(consultarMisReservas);
	}

	// HU24: POST /api/reservas/{id}/cancelacion (P1)

	private static MockHttpServletRequestBuilder cancelar(Object id) {
		return post("/api/reservas/{id}/cancelacion", id)
				.with(jwt().jwt(token -> token.subject(USUARIO.toString()).claim("rol", Rol.JUGADOR.name())));
	}

	@Test
	void ca1Cancelar_200_EXI_RES_02() throws Exception {
		UUID id = UUID.randomUUID();
		Reserva cancelada = Reserva.reconstruir(id, CANCHA, USUARIO, FECHA, LocalTime.of(18, 0), LocalTime.of(19, 0),
				new BigDecimal("80000.00"), EstadoReserva.CANCELADA, ConfirmacionAsistencia.NO_REQUERIDA,
				OrigenCancelacion.JUGADOR, null, OffsetDateTime.now(), OffsetDateTime.now(), null);
		// El solicitante sale del token, no de la petición
		when(cancelarReserva.ejecutar(id, JUGADOR)).thenReturn(cancelada);

		mvc.perform(cancelar(id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value("EXI-RES-02"))
				.andExpect(jsonPath("$.mensaje").value("La reserva se canceló correctamente."))
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.estado").value("CANCELADA"))
				// AG-14: ni el origen ni la fecha de cancelación
				.andExpect(jsonPath("$.origenCancelacion").doesNotExist())
				.andExpect(jsonPath("$.fechaCancelacion").doesNotExist());
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			ERR-RES-14 | 404 | La reserva no existe o ya no está confirmada.
			ERR-RES-15 | 403 | No tiene permiso para cancelar esta reserva.
			ERR-RES-16 | 422 | Ya no es posible cancelar la reserva porque faltan 3 horas o menos para su inicio.
			""")
	void ca3Ca4Ca5LasReglasIncumplidasRespondenSuMensaje(String codigo, int estado, String texto) throws Exception {
		when(cancelarReserva.ejecutar(any(), any())).thenThrow(new ReglaNegocioException(codigo));

		mvc.perform(cancelar(UUID.randomUUID()))
				.andExpect(status().is(estado))
				.andExpect(jsonPath("$.errores[0].codigo").value(codigo))
				.andExpect(jsonPath("$.errores[0].mensaje").value(texto));
	}

	@Test
	void idInvalido_400_ERR_GEN_07() throws Exception {
		mvc.perform(cancelar("no-es-un-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-07"));
		verifyNoInteractions(cancelarReserva);
	}

	// AG-11: sin sesión
	@Test
	void sinSesion_401_ERR_GEN_05() throws Exception {
		mvc.perform(post("/api/reservas/{id}/cancelacion", UUID.randomUUID()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errores[0].codigo").value("ERR-GEN-05"));
		verifyNoInteractions(cancelarReserva);
	}

}
