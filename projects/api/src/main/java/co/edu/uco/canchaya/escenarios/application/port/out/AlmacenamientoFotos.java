package co.edu.uco.canchaya.escenarios.application.port.out;

/** Dónde quedan las fotos de los escenarios (D13, D31: Supabase Storage). */
public interface AlmacenamientoFotos {

	/**
	 * @param nombre nombre con el que se guarda, con su extensión
	 * @return dirección (URL) pública de la foto
	 */
	String subir(String nombre, byte[] contenido, String tipoContenido);

	/** Si falla, se registra en el log y no se lanza error: es limpieza. */
	void borrar(String url);

}
