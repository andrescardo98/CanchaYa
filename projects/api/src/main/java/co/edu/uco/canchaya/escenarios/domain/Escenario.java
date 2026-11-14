package co.edu.uco.canchaya.escenarios.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

public final class Escenario {

	private static final int DECIMALES_COORDENADA = 6;
	private static final BigDecimal LATITUD_MAXIMA = BigDecimal.valueOf(90);
	private static final BigDecimal LONGITUD_MAXIMA = BigDecimal.valueOf(180);

	private final UUID id;
	private final String nombre;
	private final String descripcion;
	private final String direccion;
	private final UUID ciudadId;
	private final BigDecimal latitud;
	private final BigDecimal longitud;
	private final String telefonoContacto;
	private final String correoContacto;
	private final List<String> fotos;
	private final UUID administradorId;
	private final EstadoEscenario estado;

	private Escenario(UUID id, String nombre, String descripcion, String direccion, UUID ciudadId,
			BigDecimal latitud, BigDecimal longitud, String telefonoContacto, String correoContacto,
			List<String> fotos, UUID administradorId, EstadoEscenario estado) {
		this.id = id;
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.direccion = direccion;
		this.ciudadId = ciudadId;
		this.latitud = latitud;
		this.longitud = longitud;
		this.telefonoContacto = telefonoContacto;
		this.correoContacto = correoContacto;
		this.fotos = List.copyOf(fotos);
		this.administradorId = administradorId;
		this.estado = estado;
	}

	/**
	 * Responsabilidad «registrar». Las políticas 2 y 4 necesitan la base de datos, y el formato y
	 * el peso de cada foto se revisan antes de subirla: las tres las revisa el caso de uso.
	 *
	 * @throws ReglaNegocioException ERR-ESC-01, ERR-ESC-03 o ERR-ESC-13
	 */
	public static Escenario registrar(String nombre, String descripcion, String direccion, UUID ciudadId,
			BigDecimal latitud, BigDecimal longitud, String telefonoContacto, String correoContacto,
			List<String> fotos, Solicitante solicitante, int maximoFotos) {

		validarSolicitante(solicitante);
		validarUbicacion(latitud, longitud);
		validarCantidadFotos(fotos.size(), maximoFotos);

		// Política 1: queda asociado al solicitante como propietario. Política 5: nace INACTIVO.
		return new Escenario(UUID.randomUUID(), recortar(nombre), opcional(descripcion), recortar(direccion),
				ciudadId, redondear(latitud), redondear(longitud), recortar(telefonoContacto),
				opcional(correoContacto), fotos, solicitante.usuarioId(), EstadoEscenario.INACTIVO);
	}

	/**
	 * Política 1. Se expone para que el caso de uso la revise antes de subir las fotos.
	 *
	 * @throws ReglaNegocioException ERR-ESC-01
	 */
	public static void validarSolicitante(Solicitante solicitante) {
		if (solicitante == null || !solicitante.tieneRol(Rol.ADMINISTRADOR_CANCHA)) {
			throw new ReglaNegocioException("ERR-ESC-01");
		}
	}

	/**
	 * Política 3 (D14).
	 *
	 * @throws ReglaNegocioException ERR-ESC-03
	 */
	public static void validarUbicacion(BigDecimal latitud, BigDecimal longitud) {
		if (fueraDeRango(latitud, LATITUD_MAXIMA) || fueraDeRango(longitud, LONGITUD_MAXIMA)) {
			throw new ReglaNegocioException("ERR-ESC-03");
		}
	}

	/**
	 * Política 13, la parte de la cantidad (PAR-ESC-01).
	 *
	 * @throws ReglaNegocioException ERR-ESC-13
	 */
	public static void validarCantidadFotos(int cantidad, int maximoFotos) {
		if (cantidad > maximoFotos) {
			throw new ReglaNegocioException("ERR-ESC-13", new TextoCatalogo("ERR-ESC-13.cantidad", maximoFotos));
		}
	}

	/** Vuelve a armar un escenario ya guardado, sin repetir las validaciones del registro. */
	public static Escenario reconstruir(UUID id, String nombre, String descripcion, String direccion, UUID ciudadId,
			BigDecimal latitud, BigDecimal longitud, String telefonoContacto, String correoContacto,
			List<String> fotos, UUID administradorId, EstadoEscenario estado) {
		return new Escenario(id, nombre, descripcion, direccion, ciudadId, latitud, longitud, telefonoContacto,
				correoContacto, fotos, administradorId, estado);
	}

	/** Política 4: el mismo registro con otro identificador. */
	public Escenario conNuevoId() {
		return new Escenario(UUID.randomUUID(), nombre, descripcion, direccion, ciudadId, latitud, longitud,
				telefonoContacto, correoContacto, fotos, administradorId, estado);
	}

	/** RN-05. */
	public boolean esPropietario(UUID usuarioId) {
		return administradorId.equals(usuarioId);
	}

	/** RN-04. */
	public boolean estaActivo() {
		return estado == EstadoEscenario.ACTIVO;
	}

	private static boolean fueraDeRango(BigDecimal valor, BigDecimal maximo) {
		return valor == null || valor.abs().compareTo(maximo) > 0;
	}

	// Precisión de 6 decimales del enriquecido, la misma de la columna NUMERIC
	private static BigDecimal redondear(BigDecimal coordenada) {
		return coordenada.setScale(DECIMALES_COORDENADA, RoundingMode.HALF_UP);
	}

	private static String recortar(String texto) {
		return texto == null ? null : texto.strip();
	}

	// Un opcional en blanco se guarda vacío (null)
	private static String opcional(String texto) {
		String recortado = recortar(texto);
		return recortado == null || recortado.isEmpty() ? null : recortado;
	}

	public UUID getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public String getDireccion() {
		return direccion;
	}

	public UUID getCiudadId() {
		return ciudadId;
	}

	public BigDecimal getLatitud() {
		return latitud;
	}

	public BigDecimal getLongitud() {
		return longitud;
	}

	public String getTelefonoContacto() {
		return telefonoContacto;
	}

	public String getCorreoContacto() {
		return correoContacto;
	}

	public List<String> getFotos() {
		return fotos;
	}

	public UUID getAdministradorId() {
		return administradorId;
	}

	public EstadoEscenario getEstado() {
		return estado;
	}

}
