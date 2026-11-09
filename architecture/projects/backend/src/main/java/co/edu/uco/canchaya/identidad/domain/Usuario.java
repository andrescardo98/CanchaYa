package co.edu.uco.canchaya.identidad.domain;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

public final class Usuario {

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
