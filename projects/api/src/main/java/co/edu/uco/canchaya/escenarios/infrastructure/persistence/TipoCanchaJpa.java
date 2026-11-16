package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TipoCanchaJpa extends JpaRepository<TipoCanchaEntidad, UUID> {

	List<TipoCanchaEntidad> findAllByOrderByCodigoAsc();

}
