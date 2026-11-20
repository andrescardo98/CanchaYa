package co.edu.uco.canchaya.reservas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU21: responsabilidad «confirmar» de Reserva (políticas 1, 2, 5 a 9, 11, 12 y 31). */
class ReservaConfirmarTest {

	/** Lunes 23 de noviembre de 2026, a las 05:00 en Bogotá. */
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 5, 0);
	private static final LocalDate HOY = AHORA.toLocalDate();
	private static final LocalDate MANANA = HOY.plusDays(1);
	private static final UUID CANCHA = UUID.randomUUID();
	private static final Solicitante JUGADOR = new Solicitante(UUID.randomUUID(), Rol.JUGADOR);
	private static final BigDecimal PRECIO_HORA = new BigDecimal("80000");

	private static final ParametrosReserva INACTIVA = new ParametrosReserva(30, 2, 2, 5, 3, 5, LocalTime.NOON, 3, 24,
			250, false);
	private static final ParametrosReserva ACTIVA = new ParametrosReserva(30, 2, 2, 5, 3, 5, LocalTime.NOON, 3, 24,
			250, true);

	private static LocalTime hora(String texto) {
		return LocalTime.parse(texto);
	}

	/** Franjas de una hora de la fecha, una por cada hora de inicio. */
	private static List<Franja> franjas(LocalDate fecha, String... inicios) {
		return Arrays.stream(inicios).map(inicio -> new Franja(fecha, hora(inicio), hora(inicio).plusHours(1))).toList();
	}

	/** Datos que pasan todas las políticas; cada prueba cambia solo lo que revisa. */
	private static final class Solicitud {

		Solicitante jugador = JUGADOR;
		boolean canchaDisponible = true;
		LocalDate fecha = MANANA;
		String inicio = "18:00";
		String fin = "19:00";
		List<Franja> libres = franjas(MANANA, "17:00", "18:00", "19:00", "20:00");
		boolean bloqueada;
		int activas;
		int horasDelDia;
		LocalDateTime ahora = AHORA;
		ParametrosReserva parametros = INACTIVA;

		Reserva confirmar() {
			return Reserva.confirmar(CANCHA, jugador, fecha, hora(inicio), hora(fin), PRECIO_HORA, canchaDisponible,
					libres, bloqueada, activas, horasDelDia, ahora, parametros);
		}

	}

	private static void esperarError(Solicitud solicitud, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(solicitud::confirmar)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
	}

	@Test
	void CA1_franjaLibre_reservaConfirmadaConPrecio() {
		Reserva reserva = new Solicitud().confirmar();

		assertThat(reserva.getId()).isNotNull();
		assertThat(reserva.getCanchaId()).isEqualTo(CANCHA);
		assertThat(reserva.getJugadorId()).isEqualTo(JUGADOR.usuarioId());
		assertThat(reserva.getFecha()).isEqualTo(MANANA);
		assertThat(reserva.getHoraInicio()).isEqualTo(hora("18:00"));
		assertThat(reserva.getHoraFin()).isEqualTo(hora("19:00"));
		// Política 11: precio por hora × 1, guardado con 2 decimales (AG-19)
		assertThat(reserva.getPrecio()).isEqualTo(new BigDecimal("80000.00"));
		assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
		assertThat(reserva.getFechaCreacion())
				.isEqualTo(AHORA.atZone(ZoneId.of("America/Bogota")).toOffsetDateTime());
		assertThat(reserva.getOrigenCancelacion()).isNull();
		assertThat(reserva.getMotivoCancelacion()).isNull();
		assertThat(reserva.getFechaCancelacion()).isNull();
		assertThat(reserva.getFechaSolicitudConfirmacion()).isNull();
	}

	// CA2, D33: con PAR-RES-12 en «No» nunca se pide confirmación, aunque falten muchas horas
	@ParameterizedTest
	@CsvSource({ "18:00, 19:00", "06:00, 07:00", "12:00, 14:00" })
	void CA2_confirmacionInactiva_siempreNoRequerida(String inicio, String fin) {
		Solicitud solicitud = new Solicitud();
		solicitud.inicio = inicio;
		solicitud.fin = fin;
		solicitud.libres = franjas(MANANA, "06:00", "12:00", "13:00", "18:00");

		assertThat(solicitud.confirmar().getConfirmacionAsistencia()).isEqualTo(ConfirmacionAsistencia.NO_REQUERIDA);
	}

	// CA2, política 12: con la confirmación activa, PENDIENTE si faltan 5 h o más (PAR-RES-06) y
	// empieza desde las 12:00 (PAR-RES-07)
	@ParameterizedTest
	@CsvSource({
			"2026-11-23T13:00, 18:00, PENDIENTE",     // faltan exactamente 5 h: borde incluido
			"2026-11-23T13:01, 18:00, NO_REQUERIDA",  // faltan 4 h 59 min
			"2026-11-23T05:00, 12:00, PENDIENTE",     // empieza justo a las 12:00 m.
			"2026-11-23T05:00, 11:00, NO_REQUERIDA",  // empieza antes de las 12:00 m.
			"2026-11-22T05:00, 11:00, NO_REQUERIDA" }) // aunque falten más de 24 h
	void CA2_confirmacionActiva_pendienteONoRequerida(LocalDateTime ahora, String inicio,
			ConfirmacionAsistencia esperada) {
		Solicitud solicitud = new Solicitud();
		solicitud.ahora = ahora;
		solicitud.fecha = HOY;
		solicitud.inicio = inicio;
		solicitud.fin = hora(inicio).plusHours(1).toString();
		solicitud.libres = franjas(HOY, inicio);
		solicitud.parametros = ACTIVA;

		assertThat(solicitud.confirmar().getConfirmacionAsistencia()).isEqualTo(esperada);
	}

	// CA3, política 6: la franja tiene una reserva confirmada, así que no viene entre las libres
	@Test
	void CA3_franjaYaReservada_ERR_RES_06() {
		Solicitud solicitud = new Solicitud();
		solicitud.libres = franjas(MANANA, "17:00", "19:00");

		esperarError(solicitud, "ERR-RES-06");
	}

	// Política 6: una franja que ya empezó no se puede reservar, aunque venga entre las libres
	@Test
	void politica6_franjaQueYaEmpezo_ERR_RES_06() {
		Solicitud solicitud = new Solicitud();
		solicitud.fecha = HOY;
		solicitud.inicio = "05:00";
		solicitud.fin = "06:00";
		solicitud.libres = franjas(HOY, "05:00");

		esperarError(solicitud, "ERR-RES-06");
	}

	// CA5, política 7: no se dice el motivo del bloqueo
	@Test
	void CA5_franjaBloqueada_ERR_RES_07() {
		Solicitud solicitud = new Solicitud();
		solicitud.bloqueada = true;

		esperarError(solicitud, "ERR-RES-07");
	}

	// CA6, política 8 (PAR-RES-04)
	@Test
	void CA6_cincoActivas_ERR_RES_08() {
		Solicitud solicitud = new Solicitud();
		solicitud.activas = 5;
		esperarError(solicitud, "ERR-RES-08");

		solicitud.activas = 4;
		assertThat(solicitud.confirmar().getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
	}

	// CA7, política 9 (PAR-RES-03): las horas que ya tiene ese día, en cualquier cancha, más las nuevas
	@Test
	void CA7_superaDosHorasEnElDia_ERR_RES_09() {
		Solicitud solicitud = new Solicitud();
		solicitud.horasDelDia = 1;
		solicitud.fin = "20:00";
		esperarError(solicitud, "ERR-RES-09");

		solicitud.fin = "19:00";
		assertThat(solicitud.confirmar().horas()).isEqualTo(1);
	}

	// ERR-RES-09 lleva la fecha en dd/mm/aaaa (AG-18)
	@Test
	void CA7_ERR_RES_09_llevaLaFechaConFormato() {
		Solicitud solicitud = new Solicitud();
		solicitud.horasDelDia = 2;

		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(solicitud::confirmar).actual();
		assertThat(error.getParametros()).containsExactly("24/11/2026");
	}

	// CA14: ya tiene una reserva de 2 horas esa fecha
	@Test
	void CA14_yaTieneDosHorasEseDia_ERR_RES_09() {
		Solicitud solicitud = new Solicitud();
		solicitud.horasDelDia = 2;

		esperarError(solicitud, "ERR-RES-09");
	}

	// CA8, política 2 (PAR-RES-01): la ventana incluye hoy y hoy + 30
	@Test
	void CA8_fueraDeVentana_ERR_RES_02() {
		Solicitud solicitud = new Solicitud();
		solicitud.fecha = HOY.minusDays(1);
		solicitud.libres = List.of();
		esperarError(solicitud, "ERR-RES-02");

		solicitud.fecha = HOY.plusDays(31);
		esperarError(solicitud, "ERR-RES-02");

		solicitud.fecha = HOY.plusDays(30);
		solicitud.libres = franjas(HOY.plusDays(30), "18:00");
		assertThat(solicitud.confirmar().getFecha()).isEqualTo(HOY.plusDays(30));
	}

	// CA9, política 1
	@Test
	void CA9_canchaNoDisponible_ERR_RES_01() {
		Solicitud solicitud = new Solicitud();
		solicitud.canchaDisponible = false;
		solicitud.libres = List.of();

		esperarError(solicitud, "ERR-RES-01");
	}

	// CA10, política 5: se revisa primero, así no se le dice a otro rol si la cancha existe
	@ParameterizedTest
	@EnumSource(value = Rol.class, names = { "ADMINISTRADOR_CANCHA", "ADMINISTRADOR_SISTEMA" })
	void CA10_noJugador_ERR_RES_05(Rol rol) {
		Solicitud solicitud = new Solicitud();
		solicitud.jugador = new Solicitante(UUID.randomUUID(), rol);
		solicitud.canchaDisponible = false;

		esperarError(solicitud, "ERR-RES-05");
	}

	// CA11: dos franjas libres y seguidas son una sola reserva, con el precio de las dos
	@Test
	void CA11_dosFranjas_unaReservaConPrecioDoble() {
		Solicitud solicitud = new Solicitud();
		solicitud.fin = "20:00";

		Reserva reserva = solicitud.confirmar();

		assertThat(reserva.getHoraInicio()).isEqualTo(hora("18:00"));
		assertThat(reserva.getHoraFin()).isEqualTo(hora("20:00"));
		assertThat(reserva.horas()).isEqualTo(2);
		assertThat(reserva.getPrecio()).isEqualTo(new BigDecimal("160000.00"));
	}

	// CA11 y P3 (D10): las franjas de 11 y 12 salen de horarios contiguos (06–12 y 12–14) y sirven
	@Test
	void CA11_franjasSeguidasDeHorariosContiguos_seConfirma() {
		Solicitud solicitud = new Solicitud();
		solicitud.inicio = "11:00";
		solicitud.fin = "13:00";
		solicitud.libres = franjas(MANANA, "10:00", "11:00", "12:00", "13:00");

		assertThat(solicitud.confirmar().horas()).isEqualTo(2);
	}

	// CA12, política 31 y P9: la duración es de 1 o 2 horas en punto
	@ParameterizedTest
	@CsvSource({ "18:00, 21:00", "18:00, 18:00", "19:00, 18:00", "18:30, 19:30", "18:00, 19:30" })
	void CA12_duracionInvalida_ERR_RES_31(String inicio, String fin) {
		Solicitud solicitud = new Solicitud();
		solicitud.inicio = inicio;
		solicitud.fin = fin;

		esperarError(solicitud, "ERR-RES-31");
	}

	// CA13: si la segunda franja no sirve, no se crea nada (confirmar lanza antes de construir)
	@Test
	void CA13_segundaFranjaNoDisponible_noSeGuardaNinguna() {
		Solicitud reservada = new Solicitud();
		reservada.fin = "20:00";
		reservada.libres = franjas(MANANA, "18:00");
		esperarError(reservada, "ERR-RES-06");

		Solicitud bloqueada = new Solicitud();
		bloqueada.fin = "20:00";
		bloqueada.bloqueada = true;
		esperarError(bloqueada, "ERR-RES-07");

		// La segunda franja está fuera del horario: tampoco viene entre las libres
		Solicitud fueraDelHorario = new Solicitud();
		fueraDelHorario.inicio = "20:00";
		fueraDelHorario.fin = "22:00";
		esperarError(fueraDelHorario, "ERR-RES-06");
	}

	// Política 10: el caso de uso cambia el id si ya existe, sin cambiar nada más
	@Test
	void politica10_conNuevoId_soloCambiaElId() {
		Reserva reserva = new Solicitud().confirmar();

		Reserva otra = reserva.conNuevoId();

		assertThat(otra.getId()).isNotEqualTo(reserva.getId());
		assertThat(otra).usingRecursiveComparison().ignoringFields("id").isEqualTo(reserva);
	}

}
