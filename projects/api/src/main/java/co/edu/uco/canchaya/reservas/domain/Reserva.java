package co.edu.uco.canchaya.reservas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * Reserva de un jugador sobre una cancha para una fecha y 1 o 2 franjas seguidas (D10). La cancha
 * y el jugador se guardan por su identificador: Reservas no usa las clases Cancha ni Usuario.
 *
 * <p>HU20 la crea con lo que necesita la disponibilidad, HU21 agrega {@code confirmar} y HU24
 * {@code cancelar}. Solo cambian el estado y los datos de la cancelación, y únicamente con los
 * métodos de negocio (clases.md: no hay métodos set).
 */
public final class Reserva {

	// D27: todo se calcula en Bogotá
	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	// AG-18: así va la fecha en los mensajes (ERR-RES-09)
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final UUID id;
	private final UUID canchaId;
	private final UUID jugadorId;
	private final LocalDate fecha;
	private final LocalTime horaInicio;
	private final LocalTime horaFin;
	private final BigDecimal precio;
	private EstadoReserva estado;
	private final ConfirmacionAsistencia confirmacionAsistencia;
	private final OffsetDateTime fechaSolicitudConfirmacion;
	private OrigenCancelacion origenCancelacion;
	private final String motivoCancelacion;
	private final OffsetDateTime fechaCreacion;
	private OffsetDateTime fechaCancelacion;

	private Reserva(UUID id, UUID canchaId, UUID jugadorId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
			BigDecimal precio, EstadoReserva estado, ConfirmacionAsistencia confirmacionAsistencia,
			OffsetDateTime fechaSolicitudConfirmacion, OrigenCancelacion origenCancelacion, String motivoCancelacion,
			OffsetDateTime fechaCreacion, OffsetDateTime fechaCancelacion) {
		this.id = id;
		this.canchaId = canchaId;
		this.jugadorId = jugadorId;
		this.fecha = fecha;
		this.horaInicio = horaInicio;
		this.horaFin = horaFin;
		this.precio = precio;
		this.estado = estado;
		this.confirmacionAsistencia = confirmacionAsistencia;
		this.fechaSolicitudConfirmacion = fechaSolicitudConfirmacion;
		this.origenCancelacion = origenCancelacion;
		this.motivoCancelacion = motivoCancelacion;
		this.fechaCreacion = fechaCreacion;
		this.fechaCancelacion = fechaCancelacion;
	}

	/**
	 * Responsabilidad «verificarDisponibilidad» (HU20). Dice si todas las franjas de
	 * [horaInicio, horaFin) se pueden reservar. No explica por qué no: eso lo hace confirmar (HU21)
	 * con ERR-RES-31, ERR-RES-06 o ERR-RES-07.
	 *
	 * @param canchaDisponible si la cancha existe, está DISPONIBLE y su escenario está ACTIVO
	 *        (puerto ConsultaCanchas; P2 de HU20)
	 * @param franjasLibres franjas de la fecha calculadas con HorarioRecurrente.calcularFranjas, sin
	 *        las ocupadas por reservas confirmadas ni bloqueos
	 * @param ahora fecha y hora actual en America/Bogota (D27)
	 * @return verdadero si el rango es de 1 a PAR-RES-02 franjas en punto y todas están libres y sin empezar
	 * @throws ReglaNegocioException ERR-RES-01 (política 1) o ERR-RES-02 (política 2)
	 */
	public static boolean verificarDisponibilidad(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
			boolean canchaDisponible, List<Franja> franjasLibres, LocalDateTime ahora, ParametrosReserva parametros) {
		Objects.requireNonNull(fecha, "fecha");
		Objects.requireNonNull(horaInicio, "horaInicio");
		Objects.requireNonNull(horaFin, "horaFin");
		Objects.requireNonNull(franjasLibres, "franjasLibres");
		Objects.requireNonNull(ahora, "ahora");
		Objects.requireNonNull(parametros, "parametros");

		// Política 1
		if (!canchaDisponible) {
			throw new ReglaNegocioException("ERR-RES-01");
		}
		// Política 2 (RN-11): la misma ventana inclusiva que calcularFranjas (P6 de HU20)
		LocalDate hoy = ahora.toLocalDate();
		if (fecha.isBefore(hoy) || fecha.isAfter(hoy.plusDays(parametros.ventanaReservaDias()))) {
			throw new ReglaNegocioException("ERR-RES-02");
		}

		// D10 y PAR-RES-02: el rango debe ser de franjas enteras en punto, entre 1 y el máximo
		if (!enPunto(horaInicio) || !enPunto(horaFin) || !horaFin.isAfter(horaInicio)) {
			return false;
		}
		long horas = Duration.between(horaInicio, horaFin).toHours();
		if (horas > parametros.franjasMaximasPorReserva()) {
			return false;
		}

		// Política 3: cada franja del rango tiene que estar entre las libres (dentro del horario,
		// sin reserva confirmada ni bloqueo). Política 4: y no puede haber empezado
		Set<Franja> libres = new HashSet<>(franjasLibres);
		for (LocalTime inicio = horaInicio; inicio.isBefore(horaFin); inicio = inicio.plusHours(1)) {
			Franja franja = new Franja(fecha, inicio, inicio.plusHours(1));
			if (!libres.contains(franja) || !franja.inicio().isAfter(ahora)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Responsabilidad «confirmar» (HU21). Crea la reserva CONFIRMADA si cumple las políticas 5, 1,
	 * 2, 31, 6, 7, 8 y 9, en el orden de la columna Excepciones de clases.md. La 10 (id repetido) la
	 * revisa el caso de uso con {@link #conNuevoId()}; la 13 (correo) es de HU22.
	 *
	 * @param jugador quien reserva, tomado del token de acceso
	 * @param precioHora precio de la cancha hoy; puede ser vacío si la cancha no está disponible
	 * @param canchaDisponible si la cancha existe, está DISPONIBLE y su escenario está ACTIVO
	 * @param franjasLibres franjas de la fecha (HorarioRecurrente.calcularFranjas) sin las que tienen
	 *        una reserva confirmada. Los bloqueos van aparte para responder ERR-RES-07 y no ERR-RES-06
	 * @param cubiertaPorBloqueo si algún bloqueo de la cancha se cruza con [horaInicio, horaFin)
	 * @param reservasActivasJugador reservas CONFIRMADAS del jugador que no han terminado (P4)
	 * @param horasReservadasEnElDia horas CONFIRMADAS del jugador en esa fecha, en cualquier cancha
	 * @param ahora fecha y hora actual en America/Bogota (D27)
	 * @throws ReglaNegocioException ERR-RES-05, ERR-RES-01, ERR-RES-02, ERR-RES-31, ERR-RES-06,
	 *         ERR-RES-07, ERR-RES-08 o ERR-RES-09 (con la fecha en dd/mm/aaaa)
	 */
	public static Reserva confirmar(UUID canchaId, Solicitante jugador, LocalDate fecha, LocalTime horaInicio,
			LocalTime horaFin, BigDecimal precioHora, boolean canchaDisponible, List<Franja> franjasLibres,
			boolean cubiertaPorBloqueo, int reservasActivasJugador, int horasReservadasEnElDia,
			LocalDateTime ahora, ParametrosReserva parametros) {
		Objects.requireNonNull(canchaId, "canchaId");
		Objects.requireNonNull(jugador, "jugador");
		Objects.requireNonNull(fecha, "fecha");
		Objects.requireNonNull(horaInicio, "horaInicio");
		Objects.requireNonNull(horaFin, "horaFin");
		Objects.requireNonNull(franjasLibres, "franjasLibres");
		Objects.requireNonNull(ahora, "ahora");
		Objects.requireNonNull(parametros, "parametros");

		// Política 5: va primero, así a otro rol no se le dice si la cancha existe (CA10)
		if (!jugador.tieneRol(Rol.JUGADOR)) {
			throw new ReglaNegocioException("ERR-RES-05");
		}
		// Política 1
		if (!canchaDisponible) {
			throw new ReglaNegocioException("ERR-RES-01");
		}
		Objects.requireNonNull(precioHora, "precioHora");
		// Política 2 (RN-11): de hoy a hoy + PAR-RES-01, los dos incluidos (P6 de HU20)
		LocalDate hoy = ahora.toLocalDate();
		if (fecha.isBefore(hoy) || fecha.isAfter(hoy.plusDays(parametros.ventanaReservaDias()))) {
			throw new ReglaNegocioException("ERR-RES-02");
		}
		// Política 31 (D10, PAR-RES-02, P9): horas en punto y de 1 al máximo de franjas
		if (!enPunto(horaInicio) || !enPunto(horaFin) || !horaFin.isAfter(horaInicio)
				|| Duration.between(horaInicio, horaFin).toHours() > parametros.franjasMaximasPorReserva()) {
			throw new ReglaNegocioException("ERR-RES-31");
		}
		int horas = (int) Duration.between(horaInicio, horaFin).toHours();
		// Política 6: cada franja está libre, dentro del horario y sin empezar. No se exige que las dos
		// salgan del mismo horario recurrente: basta con que sean seguidas (P3, D10)
		Set<Franja> libres = new HashSet<>(franjasLibres);
		for (LocalTime inicio = horaInicio; inicio.isBefore(horaFin); inicio = inicio.plusHours(1)) {
			Franja franja = new Franja(fecha, inicio, inicio.plusHours(1));
			if (!libres.contains(franja) || !franja.inicio().isAfter(ahora)) {
				throw new ReglaNegocioException("ERR-RES-06");
			}
		}
		// Política 7: no se dice el motivo del bloqueo
		if (cubiertaPorBloqueo) {
			throw new ReglaNegocioException("ERR-RES-07");
		}
		// Política 8 (PAR-RES-04): una reserva de 2 horas cuenta como una
		if (reservasActivasJugador >= parametros.reservasActivasMaximas()) {
			throw new ReglaNegocioException("ERR-RES-08");
		}
		// Política 9 (PAR-RES-03)
		if (horasReservadasEnElDia + horas > parametros.horasMaximasPorDia()) {
			throw new ReglaNegocioException("ERR-RES-09", fecha.format(FECHA));
		}

		// Política 11: el precio se fija hoy y no cambia aunque la cancha cambie de precio (HU9)
		BigDecimal precio = precioHora.multiply(BigDecimal.valueOf(horas)).setScale(2, RoundingMode.HALF_UP);
		ConfirmacionAsistencia confirmacion = calcularConfirmacionInicial(fecha, horaInicio, ahora, parametros);
		return new Reserva(UUID.randomUUID(), canchaId, jugador.usuarioId(), fecha, horaInicio, horaFin, precio,
				EstadoReserva.CONFIRMADA, confirmacion, null, null, null, ahora.atZone(BOGOTA).toOffsetDateTime(),
				null);
	}

	/**
	 * Política 12 y D33. Con PAR-RES-12 en «No» nunca se pide confirmar. Si está activa, se pide
	 * cuando faltan PAR-RES-06 horas o más y la reserva empieza desde PAR-RES-07.
	 */
	private static ConfirmacionAsistencia calcularConfirmacionInicial(LocalDate fecha, LocalTime horaInicio,
			LocalDateTime ahora, ParametrosReserva parametros) {
		if (!parametros.confirmacionAsistenciaActiva()) {
			return ConfirmacionAsistencia.NO_REQUERIDA;
		}
		boolean conAnticipacion = Duration.between(ahora, fecha.atTime(horaInicio))
				.compareTo(Duration.ofHours(parametros.horasAnticipacionConfirmacion())) >= 0;
		boolean desdeLaHoraMinima = !horaInicio.isBefore(parametros.horaMinimaConfirmacion());
		return conAnticipacion && desdeLaHoraMinima ? ConfirmacionAsistencia.PENDIENTE
				: ConfirmacionAsistencia.NO_REQUERIDA;
	}

	/**
	 * Responsabilidad «cancelar» (HU24), solo la parte del jugador (P4 de HU24): el propietario por
	 * fuerza mayor, con las políticas 17 y 18, lo agrega HU32. Las políticas van en el orden de
	 * clases.md. La 20 (la franja queda libre) no necesita código: la restricción de exclusión y las
	 * consultas de disponibilidad solo cuentan las reservas CONFIRMADAS. La 21 (correo) es de HU25.
	 *
	 * @param solicitante quien cancela, tomado del token de acceso
	 * @param ahora fecha y hora actual del servidor en America/Bogota (D27), no la del dispositivo
	 * @throws ReglaNegocioException ERR-RES-14 (política 14), ERR-RES-15 (política 15) o ERR-RES-16
	 *         (política 16)
	 */
	public void cancelar(Solicitante solicitante, LocalDateTime ahora, ParametrosReserva parametros) {
		Objects.requireNonNull(solicitante, "solicitante");
		Objects.requireNonNull(ahora, "ahora");
		Objects.requireNonNull(parametros, "parametros");

		// Política 14: el caso de uso ya lo revisó al buscarla; aquí se repite para no depender de él
		if (!estaConfirmada()) {
			throw new ReglaNegocioException("ERR-RES-14");
		}
		// Política 15: va antes que la 16, así a otro usuario no se le habla del plazo
		if (!jugadorId.equals(solicitante.usuarioId())) {
			throw new ReglaNegocioException("ERR-RES-15");
		}
		// Política 16 (RN-10, D06, PAR-RES-05): deben faltar MÁS de esas horas; con exactamente 3 no
		if (Duration.between(ahora, inicio()).compareTo(Duration.ofHours(parametros.horasMinimasCancelacion())) <= 0) {
			throw new ReglaNegocioException("ERR-RES-16");
		}

		// Política 19
		this.estado = EstadoReserva.CANCELADA;
		this.origenCancelacion = OrigenCancelacion.JUGADOR;
		this.fechaCancelacion = ahora.atZone(BOGOTA).toOffsetDateTime();
	}

	/**
	 * Política 10: la misma reserva con otro identificador, para cuando el generado ya existe. No
	 * está en clases.md; queda en los pendientes de los Excel de HU21.
	 */
	public Reserva conNuevoId() {
		return new Reserva(UUID.randomUUID(), canchaId, jugadorId, fecha, horaInicio, horaFin, precio, estado,
				confirmacionAsistencia, fechaSolicitudConfirmacion, origenCancelacion, motivoCancelacion,
				fechaCreacion, fechaCancelacion);
	}

	/**
	 * Vuelve a armar una reserva ya guardada, sin repetir las validaciones. A diferencia de
	 * clases.md, recibe también fechaSolicitudConfirmacion (D34), que el constructor y la tabla sí
	 * tienen; queda anotado en los pendientes de los Excel de HU20.
	 */
	public static Reserva reconstruir(UUID id, UUID canchaId, UUID jugadorId, LocalDate fecha, LocalTime horaInicio,
			LocalTime horaFin, BigDecimal precio, EstadoReserva estado, ConfirmacionAsistencia confirmacionAsistencia,
			OrigenCancelacion origenCancelacion, String motivoCancelacion, OffsetDateTime fechaCreacion,
			OffsetDateTime fechaCancelacion, OffsetDateTime fechaSolicitudConfirmacion) {
		return new Reserva(id, canchaId, jugadorId, fecha, horaInicio, horaFin, precio, estado,
				confirmacionAsistencia, fechaSolicitudConfirmacion, origenCancelacion, motivoCancelacion,
				fechaCreacion, fechaCancelacion);
	}

	/** Fecha y hora de inicio de la reserva. */
	public LocalDateTime inicio() {
		return fecha.atTime(horaInicio);
	}

	/** Fecha y hora de fin de la reserva. */
	public LocalDateTime fin() {
		return fecha.atTime(horaFin);
	}

	/** Horas que cubre la reserva (1 o 2). */
	public int horas() {
		return (int) Duration.between(horaInicio, horaFin).toHours();
	}

	public boolean estaConfirmada() {
		return estado == EstadoReserva.CONFIRMADA;
	}

	/**
	 * Política 13 y D33 (HU22, CA1): si el jugador deberá confirmar su asistencia, COR-RES-01 lleva
	 * el aviso. Con PAR-RES-12 en «No» la reserva nace NO_REQUERIDA y esto da falso.
	 */
	public boolean requiereConfirmacionAsistencia() {
		return confirmacionAsistencia == ConfirmacionAsistencia.PENDIENTE;
	}

	/**
	 * HU23, P1: vigente es una reserva CONFIRMADA que todavía no termina, aunque ya haya empezado.
	 * Es la misma idea de «reserva activa» de HU21 (P4). Las CANCELADAS y NO_ASISTIO son pasadas
	 * aunque su fecha sea futura.
	 *
	 * @param ahora fecha y hora actual en America/Bogota (D27)
	 */
	public boolean esVigente(LocalDateTime ahora) {
		Objects.requireNonNull(ahora, "ahora");
		return estaConfirmada() && fin().isAfter(ahora);
	}

	/**
	 * Política 16 (RN-10, D06): el jugador puede cancelar una reserva CONFIRMADA solo si faltan más de
	 * PAR-RES-05 horas para el inicio; con exactamente esas horas ya no. HU23 la usa para mostrar la
	 * opción de cancelar (CA3) y HU24 la usará para lanzar ERR-RES-16.
	 *
	 * @param ahora fecha y hora actual en America/Bogota (D27)
	 */
	public boolean sePuedeCancelarPorJugador(LocalDateTime ahora, ParametrosReserva parametros) {
		Objects.requireNonNull(ahora, "ahora");
		Objects.requireNonNull(parametros, "parametros");
		return estaConfirmada()
				&& Duration.between(ahora, inicio()).compareTo(Duration.ofHours(parametros.horasMinimasCancelacion())) > 0;
	}

	private static boolean enPunto(LocalTime hora) {
		return hora.getMinute() == 0 && hora.getSecond() == 0 && hora.getNano() == 0;
	}

	public UUID getId() {
		return id;
	}

	public UUID getCanchaId() {
		return canchaId;
	}

	public UUID getJugadorId() {
		return jugadorId;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public LocalTime getHoraInicio() {
		return horaInicio;
	}

	public LocalTime getHoraFin() {
		return horaFin;
	}

	public BigDecimal getPrecio() {
		return precio;
	}

	public EstadoReserva getEstado() {
		return estado;
	}

	public ConfirmacionAsistencia getConfirmacionAsistencia() {
		return confirmacionAsistencia;
	}

	public OffsetDateTime getFechaSolicitudConfirmacion() {
		return fechaSolicitudConfirmacion;
	}

	public OrigenCancelacion getOrigenCancelacion() {
		return origenCancelacion;
	}

	public String getMotivoCancelacion() {
		return motivoCancelacion;
	}

	public OffsetDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public OffsetDateTime getFechaCancelacion() {
		return fechaCancelacion;
	}

}
