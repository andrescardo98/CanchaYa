package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.TipoCancha;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Solo lectura en el Release 1: los datos los carga la V2
@Entity
@Table(name = "tipo_cancha")
class TipoCanchaEntidad {

	@Id
	@Column(name = "id")
	private UUID id;

	@Column(name = "codigo", length = 5)
	private String codigo;

	@Column(name = "nombre", length = 50)
	private String nombre;

	@Column(name = "capacidad_jugadores")
	private short capacidadJugadores;

	protected TipoCanchaEntidad() {
	}

	TipoCancha aDominio() {
		return TipoCancha.reconstruir(id, codigo, nombre, capacidadJugadores);
	}

}
