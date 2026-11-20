package co.edu.uco.canchaya.reservas.infrastructure.rest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * HU21. Los cuatro campos son obligatorios y se informan todos a la vez (ERR-GEN-01, AG-03). Un
 * valor que no se puede leer, como "25:00", responde ERR-GEN-07. Una hora que se lee pero no es en
 * punto, como "18:30", llega al dominio y responde ERR-RES-31 (P9).
 */
record SolicitudReserva(

		@NotNull(message = "ERR-GEN-01")
		UUID canchaId,

		@NotNull(message = "ERR-GEN-01")
		LocalDate fecha,

		@NotNull(message = "ERR-GEN-01")
		LocalTime horaInicio,

		@NotNull(message = "ERR-GEN-01")
		LocalTime horaFin) {
}
