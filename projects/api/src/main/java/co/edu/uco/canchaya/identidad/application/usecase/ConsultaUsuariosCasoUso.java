package co.edu.uco.canchaya.identidad.application.usecase;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios;
import co.edu.uco.canchaya.identidad.application.port.out.RepositorioUsuarios;

// Hexagonal simplificada: solo lectura, sin reglas propias
@Service
public class ConsultaUsuariosCasoUso implements ConsultaUsuarios {

	private final RepositorioUsuarios repositorio;

	public ConsultaUsuariosCasoUso(RepositorioUsuarios repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ContactoUsuario> buscarContactos(Collection<UUID> usuarioIds) {
		if (usuarioIds.isEmpty()) {
			return List.of();
		}
		return repositorio.buscarPorIds(usuarioIds).stream()
				.map(usuario -> new ContactoUsuario(usuario.getId(), usuario.getNombre(),
						usuario.getCorreoElectronico()))
				.toList();
	}

}
