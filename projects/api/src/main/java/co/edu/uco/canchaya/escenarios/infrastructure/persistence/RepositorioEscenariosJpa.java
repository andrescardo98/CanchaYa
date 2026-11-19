package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;

@Repository
class RepositorioEscenariosJpa implements RepositorioEscenarios {

	private final EscenarioJpa jpa;

	RepositorioEscenariosJpa(EscenarioJpa jpa) {
		this.jpa = jpa;
	}

	@Override
	public boolean existePorId(UUID id) {
		return jpa.existsById(id);
	}

	// El escenario y sus fotos entran juntos o no entra nada
	@Override
	@Transactional
	public void guardar(Escenario escenario) {
		jpa.saveAndFlush(EscenarioEntidad.desde(escenario));
	}

	// Transacción para poder leer las fotos, que se cargan aparte
	@Override
	@Transactional(readOnly = true)
	public Optional<Escenario> buscarPorId(UUID id) {
		return jpa.findById(id).map(EscenarioEntidad::aDominio);
	}

	/*
	 * HU8: los datos y la lista de fotos entran juntos o no entra nada. Hibernate inserta las filas
	 * nuevas antes de borrar las viejas, y dos fotos con la misma posición chocarían con
	 * uq_foto_escenario_orden; por eso primero se borran las viejas (flush) y luego se agregan las nuevas.
	 */
	@Override
	@Transactional
	public void actualizar(Escenario escenario) {
		EscenarioEntidad entidad = jpa.findById(escenario.getId()).orElseThrow();
		entidad.copiarDatosModificables(escenario);
		entidad.quitarFotos();
		jpa.flush();
		entidad.agregarFotos(escenario.getFotos());
		jpa.flush();
	}

	@Override
	@Transactional
	public void actualizarEstado(Escenario escenario) {
		jpa.actualizarEstado(escenario.getId(), escenario.getEstado());
	}

	@Override
	@Transactional(readOnly = true)
	public List<ResumenEscenario> listar(EstadoEscenario estado) {
		List<EscenarioEntidad> entidades = estado == null
				? jpa.findAllByOrderByNombreAsc()
				: jpa.findByEstadoOrderByNombreAsc(estado);
		return entidades.stream().map(EscenarioEntidad::aResumen).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<UUID> idsCanchas(UUID escenarioId) {
		return jpa.idsCanchas(escenarioId);
	}

}
