package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.NombresCancha;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

@Repository
class RepositorioCanchasJpa implements RepositorioCanchas {

	private static final String NOMBRE_UNICO = "uq_cancha_escenario_nombre";

	private final CanchaJpa jpa;

	RepositorioCanchasJpa(CanchaJpa jpa) {
		this.jpa = jpa;
	}

	@Override
	public boolean existePorId(UUID id) {
		return jpa.existsById(id);
	}

	@Override
	public boolean existeNombreEnEscenario(UUID escenarioId, String nombre) {
		return jpa.existeNombreEnEscenario(escenarioId, nombre);
	}

	@Override
	@Transactional
	public void guardar(Cancha cancha) {
		try {
			// flush para que la base rechace aquí y no al confirmar, donde ya no se podría traducir
			jpa.saveAndFlush(CanchaEntidad.desde(cancha));
		} catch (DataIntegrityViolationException e) {
			// Otra solicitud guardó el mismo nombre entre la revisión del caso de uso y este insert
			if (NOMBRE_UNICO.equals(nombreRestriccion(e))) {
				throw new ReglaNegocioException("ERR-CAN-03", cancha.getNombre());
			}
			throw e;
		}
	}

	@Override
	public boolean existeNombreEnOtraCancha(UUID escenarioId, String nombre, UUID canchaId) {
		return jpa.existeNombreEnOtraCancha(escenarioId, nombre, canchaId);
	}

	@Override
	@Transactional
	public void actualizar(Cancha cancha) {
		// guardar no sirve: haría INSERT (isNew). Se carga la fila y se cambian solo los datos modificables
		CanchaEntidad entidad = jpa.findById(cancha.getId()).orElseThrow();
		entidad.copiarDatosModificables(cancha);
		try {
			// flush por la misma razón que en guardar: traducir aquí el rechazo de la base
			jpa.flush();
		} catch (DataIntegrityViolationException e) {
			// Otra solicitud dejó el mismo nombre entre la revisión del caso de uso y este update
			if (NOMBRE_UNICO.equals(nombreRestriccion(e))) {
				throw new ReglaNegocioException("ERR-CAN-08", cancha.getNombre());
			}
			throw e;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<Cancha> listarPorEscenario(UUID escenarioId) {
		return jpa.findByEscenarioIdOrderByNombreAsc(escenarioId).stream().map(CanchaEntidad::aDominio).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Cancha> buscarPorId(UUID id) {
		return jpa.findById(id).map(CanchaEntidad::aDominio);
	}

	@Override
	// MANDATORY: sin la transacción de quien llama, el bloqueo se soltaría en seguida y no serviría
	@Transactional(propagation = Propagation.MANDATORY)
	public void tomarTurno(UUID id) {
		jpa.bloquearFila(id);
	}

	@Override
	@Transactional
	public void actualizarEstado(Cancha cancha) {
		jpa.actualizarEstado(cancha.getId(), cancha.getEstado());
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, NombresCancha> nombresConEscenario(Collection<UUID> canchaIds) {
		if (canchaIds.isEmpty()) {
			return Map.of();
		}
		Map<UUID, NombresCancha> nombres = new HashMap<>();
		for (Object[] fila : jpa.nombresConEscenario(canchaIds)) {
			nombres.put((UUID) fila[0], new NombresCancha((String) fila[1], (String) fila[2]));
		}
		return nombres;
	}

	private static String nombreRestriccion(Throwable error) {
		for (Throwable causa = error; causa != null; causa = causa.getCause()) {
			if (causa instanceof ConstraintViolationException violacion) {
				return violacion.getConstraintName();
			}
		}
		return null;
	}

}
