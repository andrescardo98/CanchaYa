package co.edu.uco.canchaya.identidad.domain;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

public final class Usuario {

	private static final Pattern TIENE_LETRA = Pattern.compile(".*\\p{L}.*");
	private static final Pattern TIENE_NUMERO = Pattern.compile(".*[0-9].*");

	// nombre, correo, contraseña y celular cambian con actualizarPerfil (HU3); el resto no
	private final UUID id;
	private String nombre;
	private String correoElectronico;
	private String contrasenaHash;
	private final TipoDocumento tipoDocumento;
	private final String numeroDocumento;
	private String numeroCelular;
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
	 * Responsabilidad «registrarse». Las políticas 1, 2 y 4 necesitan la base de datos y las
	 * revisa el caso de uso. Recibe la contraseña ya cifrada (política 5).
	 *
	 * @throws ReglaNegocioException ERR-USU-03, ERR-USU-13, ERR-USU-14 o ERR-USU-15
	 */
	public static Usuario registrar(String nombre, String correoElectronico, String contrasenaHash,
			TipoDocumento tipoDocumento, String numeroDocumento, String numeroCelular, Rol rol,
			boolean autorizacionAcudiente, OffsetDateTime ahora) {

		// Política 3
		if (rol == null || !rol.sePuedeElegirAlRegistrarse()) {
			throw new ReglaNegocioException("ERR-USU-03");
		}
		// Política 13
		if (tipoDocumento == null) {
			throw new ReglaNegocioException("ERR-USU-13", "desconocido", "debe ser CC, CE, TI, PASAPORTE o NIT");
		}
		if (!tipoDocumento.validarNumero(numeroDocumento)) {
			throw new ReglaNegocioException("ERR-USU-13", tipoDocumento.name(), tipoDocumento.getFormatoEsperado());
		}
		if (tipoDocumento == TipoDocumento.TI) {
			// Política 14
			if (rol != Rol.JUGADOR) {
				throw new ReglaNegocioException("ERR-USU-14");
			}
			// Política 15
			if (!autorizacionAcudiente) {
				throw new ReglaNegocioException("ERR-USU-15");
			}
		}

		return new Usuario(UUID.randomUUID(), nombre == null ? null : nombre.strip(), normalizarCorreo(correoElectronico),
				contrasenaHash, tipoDocumento, numeroDocumento, numeroCelular, rol, ahora);
	}

	/** Vuelve a armar un usuario ya guardado, sin repetir las validaciones del registro. */
	public static Usuario reconstruir(UUID id, String nombre, String correoElectronico, String contrasenaHash,
			TipoDocumento tipoDocumento, String numeroDocumento, String numeroCelular, Rol rol,
			OffsetDateTime fechaRegistro) {
		return new Usuario(id, nombre, correoElectronico, contrasenaHash, tipoDocumento, numeroDocumento,
				numeroCelular, rol, fechaRegistro);
	}

	/**
	 * Responsabilidad «actualizarPerfil». Las políticas 8, 10 y 12 necesitan la base de datos o la
	 * contraseña sin cifrar y las revisa el caso de uso. La política 11 se cumple por diseño: el
	 * método no recibe tipo ni número de documento ni rol. Recibe la contraseña ya cifrada (política 5).
	 *
	 * @param nuevaContrasenaHash null si el usuario no escribió una nueva contraseña (CA4)
	 * @throws ReglaNegocioException ERR-USU-09
	 */
	public void actualizarPerfil(Solicitante solicitante, String nombre, String correoElectronico,
			String numeroCelular, String nuevaContrasenaHash) {
		validarQueEsElMismoUsuario(solicitante);
		this.nombre = nombre == null ? null : nombre.strip();
		this.correoElectronico = normalizarCorreo(correoElectronico);
		this.numeroCelular = numeroCelular;
		if (nuevaContrasenaHash != null) {
			this.contrasenaHash = nuevaContrasenaHash;
		}
	}

	/**
	 * Política 9: un usuario solo puede ver y modificar su propio perfil. La usan actualizarPerfil
	 * y la consulta del perfil que llena el formulario.
	 *
	 * @throws ReglaNegocioException ERR-USU-09
	 */
	public void validarQueEsElMismoUsuario(Solicitante solicitante) {
		if (solicitante == null || !id.equals(solicitante.usuarioId())) {
			throw new ReglaNegocioException("ERR-USU-09");
		}
	}

	/**
	 * Política 12. Se llama antes de cifrar, porque después ya no se puede revisar.
	 *
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

	/** El correo se guarda y se compara en minúscula, para que la política 1 no dependa de cómo se escribió. */
	public static String normalizarCorreo(String correoElectronico) {
		return correoElectronico == null ? null : correoElectronico.strip().toLowerCase(Locale.ROOT);
	}

	/** Política 4: el mismo registro con otro identificador. */
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
