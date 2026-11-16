package co.edu.uco.canchaya.escenarios.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

public final class Cancha {

	private static final int DECIMALES_PRECIO = 2;

	private final UUID id;
	private final UUID escenarioId;
	private final UUID tipoCanchaId;
	private final String nombre;
	private final BigDecimal precioHora;
	private final EstadoCancha estado;

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
