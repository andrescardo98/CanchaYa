package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Una fila por cada URL del atributo fotos de Escenario. No es objeto de dominio. */
@Entity
@Table(name = "foto_escenario")
class FotoEscenarioEntidad {

	@Id
	@Column(name = "id")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "escenario_id")
	private EscenarioEntidad escenario;

	@Column(name = "url", length = 500)
	private String url;

	@Column(name = "orden")
	private short orden;

	protected FotoEscenarioEntidad() {
	}

	FotoEscenarioEntidad(EscenarioEntidad escenario, String url, short orden) {
		this.id = UUID.randomUUID();
		this.escenario = escenario;
		this.url = url;
		this.orden = orden;
	}

}
