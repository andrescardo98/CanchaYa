package co.edu.uco.canchaya.escenarios.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

public final class Escenario {

	private static final int DECIMALES_COORDENADA = 6;
	private static final BigDecimal LATITUD_MAXIMA = BigDecimal.valueOf(90);
	private static final BigDecimal LONGITUD_MAXIMA = BigDecimal.valueOf(180);
	// Precisión y rango del atributo calificacionPromedio del enriquecido
	private static final int DECIMALES_CALIFICACION = 1;
	private static final int CALIFICACION_MINIMA = 1;
	private static final int CALIFICACION_MAXIMA = 5;

	private final UUID id;
	// No son final: los cambia modificar (HU8)
	private String nombre;
	private String descripcion;
	private String direccion;
	private UUID ciudadId;
	private BigDecimal latitud;
	private BigDecimal longitud;
	private String telefonoContacto;
	private String correoContacto;
	private List<String> fotos;
	private final UUID administradorId;
	// No es final: lo cambia cambiarEstado (HU13)
	private EstadoEscenario estado;

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

	/**
	 * Responsabilidad «modificar» (HU8). La política 6 (que exista el escenario), la 2 (que exista
	 * la ciudad) y el formato y el peso de cada foto nueva los revisa el caso de uso antes de
	 * llamarlo. El id, el propietario y el estado no cambian.
	 *
	 * @param fotos direcciones de las fotos que quedan: las conservadas y las nuevas ya subidas
	 * @throws ReglaNegocioException ERR-ESC-07, ERR-ESC-03 o ERR-ESC-13
	 */
	public void modificar(String nombre, String descripcion, String direccion, UUID ciudadId, BigDecimal latitud,
			BigDecimal longitud, String telefonoContacto, String correoContacto, List<String> fotos,
			Solicitante solicitante, int maximoFotos) {
		validarPropietario(solicitante);
		validarUbicacion(latitud, longitud);
		validarCantidadFotos(fotos.size(), maximoFotos);

		// Se cambia todo después de validar, así un error no deja el escenario a medias
		this.nombre = recortar(nombre);
		this.descripcion = opcional(descripcion);
		this.direccion = recortar(direccion);
		this.ciudadId = ciudadId;
		this.latitud = redondear(latitud);
		this.longitud = redondear(longitud);
		this.telefonoContacto = recortar(telefonoContacto);
		this.correoContacto = opcional(correoContacto);
		this.fotos = List.copyOf(fotos);
	}

	/**
	 * Política 7 (RN-05, RN-15): solo el propietario modifica el escenario. El encargado de sede
	 * también es administrador de cancha, pero no es el propietario, y el administrador del
	 * sistema tampoco puede (P3 de la especificación de HU8). Se expone para que el caso de uso la
	 * revise antes de subir las fotos y para la consulta que llena el formulario.
	 *
	 * @throws ReglaNegocioException ERR-ESC-07
	 */
	public void validarPropietario(Solicitante solicitante) {
		if (solicitante == null || !solicitante.tieneRol(Rol.ADMINISTRADOR_CANCHA)
				|| !esPropietario(solicitante.usuarioId())) {
			throw new ReglaNegocioException("ERR-ESC-07");
		}
	}

	/**
	 * Responsabilidad «cambiarEstado» (HU13). La política 6 (que exista) la revisa el caso de uso
	 * antes de llamarlo, y la 15 (el correo al propietario) también, después de guardar (D15).
	 *
	 * @param tieneReservasConfirmadasFuturas lo responde el módulo Reservas (D08); solo importa al inactivar
	 * @throws ReglaNegocioException ERR-ESC-08, ERR-ESC-09 o ERR-ESC-14
	 */
	public void cambiarEstado(EstadoEscenario nuevoEstado, Solicitante solicitante,
			boolean tieneReservasConfirmadasFuturas) {
		Objects.requireNonNull(nuevoEstado, "nuevoEstado");
		validarAdministradorSistema(solicitante);
		// Política 9
		if (estado == nuevoEstado) {
			throw new ReglaNegocioException("ERR-ESC-09", new TextoCatalogo("estado." + nuevoEstado.name()));
		}
		// Política 14 (RN-17, D08)
		if (nuevoEstado == EstadoEscenario.INACTIVO && tieneReservasConfirmadasFuturas) {
			throw new ReglaNegocioException("ERR-ESC-14");
		}
		// Política 10 (RN-04): desde aquí estaActivo() decide si el escenario sale en la búsqueda
		estado = nuevoEstado;
	}

	/**
	 * Política 8 (RN-16). Se expone para que el caso de uso la revise antes de buscar el escenario:
	 * así quien no tiene permiso no puede averiguar qué identificadores existen.
	 *
	 * @throws ReglaNegocioException ERR-ESC-08
	 */
	public static void validarAdministradorSistema(Solicitante solicitante) {
		if (solicitante == null || !solicitante.tieneRol(Rol.ADMINISTRADOR_SISTEMA)) {
			throw new ReglaNegocioException("ERR-ESC-08");
		}
	}

	/**
	 * HU29 (RN-15, AG-12): la agenda la ven el propietario y los encargados asignados, los dos con
	 * rol ADMINISTRADOR_CANCHA (D07). Que el encargado esté asignado lo consulta el caso de uso y
	 * llega como valor lógico, como en Bloqueo (clases.md).
	 *
	 * @return verdadero si es el propietario y falso si es un encargado asignado; la agenda lo usa
	 *         para las opciones que solo tiene el propietario (D02)
	 * @throws ReglaNegocioException ERR-GEN-06 si no es ninguno de los dos (CA6)
	 */
	public boolean validarAccesoAgenda(Solicitante solicitante, boolean esEncargadoAsignado) {
		if (solicitante == null || !solicitante.tieneRol(Rol.ADMINISTRADOR_CANCHA)) {
			throw new ReglaNegocioException("ERR-GEN-06");
		}
		if (esPropietario(solicitante.usuarioId())) {
			return true;
		}
		if (esEncargadoAsignado) {
			return false;
		}
		throw new ReglaNegocioException("ERR-GEN-06");
	}

	/** RN-05. */
	public boolean esPropietario(UUID usuarioId) {
		return administradorId.equals(usuarioId);
	}

	/** RN-04. */
	public boolean estaActivo() {
		return estado == EstadoEscenario.ACTIVO;
	}

	/**
	 * Responsabilidad «calcularCalificacionPromedio» (HU19), D05: se calcula al consultar y no se
	 * guarda. La política 6 (que exista el escenario) la revisa el caso de uso.
	 *
	 * @param calificaciones las de las reseñas del escenario, que llegan por el puerto
	 *        ConsultaCalificaciones
	 * @return el promedio con un decimal, o vacío si no hay calificaciones (nunca cero)
	 */
	public static Optional<BigDecimal> calcularCalificacionPromedio(List<Short> calificaciones) {
		if (calificaciones == null) {
			return Optional.empty();
		}
		// Política 11: solo cuentan las calificaciones de 1 a 5
		List<Short> validas = calificaciones.stream()
				.filter(c -> c != null && c >= CALIFICACION_MINIMA && c <= CALIFICACION_MAXIMA)
				.toList();
		// Política 12: sin reseñas no se presenta calificación
		if (validas.isEmpty()) {
			return Optional.empty();
		}
		int suma = validas.stream().mapToInt(Short::intValue).sum();
		return Optional.of(BigDecimal.valueOf(suma)
				.divide(BigDecimal.valueOf(validas.size()), DECIMALES_CALIFICACION, RoundingMode.HALF_UP));
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
