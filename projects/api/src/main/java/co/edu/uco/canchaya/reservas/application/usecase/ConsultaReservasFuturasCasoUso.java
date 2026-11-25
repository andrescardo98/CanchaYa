package co.edu.uco.canchaya.reservas.application.usecase;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.UUID;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaReservasFuturas;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;

/**
 * Reservas cumple el contrato que define Escenarios (D08): así Reservas depende de Escenarios y
 * no al revés. «Futura» es la que todavía no empieza, con la hora del servidor en Bogotá (D27).
 */
@Service
public class ConsultaReservasFuturasCasoUso implements ConsultaReservasFuturas {

	private final RepositorioReservas repositorio;
	private final Clock reloj;

	public ConsultaReservasFuturasCasoUso(RepositorioReservas repositorio, Clock reloj) {
		this.repositorio = repositorio;
		this.reloj = reloj;
	}

	@Override
	public boolean existenConfirmadasFuturas(Collection<UUID> canchaIds) {
		return !canchaIds.isEmpty()
				&& repositorio.existenConfirmadasQueEmpiezanDespuesDe(canchaIds, LocalDateTime.now(reloj));
	}

	@Override
	public boolean existenConfirmadasFuturasEnHorario(Collection<UUID> canchaIds, DayOfWeek dia,
			LocalTime horaInicio, LocalTime horaFin) {
		return !canchaIds.isEmpty() && repositorio.existenConfirmadasEnHorarioQueEmpiezanDespuesDe(canchaIds, dia,
				horaInicio, horaFin, LocalDateTime.now(reloj));
	}

}
