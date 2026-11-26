package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios.BloqueoAgenda;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios.CanchaAgenda;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios.DatosAgenda;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU29: lo que Escenarios entrega a la agenda y quién lo puede pedir (CA1, CA2, CA6). */
class ConsultaEscenariosCasoUsoTest {

	private static final UUID ANA = UUID.randomUUID();
	private static final UUID ENCARGADA = UUID.randomUUID();
	private static final LocalDate LUNES = LocalDate.of(2026, 11, 23);
	private static final LocalDate MIERCOLES = LUNES.plusDays(2);
	private static final LocalDate DOMINGO = LUNES.plusDays(6);

	private CanchasEnMemoria datos;
	private EncargadosEnMemoria encargados;
	private HorariosEnMemoria horarios;
	private BloqueosEnMemoria bloqueos;
	private ConsultaEscenariosCasoUso casoUso;
	private Escenario escenario;
	private Cancha cancha2;
	private Cancha cancha1;
	private Cancha deOtroEscenario;

	@BeforeEach
	void preparar() {
		datos = new CanchasEnMemoria();
		encargados = new EncargadosEnMemoria();
		horarios = new HorariosEnMemoria();
		bloqueos = BloqueosEnMemoria.sinBloqueos();
		escenario = datos.escenarioDe(ANA, "Canchas La 70", EstadoEscenario.ACTIVO);
		cancha2 = datos.cancha(escenario, "Cancha 2", EstadoCancha.INACTIVA);
		cancha1 = datos.cancha(escenario, "Cancha 1", EstadoCancha.DISPONIBLE);
		deOtroEscenario = datos.cancha(datos.escenarioDe(UUID.randomUUID(), "Golazo", EstadoEscenario.ACTIVO),
				"Cancha 1", EstadoCancha.DISPONIBLE);
		horarios.guardados.add(HorarioRecurrente.reconstruir(UUID.randomUUID(), escenario.getId(),
				DiaSemana.MIERCOLES, LocalTime.of(18, 0), LocalTime.of(20, 0)));
		casoUso = new ConsultaEscenariosCasoUso(datos.repositorioEscenarios(), encargados, datos.repositorioCanchas(),
				horarios, bloqueos);
	}

	private static Solicitante administrador(UUID id) {
		return new Solicitante(id, Rol.ADMINISTRADOR_CANCHA);
	}

	private static BloqueoAgenda bloqueo(Cancha cancha, LocalDate fecha) {
		return new BloqueoAgenda(UUID.randomUUID(), cancha.getId(), fecha, LocalTime.of(18, 0), LocalTime.of(19, 0),
				"MANTENIMIENTO", "Cambio de malla");
	}

	private static void esperarErrGen06(Runnable accion) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo("ERR-GEN-06");
	}

	// CA1: todas las canchas del escenario, por nombre y en cualquier estado; las de otro escenario no
	@Test
	void ca1TodasLasCanchasDelEscenario() {
		DatosAgenda agenda = casoUso.datosAgenda(escenario.getId(), administrador(ANA), MIERCOLES, MIERCOLES);

		assertThat(agenda.escenarioId()).isEqualTo(escenario.getId());
		assertThat(agenda.escenario()).isEqualTo("Canchas La 70");
		assertThat(agenda.propietario()).isTrue();
		assertThat(agenda.canchas()).extracting(CanchaAgenda::id, CanchaAgenda::nombre, CanchaAgenda::estado)
				.containsExactly(tuple(cancha1.getId(), "Cancha 1", "DISPONIBLE"),
						tuple(cancha2.getId(), "Cancha 2", "INACTIVA"));
	}

	// CA1 y CA2: una entrada por cada fecha del rango, en orden, con las franjas de los horarios de ese día
	@Test
	void ca2LasFranjasDeCadaDiaDeLaSemana() {
		DatosAgenda agenda = casoUso.datosAgenda(escenario.getId(), administrador(ANA), LUNES, DOMINGO);

		assertThat(agenda.franjasPorFecha().keySet()).containsExactly(LUNES, LUNES.plusDays(1), MIERCOLES,
				LUNES.plusDays(3), LUNES.plusDays(4), LUNES.plusDays(5), DOMINGO);
		assertThat(agenda.franjasPorFecha().get(LUNES)).isEmpty();
		assertThat(agenda.franjasPorFecha().get(MIERCOLES)).extracting(Franja::horaInicio)
				.containsExactly(LocalTime.of(18, 0), LocalTime.of(19, 0));
	}

	// CA1 y CA2 (P2): los bloqueos de las canchas del escenario dentro del rango, en una sola consulta
	@Test
	void ca2LosBloqueosDelRangoDeSusCanchas() {
		BloqueoAgenda delMiercoles = bloqueo(cancha1, MIERCOLES);
		bloqueos.deAgenda.add(delMiercoles);
		bloqueos.deAgenda.add(bloqueo(cancha1, DOMINGO.plusDays(1)));
		bloqueos.deAgenda.add(bloqueo(deOtroEscenario, MIERCOLES));

		DatosAgenda agenda = casoUso.datosAgenda(escenario.getId(), administrador(ANA), LUNES, DOMINGO);

		assertThat(agenda.bloqueos()).containsExactly(delMiercoles);
		assertThat(bloqueos.consultasDeAgenda).isEqualTo(1);
	}

	// CA6 y RN-15: el encargado asignado también la ve, pero no como propietario (D02)
	@Test
	void ca6ElEncargadoAsignadoLaVe() {
		encargados.asignar(escenario.getId(), ENCARGADA);

		assertThat(casoUso.datosAgenda(escenario.getId(), administrador(ENCARGADA), MIERCOLES, MIERCOLES)
				.propietario()).isFalse();
	}

	// CA6: ni propietario ni encargado de este escenario (aunque sea encargado de otro); no se lee nada más
	@Test
	void ca6OtroAdministradorRecibeErrGen06() {
		encargados.asignar(UUID.randomUUID(), ENCARGADA);

		esperarErrGen06(() -> casoUso.datosAgenda(escenario.getId(), administrador(ENCARGADA), MIERCOLES, MIERCOLES));
		assertThat(bloqueos.consultasDeAgenda).isZero();
	}

	// CA6 con cualquier otro rol, aunque el id sea el del propietario
	@ParameterizedTest
	@EnumSource(value = Rol.class, names = "ADMINISTRADOR_CANCHA", mode = EnumSource.Mode.EXCLUDE)
	void ca6OtroRolRecibeErrGen06(Rol rol) {
		esperarErrGen06(() -> casoUso.datosAgenda(escenario.getId(), new Solicitante(ANA, rol), MIERCOLES, MIERCOLES));
	}

	// P7: el escenario que no existe responde igual que el que no es suyo, así no se sabe cuáles existen
	@Test
	void p7ElEscenarioQueNoExisteTambienEsErrGen06() {
		esperarErrGen06(() -> casoUso.datosAgenda(UUID.randomUUID(), administrador(ANA), MIERCOLES, MIERCOLES));
	}

	@Test
	void sinSolicitanteEsErrGen06() {
		esperarErrGen06(() -> casoUso.datosAgenda(escenario.getId(), null, MIERCOLES, MIERCOLES));
	}

	@Test
	void hastaAntesDeDesdeEsUnErrorDeQuienLlama() {
		assertThatThrownBy(() -> casoUso.datosAgenda(escenario.getId(), administrador(ANA), DOMINGO, LUNES))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
