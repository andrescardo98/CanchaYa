package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.canchaya.escenarios.application.port.in.BuscarCanchas;
import co.edu.uco.canchaya.escenarios.application.port.in.BuscarCanchas.CriteriosBusqueda;
import co.edu.uco.canchaya.escenarios.application.port.in.BuscarCanchas.ResultadoBusqueda;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/**
 * HU18: búsqueda pública, sin sesión (AG-11). Va aparte de CanchaController, que es del propietario.
 * Los errores los arma ManejadorErrores: ERR-GEN-01, 02 y 04 como 400 y ERR-HOR-10 como 422.
 */
@RestController
class BusquedaCanchasController {

	private final BuscarCanchas buscarCanchas;
	private final CatalogoMensajes catalogo;

	BusquedaCanchasController(BuscarCanchas buscarCanchas, CatalogoMensajes catalogo) {
		this.buscarCanchas = buscarCanchas;
		this.catalogo = catalogo;
	}

	@GetMapping("/api/canchas/busqueda")
	RespuestaBusquedaCanchas buscar(@RequestParam(required = false) BigDecimal latitud,
			@RequestParam(required = false) BigDecimal longitud,
			@RequestParam(required = false) Integer radioKm,
			@RequestParam(required = false) UUID departamentoId,
			@RequestParam(required = false) UUID ciudadId,
			@RequestParam(required = false) UUID tipoCanchaId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora) {
		ResultadoBusqueda resultado = buscarCanchas.ejecutar(new CriteriosBusqueda(latitud, longitud, radioKm,
				departamentoId, ciudadId, tipoCanchaId, fecha, hora));

		// CA7 y AG-09: sin resultados gana INF-ESC-01, porque explica por qué la lista está vacía (P6)
		if (resultado.canchas().isEmpty()) {
			return RespuestaBusquedaCanchas.de(resultado, "INF-ESC-01", catalogo.texto("INF-ESC-01"));
		}
		// CA2: sin la ubicación del jugador se busca por departamento y ciudad, sin ordenar por distancia
		if (!resultado.conUbicacion()) {
			return RespuestaBusquedaCanchas.de(resultado, "INF-ESC-03", catalogo.texto("INF-ESC-03"));
		}
		return RespuestaBusquedaCanchas.de(resultado, null, null);
	}

}
