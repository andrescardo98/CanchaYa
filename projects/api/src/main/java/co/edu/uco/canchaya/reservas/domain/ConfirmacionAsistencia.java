package co.edu.uco.canchaya.reservas.domain;

/**
 * Confirmación de asistencia de una reserva (RN-13). Es independiente del estado. Son los mismos
 * valores que acepta la columna reserva.confirmacion_asistencia.
 */
public enum ConfirmacionAsistencia {

	// Hay que pedirle al jugador que confirme (HU26)
	PENDIENTE,

	// El jugador confirmó desde el enlace del correo (HU27)
	CONFIRMADA,

	// No se pide: poca anticipación, antes de las 12:00 m. o confirmación inactiva (PAR-RES-12, D33)
	NO_REQUERIDA

}
