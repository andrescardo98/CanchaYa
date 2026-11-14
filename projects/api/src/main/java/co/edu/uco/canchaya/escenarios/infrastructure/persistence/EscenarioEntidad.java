package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * La columna ubicacion no se mapea: la base la calcula sola a partir de latitud y longitud
 * (columna generada) y solo la usa la búsqueda por radio (HU18).
 */
@Entity
@Table(name = "escenario")
class EscenarioEntidad implements Persistable<UUID> {

	@Id
	@Column(name = "id")
	private UUID id;

	@Column(name = "nombre", length = 100)
	private String nombre;

	@Column(name = "descripcion", length = 500)
	private String descripcion;

	@Column(name = "direccion", length = 200)
	private String direccion;

	@Column(name = "ciudad_id")
	private UUID ciudadId;

	@Column(name = "latitud", precision = 8, scale = 6)
	private BigDecimal latitud;

	@Column(name = "longitud", precision = 9, scale = 6)
	private BigDecimal longitud;

	@Column(name = "telefono_contacto", length = 15)
	private String telefonoContacto;

	@Column(name = "correo_contacto", length = 150)
	private String correoContacto;

	@Column(name = "administrador_id")
	private UUID administradorId;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", length = 10)
	private EstadoEscenario estado;

	// Tabla foto_escenario: la lista de fotos se guarda aparte, con su posición
	@OneToMany(mappedBy = "escenario", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<FotoEscenarioEntidad> fotos = new ArrayList<>();

	@Transient
	private boolean nuevo = true;

	protected EscenarioEntidad() {
	}

	static EscenarioEntidad desde(Escenario escenario) {
		EscenarioEntidad entidad = new EscenarioEntidad();
		entidad.id = escenario.getId();
		entidad.nombre = escenario.getNombre();
		entidad.descripcion = escenario.getDescripcion();
		entidad.direccion = escenario.getDireccion();
		entidad.ciudadId = escenario.getCiudadId();
		entidad.latitud = escenario.getLatitud();
		entidad.longitud = escenario.getLongitud();
		entidad.telefonoContacto = escenario.getTelefonoContacto();
		entidad.correoContacto = escenario.getCorreoContacto();
		entidad.administradorId = escenario.getAdministradorId();
		entidad.estado = escenario.getEstado();
		List<String> urls = escenario.getFotos();
		for (int i = 0; i < urls.size(); i++) {
			// orden va de 1 a 5 (ck_foto_escenario_orden)
			entidad.fotos.add(new FotoEscenarioEntidad(entidad, urls.get(i), (short) (i + 1)));
		}
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
