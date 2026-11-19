package co.edu.uco.canchaya.reservas.application.port.in;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** HU20: las franjas de una cancha en una fecha, libres u ocupadas. Se consulta sin sesión (AG-11). */
public interface ConsultarDisponibilidad {

	/**
	 * @param fecha si es vacía (null), la de hoy en America/Bogota
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-RES-01 si la cancha no
	 *         existe, no está DISPONIBLE o su escenario no está ACTIVO (CA6), o ERR-HOR-10 si la fecha
	 *         está fuera de la ventana (CA5)
	 */
	Disponibilidad ejecutar(UUID canchaId, LocalDate fecha);

	/**
	 * @param fechaMinima hoy, y fechaMaxima hoy + PAR-RES-01: los límites del selector de fecha
	 * @param franjasMaximasPorReserva PAR-RES-02, para que la pantalla deje elegir hasta ese número (CA7)
	 * @param franjas por hora de inicio; vacía si ese día no hay franjas (CA4)
	 */
	record Disponibilidad(UUID canchaId, LocalDate fecha, LocalDate fechaMinima, LocalDate fechaMaxima,
			int franjasMaximasPorReserva, List<FranjaDisponible> franjas) {
	}

	/** Una franja sin quién la ocupa ni por qué (restricción de diseño de HU20). */
	record FranjaDisponible(LocalTime horaInicio, LocalTime horaFin, boolean libre) {
	}

}
