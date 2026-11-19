package co.edu.uco.canchaya.reservas.domain;

/** Quién canceló una reserva. Son los mismos valores que acepta la columna reserva.origen_cancelacion. */
public enum OrigenCancelacion {

	// HU24, con más de 3 horas de anticipación
	JUGADOR,

	// El propietario, por fuerza mayor y con justificación (HU32)
	ADMINISTRADOR,

	// La tarea programada, porque el jugador no confirmó a tiempo (HU28)
	SISTEMA_SIN_CONFIRMACION

}
