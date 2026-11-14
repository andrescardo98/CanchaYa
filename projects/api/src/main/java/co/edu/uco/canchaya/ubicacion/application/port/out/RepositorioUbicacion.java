package co.edu.uco.canchaya.ubicacion.application.port.out;

import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.ubicacion.domain.Ciudad;
import co.edu.uco.canchaya.ubicacion.domain.Departamento;

public interface RepositorioUbicacion {

	boolean existeCiudad(UUID ciudadId);

	List<Departamento> listarDepartamentos();

	List<Ciudad> listarCiudades(UUID departamentoId);

}
