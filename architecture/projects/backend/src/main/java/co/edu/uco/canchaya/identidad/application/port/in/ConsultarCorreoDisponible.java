package co.edu.uco.canchaya.identidad.application.port.in;

// HU1, CA8: el formulario pregunta si el correo está libre al salir del campo
public interface ConsultarCorreoDisponible {

	boolean ejecutar(String correoElectronico);

}
