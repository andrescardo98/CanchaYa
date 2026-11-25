package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioHorarios;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

@Repository
class RepositorioHorariosJpa implements RepositorioHorarios {

	// Código de PostgreSQL para "viola una restricción de exclusión"
	private static final String EXCLUSION = "23P01";

	// En la base el día es texto y ordenado por texto quedaría JUEVES antes que LUNES
	private static final Comparator<HorarioRecurrente> POR_DIA_Y_HORA = Comparator
			.comparing(HorarioRecurrente::getDiaSemana)
			.thenComparing(HorarioRecurrente::getHoraInicio);

	private final HorarioRecurrenteJpa jpa;

	RepositorioHorariosJpa(HorarioRecurrenteJpa jpa) {
		this.jpa = jpa;
	}

	@Override
	public boolean existePorId(UUID id) {
		return jpa.existsById(id);
	}

	@Override
	@Transactional
	public void guardar(HorarioRecurrente horario) {
		try {
			// flush para que la base rechace aquí y no al confirmar, donde ya no se podría traducir
			jpa.saveAndFlush(HorarioRecurrenteEntidad.desde(horario));
		} catch (DataIntegrityViolationException e) {
			// Dos peticiones al mismo tiempo pasaron la política 4 y la base frenó la segunda
			if (esCruce(e)) {
				throw new ReglaNegocioException("ERR-HOR-04", new TextoCatalogo("dia." + horario.getDiaSemana().name()));
			}
			throw e;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<HorarioRecurrente> buscarPorId(UUID id) {
		return jpa.findById(id).map(HorarioRecurrenteEntidad::aDominio);
	}

	@Override
	@Transactional
	public void actualizar(HorarioRecurrente horario) {
		// El caso de uso acaba de leerlo, así que si no está es un error de datos y no del usuario
		HorarioRecurrenteEntidad entidad = jpa.findById(horario.getId())
				.orElseThrow(() -> new IllegalStateException("No existe el horario " + horario.getId()));
		entidad.cambiar(horario);
		try {
			// flush para que la base rechace aquí y no al confirmar, donde ya no se podría traducir
			jpa.saveAndFlush(entidad);
		} catch (DataIntegrityViolationException e) {
			// Dos peticiones al mismo tiempo pasaron la política 7 y la base frenó la segunda
			if (esCruce(e)) {
				throw new ReglaNegocioException("ERR-HOR-07", new TextoCatalogo("dia." + horario.getDiaSemana().name()));
			}
			throw e;
		}
	}

	// HU12. Si no existe no hace nada: dos eliminaciones al mismo tiempo no dejan datos malos (spec, concurrencia)
	@Override
	@Transactional
	public void eliminar(UUID id) {
		jpa.deleteById(id);
		jpa.flush();
	}

	@Override
	@Transactional(readOnly = true)
	public List<HorarioRecurrente> buscarPorEscenarioYDia(UUID escenarioId, DiaSemana diaSemana) {
		return jpa.findByEscenarioIdAndDiaSemana(escenarioId, diaSemana).stream()
				.map(HorarioRecurrenteEntidad::aDominio)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<HorarioRecurrente> buscarPorEscenario(UUID escenarioId) {
		return jpa.findByEscenarioId(escenarioId).stream()
				.map(HorarioRecurrenteEntidad::aDominio)
				.sorted(POR_DIA_Y_HORA)
				.toList();
	}

	// Hibernate no saca el nombre de una restricción de exclusión, así que se mira el estado SQL.
	// La tabla solo tiene una (ex_horario_recurrente_sin_cruce)
	private static boolean esCruce(Throwable error) {
		for (Throwable causa = error; causa != null; causa = causa.getCause()) {
			if (causa instanceof SQLException sql && EXCLUSION.equals(sql.getSQLState())) {
				return true;
			}
		}
		return false;
	}

}
