package co.edu.uco.canchaya.reservas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.DatosCorreoCancha;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios.ContactoUsuario;
import co.edu.uco.canchaya.reservas.domain.ConfirmacionAsistencia;
import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.OrigenCancelacion;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.application.port.out.EnvioCorreo;
import co.edu.uco.canchaya.transversal.application.port.out.EnvioCorreo.Correo;

/**
 * HU25, política 21 (RN-06, D15): toda cancelación se avisa al jugador y al propietario con la
 * plantilla de su origen. CA2 y CA3 se prueban con reservas armadas aquí, porque la cancelación del
 * propietario (HU32) y la automática (HU28) todavía no existen (P1).
 */
@ExtendWith(OutputCaptureExtension.class)
class NotificacionCancelacionTest {

	private static final UUID JUGADOR = UUID.randomUUID();
	private static final UUID PROPIETARIO = UUID.randomUUID();
	private static final ContactoUsuario CONTACTO_JUGADOR = new ContactoUsuario(JUGADOR, "Juan Gómez",
			"juan@correo.com", "+573001234567");
	private static final ContactoUsuario CONTACTO_PROPIETARIO = new ContactoUsuario(PROPIETARIO, "Ana Pérez",
			"ana@correo.com", "+573007654321");
	// 18/11/2026 09:15 en Bogotá, guardada en UTC como podría llegar de la base (D27)
	private static final OffsetDateTime FECHA_CANCELACION = OffsetDateTime.of(2026, 11, 18, 14, 15, 0, 0,
			ZoneOffset.UTC);

	private final UUID canchaId = UUID.randomUUID();
	private ConsultaCanchas canchas;
	private ConsultaUsuarios usuarios;
	private CorreoEnMemoria correo;
	private NotificacionCancelacion notificacion;

	@BeforeEach
	void preparar() {
		canchas = mock(ConsultaCanchas.class);
		usuarios = mock(ConsultaUsuarios.class);
		correo = new CorreoEnMemoria();
		when(canchas.buscarDatosCorreo(canchaId)).thenReturn(Optional.of(new DatosCorreoCancha("Cancha 1",
				"Canchas La 70", "Calle 70 # 45-10", "Rionegro", PROPIETARIO)));
		when(usuarios.buscarContactos(List.of(JUGADOR, PROPIETARIO)))
				.thenReturn(List.of(CONTACTO_JUGADOR, CONTACTO_PROPIETARIO));
		notificacion = new NotificacionCancelacion(canchas, usuarios, correo);
	}

	private Reserva canceladaPor(OrigenCancelacion origen, String motivo) {
		return Reserva.reconstruir(UUID.randomUUID(), canchaId, JUGADOR, LocalDate.of(2026, 11, 20),
				LocalTime.of(18, 0), LocalTime.of(20, 0), new BigDecimal("160000.00"), EstadoReserva.CANCELADA,
				ConfirmacionAsistencia.NO_REQUERIDA, origen, motivo, null, FECHA_CANCELACION, null);
	}

	private static List<Object> asunto() {
		return List.of("Canchas La 70", "20/11/2026", "18:00");
	}

	@Test
	void CA1_canceloElJugador_losDosRecibenCorRes03() {
		notificacion.notificar(canceladaPor(OrigenCancelacion.JUGADOR, null));

		assertThat(correo.enviados).containsExactly(
				new Correo("COR-RES-03", "juan@correo.com", asunto(), cuerpoCorRes03("Juan Gómez")),
				new Correo("COR-RES-03", "ana@correo.com", asunto(), cuerpoCorRes03("Ana Pérez")));
	}

	// {nombreDestinatario}, {fechaCancelacion}, {escenario}, {direccion}, {ciudad}, {cancha}, {fecha},
	// {horaInicio}, {horaFin}, {precio}, {nombreJugador}
	private static List<Object> cuerpoCorRes03(String destinatario) {
		return List.of(destinatario, "18/11/2026 09:15", "Canchas La 70", "Calle 70 # 45-10", "Rionegro", "Cancha 1",
				"20/11/2026", "18:00", "20:00", "$ 160.000", "Juan Gómez");
	}

	@Test
	void CA2_canceloElPropietario_losDosRecibenCorRes04ConLaJustificacion() {
		notificacion.notificar(canceladaPor(OrigenCancelacion.ADMINISTRADOR, "Falla de energía"));

		assertThat(correo.enviados).containsExactly(
				new Correo("COR-RES-04", "juan@correo.com", asunto(), cuerpoCorRes04("Juan Gómez")),
				new Correo("COR-RES-04", "ana@correo.com", asunto(), cuerpoCorRes04("Ana Pérez")));
	}

	// {nombreDestinatario}, {escenario}, {fechaCancelacion}, {escenario}, {direccion}, {ciudad},
	// {cancha}, {fecha}, {horaInicio}, {horaFin}, {precio}, {justificacion}
	private static List<Object> cuerpoCorRes04(String destinatario) {
		return List.of(destinatario, "Canchas La 70", "18/11/2026 09:15", "Canchas La 70", "Calle 70 # 45-10",
				"Rionegro", "Cancha 1", "20/11/2026", "18:00", "20:00", "$ 160.000", "Falla de energía");
	}

	@Test
	void CA3_canceloElSistema_losDosRecibenCorRes05() {
		notificacion.notificar(canceladaPor(OrigenCancelacion.SISTEMA_SIN_CONFIRMACION, null));

		assertThat(correo.enviados).containsExactly(
				new Correo("COR-RES-05", "juan@correo.com", asunto(), cuerpoCorRes05("Juan Gómez")),
				new Correo("COR-RES-05", "ana@correo.com", asunto(), cuerpoCorRes05("Ana Pérez")));
	}

	// {nombreDestinatario}, {escenario}, {direccion}, {ciudad}, {cancha}, {fecha}, {horaInicio},
	// {horaFin}, {precio}, {nombreJugador}
	private static List<Object> cuerpoCorRes05(String destinatario) {
		return List.of(destinatario, "Canchas La 70", "Calle 70 # 45-10", "Rionegro", "Cancha 1", "20/11/2026",
				"18:00", "20:00", "$ 160.000", "Juan Gómez");
	}

	@Test
	void CA4_siUnCorreoFalla_elOtroSaleYNoSeLanzaNada(CapturedOutput salida) {
		correo.fallaCon = "juan@correo.com";
		Reserva reserva = canceladaPor(OrigenCancelacion.JUGADOR, null);

		assertThatCode(() -> notificacion.notificar(reserva)).doesNotThrowAnyException();

		assertThat(correo.enviados).extracting(Correo::destinatario).containsExactly("ana@correo.com");
		assertThat(salida.getAll()).contains("ERR-GEN-08 codigoCorreo=COR-RES-03 destinatario=jugador reservaId="
				+ reserva.getId() + " detalle=java.lang.IllegalStateException");
	}

	@Test
	void CA4_sinLaCancha_noSaleNingunoYSeRegistranLosDos(CapturedOutput salida) {
		when(canchas.buscarDatosCorreo(canchaId)).thenReturn(Optional.empty());

		notificacion.notificar(canceladaPor(OrigenCancelacion.JUGADOR, null));

		assertThat(correo.enviados).isEmpty();
		assertThat(salida.getAll()).contains("codigoCorreo=COR-RES-03 destinatario=jugador")
				.contains("codigoCorreo=COR-RES-03 destinatario=propietario");
	}

	// COR-RES-03 lleva {nombreJugador} también en el correo del propietario
	@Test
	void CA4_sinElJugador_enCorRes03NoSaleNinguno(CapturedOutput salida) {
		when(usuarios.buscarContactos(any())).thenReturn(List.of(CONTACTO_PROPIETARIO));

		notificacion.notificar(canceladaPor(OrigenCancelacion.JUGADOR, null));

		assertThat(correo.enviados).isEmpty();
		assertThat(salida.getAll()).contains("codigoCorreo=COR-RES-03 destinatario=jugador")
				.contains("codigoCorreo=COR-RES-03 destinatario=propietario");
	}

	// COR-RES-04 no lleva el nombre del jugador: el propietario igual recibe el suyo
	@Test
	void CA4_sinElJugador_enCorRes04ElPropietarioSiLoRecibe(CapturedOutput salida) {
		when(usuarios.buscarContactos(any())).thenReturn(List.of(CONTACTO_PROPIETARIO));

		notificacion.notificar(canceladaPor(OrigenCancelacion.ADMINISTRADOR, "Falla de energía"));

		assertThat(correo.enviados).extracting(Correo::destinatario).containsExactly("ana@correo.com");
		assertThat(salida.getAll()).contains("codigoCorreo=COR-RES-04 destinatario=jugador")
				.doesNotContain("destinatario=propietario");
	}

	@Test
	void CA4_sinElPropietario_soloSaleElDelJugador(CapturedOutput salida) {
		when(usuarios.buscarContactos(any())).thenReturn(List.of(CONTACTO_JUGADOR));

		notificacion.notificar(canceladaPor(OrigenCancelacion.JUGADOR, null));

		assertThat(correo.enviados).extracting(Correo::destinatario).containsExactly("juan@correo.com");
		assertThat(salida.getAll()).contains("codigoCorreo=COR-RES-03 destinatario=propietario")
				.doesNotContain("destinatario=jugador");
	}

	@Test
	void CA4_siLaConsultaDeDatosFalla_seRegistranLosDosYNoSeLanzaNada(CapturedOutput salida) {
		when(canchas.buscarDatosCorreo(canchaId)).thenThrow(new IllegalStateException("base caída"));

		assertThatCode(() -> notificacion.notificar(canceladaPor(OrigenCancelacion.JUGADOR, null)))
				.doesNotThrowAnyException();

		assertThat(correo.enviados).isEmpty();
		assertThat(salida.getAll()).contains("codigoCorreo=COR-RES-03 destinatario=jugador")
				.contains("codigoCorreo=COR-RES-03 destinatario=propietario");
	}

	// P3, RT-12: el papel del destinatario, no su dirección ni el mensaje del error
	@Test
	void CA4_elLogNoLlevaDatosDelDestinatario(CapturedOutput salida) {
		correo.fallaCon = "ana@correo.com";

		notificacion.notificar(canceladaPor(OrigenCancelacion.JUGADOR, null));

		assertThat(salida.getAll()).contains("destinatario=propietario").doesNotContain("ana@correo.com")
				.doesNotContain("Ana Pérez");
	}

	/** EnvioCorreo falso. El adaptador real nunca lanza; la notificación igual se protege (D15). */
	private static final class CorreoEnMemoria implements EnvioCorreo {

		final List<Correo> enviados = new ArrayList<>();
		String fallaCon;

		@Override
		public void enviar(Correo correo) {
			if (correo.destinatario().equals(fallaCon)) {
				// El mensaje trae datos del correo: no debe llegar al log
				throw new IllegalStateException("no se pudo enviar a " + correo.destinatario() + ", "
						+ correo.parametrosCuerpo().getFirst());
			}
			enviados.add(correo);
		}

	}

}
