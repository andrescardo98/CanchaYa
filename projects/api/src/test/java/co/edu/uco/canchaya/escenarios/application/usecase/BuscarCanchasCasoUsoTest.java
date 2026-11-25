package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.uco.canchaya.escenarios.application.port.in.BuscarCanchas.CanchaEncontrada;
import co.edu.uco.canchaya.escenarios.application.port.in.BuscarCanchas.CriteriosBusqueda;
import co.edu.uco.canchaya.escenarios.application.port.in.BuscarCanchas.ResultadoBusqueda;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.RangoBloqueado;
import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaFranjasOcupadas;
import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaFranjasOcupadas.RangoOcupado;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBusquedaCanchas.FiltroBusqueda;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

/** HU18: búsqueda de canchas, con repositorios en memoria. */
class BuscarCanchasCasoUsoTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	/** Lunes 23 de noviembre de 2026, a las 10:00 en Bogotá (D27). */
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 10, 0);
	private static final Clock RELOJ = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
	private static final LocalDate HOY = AHORA.toLocalDate();
	/** El lunes siguiente: ninguna franja ha empezado. */
	private static final LocalDate LUNES_SIGUIENTE = HOY.plusDays(7);

	// PAR-ESC-04 a PAR-ESC-06 y PAR-RES-01. Se pasan como parámetros: los valores reales los lee la configuración
	private static final int RADIO_DEFECTO = 5;
	private static final int RADIO_MAXIMO = 30;
	private static final int RADIO_MINIMO = 1;
	private static final int VENTANA_DIAS = 30;

	private static final BigDecimal LAT = new BigDecimal("6.155100");
	private static final BigDecimal LNG = new BigDecimal("-75.373700");
	private static final UUID ANTIOQUIA = UUID.randomUUID();
	private static final UUID RIONEGRO = UUID.randomUUID();
	private static final UUID F5 = UUID.randomUUID();
	private static final UUID F7 = UUID.randomUUID();

	private CanchasEnMemoria datos;
	private BusquedaEnMemoria busqueda;
	private HorariosEnMemoria horarios;
	private final Map<UUID, List<RangoBloqueado>> bloqueos = new HashMap<>();
	private final Map<UUID, List<RangoOcupado>> reservas = new HashMap<>();
	private final List<LocalDate> fechasConsultadasEnReservas = new ArrayList<>();
	private List<Short> calificaciones;
	private BuscarCanchasCasoUso casoUso;

	@BeforeEach
	void preparar() {
		datos = new CanchasEnMemoria();
		busqueda = new BusquedaEnMemoria();
		horarios = new HorariosEnMemoria();
		calificaciones = List.of();
		casoUso = casoUso(RADIO_MAXIMO);
	}

	private BuscarCanchasCasoUso casoUso(int radioMaximo) {
		ConsultaFranjasOcupadas franjasOcupadas = (canchaIds, fecha) -> {
			fechasConsultadasEnReservas.add(fecha);
			Map<UUID, List<RangoOcupado>> porCancha = new HashMap<>(reservas);
			porCancha.keySet().retainAll(canchaIds);
			return porCancha;
		};
		return new BuscarCanchasCasoUso(busqueda, horarios,
				new BloqueosEnMemoria((canchaId, fecha) -> bloqueos.getOrDefault(canchaId, List.of())),
				franjasOcupadas, datos.repositorioEscenarios(), canchaIds -> calificaciones, RELOJ,
				RADIO_DEFECTO, radioMaximo, RADIO_MINIMO, VENTANA_DIAS);
	}

	// --- Datos de prueba ---------------------------------------------------------------------------

	private Escenario escenario(String nombre, EstadoEscenario estado, double distanciaMetros) {
		Escenario escenario = Escenario.reconstruir(UUID.randomUUID(), nombre, null, "Calle 1 # 2-3", RIONEGRO,
				new BigDecimal("6.160000"), new BigDecimal("-75.370000"), "+573001234567", null, List.of(),
				UUID.randomUUID(), estado);
		datos.escenarios.put(escenario.getId(), escenario);
		busqueda.ubicar(escenario, distanciaMetros);
		return escenario;
	}

	/** Escenario ACTIVO que atiende los lunes de 08:00 a 12:00. */
	private Escenario escenarioConHorario(String nombre, double distanciaMetros) {
		Escenario escenario = escenario(nombre, EstadoEscenario.ACTIVO, distanciaMetros);
		horario(escenario, 8, 12);
		return escenario;
	}

	private void horario(Escenario escenario, int desde, int hasta) {
		horarios.guardados.add(HorarioRecurrente.reconstruir(UUID.randomUUID(), escenario.getId(), DiaSemana.LUNES,
				LocalTime.of(desde, 0), LocalTime.of(hasta, 0)));
	}

	private Cancha cancha(Escenario escenario, String nombre, EstadoCancha estado, UUID tipo) {
		Cancha cancha = Cancha.reconstruir(UUID.randomUUID(), escenario.getId(), tipo, nombre,
				new BigDecimal("80000.00"), estado);
		datos.canchas.add(cancha);
		busqueda.agregar(cancha, escenario, tipo.equals(F5) ? "Fútbol 5" : "Fútbol 7", ANTIOQUIA);
		return cancha;
	}

	private Cancha cancha(Escenario escenario, String nombre) {
		return cancha(escenario, nombre, EstadoCancha.DISPONIBLE, F5);
	}

	private void reserva(Cancha cancha, int desde, int hasta) {
		reservas.computeIfAbsent(cancha.getId(), id -> new ArrayList<>())
				.add(new RangoOcupado(LocalTime.of(desde, 0), LocalTime.of(hasta, 0)));
	}

	private void bloqueo(Cancha cancha, int desde, int hasta) {
		bloqueos.computeIfAbsent(cancha.getId(), id -> new ArrayList<>())
				.add(new RangoBloqueado(LocalTime.of(desde, 0), LocalTime.of(hasta, 0)));
	}

	// --- Criterios ---------------------------------------------------------------------------------

	private static CriteriosBusqueda sinFiltros() {
		return new CriteriosBusqueda(null, null, null, null, null, null, null, null);
	}

	private static CriteriosBusqueda conUbicacion(Integer radioKm) {
		return new CriteriosBusqueda(LAT, LNG, radioKm, null, null, null, null, null);
	}

	private static CriteriosBusqueda conFechaYHora(LocalDate fecha, LocalTime hora) {
		return new CriteriosBusqueda(null, null, null, null, null, null, fecha, hora);
	}

	private static List<String> nombres(ResultadoBusqueda resultado) {
		return resultado.canchas().stream().map(c -> c.nombreEscenario() + " / " + c.nombreCancha()).toList();
	}

	private static ReglaNegocioException esperarError(Runnable accion, String codigo) {
		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(accion::run)
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
		return error;
	}

	// --- CA1 y RN-04 -------------------------------------------------------------------------------

	// CA1: con ubicación y radio salen las canchas dentro del radio, de la más cercana a la más lejana, con su
	// distancia en km con un decimal
	@Test
	void ca1ConUbicacion_dentroDelRadioYOrdenadasPorDistancia() {
		cancha(escenarioConHorario("Lejos", 8_049), "Cancha 1");
		cancha(escenarioConHorario("Cerca", 2_351), "Cancha 1");
		cancha(escenarioConHorario("Fuera", 15_000), "Cancha 1");

		ResultadoBusqueda resultado = casoUso.ejecutar(conUbicacion(10));

		assertThat(resultado.canchas()).extracting(CanchaEncontrada::nombreEscenario, CanchaEncontrada::distanciaKm)
				.containsExactly(tuple("Cerca", new BigDecimal("2.4")), tuple("Lejos", new BigDecimal("8.0")));
		assertThat(resultado.conUbicacion()).isTrue();
		assertThat(resultado.radioKm()).isEqualTo(10);
		assertThat(busqueda.filtrosRecibidos).extracting(FiltroBusqueda::radioMetros).containsExactly(10_000);
	}

	// CA1 y P9: cada resultado trae lo que muestra la pantalla, el escenarioId para ir al perfil (CA8) y las
	// coordenadas del escenario para el mapa
	@Test
	void ca1CadaResultadoTraeLoQueMuestraLaPantalla() {
		Escenario escenario = escenarioConHorario("La 70", 1_000);
		Cancha cancha = cancha(escenario, "Cancha 1");

		CanchaEncontrada encontrada = casoUso.ejecutar(conUbicacion(null)).canchas().getFirst();

		assertThat(encontrada).isEqualTo(new CanchaEncontrada(escenario.getId(), "La 70", escenario.getLatitud(),
				escenario.getLongitud(), cancha.getId(), "Cancha 1", "Fútbol 5", new BigDecimal("80000.00"),
				new BigDecimal("1.0"), null));
	}

	// CA1, política 11 de Cancha (RN-04): no salen la cancha INACTIVA, las del escenario INACTIVO ni las del
	// escenario sin horarios
	@Test
	void ca1SoloCanchasQueCumplenRN04() {
		Escenario activo = escenarioConHorario("Activo", 1_000);
		cancha(activo, "Disponible");
		cancha(activo, "Inactiva", EstadoCancha.INACTIVA, F5);
		Escenario inactivo = escenario("Inactivo", EstadoEscenario.INACTIVO, 1_000);
		horario(inactivo, 8, 12);
		cancha(inactivo, "Cancha 1");
		cancha(escenario("Sin horarios", EstadoEscenario.ACTIVO, 1_000), "Cancha 1");

		assertThat(nombres(casoUso.ejecutar(sinFiltros()))).containsExactly("Activo / Disponible");
	}

	// RN-04 pide «al menos un horario» de cualquier día, no del día de hoy
	@Test
	void ca1UnHorarioDeOtroDiaBastaParaPublicar() {
		Escenario escenario = escenario("Solo sábados", EstadoEscenario.ACTIVO, 1_000);
		horarios.guardados.add(HorarioRecurrente.reconstruir(UUID.randomUUID(), escenario.getId(), DiaSemana.SABADO,
				LocalTime.of(8, 0), LocalTime.of(10, 0)));
		cancha(escenario, "Cancha 1");

		assertThat(nombres(casoUso.ejecutar(sinFiltros()))).containsExactly("Solo sábados / Cancha 1");
	}

	// D05 y AG-20: sin reseñas la calificación es vacía, nunca cero; con reseñas, con un decimal
	@Test
	void ca1CalificacionPromedioDelEscenario() {
		cancha(escenarioConHorario("La 70", 1_000), "Cancha 1");
		assertThat(casoUso.ejecutar(sinFiltros()).canchas().getFirst().calificacionPromedio()).isNull();

		calificaciones = List.of((short) 4, (short) 5, (short) 4);
		assertThat(casoUso.ejecutar(sinFiltros()).canchas().getFirst().calificacionPromedio())
				.isEqualByComparingTo("4.3");
	}

	// --- CA2 y CA3: sin ubicación, por departamento y ciudad ---------------------------------------

	// CA2: sin ubicación busca sin radio, sin distancia y ordena por nombre del escenario y de la cancha
	@Test
	void ca2SinUbicacion_sinDistanciaYOrdenadasPorNombre() {
		Escenario zeta = escenarioConHorario("Zeta", 1_000);
		cancha(zeta, "Cancha 2");
		cancha(zeta, "Cancha 1");
		cancha(escenarioConHorario("Alfa", 20_000), "Cancha 1");

		ResultadoBusqueda resultado = casoUso.ejecutar(sinFiltros());

		assertThat(nombres(resultado)).containsExactly("Alfa / Cancha 1", "Zeta / Cancha 1", "Zeta / Cancha 2");
		assertThat(resultado.canchas()).extracting(CanchaEncontrada::distanciaKm).containsOnlyNulls();
		assertThat(resultado.conUbicacion()).isFalse();
		assertThat(resultado.radioKm()).isNull();
		assertThat(busqueda.filtrosRecibidos.getFirst().conUbicacion()).isFalse();
	}

	// CA2: la ubicación son dos datos; si llega uno sin el otro falta el otro
	@Test
	void ca2LatitudSinLongitud_ERR_GEN_01() {
		ReglaNegocioException error = esperarError(() -> casoUso.ejecutar(
				new CriteriosBusqueda(LAT, null, null, null, null, null, null, null)), "ERR-GEN-01");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("campo.longitud"));

		error = esperarError(() -> casoUso.ejecutar(
				new CriteriosBusqueda(null, LNG, null, null, null, null, null, null)), "ERR-GEN-01");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("campo.latitud"));
	}

	// Coordenadas imposibles del jugador: ERR-GEN-04 con su rango
	@Test
	void coordenadasFueraDeRango_ERR_GEN_04() {
		ReglaNegocioException error = esperarError(() -> casoUso.ejecutar(
				new CriteriosBusqueda(new BigDecimal("91"), LNG, null, null, null, null, null, null)), "ERR-GEN-04");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("campo.latitud"), -90, 90);

		error = esperarError(() -> casoUso.ejecutar(
				new CriteriosBusqueda(LAT, new BigDecimal("-180.5"), null, null, null, null, null, null)), "ERR-GEN-04");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("campo.longitud"), -180, 180);
		assertThat(busqueda.filtrosRecibidos).isEmpty();
	}

	// CA3 y CA4: los filtros de departamento, ciudad y tipo llegan al repositorio, que es el que filtra
	@Test
	void ca3Ca4LosFiltrosLleganAlRepositorio() {
		UUID ciudad = UUID.randomUUID();
		casoUso.ejecutar(new CriteriosBusqueda(LAT, LNG, 3, ANTIOQUIA, ciudad, F7, null, null));

		assertThat(busqueda.filtrosRecibidos).containsExactly(
				new FiltroBusqueda(LAT, LNG, 3_000, ANTIOQUIA, ciudad, F7));
	}

	// CA3: con departamento y ciudad salen solo las canchas de ahí
	@Test
	void ca3PorDepartamentoYCiudad_soloLasDeAhi() {
		cancha(escenarioConHorario("En Rionegro", 1_000), "Cancha 1");

		assertThat(nombres(casoUso.ejecutar(new CriteriosBusqueda(null, null, null, ANTIOQUIA, RIONEGRO, null, null,
				null)))).containsExactly("En Rionegro / Cancha 1");
		assertThat(casoUso.ejecutar(new CriteriosBusqueda(null, null, null, ANTIOQUIA, UUID.randomUUID(), null, null,
				null)).canchas()).isEmpty();
	}

	// CA4: con un tipo salen solo las canchas de ese tipo, aunque el escenario tenga de otros
	@Test
	void ca4PorTipo_soloLasDeEseTipo() {
		Escenario escenario = escenarioConHorario("La 70", 1_000);
		cancha(escenario, "Cancha 5", EstadoCancha.DISPONIBLE, F5);
		cancha(escenario, "Cancha 7", EstadoCancha.DISPONIBLE, F7);

		assertThat(nombres(casoUso.ejecutar(new CriteriosBusqueda(null, null, null, null, null, F7, null, null))))
				.containsExactly("La 70 / Cancha 7");
	}

	// --- CA5: disponibilidad -------------------------------------------------------------------------

	// CA5: con fecha y hora no sale la cancha con reserva CONFIRMADA en esa franja, ni la bloqueada, ni la de un
	// escenario que no atiende a esa hora. Una reserva o un bloqueo en otra hora no estorban
	@Test
	void ca5FechaYHora_soloConLaFranjaLibre() {
		Escenario escenario = escenarioConHorario("La 70", 1_000);
		Cancha libre = cancha(escenario, "Libre");
		reserva(libre, 8, 9);
		bloqueo(libre, 11, 12);
		reserva(cancha(escenario, "Reservada"), 9, 11);
		bloqueo(cancha(escenario, "Bloqueada"), 10, 11);
		Escenario tarde = escenario("Solo tarde", EstadoEscenario.ACTIVO, 1_000);
		horario(tarde, 14, 22);
		cancha(tarde, "Cancha 1");

		ResultadoBusqueda resultado = casoUso.ejecutar(conFechaYHora(LUNES_SIGUIENTE, LocalTime.of(10, 0)));

		assertThat(nombres(resultado)).containsExactly("La 70 / Libre");
		assertThat(fechasConsultadasEnReservas).containsExactly(LUNES_SIGUIENTE);
	}

	// CA5 y política 12: hoy a las 10:00 la franja de las 10:00 ya empezó y no se ofrece; la de las 11:00 sí
	@Test
	void ca5HoyHoraQueYaEmpezo_noSale() {
		cancha(escenarioConHorario("La 70", 1_000), "Cancha 1");

		assertThat(casoUso.ejecutar(conFechaYHora(HOY, LocalTime.of(10, 0))).canchas()).isEmpty();
		assertThat(casoUso.ejecutar(conFechaYHora(HOY, LocalTime.of(11, 0))).canchas()).hasSize(1);
	}

	// CA5: sin fecha ni hora no se filtra por disponibilidad ni se consultan reservas
	@Test
	void ca5SinFechaNiHora_noFiltraPorDisponibilidad() {
		Cancha ocupada = cancha(escenarioConHorario("La 70", 1_000), "Cancha 1");
		reserva(ocupada, 8, 12);

		assertThat(casoUso.ejecutar(sinFiltros()).canchas()).hasSize(1);
		assertThat(fechasConsultadasEnReservas).isEmpty();
	}

	// CA5 y P2: con solo la fecha sale la cancha con al menos una franja libre; no la que tiene todo ocupado
	// entre reservas y bloqueos, ni la de un escenario que no atiende ese día
	@Test
	void ca5SoloFecha_alMenosUnaFranjaLibre() {
		Escenario escenario = escenarioConHorario("La 70", 1_000);
		Cancha unaLibre = cancha(escenario, "Una libre");
		reserva(unaLibre, 8, 10);
		bloqueo(unaLibre, 10, 11);
		Cancha llena = cancha(escenario, "Llena");
		reserva(llena, 8, 10);
		bloqueo(llena, 10, 12);
		Escenario sabados = escenario("Solo sábados", EstadoEscenario.ACTIVO, 1_000);
		horarios.guardados.add(HorarioRecurrente.reconstruir(UUID.randomUUID(), sabados.getId(), DiaSemana.SABADO,
				LocalTime.of(8, 0), LocalTime.of(10, 0)));
		cancha(sabados, "Cancha 1");

		assertThat(nombres(casoUso.ejecutar(conFechaYHora(LUNES_SIGUIENTE, null)))).containsExactly("La 70 / Una libre");
	}

	// CA5: la hora sin la fecha no dice qué día buscar
	@Test
	void ca5HoraSinFecha_ERR_GEN_01() {
		ReglaNegocioException error = esperarError(
				() -> casoUso.ejecutar(conFechaYHora(null, LocalTime.of(10, 0))), "ERR-GEN-01");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("campo.fecha"));
	}

	// CA5 y P5: las franjas empiezan en punto; 14:30 no es una hora de inicio
	@Test
	void ca5HoraNoEnPunto_ERR_GEN_02() {
		ReglaNegocioException error = esperarError(
				() -> casoUso.ejecutar(conFechaYHora(LUNES_SIGUIENTE, LocalTime.of(14, 30))), "ERR-GEN-02");
		assertThat(error.getParametros())
				.containsExactly(new TextoCatalogo("campo.hora"), new TextoCatalogo("formato.hora"));
	}

	// --- CA6, CA7, CA10 y CA11 -----------------------------------------------------------------------

	// CA6: una fecha fuera de la ventana se rechaza antes de buscar, aunque no haya ninguna cancha
	@ParameterizedTest
	@ValueSource(ints = { -1, 31 })
	void ca6FechaFueraDeVentana_ERR_HOR_10(int dias) {
		esperarError(() -> casoUso.ejecutar(conFechaYHora(HOY.plusDays(dias), null)), "ERR-HOR-10");
		assertThat(busqueda.filtrosRecibidos).isEmpty();
	}

	// CA6: la respuesta trae hoy y hoy + 30 para que el selector de fecha no deje elegir otras
	@Test
	void ca6LaRespuestaTraeLasFechasMinimaYMaxima() {
		ResultadoBusqueda resultado = casoUso.ejecutar(conFechaYHora(HOY.plusDays(30), null));

		assertThat(resultado.fechaMinima()).isEqualTo(HOY);
		assertThat(resultado.fechaMaxima()).isEqualTo(HOY.plusDays(30));
	}

	// CA7: ninguna cancha cumple los filtros; la lista sale vacía (INF-ESC-01 lo agrega el controlador)
	@Test
	void ca7SinResultados_listaVacia() {
		cancha(escenarioConHorario("Lejos", 20_000), "Cancha 1");

		assertThat(casoUso.ejecutar(conUbicacion(10)).canchas()).isEmpty();
	}

	// CA10: con ubicación y sin radio se usa PAR-ESC-04 (5 km): el escenario a 7 km no sale
	@Test
	void ca10SinRadio_usaElRadioPorDefecto() {
		cancha(escenarioConHorario("A 4 km", 4_000), "Cancha 1");
		cancha(escenarioConHorario("A 7 km", 7_000), "Cancha 1");

		ResultadoBusqueda resultado = casoUso.ejecutar(conUbicacion(null));

		assertThat(nombres(resultado)).containsExactly("A 4 km / Cancha 1");
		assertThat(resultado.radioKm()).isEqualTo(RADIO_DEFECTO);
		assertThat(busqueda.filtrosRecibidos).extracting(FiltroBusqueda::radioMetros).containsExactly(5_000);
	}

	// CA11: el radio va de PAR-ESC-06 (1 km) a PAR-ESC-05 (30 km); fuera de ahí no se busca. P3: se valida
	// también sin ubicación
	@ParameterizedTest
	@ValueSource(ints = { 0, 31, 100, -5 })
	void ca11RadioFueraDeRango_ERR_GEN_04(int radio) {
		ReglaNegocioException error = esperarError(() -> casoUso.ejecutar(conUbicacion(radio)), "ERR-GEN-04");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("campo.radioKm"), 1, 30);

		esperarError(() -> casoUso.ejecutar(new CriteriosBusqueda(null, null, radio, null, null, null, null, null)),
				"ERR-GEN-04");
		assertThat(busqueda.filtrosRecibidos).isEmpty();
	}

	// CA11: 1 y 30 km son los bordes y se aceptan; los límites viajan en la respuesta para la pantalla
	@ParameterizedTest
	@ValueSource(ints = { 1, 30 })
	void ca11BordesDelRadio_seAceptan(int radio) {
		ResultadoBusqueda resultado = casoUso.ejecutar(conUbicacion(radio));

		assertThat(resultado.radioKm()).isEqualTo(radio);
		assertThat(resultado.radioMinimoKm()).isEqualTo(1);
		assertThat(resultado.radioMaximoKm()).isEqualTo(30);
	}

	// P3: sin ubicación el radio no filtra y no se devuelve
	@Test
	void p3RadioSinUbicacion_noFiltra() {
		cancha(escenarioConHorario("A 20 km", 20_000), "Cancha 1");

		ResultadoBusqueda resultado = casoUso.ejecutar(new CriteriosBusqueda(null, null, 2, null, null, null, null, null));

		assertThat(resultado.canchas()).hasSize(1);
		assertThat(resultado.radioKm()).isNull();
	}

	// Regla 7: cambiar PAR-ESC-05 en la configuración cambia el máximo sin tocar el código
	@Test
	void parametrosDeRadio_seLeenDeLaConfiguracion() {
		BuscarCanchasCasoUso conMaximoDe50 = casoUso(50);

		assertThat(conMaximoDe50.ejecutar(conUbicacion(40)).radioKm()).isEqualTo(40);
		ReglaNegocioException error = esperarError(() -> conMaximoDe50.ejecutar(conUbicacion(51)), "ERR-GEN-04");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("campo.radioKm"), 1, 50);
	}

}
