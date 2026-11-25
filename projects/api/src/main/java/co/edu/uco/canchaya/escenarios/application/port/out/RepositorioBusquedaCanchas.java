package co.edu.uco.canchaya.escenarios.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;

/**
 * HU18: candidatas de la búsqueda. Filtra solo por ubicación, radio, departamento, ciudad y tipo;
 * RN-04 la decide el caso de uso con Cancha.publicarEnBusqueda (P7).
 */
public interface RepositorioBusquedaCanchas {

	/**
	 * Con punto: solo las de escenarios a menos de radioMetros, de la más cercana a la más lejana y,
	 * a igual distancia, por nombre del escenario y de la cancha (CA1). Sin punto: por nombre del
	 * escenario y de la cancha (CA2).
	 */
	List<Candidata> buscar(FiltroBusqueda filtro);

	/**
	 * Todos opcionales. latitud, longitud y radioMetros van juntos: o los tres o ninguno.
	 */
	record FiltroBusqueda(BigDecimal latitud, BigDecimal longitud, Integer radioMetros, UUID departamentoId,
			UUID ciudadId, UUID tipoCanchaId) {

		public boolean conUbicacion() {
			return latitud != null;
		}
	}

	/**
	 * @param escenario         sin fotos: la búsqueda no las muestra
	 * @param distanciaMetros   vacía (null) si el filtro no trae punto
	 */
	record Candidata(Cancha cancha, Escenario escenario, String nombreTipoCancha, Double distanciaMetros) {
	}

}
