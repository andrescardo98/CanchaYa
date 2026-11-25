package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.in.BuscarCanchas.CanchaEncontrada;
import co.edu.uco.canchaya.escenarios.application.port.in.BuscarCanchas.ResultadoBusqueda;

/**
 * HU18. Sin estados ni datos del administrador (AG-14). radioKm y distanciaKm vienen vacíos sin
 * ubicación (CA2); los límites del radio y de la fecha son para que la pantalla no los escriba fijos.
 * codigo y mensaje solo vienen con INF-ESC-01 (CA7) o INF-ESC-03 (CA2).
 */
record RespuestaBusquedaCanchas(List<Resultado> resultados, Integer radioKm, int radioMinimoKm, int radioMaximoKm,
		LocalDate fechaMinima, LocalDate fechaMaxima, String codigo, String mensaje) {

	static RespuestaBusquedaCanchas de(ResultadoBusqueda resultado, String codigo, String mensaje) {
		return new RespuestaBusquedaCanchas(resultado.canchas().stream().map(Resultado::de).toList(),
				resultado.radioKm(), resultado.radioMinimoKm(), resultado.radioMaximoKm(), resultado.fechaMinima(),
				resultado.fechaMaxima(), codigo, mensaje);
	}

	/** latitud y longitud son las del escenario, para su marcador en el mapa (P9). */
	record Resultado(UUID escenarioId, String nombreEscenario, BigDecimal latitud, BigDecimal longitud, UUID canchaId,
			String nombreCancha, String tipoCancha, BigDecimal precioHora, BigDecimal distanciaKm,
			BigDecimal calificacionPromedio) {

		static Resultado de(CanchaEncontrada cancha) {
			return new Resultado(cancha.escenarioId(), cancha.nombreEscenario(), cancha.latitud(), cancha.longitud(),
					cancha.canchaId(), cancha.nombreCancha(), cancha.tipoCancha(), cancha.precioHora(),
					cancha.distanciaKm(), cancha.calificacionPromedio());
		}

	}

}
