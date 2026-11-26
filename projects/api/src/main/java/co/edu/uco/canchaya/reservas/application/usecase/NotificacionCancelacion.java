package co.edu.uco.canchaya.reservas.application.usecase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.DatosCorreoCancha;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios.ContactoUsuario;
import co.edu.uco.canchaya.reservas.domain.OrigenCancelacion;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.application.port.out.EnvioCorreo;
import co.edu.uco.canchaya.transversal.application.port.out.EnvioCorreo.Correo;
import co.edu.uco.canchaya.transversal.domain.FormatoTexto;

/**
 * HU25, política 21 (RN-06, D15, PAR-GEN-02): toda cancelación se avisa por correo al jugador y al
 * propietario del escenario, con la plantilla que corresponde a quién canceló. Cada correo va por su
 * lado: si uno falla, el otro igual sale. Nada se reintenta ni se lanza.
 *
 * <p>No es un caso de uso: la llaman los casos de uso que cancelan, siempre después de su
 * transacción. Hoy {@link CancelarReservaCasoUso} (HU24); después la cancelación del propietario
 * (HU32) y la automática (HU28) (P1 y P2 de HU25).
 */
@Component
public class NotificacionCancelacion {

	private static final Logger LOG = LoggerFactory.getLogger(NotificacionCancelacion.class);
	static final String CANCELO_JUGADOR = "COR-RES-03";
	static final String CANCELO_PROPIETARIO = "COR-RES-04";
	static final String CANCELO_SISTEMA = "COR-RES-05";
	// P3: en el log va el papel, no la dirección (RT-12)
	private static final String AL_JUGADOR = "jugador";
	private static final String AL_PROPIETARIO = "propietario";

	private final ConsultaCanchas consultaCanchas;
	private final ConsultaUsuarios consultaUsuarios;
	private final EnvioCorreo envioCorreo;

	public NotificacionCancelacion(ConsultaCanchas consultaCanchas, ConsultaUsuarios consultaUsuarios,
			EnvioCorreo envioCorreo) {
		this.consultaCanchas = consultaCanchas;
		this.consultaUsuarios = consultaUsuarios;
		this.envioCorreo = envioCorreo;
	}

	/**
	 * Envía los dos correos a la dirección que tienen registrada ahora.
	 *
	 * @param reserva una reserva ya CANCELADA y guardada, con su origen y su fecha de cancelación
	 */
	public void notificar(Reserva reserva) {
		String codigo = plantilla(reserva.getOrigenCancelacion());
		Optional<DatosCorreoCancha> datos;
		Optional<ContactoUsuario> jugador;
		Optional<ContactoUsuario> propietario;
		try {
			datos = consultaCanchas.buscarDatosCorreo(reserva.getCanchaId());
			List<ContactoUsuario> contactos = datos
					.map(d -> consultaUsuarios.buscarContactos(List.of(reserva.getJugadorId(), d.administradorId())))
					.orElse(List.of());
			jugador = contacto(contactos, reserva.getJugadorId());
			propietario = datos.flatMap(d -> contacto(contactos, d.administradorId()));
		} catch (RuntimeException e) {
			registrarFalla(codigo, AL_JUGADOR, reserva, e.getClass().getName());
			registrarFalla(codigo, AL_PROPIETARIO, reserva, e.getClass().getName());
			return;
		}

		enviar(codigo, AL_JUGADOR, reserva, datos.isPresent() && jugador.isPresent()
				? Optional.of(() -> correo(codigo, reserva, datos.get(), jugador.get(), jugador.get()))
				: Optional.empty());
		// COR-RES-03 y COR-RES-05 llevan {nombreJugador} también en el correo del propietario
		boolean faltaElJugador = !CANCELO_PROPIETARIO.equals(codigo) && jugador.isEmpty();
		enviar(codigo, AL_PROPIETARIO, reserva, datos.isPresent() && propietario.isPresent() && !faltaElJugador
				? Optional.of(() -> correo(codigo, reserva, datos.get(), propietario.get(), jugador.orElse(null)))
				: Optional.empty());
	}

	private static String plantilla(OrigenCancelacion origen) {
		return switch (origen) {
			case JUGADOR -> CANCELO_JUGADOR;
			case ADMINISTRADOR -> CANCELO_PROPIETARIO;
			case SISTEMA_SIN_CONFIRMACION -> CANCELO_SISTEMA;
		};
	}

	private void enviar(String codigo, String papel, Reserva reserva, Optional<Supplier<Correo>> armar) {
		if (armar.isEmpty()) {
			registrarFalla(codigo, papel, reserva, "no se encontró la cancha, su escenario o un destinatario");
			return;
		}
		try {
			envioCorreo.enviar(armar.get().get());
		} catch (RuntimeException e) {
			// Solo el tipo del error: su mensaje podría traer la dirección o datos del correo (P3)
			registrarFalla(codigo, papel, reserva, e.getClass().getName());
		}
	}

	private static void registrarFalla(String codigo, String papel, Reserva reserva, String detalle) {
		LOG.error("ERR-GEN-08 codigoCorreo={} destinatario={} reservaId={} detalle={}", codigo, papel,
				reserva.getId(), detalle);
	}

	private static Optional<ContactoUsuario> contacto(List<ContactoUsuario> contactos, UUID id) {
		return contactos.stream().filter(c -> c.id().equals(id)).findFirst();
	}

	/**
	 * La misma plantilla para los dos: solo cambian la dirección y {nombreDestinatario}.
	 *
	 * @param jugador vacío (null) solo en COR-RES-04, que no lleva el nombre del jugador
	 */
	private static Correo correo(String codigo, Reserva reserva, DatosCorreoCancha datos, ContactoUsuario destinatario,
			ContactoUsuario jugador) {
		String fecha = FormatoTexto.fecha(reserva.getFecha());
		String horaInicio = FormatoTexto.hora(reserva.getHoraInicio());
		String horaFin = FormatoTexto.hora(reserva.getHoraFin());
		String precio = FormatoTexto.precio(reserva.getPrecio());
		// Los tres asuntos: ...: {escenario}, {fecha} a las {horaInicio}
		List<Object> asunto = List.of(datos.escenario(), fecha, horaInicio);
		List<Object> cuerpo = switch (codigo) {
			// {nombreDestinatario}, {fechaCancelacion}, {escenario}, {direccion}, {ciudad}, {cancha},
			// {fecha}, {horaInicio}, {horaFin}, {precio}, {nombreJugador}
			case CANCELO_JUGADOR -> List.of(destinatario.nombre(), FormatoTexto.fechaHora(reserva.getFechaCancelacion()),
					datos.escenario(), datos.direccion(), datos.ciudad(), datos.cancha(), fecha, horaInicio, horaFin,
					precio, jugador.nombre());
			// {nombreDestinatario}, {escenario}, {fechaCancelacion}, {escenario}, {direccion}, {ciudad},
			// {cancha}, {fecha}, {horaInicio}, {horaFin}, {precio}, {justificacion}
			case CANCELO_PROPIETARIO -> List.of(destinatario.nombre(), datos.escenario(),
					FormatoTexto.fechaHora(reserva.getFechaCancelacion()), datos.escenario(), datos.direccion(),
					datos.ciudad(), datos.cancha(), fecha, horaInicio, horaFin, precio, reserva.getMotivoCancelacion());
			// {nombreDestinatario}, {escenario}, {direccion}, {ciudad}, {cancha}, {fecha}, {horaInicio},
			// {horaFin}, {precio}, {nombreJugador}
			default -> List.of(destinatario.nombre(), datos.escenario(), datos.direccion(), datos.ciudad(),
					datos.cancha(), fecha, horaInicio, horaFin, precio, jugador.nombre());
		};
		return new Correo(codigo, destinatario.correoElectronico(), asunto, cuerpo);
	}

}
