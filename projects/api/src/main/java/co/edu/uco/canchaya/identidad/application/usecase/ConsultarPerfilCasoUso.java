package co.edu.uco.canchaya.identidad.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uco.canchaya.identidad.application.port.in.ConsultarPerfil;
import co.edu.uco.canchaya.identidad.application.port.out.RepositorioUsuarios;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

@Service
public class ConsultarPerfilCasoUso implements ConsultarPerfil {

	private final RepositorioUsuarios repositorio;

	public ConsultarPerfilCasoUso(RepositorioUsuarios repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional(readOnly = true)
	public Usuario ejecutar(Solicitante solicitante, UUID id) {
		// Política 8
		Usuario usuario = repositorio.buscarPorId(id).orElseThrow(() -> new ReglaNegocioException("ERR-USU-08"));
		// Política 9: el documento y el celular son datos sensibles (AG-15); solo los ve su dueño
		usuario.validarQueEsElMismoUsuario(solicitante);
		return usuario;
	}

}
