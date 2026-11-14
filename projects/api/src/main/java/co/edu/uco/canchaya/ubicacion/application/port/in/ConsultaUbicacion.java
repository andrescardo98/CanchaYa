package co.edu.uco.canchaya.ubicacion.application.port.in;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.ubicacion.domain.Ciudad;
import co.edu.uco.canchaya.ubicacion.domain.Departamento;

/** Lo que Ubicación ofrece: el catálogo de departamentos y ciudades (HU5, HU18). */
public interface ConsultaUbicacion {

	boolean existeCiudad(UUID ciudadId);

	/** Ordenados por nombre. */
	List<Departamento> listarDepartamentos();

	/** Solo las del departamento, ordenadas por nombre (HU5, CA6). */
	List<Ciudad> listarCiudades(UUID departamentoId);

	/** Las que no existen no vienen en la respuesta (HU13: la ciudad de cada escenario en la lista). */
	List<Ciudad> buscarCiudades(Collection<UUID> ciudadIds);

}
