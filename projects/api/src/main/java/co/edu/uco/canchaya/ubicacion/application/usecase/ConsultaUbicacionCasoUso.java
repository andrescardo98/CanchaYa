package co.edu.uco.canchaya.ubicacion.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.ubicacion.application.port.in.ConsultaUbicacion;
import co.edu.uco.canchaya.ubicacion.application.port.out.RepositorioUbicacion;
import co.edu.uco.canchaya.ubicacion.domain.Ciudad;
import co.edu.uco.canchaya.ubicacion.domain.Departamento;

// Hexagonal simplificada: solo lectura, sin reglas propias
@Service
public class ConsultaUbicacionCasoUso implements ConsultaUbicacion {

	private final RepositorioUbicacion repositorio;

	public ConsultaUbicacionCasoUso(RepositorioUbicacion repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	public boolean existeCiudad(UUID ciudadId) {
		return ciudadId != null && repositorio.existeCiudad(ciudadId);
	}

	@Override
	public List<Departamento> listarDepartamentos() {
		return repositorio.listarDepartamentos();
	}

	@Override
	public List<Ciudad> listarCiudades(UUID departamentoId) {
		return repositorio.listarCiudades(departamentoId);
	}

}
