package co.edu.uco.canchaya.reservas.application.usecase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.CanchaReservable;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.DatosCorreoCancha;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.RangoBloqueado;
import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios.ContactoUsuario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.reservas.application.port.in.ReservarCancha;
import co.edu.uco.canchaya.reservas.application.port.out.RepositorioReservas;
import co.edu.uco.canchaya.reservas.domain.ParametrosReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.application.port.out.EnvioCorreo;
import co.edu.uco.canchaya.transversal.application.port.out.EnvioCorreo.Correo;
import co.edu.uco.canchaya.transversal.domain.FormatoTexto;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

/**
 * HU21. Todo va en una sola transacción porque el turno de la cancha dura hasta que la transacción
 * termina (D36): mientras tanto, otra reserva o un bloqueo de la misma cancha esperan (D29 punto 2).
 *
 * <p>HU22. Los correos van después de {@code transaccion.execute}, en el mismo hilo (P1): cuando
 * salen, la reserva ya quedó guardada y el turno se soltó. Si un correo fallara dentro de la
 * transacción podría revertir la reserva, o avisar una reserva que al final no se guardó (D15).
 */
@Service
public class ReservarCanchaCasoUso implements ReservarCancha {

	private static final Logger LOG = LoggerFactory.getLogger(ReservarCanchaCasoUso.class);
	static final String CORREO_JUGADOR = "COR-RES-01";
	static final String CORREO_PROPIETARIO = "COR-RES-02";

	private final ConsultaCanchas consultaCanchas;
	private final RepositorioReservas repositorioReservas;
	private final ConsultaUsuarios consultaUsuarios;
	private final EnvioCorreo envioCorreo;
	private final ParametrosReserva parametros;
	private final Clock reloj;
	// Es la TransactionTemplate de Spring; la interfaz deja probar el caso de uso sin base
	private final TransactionOperations transaccion;

	public ReservarCanchaCasoUso(ConsultaCanchas consultaCanchas, RepositorioReservas repositorioReservas,
			ConsultaUsuarios consultaUsuarios, EnvioCorreo envioCorreo, ParametrosReserva parametros, Clock reloj,
			TransactionOperations transaccion) {
		this.consultaCanchas = consultaCanchas;
		this.repositorioReservas = repositorioReservas;
		this.consultaUsuarios = consultaUsuarios;
		this.envioCorreo = envioCorreo;
		this.parametros = parametros;
		this.reloj = reloj;
		this.transaccion = transaccion;
	}

	@Override
	public ReservaConfirmada ejecutar(UUID canchaId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
			Solicitante solicitante) {
		ReservaConfirmada confirmada = transaccion
				.execute(estado -> reservar(canchaId, fecha, horaInicio, horaFin, solicitante));
		// 9. Fuera de la transacción: pase lo que pase aquí, la reserva sigue CONFIRMADA (CA3)
		notificar(confirmada.reserva());
		return confirmada;
	}

	private ReservaConfirmada reservar(UUID canchaId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
			Solicitante solicitante) {
		// 1. D29 punto 2: el turno se toma antes de leer franjas y reservas, si no se leería algo viejo
		consultaCanchas.tomarTurno(canchaId);
		LocalDateTime ahora = LocalDateTime.now(reloj);

		// 2. Política 1. Sin cancha no se piden franjas; confirmar decide entre ERR-RES-05 y ERR-RES-01
		Optional<CanchaReservable> cancha = consultaCanchas.buscarReservable(canchaId);
		List<Franja> franjasLibres = List.of();
		boolean cubiertaPorBloqueo = false;
		if (cancha.isPresent()) {
			// 3. Ya vienen sin las franjas que empezaron
			List<Franja> franjas = franjasDelDia(canchaId, fecha);
			// 4. Reservas y bloqueos por separado, para distinguir ERR-RES-06 de ERR-RES-07
			List<Reserva> reservas = repositorioReservas.confirmadasDeCanchaEnFecha(canchaId, fecha);
			franjasLibres = franjas.stream()
					.filter(f -> reservas.stream().noneMatch(r -> f.seCruzaCon(r.getFecha(), r.getHoraInicio(), r.getHoraFin())))
					.toList();
			List<RangoBloqueado> bloqueos = consultaCanchas.bloqueosDelDia(canchaId, fecha);
			cubiertaPorBloqueo = bloqueos.stream()
					.anyMatch(b -> b.horaInicio().isBefore(horaFin) && horaInicio.isBefore(b.horaFin()));
		}

		// 5. Políticas 8 y 9: los límites del jugador
		UUID jugadorId = solicitante.usuarioId();
		int activas = repositorioReservas.contarActivasDeJugador(jugadorId, ahora);
		int horasDelDia = repositorioReservas.horasConfirmadasDeJugadorEnFecha(jugadorId, fecha);

		// 6
		BigDecimal precioHora = cancha.map(CanchaReservable::precioHora).orElse(null);
		Reserva reserva = Reserva.confirmar(canchaId, solicitante, fecha, horaInicio, horaFin, precioHora,
				cancha.isPresent(), franjasLibres, cubiertaPorBloqueo, activas, horasDelDia, ahora, parametros);

		// 7. Política 10. ERR-RES-10 solo va al log; el usuario no lo ve
		while (repositorioReservas.existePorId(reserva.getId())) {
			LOG.warn("ERR-RES-10 objeto=Reserva id={}", reserva.getId());
			reserva = reserva.conNuevoId();
		}

		// 8. Si la restricción de exclusión rechaza el cruce, el repositorio lanza ERR-RES-06
		repositorioReservas.guardar(reserva);
		return new ReservaConfirmada(reserva, cancha.get().nombre(), cancha.get().nombreEscenario());
	}

	/**
	 * Política 13 (RN-06, D15, PAR-GEN-02): COR-RES-01 al jugador y COR-RES-02 al propietario, a
	 * la dirección que tienen registrada ahora. Cada correo va por su lado: si uno falla, el otro
	 * igual sale. Nada se reintenta ni se lanza.
	 */
	private void notificar(Reserva reserva) {
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
			registrarFalla(CORREO_JUGADOR, reserva, e.getClass().getName());
			registrarFalla(CORREO_PROPIETARIO, reserva, e.getClass().getName());
			return;
		}
		enviar(CORREO_JUGADOR, reserva, datos.isPresent() && jugador.isPresent()
				? Optional.of(() -> correoJugador(reserva, datos.get(), jugador.get()))
				: Optional.empty());
		// COR-RES-02 lleva el nombre y el celular del jugador: sin él tampoco se puede armar
		enviar(CORREO_PROPIETARIO, reserva, datos.isPresent() && jugador.isPresent() && propietario.isPresent()
				? Optional.of(() -> correoPropietario(reserva, datos.get(), jugador.get(), propietario.get()))
				: Optional.empty());
	}

	private void enviar(String codigo, Reserva reserva, Optional<Supplier<Correo>> armar) {
		if (armar.isEmpty()) {
			registrarFalla(codigo, reserva, "no se encontró la cancha, su escenario o un destinatario");
			return;
		}
		try {
			envioCorreo.enviar(armar.get().get());
		} catch (RuntimeException e) {
			// Solo el tipo del error: su mensaje podría traer datos del correo, como el celular (P4, RT-12)
			registrarFalla(codigo, reserva, e.getClass().getName());
		}
	}

	private static void registrarFalla(String codigo, Reserva reserva, String detalle) {
		LOG.error("ERR-GEN-08 codigoCorreo={} reservaId={} detalle={}", codigo, reserva.getId(), detalle);
	}

	private static Optional<ContactoUsuario> contacto(List<ContactoUsuario> contactos, UUID id) {
		return contactos.stream().filter(c -> c.id().equals(id)).findFirst();
	}

	// CA1. El aviso solo va si la confirmación quedó PENDIENTE; con PAR-RES-12 en «No», vacío (D33)
	private static Correo correoJugador(Reserva reserva, DatosCorreoCancha datos, ContactoUsuario jugador) {
		String fecha = FormatoTexto.fecha(reserva.getFecha());
		String horaInicio = FormatoTexto.hora(reserva.getHoraInicio());
		Object aviso = reserva.requiereConfirmacionAsistencia()
				? new TextoCatalogo(CORREO_JUGADOR + ".avisoConfirmacion")
				: "";
		return new Correo(CORREO_JUGADOR, jugador.correoElectronico(),
				// Asunto: Reserva confirmada: {escenario}, {fecha} a las {horaInicio}
				List.of(datos.escenario(), fecha, horaInicio),
				// Cuerpo: {nombreJugador}, {escenario}, {direccion}, {ciudad}, {cancha}, {fecha},
				// {horaInicio}, {horaFin}, {precio}, {avisoConfirmacion}
				List.of(jugador.nombre(), datos.escenario(), datos.direccion(), datos.ciudad(), datos.cancha(), fecha,
						horaInicio, FormatoTexto.hora(reserva.getHoraFin()), FormatoTexto.precio(reserva.getPrecio()),
						aviso));
	}

	// CA2. D19: solo el propietario, no los encargados
	private static Correo correoPropietario(Reserva reserva, DatosCorreoCancha datos, ContactoUsuario jugador,
			ContactoUsuario propietario) {
		String fecha = FormatoTexto.fecha(reserva.getFecha());
		String horaInicio = FormatoTexto.hora(reserva.getHoraInicio());
		return new Correo(CORREO_PROPIETARIO, propietario.correoElectronico(),
				// Asunto: Nueva reserva en {escenario}: {cancha}, {fecha} a las {horaInicio}
				List.of(datos.escenario(), datos.cancha(), fecha, horaInicio),
				// Cuerpo: {nombrePropietario}, {cancha}, {fecha}, {horaInicio}, {horaFin},
				// {nombreJugador}, {celularJugador}
				List.of(propietario.nombre(), datos.cancha(), fecha, horaInicio, FormatoTexto.hora(reserva.getHoraFin()),
						jugador.nombre(), jugador.numeroCelular()));
	}

	// P1: fuera de la ventana, Escenarios responde ERR-HOR-10, pero al jugador que reserva se le
	// dice ERR-RES-02. Se sigue sin franjas y confirmar lanza el error con su propia regla
	private List<Franja> franjasDelDia(UUID canchaId, LocalDate fecha) {
		try {
			return consultaCanchas.franjasDelDia(canchaId, fecha);
		} catch (ReglaNegocioException e) {
			if ("ERR-HOR-10".equals(e.getCodigoMensaje())) {
				return List.of();
			}
			throw e;
		}
	}

}
