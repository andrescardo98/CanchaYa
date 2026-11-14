package co.edu.uco.canchaya.ubicacion.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import co.edu.uco.canchaya.ubicacion.application.port.out.RepositorioUbicacion;
import co.edu.uco.canchaya.ubicacion.domain.Ciudad;
import co.edu.uco.canchaya.ubicacion.domain.Departamento;

@Repository
class RepositorioUbicacionJpa implements RepositorioUbicacion {

	private final DepartamentoJpa departamentos;
	private final CiudadJpa ciudades;

	RepositorioUbicacionJpa(DepartamentoJpa departamentos, CiudadJpa ciudades) {
		this.departamentos = departamentos;
		this.ciudades = ciudades;
	}

	@Override
	public boolean existeCiudad(UUID ciudadId) {
		return ciudades.existsById(ciudadId);
	}

	@Override
	public List<Departamento> listarDepartamentos() {
		return departamentos.findAllByOrderByNombreAsc().stream().map(DepartamentoEntidad::aDominio).toList();
	}

	@Override
	public List<Ciudad> listarCiudades(UUID departamentoId) {
		return ciudades.findByDepartamentoIdOrderByNombreAsc(departamentoId).stream().map(CiudadEntidad::aDominio)
				.toList();
	}

}
