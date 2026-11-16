package co.edu.uco.canchaya.escenarios.domain;

import java.util.UUID;

/**
 * En el Release 1 el catálogo es de solo lectura: lo carga la migración V2 (F5, F7 y F11).
 * Registrar, modificar y eliminar tipos son del Release 2 (HU15 a HU17, D04).
 */
public final class TipoCancha {

	private final UUID id;
	private final String codigo;
	private final String nombre;
	private final int capacidadJugadores;

	private TipoCancha(UUID id, String codigo, String nombre, int capacidadJugadores) {
		this.id = id;
		this.codigo = codigo;
		this.nombre = nombre;
		this.capacidadJugadores = capacidadJugadores;
	}

	public static TipoCancha reconstruir(UUID id, String codigo, String nombre, int capacidadJugadores) {
		return new TipoCancha(id, codigo, nombre, capacidadJugadores);
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

	public int getCapacidadJugadores() {
		return capacidadJugadores;
	}

}
