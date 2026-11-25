package co.edu.uco.canchaya.escenarios.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** HU18: búsqueda pública de canchas, sin sesión (AG-11). */
public interface BuscarCanchas {

	/**
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-GEN-01, ERR-GEN-02,
	 *         ERR-GEN-04 o ERR-HOR-10, siempre antes de consultar las canchas
	 */
	ResultadoBusqueda ejecutar(CriteriosBusqueda criterios);

	/** Todos opcionales. latitud y longitud son la ubicación del jugador, que entrega el navegador. */
	record CriteriosBusqueda(BigDecimal latitud, BigDecimal longitud, Integer radioKm, UUID departamentoId,
			UUID ciudadId, UUID tipoCanchaId, LocalDate fecha, LocalTime hora) {
	}

	/**
	 * @param radioKm      el radio aplicado; vacío (null) si la búsqueda fue sin ubicación (P3)
	 * @param conUbicacion si se buscó con la ubicación del jugador (CA1) o sin ella (CA2)
	 */
	record ResultadoBusqueda(List<CanchaEncontrada> canchas, boolean conUbicacion, Integer radioKm,
			int radioMinimoKm, int radioMaximoKm, LocalDate fechaMinima, LocalDate fechaMaxima) {
	}

	/**
	 * @param distanciaKm          con un decimal; vacía (null) sin ubicación (CA2)
	 * @param calificacionPromedio vacía (null) si el escenario no tiene reseñas, nunca cero (D05, AG-20)
	 */
	record CanchaEncontrada(UUID escenarioId, String nombreEscenario, BigDecimal latitud, BigDecimal longitud,
			UUID canchaId, String nombreCancha, String tipoCancha, BigDecimal precioHora, BigDecimal distanciaKm,
			BigDecimal calificacionPromedio) {
	}

}
