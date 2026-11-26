package co.edu.uco.canchaya.escenarios.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
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
	// Los cambia «modificar» (HU11); el id y el escenario no
	private DiaSemana diaSemana;
	private LocalTime horaInicio;
	private LocalTime horaFin;

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
		validarHoras(horaInicio, horaFin);

		HorarioRecurrente nuevo = new HorarioRecurrente(UUID.randomUUID(), escenario.getId(), diaSemana, horaInicio,
				horaFin);
		// Política 4
		if (otrosHorariosDelDia.stream().anyMatch(nuevo::seCruzaCon)) {
			throw new ReglaNegocioException("ERR-HOR-04", new TextoCatalogo("dia." + diaSemana.name()));
		}
		return nuevo;
	}

	/**
	 * Responsabilidad «modificar» (HU11). La política 6 (que exista el horario) la revisa el caso de
	 * uso antes de llamarlo. La política 14 (RN-17, D09) se cumple por diseño: el cambio aplica hacia
	 * adelante y no toca las reservas ya hechas.
	 *
	 * @param escenario           el escenario de este horario, para saber quién es el propietario
	 * @param otrosHorariosDelDia horarios del escenario en el día nuevo; puede incluir a este mismo
	 * @throws ReglaNegocioException ERR-HOR-02, ERR-HOR-13, ERR-HOR-03 o ERR-HOR-07
	 * @throws IllegalArgumentException si el escenario no es el del horario (error de programación)
	 */
	public void modificar(Escenario escenario, DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin,
			List<HorarioRecurrente> otrosHorariosDelDia, Solicitante solicitante) {
		Objects.requireNonNull(escenario, "escenario");
		Objects.requireNonNull(diaSemana, "diaSemana");
		Objects.requireNonNull(horaInicio, "horaInicio");
		Objects.requireNonNull(horaFin, "horaFin");
		if (!escenario.getId().equals(escenarioId)) {
			throw new IllegalArgumentException("El escenario " + escenario.getId() + " no es el del horario " + id);
		}

		validarPropietario(escenario, solicitante);
		validarHoras(horaInicio, horaFin);
		// Política 7: como la 4, pero sin contar el horario que se modifica
		HorarioRecurrente propuesto = new HorarioRecurrente(id, escenarioId, diaSemana, horaInicio, horaFin);
		if (otrosHorariosDelDia.stream().filter(otro -> !otro.id.equals(id)).anyMatch(propuesto::seCruzaCon)) {
			throw new ReglaNegocioException("ERR-HOR-07", new TextoCatalogo("dia." + diaSemana.name()));
		}

		// Se cambia todo después de validar, así un error no deja el horario a medias
		this.diaSemana = diaSemana;
		this.horaInicio = horaInicio;
		this.horaFin = horaFin;
	}

	/**
	 * Responsabilidad «eliminar» (HU12). Solo revisa: el borrado lo hace el caso de uso, que antes
	 * revisa la política 6 (que exista el horario) y pregunta a Reservas si hay reservas. La
	 * política 8 (RN-04) la cumple la búsqueda al no mostrar escenarios sin horarios.
	 *
	 * @param escenario                       el escenario de este horario, para saber quién es el propietario
	 * @param tieneReservasConfirmadasFuturas si hay reservas confirmadas futuras que se cruzan con
	 *                                        este día y rango de horas (D08)
	 * @throws ReglaNegocioException ERR-HOR-02 o ERR-HOR-15
	 * @throws IllegalArgumentException si el escenario no es el del horario (error de programación)
	 */
	public void validarEliminacion(Escenario escenario, Solicitante solicitante,
			boolean tieneReservasConfirmadasFuturas) {
		Objects.requireNonNull(escenario, "escenario");
		if (!escenario.getId().equals(escenarioId)) {
			throw new IllegalArgumentException("El escenario " + escenario.getId() + " no es el del horario " + id);
		}

		// Política 2 primero: quien no es el dueño no se entera de si hay reservas
		validarPropietario(escenario, solicitante);
		// Política 15 (RN-17, D08)
		if (tieneReservasConfirmadasFuturas) {
			throw new ReglaNegocioException("ERR-HOR-15");
		}
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

	/**
	 * Política 10 (RN-11) de «calcularFranjas»: solo se consultan fechas desde hoy hasta hoy +
	 * ventanaReservaDias, los dos incluidos (P6 de HU20). Va aparte porque la búsqueda (HU18, CA6)
	 * debe rechazar la fecha aunque no haya ninguna cancha a la que calcularle franjas.
	 *
	 * @param ahora              fecha y hora actual en America/Bogota (D27); hoy es su fecha
	 * @param ventanaReservaDias PAR-RES-01
	 * @throws ReglaNegocioException ERR-HOR-10 si la fecha es anterior a hoy o posterior a hoy + ventanaReservaDias
	 */
	public static void validarFechaConsultable(LocalDate fecha, LocalDateTime ahora, int ventanaReservaDias) {
		Objects.requireNonNull(fecha, "fecha");
		Objects.requireNonNull(ahora, "ahora");

		LocalDate hoy = ahora.toLocalDate();
		if (fecha.isBefore(hoy) || fecha.isAfter(hoy.plusDays(ventanaReservaDias))) {
			throw new ReglaNegocioException("ERR-HOR-10");
		}
	}

	/**
	 * Responsabilidad «calcularFranjas» (HU20, y HU21 para revalidar). La política 9 (que exista la
	 * cancha) la revisa el caso de uso antes de llamarlo. Las reservas y los bloqueos no se quitan
	 * aquí: eso lo hace Reservas con Reserva.verificarDisponibilidad.
	 *
	 * @param horariosEscenario  horarios del escenario de la cancha; solo cuentan los del día de la fecha
	 * @param ahora              fecha y hora actual en America/Bogota (D27)
	 * @param ventanaReservaDias PAR-RES-01
	 * @return las franjas de una hora de la fecha, por hora de inicio, sin las que ya empezaron
	 * @throws ReglaNegocioException ERR-HOR-10 si la fecha es anterior a hoy o posterior a hoy + ventanaReservaDias
	 */
	public static List<Franja> calcularFranjas(List<HorarioRecurrente> horariosEscenario, LocalDate fecha,
			LocalDateTime ahora, int ventanaReservaDias) {
		Objects.requireNonNull(horariosEscenario, "horariosEscenario");
		validarFechaConsultable(fecha, ahora, ventanaReservaDias);

		// Política 12: la que ya empezó no se ofrece
		return franjasDeLaFecha(horariosEscenario, fecha).stream()
				.filter(franja -> franja.inicio().isAfter(ahora))
				.toList();
	}

	/**
	 * Política 11 de «calcularFranjas» sola, sin la 10 (ventana) ni la 12 (las que ya empezaron):
	 * la agenda del administrador (HU29, CA1 y P6) muestra el día completo y cualquier fecha. Las
	 * franjas salen de los horarios actuales, aunque la fecha sea pasada.
	 *
	 * @param horariosEscenario horarios del escenario; solo cuentan los del día de la semana de la fecha
	 * @return las franjas de una hora en punto de la fecha, por hora de inicio
	 */
	public static List<Franja> franjasDeLaFecha(List<HorarioRecurrente> horariosEscenario, LocalDate fecha) {
		Objects.requireNonNull(horariosEscenario, "horariosEscenario");
		Objects.requireNonNull(fecha, "fecha");

		DiaSemana dia = DiaSemana.de(fecha.getDayOfWeek());
		List<Franja> franjas = new ArrayList<>();
		for (HorarioRecurrente horario : horariosEscenario) {
			if (horario.diaSemana != dia) {
				continue;
			}
			// Política 11: franjas de una hora en punto, desde el inicio hasta el fin del horario
			for (LocalTime inicio = horario.horaInicio; inicio.isBefore(horario.horaFin);
					inicio = inicio.plus(Franja.DURACION)) {
				franjas.add(new Franja(fecha, inicio, inicio.plus(Franja.DURACION)));
			}
		}
		franjas.sort(Comparator.comparing(Franja::horaInicio));
		return List.copyOf(franjas);
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

	// Políticas 13 y 3, iguales al definir y al modificar
	private static void validarHoras(LocalTime horaInicio, LocalTime horaFin) {
		// Política 13 (RN-18, PAR-HOR-02)
		if (!enPunto(horaInicio) || !enPunto(horaFin)) {
			throw new ReglaNegocioException("ERR-HOR-13");
		}
		// Política 3
		if (!horaFin.isAfter(horaInicio)) {
			throw new ReglaNegocioException("ERR-HOR-03");
		}
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
