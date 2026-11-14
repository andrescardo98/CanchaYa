package co.edu.uco.canchaya.identidad.application.usecase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uco.canchaya.identidad.application.port.in.ActualizarPerfil;
import co.edu.uco.canchaya.identidad.application.port.out.CifradorContrasena;
import co.edu.uco.canchaya.identidad.application.port.out.RepositorioUsuarios;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

@Service
public class ActualizarPerfilCasoUso implements ActualizarPerfil {

	private final RepositorioUsuarios repositorio;
	private final CifradorContrasena cifrador;
	private final int longitudMinimaContrasena;

	public ActualizarPerfilCasoUso(RepositorioUsuarios repositorio, CifradorContrasena cifrador,
			@Value("${canchaya.usuario.longitud-minima-contrasena}") int longitudMinimaContrasena) {
		this.repositorio = repositorio;
		this.cifrador = cifrador;
		this.longitudMinimaContrasena = longitudMinimaContrasena;
	}

	@Override
	@Transactional
	public Usuario ejecutar(Solicitante solicitante, DatosPerfil datos) {
		// Política 8
		Usuario usuario = repositorio.buscarPorId(datos.id())
				.orElseThrow(() -> new ReglaNegocioException("ERR-USU-08"));

		// Política 9, antes que la 11: si no, comparando documentos se podría adivinar el de otro usuario
		usuario.validarQueEsElMismoUsuario(solicitante);

		// Política 11
		if (esDistinto(datos.tipoDocumento(), usuario.getTipoDocumento().name())
				|| esDistinto(datos.numeroDocumento(), usuario.getNumeroDocumento())
				|| esDistinto(datos.rol(), usuario.getRol().name())) {
			throw new ReglaNegocioException("ERR-USU-11");
		}

		// CA4: vacía significa que no la cambia. Una con solo espacios sí se valida y falla
		boolean cambiaContrasena = datos.nuevaContrasena() != null && !datos.nuevaContrasena().isEmpty();
		// Política 12, antes de cifrar porque después ya no se puede revisar
		if (cambiaContrasena) {
			Usuario.validarContrasena(datos.nuevaContrasena(), longitudMinimaContrasena);
		}

		// Política 10: AG-05, sin distinguir mayúsculas y sin contar al propio usuario
		if (repositorio.existeOtroConCorreo(Usuario.normalizarCorreo(datos.correoElectronico()), usuario.getId())) {
			throw new ReglaNegocioException("ERR-USU-10");
		}

		// Política 5
		String nuevoHash = cambiaContrasena ? cifrador.cifrar(datos.nuevaContrasena()) : null;
		usuario.actualizarPerfil(solicitante, datos.nombre(), datos.correoElectronico(), datos.numeroCelular(),
				nuevoHash);
		repositorio.actualizar(usuario);
		return usuario;
	}

	/** Null significa que no se envió; cualquier otro valor tiene que ser igual al guardado. */
	private static boolean esDistinto(String enviado, String guardado) {
		return enviado != null && !enviado.strip().equals(guardado);
	}

}
