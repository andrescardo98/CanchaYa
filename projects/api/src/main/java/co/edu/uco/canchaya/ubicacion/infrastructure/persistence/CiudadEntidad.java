package co.edu.uco.canchaya.ubicacion.infrastructure.persistence;

import java.util.UUID;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import co.edu.uco.canchaya.ubicacion.domain.Ciudad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Solo lectura en el Release 1: los datos los carga la V2
@Entity
@Immutable
@Table(name = "ciudad")
class CiudadEntidad {

	@Id
	@Column(name = "id")
	private UUID id;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "codigo", length = 5)
	private String codigo;

	@Column(name = "departamento_id")
	private UUID departamentoId;

	@Column(name = "nombre", length = 100)
	private String nombre;

	protected CiudadEntidad() {
	}

	Ciudad aDominio() {
		return Ciudad.reconstruir(id, codigo, departamentoId, nombre);
	}

}
