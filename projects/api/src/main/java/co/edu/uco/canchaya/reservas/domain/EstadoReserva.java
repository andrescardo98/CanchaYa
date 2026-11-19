package co.edu.uco.canchaya.reservas.domain;

/** Estado de una reserva. Son los mismos valores que acepta la columna reserva.estado. */
public enum EstadoReserva {

	// Estado inicial: la franja queda ocupada (HU21)
	CONFIRMADA,

	// La canceló el jugador, el propietario o la tarea programada; la franja vuelve a estar libre
	CANCELADA,

	// El jugador no se presentó (HU33); no se revierte
	NO_ASISTIO

}
