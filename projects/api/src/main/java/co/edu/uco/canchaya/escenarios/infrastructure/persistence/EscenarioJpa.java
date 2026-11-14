package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface EscenarioJpa extends JpaRepository<EscenarioEntidad, UUID> {

}
