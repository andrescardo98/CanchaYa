package co.edu.uco.canchaya.ubicacion.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface DepartamentoJpa extends JpaRepository<DepartamentoEntidad, UUID> {

	List<DepartamentoEntidad> findAllByOrderByNombreAsc();

}
