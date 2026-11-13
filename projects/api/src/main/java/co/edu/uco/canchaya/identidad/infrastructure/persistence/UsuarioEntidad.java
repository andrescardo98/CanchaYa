package co.edu.uco.canchaya.identidad.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "usuario")
class UsuarioEntidad implements Persistable<UUID> {

	@Id
	@Column(name = "id")
	private UUID id;

	@Column(name = "nombre", length = 100)
	private String nombre;

	@Column(name = "correo_electronico", length = 150)
	private String correoElectronico;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "contrasena_hash", length = 60)
	private String contrasenaHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_documento", length = 10)
	private TipoDocumento tipoDocumento;

	@Column(name = "numero_documento", length = 20)
	private String numeroDocumento;

	@Column(name = "numero_celular", length = 15)
	private String numeroCelular;

	@Enumerated(EnumType.STRING)
	@Column(name = "rol", length = 25)
	private Rol rol;

	@Column(name = "fecha_registro")
	private OffsetDateTime fechaRegistro;

	@Transient
	private boolean nuevo = true;

	protected UsuarioEntidad() {
	}

	static UsuarioEntidad desde(Usuario usuario) {
		UsuarioEntidad entidad = new UsuarioEntidad();
		entidad.id = usuario.getId();
		entidad.nombre = usuario.getNombre();
		entidad.correoElectronico = usuario.getCorreoElectronico();
		entidad.contrasenaHash = usuario.getContrasenaHash();
		entidad.tipoDocumento = usuario.getTipoDocumento();
		entidad.numeroDocumento = usuario.getNumeroDocumento();
		entidad.numeroCelular = usuario.getNumeroCelular();
		entidad.rol = usuario.getRol();
		entidad.fechaRegistro = usuario.getFechaRegistro();
		return entidad;
	}

	@Override
	public UUID getId() {
		return id;
	}

	// El id ya viene asignado, así que Spring Data creería que el registro existe y haría UPDATE en vez de INSERT
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
