package co.edu.uco.canchaya.reservas.application.usecase;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaFranjasOcupadas;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.Reserva;

/**
 * Reservas cumple el contrato que define Escenarios para la búsqueda (P1 de HU18), igual que
 * ConsultaReservasFuturasCasoUso (D08): así Reservas depende de Escenarios y no al revés. Solo
 * entrega las horas: ni el jugador ni nada más de la reserva sale de este módulo.
 */
@Service
public class ConsultaFranjasOcupadasCasoUso implements ConsultaFranjasOcupadas {

	private final RepositorioReservas repositorio;

	public ConsultaFranjasOcupadasCasoUso(RepositorioReservas repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	public Map<UUID, List<RangoOcupado>> confirmadasDelDia(Collection<UUID> canchaIds, LocalDate fecha) {
		// El repositorio ya trae solo las CONFIRMADAS y por hora de inicio; aquí se agrupan por cancha
		return repositorio.confirmadasDeCanchasEnFecha(canchaIds, fecha).stream()
				.collect(Collectors.groupingBy(Reserva::getCanchaId, LinkedHashMap::new,
						Collectors.mapping(r -> new RangoOcupado(r.getHoraInicio(), r.getHoraFin()), Collectors.toList())));
	}

}
