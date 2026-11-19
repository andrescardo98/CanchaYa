package co.edu.uco.canchaya.reservas.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * Reserva de un jugador sobre una cancha para una fecha y 1 o 2 franjas seguidas (D10). La cancha
 * y el jugador se guardan por su identificador: Reservas no usa las clases Cancha ni Usuario.
 *
 * <p>HU20 la crea con lo que necesita la disponibilidad; {@code confirmar} llega con HU21.
 */
public final class Reserva {

	private final UUID id;
	private final UUID canchaId;
	private final UUID jugadorId;
	private final LocalDate fecha;
	private final LocalTime horaInicio;
	private final LocalTime horaFin;
	private final BigDecimal precio;
	private final EstadoReserva estado;
	private final ConfirmacionAsistencia confirmacionAsistencia;
	private final OffsetDateTime fechaSolicitudConfirmacion;
	private final OrigenCancelacion origenCancelacion;
	private final String motivoCancelacion;
	private final OffsetDateTime fechaCreacion;
	private final OffsetDateTime fechaCancelacion;

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
