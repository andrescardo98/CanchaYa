package co.edu.uco.canchaya.escenarios.application.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.RangoBloqueado;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios.BloqueoAgenda;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBloqueos;

/** Repositorio falso de bloqueos (HU20 y HU18): la prueba dice qué bloqueos tiene cada cancha cada día. */
final class BloqueosEnMemoria implements RepositorioBloqueos {

	private final BiFunction<UUID, LocalDate, List<RangoBloqueado>> rangos;
	int consultasPorVarias;
	// HU29: los bloqueos con tipo y motivo que devuelve deCanchasEntre, y cuántas veces se consultó
	final List<BloqueoAgenda> deAgenda = new ArrayList<>();
	int consultasDeAgenda;

	BloqueosEnMemoria(BiFunction<UUID, LocalDate, List<RangoBloqueado>> rangos) {
		this.rangos = rangos;
	}

	static BloqueosEnMemoria sinBloqueos() {
		return new BloqueosEnMemoria((canchaId, fecha) -> List.of());
	}

	@Override
	public List<RangoBloqueado> rangosDelDia(UUID canchaId, LocalDate fecha) {
		return rangos.apply(canchaId, fecha);
	}

	@Override
	public Map<UUID, List<RangoBloqueado>> rangosDelDia(Collection<UUID> canchaIds, LocalDate fecha) {
		consultasPorVarias++;
		Map<UUID, List<RangoBloqueado>> porCancha = new HashMap<>();
		for (UUID canchaId : canchaIds) {
			List<RangoBloqueado> deLaCancha = rangos.apply(canchaId, fecha);
			if (!deLaCancha.isEmpty()) {
				porCancha.put(canchaId, deLaCancha);
			}
		}
		return porCancha;
	}

	@Override
	public List<BloqueoAgenda> deCanchasEntre(Collection<UUID> canchaIds, LocalDate desde, LocalDate hasta) {
		consultasDeAgenda++;
		return deAgenda.stream().filter(b -> canchaIds.contains(b.canchaId()) && !b.fecha().isBefore(desde)
				&& !b.fecha().isAfter(hasta)).toList();
	}

}
