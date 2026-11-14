package co.edu.uco.canchaya.identidad.infrastructure.persistence;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import co.edu.uco.canchaya.identidad.application.port.out.RepositorioUsuarios;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * Si dos registros con el mismo correo o documento llegan a la vez, ambos pasan las
 * validaciones previas; la restricción única de la base rechaza el segundo y aquí se
 * traduce a ERR-USU-01 / ERR-USU-02.
 */
@Repository
class RepositorioUsuariosJpa implements RepositorioUsuarios {

	private static final Map<String, String> ERROR_POR_RESTRICCION = Map.of(
			"uq_usuario_correo", "ERR-USU-01",
			"uq_usuario_documento", "ERR-USU-02");

	private final UsuarioJpa jpa;

	RepositorioUsuariosJpa(UsuarioJpa jpa) {
		this.jpa = jpa;
	}

	@Override
	public boolean existePorCorreo(String correoElectronico) {
		return jpa.existsByCorreoElectronico(correoElectronico);
	}

	@Override
	public boolean existePorDocumento(TipoDocumento tipoDocumento, String numeroDocumento) {
		return jpa.existsByTipoDocumentoAndNumeroDocumento(tipoDocumento, numeroDocumento);
	}

	@Override
	public boolean existePorId(UUID id) {
		return jpa.existsById(id);
	}

	@Override
	public Optional<Usuario> buscarPorCorreo(String correoElectronico) {
		return jpa.findByCorreoElectronico(correoElectronico).map(UsuarioEntidad::aDominio);
	}

	@Override
	public void guardar(Usuario usuario) {
		try {
			// flush para que la base rechace aquí y no al confirmar la transacción, donde ya no se podría traducir
			jpa.saveAndFlush(UsuarioEntidad.desde(usuario));
		} catch (DataIntegrityViolationException e) {
			throw traducir(e, ERROR_POR_RESTRICCION);
		}
	}

	@Override
	public Optional<Usuario> buscarPorId(UUID id) {
		return jpa.findById(id).map(UsuarioEntidad::aDominio);
	}

	@Override
	public boolean existeOtroConCorreo(String correoElectronico, UUID idExcluido) {
		return jpa.existsByCorreoElectronicoAndIdNot(correoElectronico, idExcluido);
	}

	/** Si otro usuario guardó el mismo correo al mismo tiempo, la restricción única lo rechaza: ERR-USU-10. */
	@Override
	public void actualizar(Usuario usuario) {
		UsuarioEntidad entidad = jpa.findById(usuario.getId()).orElseThrow(() -> new ReglaNegocioException("ERR-USU-08"));
		entidad.actualizarPerfilDesde(usuario);
		try {
			jpa.saveAndFlush(entidad);
		} catch (DataIntegrityViolationException e) {
			throw traducir(e, Map.of("uq_usuario_correo", "ERR-USU-10"));
		}
	}

	private static RuntimeException traducir(DataIntegrityViolationException error, Map<String, String> codigos) {
		String codigo = codigos.get(nombreRestriccion(error));
		return codigo == null ? error : new ReglaNegocioException(codigo);
	}

	private static String nombreRestriccion(Throwable error) {
		for (Throwable causa = error; causa != null; causa = causa.getCause()) {
			if (causa instanceof ConstraintViolationException violacion) {
				return violacion.getConstraintName();
			}
		}
		return null;
	}

}
