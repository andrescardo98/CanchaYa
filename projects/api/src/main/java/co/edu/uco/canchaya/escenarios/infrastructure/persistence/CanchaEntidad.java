package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

// El escenario y el tipo se guardan por identificador, como en el dominio
@Entity
@Table(name = "cancha")
class CanchaEntidad implements Persistable<UUID> {

	@Id
	@Column(name = "id")
	private UUID id;

	@Column(name = "escenario_id")
	private UUID escenarioId;

	@Column(name = "tipo_cancha_id")
	private UUID tipoCanchaId;

	@Column(name = "nombre", length = 50)
	private String nombre;

	@Column(name = "precio_hora", precision = 10, scale = 2)
	private BigDecimal precioHora;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", length = 10)
	private EstadoCancha estado;

	@Transient
	private boolean nuevo = true;

	protected CanchaEntidad() {
	}

	static CanchaEntidad desde(Cancha cancha) {
		CanchaEntidad entidad = new CanchaEntidad();
		entidad.id = cancha.getId();
		entidad.escenarioId = cancha.getEscenarioId();
		entidad.tipoCanchaId = cancha.getTipoCanchaId();
		entidad.nombre = cancha.getNombre();
		entidad.precioHora = cancha.getPrecioHora();
		entidad.estado = cancha.getEstado();
		return entidad;
	}

	/** HU9: lo que cambia Cancha.modificar. El escenario y el estado se quedan como estaban. */
	void copiarDatosModificables(Cancha cancha) {
		this.tipoCanchaId = cancha.getTipoCanchaId();
		this.nombre = cancha.getNombre();
		this.precioHora = cancha.getPrecioHora();
	}

	Cancha aDominio() {
		return Cancha.reconstruir(id, escenarioId, tipoCanchaId, nombre, precioHora, estado);
	}

	@Override
	public UUID getId() {
		return id;
	}

	// Igual que en EscenarioEntidad: el id ya viene asignado y sin esto Spring Data haría UPDATE
	@Override
	public boolean isNew() {
		return nuevo;
	}

	@PostPersist
	@PostLoad
	void marcarComoGuardado() {
		nuevo = false;
	}

}
