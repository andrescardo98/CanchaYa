package co.edu.uco.canchaya.escenarios.application.port.in;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/**
 * Lo que Escenarios ofrece a Reservas sobre un escenario (componentes.md). Lo usa la agenda (HU29).
 * Los estados y el tipo de bloqueo van como texto: Reservas no puede usar los enum de Escenarios.
 */
public interface ConsultaEscenarios {

	/**
	 * HU29: el escenario, todas sus canchas por nombre, las franjas de cada fecha de desde a hasta
	 * (las dos incluidas) según los horarios actuales, y los bloqueos de sus canchas en esas fechas.
	 *
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-GEN-06 si el escenario
	 *         no existe o el solicitante no es su propietario ni un encargado asignado (CA6, P7)
	 */
	DatosAgenda datosAgenda(UUID escenarioId, Solicitante solicitante, LocalDate desde, LocalDate hasta);

	/**
	 * @param propietario     falso si quien consulta es encargado asignado (D02)
	 * @param franjasPorFecha una entrada por cada fecha, en orden, aunque no tenga franjas
	 */
	record DatosAgenda(UUID escenarioId, String escenario, boolean propietario, List<CanchaAgenda> canchas,
			Map<LocalDate, List<Franja>> franjasPorFecha, List<BloqueoAgenda> bloqueos) {
	}

	/** estado es el nombre de EstadoCancha. */
	record CanchaAgenda(UUID id, String nombre, String estado) {
	}

	/** tipoBloqueo es MANTENIMIENTO, TORNEO, USO_PROPIO o RESERVA_PRESENCIAL; motivo puede ser vacío (null). */
	record BloqueoAgenda(UUID id, UUID canchaId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
			String tipoBloqueo, String motivo) {
	}

}
