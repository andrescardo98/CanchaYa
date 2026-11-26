package co.edu.uco.canchaya.reservas.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.application.port.in.CancelarReserva;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.ParametrosReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * HU24. No toma el turno de la cancha: cancelar libera una franja, no la ocupa, así que no puede
 * crear un cruce. Lo que sí puede pasar es que otra operación cambie la misma reserva al mismo
 * tiempo; por eso se guarda con una actualización condicional (P3, D29 punto 3).
 *
 * <p>Los correos de cancelación los agrega HU25 después de {@code transaccion.execute}, como HU22
 * en {@link ReservarCanchaCasoUso} (P2, D15).
 */
@Service
public class CancelarReservaCasoUso implements CancelarReserva {

	private final RepositorioReservas repositorioReservas;
	private final ParametrosReserva parametros;
	private final Clock reloj;
	// Es la TransactionTemplate de Spring; la interfaz deja probar el caso de uso sin base
	private final TransactionOperations transaccion;

	public CancelarReservaCasoUso(RepositorioReservas repositorioReservas, ParametrosReserva parametros, Clock reloj,
			TransactionOperations transaccion) {
		this.repositorioReservas = repositorioReservas;
		this.parametros = parametros;
		this.reloj = reloj;
		this.transaccion = transaccion;
	}

	@Override
	public Reserva ejecutar(UUID reservaId, Solicitante solicitante) {
		return transaccion.execute(estado -> cancelar(reservaId, solicitante));
	}

	private Reserva cancelar(UUID reservaId, Solicitante solicitante) {
		// 1. La hora del servidor en Bogotá, no la del dispositivo (D27)
		LocalDateTime ahora = LocalDateTime.now(reloj);

		// 2. Política 14: que exista
		Reserva reserva = repositorioReservas.buscarPorId(reservaId)
				.orElseThrow(() -> new ReglaNegocioException("ERR-RES-14"));

		// 3. Políticas 14, 15, 16 y 19
		reserva.cancelar(solicitante, ahora, parametros);

		// 4. P3: si otra operación la cambió entre la lectura y este punto, no se pisa lo que hizo
		if (!repositorioReservas.cancelarSiSigueConfirmada(reserva)) {
			throw new ReglaNegocioException("ERR-RES-14");
		}
		return reserva;
	}

}
