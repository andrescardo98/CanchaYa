package co.edu.uco.canchaya.identidad.domain;

/**
 * Roles técnicos de un usuario (D07). El encargado de sede usa {@link #ADMINISTRADOR_CANCHA},
 * limitado por EncargadoEscenario (RN-15).
 */
public enum Rol {

	/** Reserva canchas, cancela, confirma asistencia y publica reseñas. */
	JUGADOR,

	/** Propietario de escenarios o encargado de sede (D07). */
	ADMINISTRADOR_CANCHA,

	/** Activa escenarios y administra los catálogos. No se elige al registrarse (RN-16). */
	ADMINISTRADOR_SISTEMA;

	/** Política 3 de Usuario: solo estos dos roles se pueden elegir en el registro. */
	public boolean sePuedeElegirAlRegistrarse() {
		return this == JUGADOR || this == ADMINISTRADOR_CANCHA;
	}

}
