package co.edu.uco.canchaya.identidad.application.usecase;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import co.edu.uco.canchaya.identidad.application.port.out.RepositorioUsuarios;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;

/** Repositorio de mentiras para las pruebas de HU3. Guarda una copia, como lo haría la base. */
final class RepositorioPerfilesEnMemoria implements RepositorioUsuarios {

	private final Map<UUID, Usuario> usuarios = new LinkedHashMap<>();
	int actualizaciones;

	void agregar(Usuario usuario) {
		usuarios.put(usuario.getId(), copia(usuario));
	}

	/** Lo que quedó «guardado», no el objeto que tiene el caso de uso en memoria. */
	Usuario guardado(UUID id) {
		return usuarios.get(id);
	}

	@Override
	public Optional<Usuario> buscarPorId(UUID id) {
		return Optional.ofNullable(usuarios.get(id)).map(RepositorioPerfilesEnMemoria::copia);
	}

	@Override
	public boolean existeOtroConCorreo(String correoElectronico, UUID idExcluido) {
		return usuarios.values().stream()
				.anyMatch(u -> !u.getId().equals(idExcluido) && u.getCorreoElectronico().equals(correoElectronico));
	}

	@Override
	public void actualizar(Usuario usuario) {
		actualizaciones++;
		usuarios.put(usuario.getId(), copia(usuario));
	}

	@Override
	public boolean existePorCorreo(String correoElectronico) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean existePorDocumento(TipoDocumento tipoDocumento, String numeroDocumento) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean existePorId(UUID id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Usuario> buscarPorCorreo(String correoElectronico) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void guardar(Usuario usuario) {
		throw new UnsupportedOperationException();
	}

	private static Usuario copia(Usuario u) {
		return Usuario.reconstruir(u.getId(), u.getNombre(), u.getCorreoElectronico(), u.getContrasenaHash(),
				u.getTipoDocumento(), u.getNumeroDocumento(), u.getNumeroCelular(), u.getRol(), u.getFechaRegistro());
	}

}
