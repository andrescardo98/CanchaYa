package co.edu.uco.canchaya.identidad.domain;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * Persona registrada en la plataforma: jugador, administrador de cancha (propietario o
 * encargado de sede) o administrador del sistema. Objeto de dominio Usuario del modelo de
 * dominio enriquecido.
 *
 * <p>No se crea con {@code new} desde afuera: el constructor es privado y solo lo usan los
 * métodos de fábrica. Así ningún usuario queda a medio validar.
 */
public final class Usuario {

	/** Política 12 (PAR-USU-02): al menos una letra (con o sin tilde) y al menos un número. */
	private static final Pattern TIENE_LETRA = Pattern.compile(".*\\p{L}.*");
	private static final Pattern TIENE_NUMERO = Pattern.compile(".*[0-9].*");

	private final UUID id;
	private final String nombre;
	private final String correoElectronico;
	private final String contrasenaHash;
	private final TipoDocumento tipoDocumento;
	private final String numeroDocumento;
	private final String numeroCelular;
	private final Rol rol;
	private final OffsetDateTime fechaRegistro;

	private Usuario(UUID id, String nombre, String correoElectronico, String contrasenaHash,
			TipoDocumento tipoDocumento, String numeroDocumento, String numeroCelular, Rol rol,
			OffsetDateTime fechaRegistro) {
		this.id = id;
		this.nombre = nombre;
		this.correoElectronico = correoElectronico;
		this.contrasenaHash = contrasenaHash;
		this.tipoDocumento = tipoDocumento;
		this.numeroDocumento = numeroDocumento;
		this.numeroCelular = numeroCelular;
		this.rol = rol;
		this.fechaRegistro = fechaRegistro;
	}

	/**
	 * Responsabilidad «registrarse»: crea una cuenta nueva con el rol elegido.
	 *
	 * <p>Antes de llamarlo, el caso de uso revisa las políticas que necesitan la base de datos:
	 * correo repetido (1), documento repetido (2) e identificador repetido (4), y la contraseña
	 * con {@link #validarContrasena}. La política 5 se cumple por diseño: este método recibe la
	 * contraseña ya cifrada, nunca el texto plano.
	 *
	 * @param contrasenaHash        contraseña ya cifrada con bcrypt por el puerto CifradorContrasena
	 * @param autorizacionAcudiente declaración de autorización del padre, madre o acudiente (TI, D12)
	 * @param ahora                 instante actual en America/Bogota (D27); queda como fecha de registro
	 * @throws ReglaNegocioException ERR-USU-03, ERR-USU-13, ERR-USU-14 o ERR-USU-15
	 */
	public static Usuario registrar(String nombre, String correoElectronico, String contrasenaHash,
			TipoDocumento tipoDocumento, String numeroDocumento, String numeroCelular, Rol rol,
			boolean autorizacionAcudiente, OffsetDateTime ahora) {

		// Política 3: el administrador del sistema no se registra por la aplicación (RN-16).
		if (rol == null || !rol.sePuedeElegirAlRegistrarse()) {
			throw new ReglaNegocioException("ERR-USU-03");
		}
		// Política 13: tipo admitido y número con el formato de ese tipo.
		if (tipoDocumento == null) {
			throw new ReglaNegocioException("ERR-USU-13", "desconocido", "debe ser CC, CE, TI, PASAPORTE o NIT");
		}
		if (!tipoDocumento.validarNumero(numeroDocumento)) {
			throw new ReglaNegocioException("ERR-USU-13", tipoDocumento.name(), tipoDocumento.getFormatoEsperado());
		}
		if (tipoDocumento == TipoDocumento.TI) {
			// Política 14: la tarjeta de identidad solo se admite con rol JUGADOR.
			if (rol != Rol.JUGADOR) {
				throw new ReglaNegocioException("ERR-USU-14");
			}
			// Política 15: el menor declara la autorización de su acudiente (Ley 1581 de 2012).
			if (!autorizacionAcudiente) {
				throw new ReglaNegocioException("ERR-USU-15");
			}
		}

		return new Usuario(UUID.randomUUID(), nombre == null ? null : nombre.strip(), normalizarCorreo(correoElectronico),
				contrasenaHash, tipoDocumento, numeroDocumento, numeroCelular, rol, ahora);
	}

	/**
	 * Política 12: la contraseña tiene la longitud mínima (PAR-USU-01) y al menos una letra y un
	 * número (PAR-USU-02). El caso de uso la llama antes de cifrarla, en el registro y al
	 * actualizar el perfil.
	 *
	 * @param contrasena     contraseña en texto plano; no se guarda
	 * @param longitudMinima valor de PAR-USU-01, leído de la configuración
	 * @throws ReglaNegocioException ERR-USU-12
	 */
	public static void validarContrasena(String contrasena, int longitudMinima) {
		if (contrasena == null
				|| contrasena.length() < longitudMinima
				|| !TIENE_LETRA.matcher(contrasena).matches()
				|| !TIENE_NUMERO.matcher(contrasena).matches()) {
			throw new ReglaNegocioException("ERR-USU-12");
		}
	}

	/**
	 * Pasa el correo a minúscula y le quita los espacios de los extremos, para que la unicidad
	 * (política 1) no dependa de cómo lo escribió el usuario.
	 */
	public static String normalizarCorreo(String correoElectronico) {
		return correoElectronico == null ? null : correoElectronico.strip().toLowerCase(Locale.ROOT);
	}

	/**
	 * Genera otro identificador para este mismo registro. Lo usa el caso de uso cuando el
	 * identificador ya existe (política 4: reintentar con uno nuevo).
	 */
	public Usuario conNuevoId() {
		return new Usuario(UUID.randomUUID(), nombre, correoElectronico, contrasenaHash, tipoDocumento,
				numeroDocumento, numeroCelular, rol, fechaRegistro);
	}

	public UUID getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getCorreoElectronico() {
		return correoElectronico;
	}

	public String getContrasenaHash() {
		return contrasenaHash;
	}

	public TipoDocumento getTipoDocumento() {
		return tipoDocumento;
	}

	public String getNumeroDocumento() {
		return numeroDocumento;
	}

	public String getNumeroCelular() {
		return numeroCelular;
	}

	public Rol getRol() {
		return rol;
	}

	public OffsetDateTime getFechaRegistro() {
		return fechaRegistro;
	}

}
