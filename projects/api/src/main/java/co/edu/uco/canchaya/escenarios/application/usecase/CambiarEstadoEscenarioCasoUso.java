package co.edu.uco.canchaya.escenarios.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import co.edu.uco.canchaya.escenarios.application.port.in.CambiarEstadoEscenario;
import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaReservasFuturas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios.ContactoUsuario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.application.port.out.EnvioCorreo;
import co.edu.uco.canchaya.transversal.application.port.out.EnvioCorreo.Correo;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

/**
 * Sin @Transactional a propósito: el correo se envía cuando el cambio de estado ya quedó
 * guardado (D15). Si todo el caso de uso fuera una sola transacción, el correo saldría antes de
 * confirmarla y podría avisar un cambio que al final no se guardó. Por eso solo buscar, tomar
 * los turnos, consultar las reservas y guardar van dentro de {@code transaccion.execute} (P1 del
 * arreglo de HU13); el correo va después.
 */
@Service
public class CambiarEstadoEscenarioCasoUso implements CambiarEstadoEscenario {

	private static final Logger LOG = LoggerFactory.getLogger(CambiarEstadoEscenarioCasoUso.class);
	static final String CORREO = "COR-ESC-01";
	// AG-18: dd/mm/aaaa hh:mm
	private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private final RepositorioEscenarios repositorio;
	private final RepositorioCanchas repositorioCanchas;
	private final ConsultaReservasFuturas consultaReservasFuturas;
	private final ConsultaUsuarios consultaUsuarios;
	private final EnvioCorreo envioCorreo;
	private final TransactionOperations transaccion;
	private final Clock reloj;

	public CambiarEstadoEscenarioCasoUso(RepositorioEscenarios repositorio, RepositorioCanchas repositorioCanchas,
			ConsultaReservasFuturas consultaReservasFuturas, ConsultaUsuarios consultaUsuarios,
			EnvioCorreo envioCorreo, TransactionOperations transaccion, Clock reloj) {
		this.repositorio = repositorio;
		this.repositorioCanchas = repositorioCanchas;
		this.consultaReservasFuturas = consultaReservasFuturas;
		this.consultaUsuarios = consultaUsuarios;
		this.envioCorreo = envioCorreo;
		this.transaccion = transaccion;
		this.reloj = reloj;
	}

	@Override
	public Escenario ejecutar(UUID escenarioId, EstadoEscenario nuevoEstado, Solicitante solicitante) {
		// Política 8 antes que la 6 y fuera de la transacción: quien no tiene permiso no averigua
		// qué escenarios existen ni detiene las reservas de nadie
		Escenario.validarAdministradorSistema(solicitante);

		// Al terminar execute el cambio queda guardado y se sueltan los turnos de las canchas
		Escenario escenario = transaccion.execute(estado -> {
			// Política 6
			Escenario encontrado = repositorio.buscarPorId(escenarioId)
					.orElseThrow(() -> new ReglaNegocioException("ERR-ESC-06"));
			// Política 14: solo se pregunta a Reservas cuando se va a inactivar
			boolean reservasFuturas = nuevoEstado == EstadoEscenario.INACTIVO && tieneReservasFuturas(escenarioId);
			encontrado.cambiarEstado(nuevoEstado, solicitante, reservasFuturas);
			repositorio.actualizarEstado(encontrado);
			return encontrado;
		});

		notificarPropietario(escenario);
		return escenario;
	}

	/**
	 * Toma el turno de cada cancha antes de preguntar (D29 punto 2, D36): una reserva que llegue
	 * ahora espera y después encuentra el escenario INACTIVO. Siempre en el mismo orden, por id,
	 * para que dos operaciones que tomen varios turnos no se queden esperando la una a la otra (P2).
	 */
	private boolean tieneReservasFuturas(UUID escenarioId) {
		List<UUID> canchas = repositorio.idsCanchas(escenarioId);
		canchas.stream().sorted().forEach(repositorioCanchas::tomarTurno);
		return !canchas.isEmpty() && consultaReservasFuturas.existenConfirmadasFuturas(canchas);
	}

	/**
	 * Política 15 (RN-06, D19). El estado ya quedó guardado: pase lo que pase aquí, no se revierte
	 * ni se reintenta (D15, PAR-GEN-02). El adaptador de correo ya registra sus propias fallas;
	 * esto cubre lo que falle antes de llegar a él, por ejemplo al buscar al propietario.
	 */
	private void notificarPropietario(Escenario escenario) {
		try {
			List<ContactoUsuario> contactos = consultaUsuarios.buscarContactos(List.of(escenario.getAdministradorId()));
			if (contactos.isEmpty()) {
				LOG.error("ERR-GEN-08 codigoCorreo={} escenarioId={} detalle=no se encontró al propietario {}",
						CORREO, escenario.getId(), escenario.getAdministradorId());
				return;
			}
			ContactoUsuario propietario = contactos.getFirst();
			EstadoEscenario estado = escenario.getEstado();
			TextoCatalogo textoEstado = new TextoCatalogo("estado." + estado.name());
			envioCorreo.enviar(new Correo(CORREO, propietario.correoElectronico(),
					// Asunto: Su escenario {escenario} ahora está {estado}
					List.of(escenario.getNombre(), textoEstado),
					// Cuerpo: {nombrePropietario}, {escenario}, {estado}, {fechaHora}, {textoSegunEstado}
					List.of(propietario.nombre(), escenario.getNombre(), textoEstado,
							LocalDateTime.now(reloj).format(FECHA_HORA),
							new TextoCatalogo(CORREO + ".textoSegunEstado." + estado.name()))));
		} catch (RuntimeException e) {
			LOG.error("ERR-GEN-08 codigoCorreo={} escenarioId={} detalle={}", CORREO, escenario.getId(),
					e.toString());
		}
	}

}
