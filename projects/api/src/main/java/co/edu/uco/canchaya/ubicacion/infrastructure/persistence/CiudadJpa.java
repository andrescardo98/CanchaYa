package co.edu.uco.canchaya.ubicacion.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface CiudadJpa extends JpaRepository<CiudadEntidad, UUID> {

	List<CiudadEntidad> findByDepartamentoIdOrderByNombreAsc(UUID departamentoId);

}
