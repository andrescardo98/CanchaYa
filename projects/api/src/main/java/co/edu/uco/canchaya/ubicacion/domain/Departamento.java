package co.edu.uco.canchaya.ubicacion.domain;

import java.util.UUID;

/** Catálogo del DANE. De solo lectura en el Release 1: lo carga la migración V2. */
public final class Departamento {

	private final UUID id;
	private final String codigo;
	private final String nombre;

	private Departamento(UUID id, String codigo, String nombre) {
		this.id = id;
		this.codigo = codigo;
		this.nombre = nombre;
	}

	public static Departamento reconstruir(UUID id, String codigo, String nombre) {
		return new Departamento(id, codigo, nombre);
	}

	public UUID getId() {
		return id;
	}

	public String getCodigo() {
		return codigo;
	}

	public String getNombre() {
		return nombre;
	}

}
