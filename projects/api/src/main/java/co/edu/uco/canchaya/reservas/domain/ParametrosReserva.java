package co.edu.uco.canchaya.reservas.domain;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Valores del catálogo de parámetros que usan las reglas de Reserva. Los llena la configuración al
 * arrancar (RT-10), así un cambio de valor no exige tocar el código.
 *
 * @param ventanaReservaDias            PAR-RES-01: días hacia adelante en que se puede consultar y reservar (30)
 * @param franjasMaximasPorReserva      PAR-RES-02: franjas seguidas por reserva (2)
 * @param horasMaximasPorDia            PAR-RES-03: horas por jugador en un día (2)
 * @param reservasActivasMaximas        PAR-RES-04: reservas activas por jugador (5)
 * @param horasMinimasCancelacion       PAR-RES-05: el jugador cancela si faltan más de estas horas (3)
 * @param horasAnticipacionConfirmacion PAR-RES-06: horas antes del inicio en que se pide confirmar (5)
 * @param horaMinimaConfirmacion        PAR-RES-07: solo exigen confirmación las que empiezan desde esta hora (12:00)
 * @param horasPlazoConfirmacion        PAR-RES-08: el jugador confirma si faltan más de estas horas (3)
 * @param horasPlazoInasistencia        PAR-RES-09: horas después del fin para marcar la inasistencia (24)
 * @param longitudMaximaJustificacion   PAR-RES-10: caracteres de la justificación de cancelación (250)
 * @param confirmacionAsistenciaActiva  PAR-RES-12: si las reservas nuevas exigen confirmación («No», D33).
 *                                      Agregado por P2 de HU21; falta en la hoja ParametrosReserva del Excel de clases
 */
public record ParametrosReserva(int ventanaReservaDias, int franjasMaximasPorReserva, int horasMaximasPorDia,
		int reservasActivasMaximas, int horasMinimasCancelacion, int horasAnticipacionConfirmacion,
		LocalTime horaMinimaConfirmacion, int horasPlazoConfirmacion, int horasPlazoInasistencia,
		int longitudMaximaJustificacion, boolean confirmacionAsistenciaActiva) {

	public ParametrosReserva {
		Objects.requireNonNull(horaMinimaConfirmacion, "horaMinimaConfirmacion");
	}

}
