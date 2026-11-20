package co.edu.uco.canchaya.escenarios.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

public final class Cancha {

	private static final int DECIMALES_PRECIO = 2;

	private final UUID id;
	private final UUID escenarioId;
	// No son final: los cambia modificar (HU9)
	private UUID tipoCanchaId;
	private String nombre;
	private BigDecimal precioHora;
	// No es final: lo cambia cambiarEstado (HU10)
	private EstadoCancha estado;

	private Cancha(UUID id, UUID escenarioId, UUID tipoCanchaId, String nombre, BigDecimal precioHora,
			EstadoCancha estado) {
		this.id = id;
		this.escenarioId = escenarioId;
		this.tipoCanchaId = tipoCanchaId;
		this.nombre = nombre;
		this.precioHora = precioHora;
		this.estado = estado;
	}

	/**
	 * Responsabilidad «registrar» (HU6). Las políticas 1, 3, 4 y 6 necesitan la base de datos:
	 * las revisa el caso de uso.
	 *
	 * @throws ReglaNegocioException ERR-CAN-02 o ERR-CAN-05
	 */
	public static Cancha registrar(Escenario escenario, UUID tipoCanchaId, String nombre, BigDecimal precioHora,
			Solicitante solicitante) {
		validarPropietario(escenario, solicitante);
		BigDecimal precio = validarPrecio(precioHora);

		// La cancha nace DISPONIBLE
		return new Cancha(UUID.randomUUID(), escenario.getId(), tipoCanchaId, recortar(nombre), precio,
				EstadoCancha.DISPONIBLE);
	}

	/**
	 * Responsabilidad «modificar» (HU9). Las políticas 7, 4 y 8 necesitan la base de datos: las
	 * revisa el caso de uso. La política 9 se cumple por diseño: cada reserva guarda su propio
	 * precio. El id, el escenario y el estado no cambian.
	 *
	 * @param escenario el escenario de esta cancha, para saber quién es el propietario
	 * @throws ReglaNegocioException ERR-CAN-02 o ERR-CAN-05
	 * @throws IllegalArgumentException si el escenario no es el de la cancha (error de programación)
	 */
	public void modificar(Escenario escenario, UUID tipoCanchaId, String nombre, BigDecimal precioHora,
			Solicitante solicitante) {
		validarEscenario(escenario);
		validarPropietario(escenario, solicitante);
		BigDecimal precio = validarPrecio(precioHora);

		// Se cambia todo después de validar, así un error no deja la cancha a medias
		this.tipoCanchaId = tipoCanchaId;
		this.nombre = recortar(nombre);
		this.precioHora = precio;
	}

	/**
	 * Responsabilidad «cambiarEstado» (HU10). La política 7 (que exista la cancha) la revisa el caso
	 * de uso antes de llamarlo.
	 *
	 * @param escenario el escenario de esta cancha, para saber quién es el propietario
	 * @param tieneReservasConfirmadasFuturas lo responde el módulo Reservas (D08); solo importa al inactivar
	 * @throws ReglaNegocioException ERR-CAN-02, ERR-CAN-10 o ERR-CAN-12
	 * @throws IllegalArgumentException si el escenario no es el de la cancha (error de programación)
	 */
	public void cambiarEstado(Escenario escenario, EstadoCancha nuevoEstado, Solicitante solicitante,
			boolean tieneReservasConfirmadasFuturas) {
		Objects.requireNonNull(nuevoEstado, "nuevoEstado");
		validarEscenario(escenario);
		// Política 2
		validarPropietario(escenario, solicitante);
		// Política 10
		if (estado == nuevoEstado) {
			throw new ReglaNegocioException("ERR-CAN-10", new TextoCatalogo("estado." + nuevoEstado.name()));
		}
		// Política 12 (RN-17, D08)
		if (nuevoEstado == EstadoCancha.INACTIVA && tieneReservasConfirmadasFuturas) {
			throw new ReglaNegocioException("ERR-CAN-12");
		}
		// Política 11 (RN-04): desde aquí estaDisponible() y publicarEnBusqueda() responden con el nuevo estado
		estado = nuevoEstado;
	}

	private void validarEscenario(Escenario escenario) {
		if (!escenario.getId().equals(escenarioId)) {
			throw new IllegalArgumentException("El escenario " + escenario.getId() + " no es el de la cancha " + id);
		}
	}

	/**
	 * Política 2 (RN-05, RN-15): solo el propietario del escenario gestiona sus canchas. Un
	 * encargado de sede también es administrador de cancha, pero no es el propietario.
	 *
	 * @throws ReglaNegocioException ERR-CAN-02
	 */
	public static void validarPropietario(Escenario escenario, Solicitante solicitante) {
		if (solicitante == null || !solicitante.tieneRol(Rol.ADMINISTRADOR_CANCHA)
				|| !escenario.esPropietario(solicitante.usuarioId())) {
			throw new ReglaNegocioException("ERR-CAN-02");
		}
	}

	/**
	 * Política 5. Se redondea a 2 decimales antes de comparar, porque así queda en la base
	 * (0.004 se guardaría como 0.00).
	 *
	 * @throws ReglaNegocioException ERR-CAN-05
	 */
	private static BigDecimal validarPrecio(BigDecimal precioHora) {
		BigDecimal precio = precioHora == null ? null : precioHora.setScale(DECIMALES_PRECIO, RoundingMode.HALF_UP);
		if (precio == null || precio.signum() <= 0) {
			throw new ReglaNegocioException("ERR-CAN-05");
		}
		return precio;
	}

	/** Vuelve a armar una cancha ya guardada, sin repetir las validaciones del registro. */
	public static Cancha reconstruir(UUID id, UUID escenarioId, UUID tipoCanchaId, String nombre,
			BigDecimal precioHora, EstadoCancha estado) {
		return new Cancha(id, escenarioId, tipoCanchaId, nombre, precioHora, estado);
	}

	/** Política 6: la misma cancha con otro identificador. */
	public Cancha conNuevoId() {
		return new Cancha(UUID.randomUUID(), escenarioId, tipoCanchaId, nombre, precioHora, estado);
	}

	public boolean estaDisponible() {
		return estado == EstadoCancha.DISPONIBLE;
	}

	/**
	 * Responsabilidad «publicarEnBusqueda» (HU19 y HU18), política 11 (RN-04): la cancha se
	 * muestra si está DISPONIBLE, su escenario está ACTIVO y el escenario tiene al menos un
	 * horario. El precio por hora siempre existe: {@link #registrar} no deja crear una cancha sin
	 * él. La política 7 (que exista la cancha) la revisa el caso de uso.
	 *
	 * @param escenario el escenario de esta cancha
	 * @param escenarioTieneHorarios si el escenario tiene al menos un horario recurrente; lo
	 *        consulta el caso de uso y llega como un valor simple (nota «Datos de otros módulos»
	 *        de clases.md)
	 */
	public boolean publicarEnBusqueda(Escenario escenario, boolean escenarioTieneHorarios) {
		return estaDisponible() && escenario.estaActivo() && escenarioTieneHorarios;
	}

	private static String recortar(String texto) {
		return texto == null ? null : texto.strip();
	}

	public UUID getId() {
		return id;
	}

	public UUID getEscenarioId() {
		return escenarioId;
	}

	public UUID getTipoCanchaId() {
		return tipoCanchaId;
	}

	public String getNombre() {
		return nombre;
	}

	public BigDecimal getPrecioHora() {
		return precioHora;
	}

	public EstadoCancha getEstado() {
		return estado;
	}

}
