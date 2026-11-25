package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaReservasFuturas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.application.port.out.EnvioCorreo;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

class CambiarEstadoEscenarioCasoUsoTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	private static final Clock RELOJ = Clock.fixed(
			LocalDateTime.of(2026, 11, 13, 15, 30).atZone(BOGOTA).toInstant(), BOGOTA);
	private static final Solicitante ADMINISTRADOR_SISTEMA =
			new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_SISTEMA);
	private static final UUID PROPIETARIO = UUID.randomUUID();

	private RepositorioEnMemoria repositorio;
	private ReservasFalsas reservas;
	private UsuariosFalsos usuarios;
	private CorreoFalso correo;
	// Mockito, para revisar el orden de los turnos con InOrder (ca7)
	private RepositorioCanchas canchas;
	private TransaccionFalsa transaccion;
	private CambiarEstadoEscenarioCasoUso casoUso;

	@BeforeEach
	void preparar() {
		repositorio = new RepositorioEnMemoria();
		reservas = new ReservasFalsas();
		usuarios = new UsuariosFalsos();
		usuarios.contactos.put(PROPIETARIO,
				new ConsultaUsuarios.ContactoUsuario(PROPIETARIO, "Ana Pérez", "ana@correo.com"));
		correo = new CorreoFalso();
		canchas = mock(RepositorioCanchas.class);
		transaccion = new TransaccionFalsa();
		casoUso = new CambiarEstadoEscenarioCasoUso(repositorio, canchas, reservas, usuarios, correo, transaccion,
				RELOJ);
	}

	private Escenario guardado(EstadoEscenario estado, UUID... canchas) {
		Escenario escenario = Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null, "Calle 80 # 45-10",
				UUID.randomUUID(), new BigDecimal("6.155123"), new BigDecimal("-75.373700"), "+573001234567", null,
				List.of(), PROPIETARIO, estado);
		repositorio.escenarios.put(escenario.getId(), escenario);
		repositorio.canchas.put(escenario.getId(), List.of(canchas));
		return escenario;
	}

	private void esperarError(UUID id, EstadoEscenario estado, Solicitante solicitante, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(
				() -> casoUso.ejecutar(id, estado, solicitante))
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
		// Ningún error guarda el cambio ni manda correo
		assertThat(repositorio.actualizados).isEmpty();
		assertThat(correo.enviados).isEmpty();
	}

	@Test
	void ca2ActivaGuardaYDespuesAvisaAlPropietario() {
		Escenario escenario = guardado(EstadoEscenario.INACTIVO, UUID.randomUUID());

		Escenario resultado = casoUso.ejecutar(escenario.getId(), EstadoEscenario.ACTIVO, ADMINISTRADOR_SISTEMA);

		assertThat(resultado.getEstado()).isEqualTo(EstadoEscenario.ACTIVO);
		assertThat(repositorio.actualizados).containsExactly(EstadoEscenario.ACTIVO);
		// Activar no depende de las reservas: ni se pregunta (política 14)
		assertThat(reservas.consultas).isEmpty();
		// CA8: COR-ESC-01 con los datos del propietario y la fecha en Bogotá (AG-18)
		assertThat(correo.enviados).containsExactly(new EnvioCorreo.Correo("COR-ESC-01", "ana@correo.com",
				List.of("Cancha La 80", new TextoCatalogo("estado.ACTIVO")),
				List.of("Ana Pérez", "Cancha La 80", new TextoCatalogo("estado.ACTIVO"), "13/11/2026 15:30",
						new TextoCatalogo("COR-ESC-01.textoSegunEstado.ACTIVO"))));
		// El correo sale después de guardar (D15)
		assertThat(correo.estadoGuardadoAlEnviar).isEqualTo(EstadoEscenario.ACTIVO);
	}

	@Test
	void ca3InactivaSiSusCanchasNoTienenReservasFuturas() {
		UUID cancha1 = UUID.randomUUID();
		UUID cancha2 = UUID.randomUUID();
		Escenario escenario = guardado(EstadoEscenario.ACTIVO, cancha1, cancha2);

		Escenario resultado = casoUso.ejecutar(escenario.getId(), EstadoEscenario.INACTIVO, ADMINISTRADOR_SISTEMA);

		assertThat(resultado.getEstado()).isEqualTo(EstadoEscenario.INACTIVO);
		assertThat(repositorio.actualizados).containsExactly(EstadoEscenario.INACTIVO);
		assertThat(reservas.consultas).containsExactly(List.of(cancha1, cancha2));
		assertThat(correo.enviados).singleElement().satisfies(enviado -> assertThat(enviado.parametrosCuerpo())
				.contains(new TextoCatalogo("COR-ESC-01.textoSegunEstado.INACTIVO")));
	}

	@Test
	void ca3UnEscenarioSinCanchasSeInactivaSinPreguntarAReservas() {
		Escenario escenario = guardado(EstadoEscenario.ACTIVO);

		casoUso.ejecutar(escenario.getId(), EstadoEscenario.INACTIVO, ADMINISTRADOR_SISTEMA);

		assertThat(repositorio.actualizados).containsExactly(EstadoEscenario.INACTIVO);
		assertThat(reservas.consultas).isEmpty();
	}

	@Test
	void ca4ElEscenarioNoExiste() {
		esperarError(UUID.randomUUID(), EstadoEscenario.ACTIVO, ADMINISTRADOR_SISTEMA, "ERR-ESC-06");
	}

	// CA5: se rechaza antes de buscar, así no se sabe si el identificador existe
	@ParameterizedTest
	@EnumSource(value = Rol.class, names = "ADMINISTRADOR_SISTEMA", mode = EnumSource.Mode.EXCLUDE)
	void ca5QuienNoEsAdministradorDelSistemaNoCambiaElEstado(Rol rol) {
		Escenario escenario = guardado(EstadoEscenario.INACTIVO);
		Solicitante solicitante = new Solicitante(PROPIETARIO, rol);

		esperarError(escenario.getId(), EstadoEscenario.ACTIVO, solicitante, "ERR-ESC-08");
		esperarError(UUID.randomUUID(), EstadoEscenario.ACTIVO, solicitante, "ERR-ESC-08");
		assertThat(repositorio.busquedas).isZero();
	}

	@Test
	void ca6ElEscenarioYaEstaEnEseEstado() {
		Escenario escenario = guardado(EstadoEscenario.ACTIVO);

		esperarError(escenario.getId(), EstadoEscenario.ACTIVO, ADMINISTRADOR_SISTEMA, "ERR-ESC-09");
	}

	@Test
	void ca7NoSeInactivaSiAlgunaCanchaTieneReservasConfirmadasFuturas() {
		Escenario escenario = guardado(EstadoEscenario.ACTIVO, UUID.randomUUID());
		reservas.hayFuturas = true;

		esperarError(escenario.getId(), EstadoEscenario.INACTIVO, ADMINISTRADOR_SISTEMA, "ERR-ESC-14");
		assertThat(escenario.getEstado()).isEqualTo(EstadoEscenario.ACTIVO);
	}

	@Test
	void ca8SiElCorreoFallaElCambioNoSeRevierte() {
		Escenario escenario = guardado(EstadoEscenario.INACTIVO);
		correo.fallar = true;

		Escenario resultado = casoUso.ejecutar(escenario.getId(), EstadoEscenario.ACTIVO, ADMINISTRADOR_SISTEMA);

		assertThat(resultado.getEstado()).isEqualTo(EstadoEscenario.ACTIVO);
		assertThat(repositorio.actualizados).containsExactly(EstadoEscenario.ACTIVO);
		// No se reintenta (PAR-GEN-02 = 0)
		assertThat(correo.intentos).isEqualTo(1);
	}

	@Test
	void ca8SiNoSeEncuentraAlPropietarioElCambioNoSeRevierte() {
		Escenario escenario = guardado(EstadoEscenario.INACTIVO);
		usuarios.contactos.clear();

		Escenario resultado = casoUso.ejecutar(escenario.getId(), EstadoEscenario.ACTIVO, ADMINISTRADOR_SISTEMA);

		assertThat(resultado.getEstado()).isEqualTo(EstadoEscenario.ACTIVO);
		assertThat(correo.intentos).isZero();
	}

	// CA7 y P2 de la especificación: con los turnos tomados, una reserva de HU21 no entra entre la
	// consulta y el cambio de estado. Siempre en el mismo orden (por id) para evitar un bloqueo mutuo
	@Test
	void ca7Inactivar_tomaLosTurnosEnOrdenAntesDeConsultarReservas() {
		UUID menor = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID mayor = UUID.fromString("00000000-0000-0000-0000-000000000002");
		Escenario escenario = guardado(EstadoEscenario.ACTIVO, mayor, menor);
		ConsultaReservasFuturas reservasMock = mock(ConsultaReservasFuturas.class);
		when(reservasMock.existenConfirmadasFuturas(any())).thenReturn(false);
		casoUso = new CambiarEstadoEscenarioCasoUso(repositorio, canchas, reservasMock, usuarios, correo,
				transaccion, RELOJ);

		casoUso.ejecutar(escenario.getId(), EstadoEscenario.INACTIVO, ADMINISTRADOR_SISTEMA);

		InOrder orden = inOrder(canchas, reservasMock);
		orden.verify(canchas).tomarTurno(menor);
		orden.verify(canchas).tomarTurno(mayor);
		orden.verify(reservasMock).existenConfirmadasFuturas(any());
		assertThat(repositorio.actualizados).containsExactly(EstadoEscenario.INACTIVO);
	}

	// Activar no puede romper la política 14: no hay por qué detener las reservas
	@Test
	void ca2Activar_noTomaTurnos() {
		Escenario escenario = guardado(EstadoEscenario.INACTIVO, UUID.randomUUID());

		casoUso.ejecutar(escenario.getId(), EstadoEscenario.ACTIVO, ADMINISTRADOR_SISTEMA);

		verifyNoInteractions(canchas);
	}

	// La política 8 se revisa antes de abrir la transacción: quien no tiene permiso no bloquea nada
	@Test
	void ca5SinPermiso_noTomaTurnos() {
		Escenario escenario = guardado(EstadoEscenario.ACTIVO, UUID.randomUUID());
		Solicitante propietario = new Solicitante(PROPIETARIO, Rol.ADMINISTRADOR_CANCHA);

		esperarError(escenario.getId(), EstadoEscenario.INACTIVO, propietario, "ERR-ESC-08");
		verifyNoInteractions(canchas);
		assertThat(transaccion.aperturas).isZero();
	}

	// D15: el estado se guarda dentro de la transacción y el correo sale cuando ya se cerró
	@Test
	void ca8ElCorreoSaleDespuesDeCerrarLaTransaccion() {
		Escenario escenario = guardado(EstadoEscenario.ACTIVO, UUID.randomUUID());

		casoUso.ejecutar(escenario.getId(), EstadoEscenario.INACTIVO, ADMINISTRADOR_SISTEMA);

		assertThat(repositorio.transaccionAbiertaAlGuardar).isTrue();
		assertThat(correo.transaccionAbiertaAlEnviar).isFalse();
		assertThat(correo.enviados).hasSize(1);
		assertThat(transaccion.aperturas).isEqualTo(1);
	}

	private final class RepositorioEnMemoria implements RepositorioEscenarios {

		final Map<UUID, Escenario> escenarios = new HashMap<>();
		final Map<UUID, List<UUID>> canchas = new HashMap<>();
		final List<EstadoEscenario> actualizados = new ArrayList<>();
		EstadoEscenario ultimoGuardado;
		boolean transaccionAbiertaAlGuardar;
		int busquedas;

		@Override
		public Optional<Escenario> buscarPorId(UUID id) {
			busquedas++;
			return Optional.ofNullable(escenarios.get(id));
		}

		@Override
		public void actualizar(Escenario escenario) {
			throw new UnsupportedOperationException(); // HU8
		}

		@Override
		public void actualizarEstado(Escenario escenario) {
			actualizados.add(escenario.getEstado());
			ultimoGuardado = escenario.getEstado();
			transaccionAbiertaAlGuardar = transaccion.abierta;
		}

		@Override
		public List<UUID> idsCanchas(UUID escenarioId) {
			return canchas.getOrDefault(escenarioId, List.of());
		}

		@Override
		public boolean existePorId(UUID id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void guardar(Escenario escenario) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<ResumenEscenario> listar(EstadoEscenario estado) {
			throw new UnsupportedOperationException();
		}

	}

	private static final class ReservasFalsas implements ConsultaReservasFuturas {

		final List<List<UUID>> consultas = new ArrayList<>();
		boolean hayFuturas;

		@Override
		public boolean existenConfirmadasFuturas(Collection<UUID> canchaIds) {
			consultas.add(List.copyOf(canchaIds));
			return hayFuturas;
		}

		// HU12: estas pruebas son de HU10 y HU13
		@Override
		public boolean existenConfirmadasFuturasEnHorario(Collection<UUID> canchaIds, DayOfWeek dia,
				LocalTime horaInicio, LocalTime horaFin) {
			throw new UnsupportedOperationException();
		}

	}

	private static final class UsuariosFalsos implements ConsultaUsuarios {

		final Map<UUID, ContactoUsuario> contactos = new HashMap<>();

		@Override
		public List<ContactoUsuario> buscarContactos(Collection<UUID> usuarioIds) {
			return usuarioIds.stream().filter(contactos::containsKey).map(contactos::get).toList();
		}

	}

	private final class CorreoFalso implements EnvioCorreo {

		final List<Correo> enviados = new ArrayList<>();
		EstadoEscenario estadoGuardadoAlEnviar;
		boolean fallar;
		boolean transaccionAbiertaAlEnviar;
		int intentos;

		@Override
		public void enviar(Correo correo) {
			intentos++;
			estadoGuardadoAlEnviar = repositorio.ultimoGuardado;
			transaccionAbiertaAlEnviar = transaccion.abierta;
			if (fallar) {
				// El adaptador real no lanza errores; aquí se fuerza para probar la red de seguridad
				throw new IllegalStateException("servidor SMTP caído");
			}
			enviados.add(correo);
		}

	}

	/** Hace lo que pide sin base de datos y anota si la transacción está abierta (P1, D15). */
	private static final class TransaccionFalsa implements TransactionOperations {

		boolean abierta;
		int aperturas;

		@Override
		public <T> T execute(TransactionCallback<T> accion) {
			aperturas++;
			abierta = true;
			try {
				return accion.doInTransaction(null);
			} finally {
				abierta = false;
			}
		}

	}

}
