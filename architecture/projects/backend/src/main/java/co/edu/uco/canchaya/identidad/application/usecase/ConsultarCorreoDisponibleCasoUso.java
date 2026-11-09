package co.edu.uco.canchaya.identidad.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uco.canchaya.identidad.application.primaryports.ConsultarCorreoDisponible;
import co.edu.uco.canchaya.identidad.application.secondaryports.RepositorioUsuarios;
import co.edu.uco.canchaya.identidad.domain.Usuario;

@Service
public class ConsultarCorreoDisponibleCasoUso implements ConsultarCorreoDisponible {

	private final RepositorioUsuarios repositorio;

	public ConsultarCorreoDisponibleCasoUso(RepositorioUsuarios repositorio) {
		this.repositorio = repositorio;
	}

	// Política 1
	@Override
	@Transactional(readOnly = true)
	public boolean ejecutar(String correoElectronico) {
		return !repositorio.existePorCorreo(Usuario.normalizarCorreo(correoElectronico));
	}

}
