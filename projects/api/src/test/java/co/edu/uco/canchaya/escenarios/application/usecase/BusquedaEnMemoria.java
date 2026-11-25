package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBusquedaCanchas;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;

/**
 * Repositorio falso de la búsqueda (HU18). La distancia de cada escenario la fija la prueba: la
 * medición real con PostGIS se prueba en la persistencia (T1 y T5). Filtra y ordena como lo pide
 * el contrato de RepositorioBusquedaCanchas.
 */
final class BusquedaEnMemoria implements RepositorioBusquedaCanchas {

	private record Registro(Cancha cancha, Escenario escenario, String nombreTipo, UUID departamentoId,
			double distanciaMetros) {
	}

	private final List<Registro> registros = new ArrayList<>();
	private final Map<UUID, Double> distancias = new HashMap<>();
	final List<FiltroBusqueda> filtrosRecibidos = new ArrayList<>();

	/** El escenario queda a esa distancia del punto del jugador, sea cual sea el punto. */
	void ubicar(Escenario escenario, double distanciaMetros) {
		distancias.put(escenario.getId(), distanciaMetros);
	}

	void agregar(Cancha cancha, Escenario escenario, String nombreTipo, UUID departamentoId) {
		registros.add(new Registro(cancha, escenario, nombreTipo, departamentoId,
				distancias.getOrDefault(escenario.getId(), 0.0)));
	}

	@Override
	public List<Candidata> buscar(FiltroBusqueda filtro) {
		filtrosRecibidos.add(filtro);
		Comparator<Registro> porNombre = Comparator.comparing((Registro r) -> r.escenario().getNombre())
				.thenComparing(r -> r.cancha().getNombre());
		Comparator<Registro> orden = filtro.conUbicacion()
				? Comparator.comparingDouble(Registro::distanciaMetros).thenComparing(porNombre)
				: porNombre;
		return registros.stream()
				.filter(r -> !filtro.conUbicacion() || r.distanciaMetros() <= filtro.radioMetros())
				.filter(r -> filtro.departamentoId() == null || filtro.departamentoId().equals(r.departamentoId()))
				.filter(r -> filtro.ciudadId() == null || filtro.ciudadId().equals(r.escenario().getCiudadId()))
				.filter(r -> filtro.tipoCanchaId() == null || filtro.tipoCanchaId().equals(r.cancha().getTipoCanchaId()))
				.sorted(orden)
				.map(r -> new Candidata(r.cancha(), r.escenario(), r.nombreTipo(),
						filtro.conUbicacion() ? r.distanciaMetros() : null))
				.toList();
	}

}
