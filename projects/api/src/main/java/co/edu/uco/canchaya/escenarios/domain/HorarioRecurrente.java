package co.edu.uco.canchaya.escenarios.domain;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

/**
 * Días y horas en que atiende un escenario. Rige para todas sus canchas. Las franjas de una hora
 * salen de aquí en cada consulta y no se guardan (PAR-HOR-01).
 */
public final class HorarioRecurrente {

	private final UUID id;
	private final UUID escenarioId;
	private final DiaSemana diaSemana;
	private final LocalTime horaInicio;
	private final LocalTime horaFin;

	private HorarioRecurrente(UUID id, UUID escenarioId, DiaSemana diaSemana, LocalTime horaInicio,
			LocalTime horaFin) {
		this.id = id;
		this.escenarioId = escenarioId;
		this.diaSemana = diaSemana;
		this.horaInicio = horaInicio;
		this.horaFin = horaFin;
	}

	/**
	 * Responsabilidad «definir» (HU7). La política 1 (que exista el escenario) y la 5 (que el id
	 * no se repita) necesitan la base de datos, por eso las revisa el caso de uso.
	 *
	 * @param otrosHorariosDelDia horarios del mismo escenario y día, para la política 4
	 * @throws ReglaNegocioException ERR-HOR-02, ERR-HOR-13, ERR-HOR-03 o ERR-HOR-04
	 */
	public static HorarioRecurrente definir(Escenario escenario, DiaSemana diaSemana, LocalTime horaInicio,
			LocalTime horaFin, List<HorarioRecurrente> otrosHorariosDelDia, Solicitante solicitante) {
		Objects.requireNonNull(escenario, "escenario");
		Objects.requireNonNull(diaSemana, "diaSemana");
		Objects.requireNonNull(horaInicio, "horaInicio");
		Objects.requireNonNull(horaFin, "horaFin");

		validarPropietario(escenario, solicitante);
		// Política 13 (RN-18, PAR-HOR-02)
		if (!enPunto(horaInicio) || !enPunto(horaFin)) {
			throw new ReglaNegocioException("ERR-HOR-13");
		}
		// Política 3
		if (!horaFin.isAfter(horaInicio)) {
			throw new ReglaNegocioException("ERR-HOR-03");
		}

		HorarioRecurrente nuevo = new HorarioRecurrente(UUID.randomUUID(), escenario.getId(), diaSemana, horaInicio,
				horaFin);
		// Política 4
		if (otrosHorariosDelDia.stream().anyMatch(nuevo::seCruzaCon)) {
			throw new ReglaNegocioException("ERR-HOR-04", new TextoCatalogo("dia." + diaSemana.name()));
		}
		return nuevo;
	}

	/**
	 * Política 2 (RN-05, RN-15). El encargado de sede tiene el mismo rol, pero no es el dueño. Se
	 * expone porque la lista de horarios del escenario también la necesita.
	 *
	 * @throws ReglaNegocioException ERR-HOR-02
	 */
	public static void validarPropietario(Escenario escenario, Solicitante solicitante) {
		if (solicitante == null || !escenario.esPropietario(solicitante.usuarioId())) {
			throw new ReglaNegocioException("ERR-HOR-02");
		}
	}

	/** Vuelve a armar un horario ya guardado, sin repetir las validaciones. */
	public static HorarioRecurrente reconstruir(UUID id, UUID escenarioId, DiaSemana diaSemana, LocalTime horaInicio,
			LocalTime horaFin) {
		return new HorarioRecurrente(id, escenarioId, diaSemana, horaInicio, horaFin);
	}

	/** Política 5: el mismo horario con otro identificador. */
	public HorarioRecurrente conNuevoId() {
		return new HorarioRecurrente(UUID.randomUUID(), escenarioId, diaSemana, horaInicio, horaFin);
	}

	/**
	 * Se toma la hora de fin como abierta, igual que el int4range '[)' de la base: de 6 a 12 y de
	 * 12 a 14 no se cruzan.
	 */
	public boolean seCruzaCon(HorarioRecurrente otro) {
		return diaSemana == otro.diaSemana
				&& horaInicio.isBefore(otro.horaFin)
				&& otro.horaInicio.isBefore(horaFin);
	}

	private static boolean enPunto(LocalTime hora) {
		return hora.getMinute() == 0 && hora.getSecond() == 0 && hora.getNano() == 0;
	}

	public UUID getId() {
		return id;
	}

	public UUID getEscenarioId() {
		return escenarioId;
	}

	public DiaSemana getDiaSemana() {
		return diaSemana;
	}

	public LocalTime getHoraInicio() {
		return horaInicio;
	}

	public LocalTime getHoraFin() {
		return horaFin;
	}

}
