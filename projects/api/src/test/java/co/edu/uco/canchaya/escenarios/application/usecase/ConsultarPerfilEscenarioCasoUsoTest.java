package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarPerfilEscenario.CanchaPerfil;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarPerfilEscenario.PerfilEscenario;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.escenarios.domain.TipoCancha;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.ubicacion.application.port.in.ConsultaUbicacion;
import co.edu.uco.canchaya.ubicacion.domain.Ciudad;
import co.edu.uco.canchaya.ubicacion.domain.Departamento;

/** HU19: perfil público de un escenario. */
class ConsultarPerfilEscenarioCasoUsoTest {

	private static final UUID ANTIOQUIA = UUID.randomUUID();
	private static final UUID RIONEGRO = UUID.randomUUID();

	private CanchasEnMemoria datos;
	private HorariosEnMemoria horarios;
	private List<Short> calificaciones;
	private final List<Collection<UUID>> canchasConsultadas = new ArrayList<>();
	private ConsultarPerfilEscenarioCasoUso casoUso;

	private TipoCancha futbol5;

	@BeforeEach
	void preparar() {
		datos = new CanchasEnMemoria();
		horarios = new HorariosEnMemoria();
		calificaciones = List.of();
		futbol5 = datos.tipo("F5", "Fútbol 5", 10);
		casoUso = new ConsultarPerfilEscenarioCasoUso(datos.repositorioEscenarios(), datos.repositorioCanchas(),
				horarios, datos.repositorioTipos(), new UbicacionFalsa(), canchaIds -> {
					canchasConsultadas.add(canchaIds);
					return calificaciones;
				});
	}

	private Escenario escenario(EstadoEscenario estado, List<String> fotos) {
		Escenario escenario = Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", "Grama sintética nueva",
				"Calle 80 # 45-10", RIONEGRO, new BigDecimal("6.155123"), new BigDecimal("-75.373700"),
				"+576045551234", "contacto@la80.com", fotos, UUID.randomUUID(), estado);
		datos.escenarios.put(escenario.getId(), escenario);
		return escenario;
	}

	private Cancha cancha(Escenario escenario, String nombre, EstadoCancha estado) {
		Cancha cancha = Cancha.reconstruir(UUID.randomUUID(), escenario.getId(), futbol5.getId(), nombre,
				new BigDecimal("80000.00"), estado);
		datos.canchas.add(cancha);
		return cancha;
	}

	private void conHorario(Escenario escenario) {
		horarios.guardados.add(HorarioRecurrente.reconstruir(UUID.randomUUID(), escenario.getId(), DiaSemana.LUNES,
				LocalTime.of(8, 0), LocalTime.of(22, 0)));
	}

	private static void esperarError(Runnable accion, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
	}

	@Test
	void ca1UnEscenarioActivoDevuelveSuPerfilCompleto() {
		Escenario escenario = escenario(EstadoEscenario.ACTIVO, List.of("https://fotos/1.jpg"));
		conHorario(escenario);
		Cancha cancha2 = cancha(escenario, "Cancha 2", EstadoCancha.DISPONIBLE);
		Cancha cancha1 = cancha(escenario, "Cancha 1", EstadoCancha.DISPONIBLE);

		PerfilEscenario perfil = casoUso.ejecutar(escenario.getId());

		assertThat(perfil.id()).isEqualTo(escenario.getId());
		assertThat(perfil.nombre()).isEqualTo("Cancha La 80");
		assertThat(perfil.descripcion()).isEqualTo("Grama sintética nueva");
		assertThat(perfil.direccion()).isEqualTo("Calle 80 # 45-10");
		assertThat(perfil.ciudad()).isEqualTo("Rionegro");
		assertThat(perfil.departamento()).isEqualTo("Antioquia");
		assertThat(perfil.latitud()).isEqualByComparingTo("6.155123");
		assertThat(perfil.longitud()).isEqualByComparingTo("-75.373700");
		assertThat(perfil.telefonoContacto()).isEqualTo("+576045551234");
		assertThat(perfil.correoContacto()).isEqualTo("contacto@la80.com");
		assertThat(perfil.fotos()).containsExactly("https://fotos/1.jpg");
		// Ordenadas por nombre, con su id para ir a la disponibilidad (CA5)
		assertThat(perfil.canchas()).extracting(CanchaPerfil::id, CanchaPerfil::nombre, CanchaPerfil::tipoCancha)
				.containsExactly(tuple(cancha1.getId(), "Cancha 1", "Fútbol 5"),
						tuple(cancha2.getId(), "Cancha 2", "Fútbol 5"));
		assertThat(perfil.canchas()).extracting(CanchaPerfil::precioHora)
				.allSatisfy(precio -> assertThat(precio).isEqualByComparingTo("80000"));
	}

	@Test
	void ca1LasCanchasInactivasNoAparecen() {
		Escenario escenario = escenario(EstadoEscenario.ACTIVO, List.of());
		conHorario(escenario);
		cancha(escenario, "Cancha 1", EstadoCancha.DISPONIBLE);
		cancha(escenario, "Cancha 2", EstadoCancha.INACTIVA);

		assertThat(casoUso.ejecutar(escenario.getId()).canchas()).extracting(CanchaPerfil::nombre)
				.containsExactly("Cancha 1");
	}

	@Test
	void ca1UnEscenarioSinHorariosMuestraSuPerfilSinCanchas() {
		// Política 11 de Cancha (P2 de la especificación)
		Escenario escenario = escenario(EstadoEscenario.ACTIVO, List.of());
		cancha(escenario, "Cancha 1", EstadoCancha.DISPONIBLE);

		PerfilEscenario perfil = casoUso.ejecutar(escenario.getId());

		assertThat(perfil.nombre()).isEqualTo("Cancha La 80");
		assertThat(perfil.canchas()).isEmpty();
	}

	@Test
	void ca1ElPromedioSaleDeLasCalificacionesDeTodasSusCanchas() {
		Escenario escenario = escenario(EstadoEscenario.ACTIVO, List.of());
		conHorario(escenario);
		Cancha disponible = cancha(escenario, "Cancha 1", EstadoCancha.DISPONIBLE);
		Cancha inactiva = cancha(escenario, "Cancha 2", EstadoCancha.INACTIVA);
		calificaciones = List.of((short) 4, (short) 5, (short) 4);

		PerfilEscenario perfil = casoUso.ejecutar(escenario.getId());

		assertThat(perfil.calificacionPromedio()).isEqualByComparingTo("4.3");
		// Las reseñas de una cancha que hoy está inactiva también cuentan
		assertThat(canchasConsultadas).singleElement().satisfies(
				ids -> assertThat(ids).containsExactlyInAnyOrder(disponible.getId(), inactiva.getId()));
	}

	@Test
	void ca2SinCalificacionesNoHayPromedio() {
		Escenario escenario = escenario(EstadoEscenario.ACTIVO, List.of());

		assertThat(casoUso.ejecutar(escenario.getId()).calificacionPromedio()).isNull();
	}

	@Test
	void ca3SinFotosLaListaVieneVacia() {
		Escenario escenario = escenario(EstadoEscenario.ACTIVO, List.of());

		assertThat(casoUso.ejecutar(escenario.getId()).fotos()).isEmpty();
	}

	@Test
	void ca4UnEscenarioQueNoExisteResponde_ERR_ESC_06() {
		esperarError(() -> casoUso.ejecutar(UUID.randomUUID()), "ERR-ESC-06");
	}

	@Test
	void ca4UnEscenarioInactivoRespondeIgualQueSiNoExistiera_ERR_ESC_06() {
		Escenario escenario = escenario(EstadoEscenario.INACTIVO, List.of());
		conHorario(escenario);
		cancha(escenario, "Cancha 1", EstadoCancha.DISPONIBLE);

		esperarError(() -> casoUso.ejecutar(escenario.getId()), "ERR-ESC-06");
		// No se pregunta por calificaciones de un escenario que no se va a mostrar
		assertThat(canchasConsultadas).isEmpty();
	}

	private static final class UbicacionFalsa implements ConsultaUbicacion {

		@Override
		public List<Ciudad> buscarCiudades(Collection<UUID> ciudadIds) {
			return ciudadIds.contains(RIONEGRO)
					? List.of(Ciudad.reconstruir(RIONEGRO, "05615", ANTIOQUIA, "Rionegro"))
					: List.of();
		}

		@Override
		public List<Departamento> listarDepartamentos() {
			return List.of(Departamento.reconstruir(UUID.randomUUID(), "08", "Atlántico"),
					Departamento.reconstruir(ANTIOQUIA, "05", "Antioquia"));
		}

		@Override
		public boolean existeCiudad(UUID ciudadId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Ciudad> listarCiudades(UUID departamentoId) {
			throw new UnsupportedOperationException();
		}

	}

}
