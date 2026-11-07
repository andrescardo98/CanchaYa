package co.edu.uco.canchaya.identidad.application.usecase;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uco.canchaya.identidad.application.primaryports.RegistrarUsuario;
import co.edu.uco.canchaya.identidad.application.secondaryports.CifradorContrasena;
import co.edu.uco.canchaya.identidad.application.secondaryports.RepositorioUsuarios;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

@Service
public class RegistrarUsuarioCasoUso implements RegistrarUsuario {

	private static final Logger LOG = LoggerFactory.getLogger(RegistrarUsuarioCasoUso.class);

	private final RepositorioUsuarios repositorio;
	private final CifradorContrasena cifrador;
	private final Clock reloj;
	private final int longitudMinimaContrasena;

	public RegistrarUsuarioCasoUso(RepositorioUsuarios repositorio, CifradorContrasena cifrador, Clock reloj,
			@Value("${canchaya.usuario.longitud-minima-contrasena}") int longitudMinimaContrasena) {
		this.repositorio = repositorio;
		this.cifrador = cifrador;
		this.reloj = reloj;
		this.longitudMinimaContrasena = longitudMinimaContrasena;
	}

	@Override
	@Transactional
	public Usuario ejecutar(DatosRegistro datos) {
		Usuario.validarContrasena(datos.contrasena(), longitudMinimaContrasena);

		// Política 1
		if (repositorio.existePorCorreo(Usuario.normalizarCorreo(datos.correoElectronico()))) {
			throw new ReglaNegocioException("ERR-USU-01");
		}
		// Política 2
		if (repositorio.existePorDocumento(datos.tipoDocumento(), datos.numeroDocumento())) {
			throw new ReglaNegocioException("ERR-USU-02");
		}

		Usuario usuario = Usuario.registrar(datos.nombre(), datos.correoElectronico(),
				cifrador.cifrar(datos.contrasena()), datos.tipoDocumento(), datos.numeroDocumento(),
				datos.numeroCelular(), datos.rol(), datos.autorizacionAcudiente(), OffsetDateTime.now(reloj));

		// Política 4. ERR-USU-04 solo va al log; el usuario no lo ve.
		while (repositorio.existePorId(usuario.getId())) {
			LOG.warn("ERR-USU-04 objeto=Usuario id={}", usuario.getId());
			usuario = usuario.conNuevoId();
		}

		repositorio.guardar(usuario);
		return usuario;
	}

}
