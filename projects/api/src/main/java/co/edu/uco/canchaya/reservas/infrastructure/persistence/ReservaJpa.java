package co.edu.uco.canchaya.reservas.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.uco.canchaya.reservas.domain.EstadoReserva;

interface ReservaJpa extends JpaRepository<ReservaEntidad, UUID> {

	// HU20, CA2: usa el índice ix_reserva_cancha_fecha
	// HU18: las de varias canchas (where cancha_id in ...)
	List<ReservaEntidad> findByCanchaIdInAndFechaAndEstadoOrderByHoraInicioAsc(Collection<UUID> canchaIds,
			LocalDate fecha, EstadoReserva estado);

	List<ReservaEntidad> findByCanchaIdAndFechaAndEstadoOrderByHoraInicioAsc(UUID canchaId, LocalDate fecha,
			EstadoReserva estado);

}
