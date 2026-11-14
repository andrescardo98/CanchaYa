package co.edu.uco.canchaya.ubicacion.domain;

import java.util.UUID;

/** Municipio de un departamento. De solo lectura en el Release 1: lo carga la migración V2. */
public final class Ciudad {

	private final UUID id;
	private final String codigo;
	private final UUID departamentoId;
	private final String nombre;

	private Ciudad(UUID id, String codigo, UUID departamentoId, String nombre) {
		this.id = id;
		this.codigo = codigo;
		this.departamentoId = departamentoId;
		this.nombre = nombre;
	}

	public static Ciudad reconstruir(UUID id, String codigo, UUID departamentoId, String nombre) {
		return new Ciudad(id, codigo, departamentoId, nombre);
	}

	public UUID getId() {
		return id;
	}

	public String getCodigo() {
		return codigo;
	}

	public UUID getDepartamentoId() {
		return departamentoId;
	}

	public String getNombre() {
		return nombre;
	}

}
