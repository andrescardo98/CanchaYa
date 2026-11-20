package co.edu.uco.canchaya.reservas.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import co.edu.uco.canchaya.reservas.domain.ConfirmacionAsistencia;
import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.OrigenCancelacion;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * La columna rango no se mapea: la base la calcula con la fecha y las horas, y solo la usa la
 * restricción ex_reserva_sin_cruce (D29 punto 1). La cancha y el jugador van por identificador.
 */
@Entity
@Table(name = "reserva")
class ReservaEntidad implements Persistable<UUID> {

	@Id
	@Column(name = "id")
	private UUID id;

	@Column(name = "cancha_id")
	private UUID canchaId;

	@Column(name = "jugador_id")
	private UUID jugadorId;

	@Column(name = "fecha")
	private LocalDate fecha;

	@Column(name = "hora_inicio")
	private LocalTime horaInicio;

	@Column(name = "hora_fin")
	private LocalTime horaFin;

	@Column(name = "precio", precision = 10, scale = 2)
	private BigDecimal precio;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", length = 10)
	private EstadoReserva estado;

	@Enumerated(EnumType.STRING)
	@Column(name = "confirmacion_asistencia", length = 12)
	private ConfirmacionAsistencia confirmacionAsistencia;

	@Column(name = "fecha_solicitud_confirmacion")
	private OffsetDateTime fechaSolicitudConfirmacion;

	@Enumerated(EnumType.STRING)
	@Column(name = "origen_cancelacion", length = 25)
	private OrigenCancelacion origenCancelacion;

	@Column(name = "motivo_cancelacion", length = 250)
	private String motivoCancelacion;

	@Column(name = "fecha_creacion")
	private OffsetDateTime fechaCreacion;

	@Column(name = "fecha_cancelacion")
	private OffsetDateTime fechaCancelacion;

	// El id lo pone el dominio, así que sin esto save() haría un select antes de insertar
	@Transient
	private boolean nuevo = true;

	protected ReservaEntidad() {
		// Lo pide JPA
	}

	static ReservaEntidad desde(Reserva reserva) {
		ReservaEntidad entidad = new ReservaEntidad();
		entidad.id = reserva.getId();
		entidad.canchaId = reserva.getCanchaId();
		entidad.jugadorId = reserva.getJugadorId();
		entidad.fecha = reserva.getFecha();
		entidad.horaInicio = reserva.getHoraInicio();
		entidad.horaFin = reserva.getHoraFin();
		entidad.precio = reserva.getPrecio();
		entidad.estado = reserva.getEstado();
		entidad.confirmacionAsistencia = reserva.getConfirmacionAsistencia();
		entidad.fechaSolicitudConfirmacion = reserva.getFechaSolicitudConfirmacion();
		entidad.origenCancelacion = reserva.getOrigenCancelacion();
		entidad.motivoCancelacion = reserva.getMotivoCancelacion();
		entidad.fechaCreacion = reserva.getFechaCreacion();
		entidad.fechaCancelacion = reserva.getFechaCancelacion();
		return entidad;
	}

	Reserva aDominio() {
		return Reserva.reconstruir(id, canchaId, jugadorId, fecha, horaInicio, horaFin, precio, estado,
				confirmacionAsistencia, origenCancelacion, motivoCancelacion, fechaCreacion, fechaCancelacion,
				fechaSolicitudConfirmacion);
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return nuevo;
	}

	@PostPersist
	@PostLoad
	void marcarGuardado() {
		nuevo = false;
	}

}
