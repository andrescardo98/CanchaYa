package co.edu.uco.canchaya.identidad.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.uco.canchaya.identidad.domain.TipoDocumento;

interface UsuarioJpa extends JpaRepository<UsuarioEntidad, UUID> {

	boolean existsByCorreoElectronico(String correoElectronico);

	boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

}
