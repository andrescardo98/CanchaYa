package co.edu.uco.canchaya.identidad.domain;

public enum Rol {

	JUGADOR,

	// También lo usa el encargado de sede (D07)
	ADMINISTRADOR_CANCHA,

	ADMINISTRADOR_SISTEMA;

	/** Política 3: el administrador del sistema no se registra por la aplicación (RN-16). */
	public boolean sePuedeElegirAlRegistrarse() {
		return this == JUGADOR || this == ADMINISTRADOR_CANCHA;
	}

}
