package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;

// Las horas van como texto hh:mm; si se deja a Jackson saldrían con segundos (06:00:00)
record RespuestaHorario(UUID id, DiaSemana diaSemana, String horaInicio, String horaFin) {

	private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

	static RespuestaHorario de(HorarioRecurrente horario) {
		return new RespuestaHorario(horario.getId(), horario.getDiaSemana(), horario.getHoraInicio().format(HORA),
				horario.getHoraFin().format(HORA));
	}

}
