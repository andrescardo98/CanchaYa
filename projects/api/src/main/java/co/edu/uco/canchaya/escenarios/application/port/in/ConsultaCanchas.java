package co.edu.uco.canchaya.escenarios.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.Franja;

/** Lo que Escenarios ofrece a Reservas sobre una cancha (componentes.md, D36). Lo usan HU20 y HU21. */
public interface ConsultaCanchas {

	/**
	 * Vacío si la cancha no existe, no está DISPONIBLE o su escenario no está ACTIVO (política 1 de
	 * Reserva, P2 de HU20).
	 */
	Optional<CanchaReservable> buscarReservable(UUID canchaId);

	/**
	 * HorarioRecurrente.calcularFranjas con los horarios del escenario de la cancha, la ventana de
	 * PAR-RES-01 y la hora del servidor (D27).
	 *
	 * @throws co.edu.uco.canchaya.transversal.domain.ReglaNegocioException ERR-HOR-09 si la cancha no
	 *         existe (política 9, respaldo de ERR-RES-01) o ERR-HOR-10 si la fecha está fuera de la ventana (política 10)
	 */
	List<Franja> franjasDelDia(UUID canchaId, LocalDate fecha);

	/** D36. Solo las horas: el jugador no ve el tipo ni el motivo del bloqueo (P5 de HU20). */
	List<RangoBloqueado> bloqueosDelDia(UUID canchaId, LocalDate fecha);

	/**
	 * D29 punto 2 y D36: bloquea la fila de la cancha (select ... for update) hasta que termina la
	 * transacción de quien llama. Exige una transacción abierta (Propagation.MANDATORY).
	 */
	void tomarTurno(UUID canchaId);

	/**
	 * HU22 (P2): lo que necesitan COR-RES-01 y COR-RES-02 sobre la cancha y su escenario. No filtra
	 * por estado: la reserva ya quedó confirmada y el correo se envía aunque la cancha cambie justo
	 * después. Vacío si la cancha, su escenario o su ciudad no existen.
	 */
	Optional<DatosCorreoCancha> buscarDatosCorreo(UUID canchaId);

	/** ciudad es el nombre, no el id: Reservas no puede usar Ubicación. */
	record DatosCorreoCancha(String cancha, String escenario, String direccion, String ciudad, UUID administradorId) {
	}

	/**
	 * HU23: el nombre de cada cancha y el de su escenario, en cualquier estado, porque una reserva
	 * pasada puede ser de una cancha que hoy está inactiva. Las que no existen no vienen; vacío si no
	 * se pide ninguna.
	 */
	Map<UUID, NombresCancha> nombres(Collection<UUID> canchaIds);

	/** Nombres que muestra «Mis reservas» (HU23, CA1). */
	record NombresCancha(String cancha, String escenario) {
	}

	/** nombre y nombreEscenario los usa EXI-RES-01 (HU21); precioHora, la política 11 de confirmar. */
	record CanchaReservable(UUID id, UUID escenarioId, String nombre, String nombreEscenario, BigDecimal precioHora) {
	}

	/** Horas de un bloqueo de la cancha en un día. */
	record RangoBloqueado(LocalTime horaInicio, LocalTime horaFin) {
	}

}
