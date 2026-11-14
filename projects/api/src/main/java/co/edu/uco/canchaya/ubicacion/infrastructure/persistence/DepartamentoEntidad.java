package co.edu.uco.canchaya.ubicacion.infrastructure.persistence;

import java.util.UUID;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import co.edu.uco.canchaya.ubicacion.domain.Departamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Solo lectura en el Release 1: los datos los carga la V2
@Entity
@Immutable
@Table(name = "departamento")
class DepartamentoEntidad {

	@Id
	@Column(name = "id")
	private UUID id;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "codigo", length = 2)
	private String codigo;

	@Column(name = "nombre", length = 50)
	private String nombre;

	protected DepartamentoEntidad() {
	}

	Departamento aDominio() {
		return Departamento.reconstruir(id, codigo, nombre);
	}

}
