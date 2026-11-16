package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.time.LocalTime;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
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
 * La columna rango_horas no se mapea: la base la calcula sola y solo la usa la restricción que
 * no deja cruzar dos horarios del mismo día.
 */
@Entity
@Table(name = "horario_recurrente")
class HorarioRecurrenteEntidad implements Persistable<UUID> {

	@Id
	@Column(name = "id")
	private UUID id;

	@Column(name = "escenario_id")
	private UUID escenarioId;

	@Enumerated(EnumType.STRING)
	@Column(name = "dia_semana", length = 9)
	private DiaSemana diaSemana;

	@Column(name = "hora_inicio")
	private LocalTime horaInicio;

	@Column(name = "hora_fin")
	private LocalTime horaFin;

	// El id lo pone el dominio, así que sin esto save() haría un select antes de insertar
	@Transient
	private boolean nuevo = true;

	protected HorarioRecurrenteEntidad() {
		// Lo pide JPA
	}

	static HorarioRecurrenteEntidad desde(HorarioRecurrente horario) {
		HorarioRecurrenteEntidad entidad = new HorarioRecurrenteEntidad();
		entidad.id = horario.getId();
		entidad.escenarioId = horario.getEscenarioId();
		entidad.diaSemana = horario.getDiaSemana();
		entidad.horaInicio = horario.getHoraInicio();
		entidad.horaFin = horario.getHoraFin();
		return entidad;
	}

	HorarioRecurrente aDominio() {
		return HorarioRecurrente.reconstruir(id, escenarioId, diaSemana, horaInicio, horaFin);
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
