package co.edu.uco.canchaya.escenarios.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.BuscarCanchas;
import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas.RangoBloqueado;
import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaCalificaciones;
import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaFranjasOcupadas;
import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaFranjasOcupadas.RangoOcupado;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBloqueos;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBusquedaCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBusquedaCanchas.Candidata;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBusquedaCanchas.FiltroBusqueda;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioHorarios;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

/**
 * HU18. Solo lee: no guarda nada. Sin sesión (AG-11), así que no recibe el Solicitante. Todas las
 * validaciones van antes de consultar las canchas (CA6 y CA11: «no se ejecuta la búsqueda»).
 */
@Service
public class BuscarCanchasCasoUso implements BuscarCanchas {

	/** Rango de las coordenadas geográficas. No son parámetros del catálogo: son los de la Tierra. */
	private static final int LATITUD_MAXIMA = 90;
	private static final int LONGITUD_MAXIMA = 180;
	private static final int METROS_POR_KM = 1_000;

	private final RepositorioBusquedaCanchas repositorioBusqueda;
	private final RepositorioHorarios repositorioHorarios;
	private final RepositorioBloqueos repositorioBloqueos;
	private final ConsultaFranjasOcupadas consultaFranjasOcupadas;
	private final RepositorioEscenarios repositorioEscenarios;
	private final ConsultaCalificaciones consultaCalificaciones;
	private final Clock reloj;
	private final int radioDefectoKm;
	private final int radioMaximoKm;
	private final int radioMinimoKm;
	private final int ventanaReservaDias;

	public BuscarCanchasCasoUso(RepositorioBusquedaCanchas repositorioBusqueda, RepositorioHorarios repositorioHorarios,
			RepositorioBloqueos repositorioBloqueos, ConsultaFranjasOcupadas consultaFranjasOcupadas,
			RepositorioEscenarios repositorioEscenarios, ConsultaCalificaciones consultaCalificaciones, Clock reloj,
			@Value("${canchaya.escenario.radio-busqueda-defecto-km}") int radioDefectoKm,
			@Value("${canchaya.escenario.radio-busqueda-maximo-km}") int radioMaximoKm,
			@Value("${canchaya.escenario.radio-busqueda-minimo-km}") int radioMinimoKm,
			@Value("${canchaya.reserva.ventana-dias}") int ventanaReservaDias) {
		this.repositorioBusqueda = repositorioBusqueda;
		this.repositorioHorarios = repositorioHorarios;
		this.repositorioBloqueos = repositorioBloqueos;
		this.consultaFranjasOcupadas = consultaFranjasOcupadas;
		this.repositorioEscenarios = repositorioEscenarios;
		this.consultaCalificaciones = consultaCalificaciones;
		this.reloj = reloj;
		this.radioDefectoKm = radioDefectoKm;
		this.radioMaximoKm = radioMaximoKm;
		this.radioMinimoKm = radioMinimoKm;
		this.ventanaReservaDias = ventanaReservaDias;
	}

	@Override
	public ResultadoBusqueda ejecutar(CriteriosBusqueda criterios) {
		LocalDateTime ahora = LocalDateTime.now(reloj);
		int radioKm = validar(criterios, ahora);
		boolean conUbicacion = criterios.latitud() != null;

		// CA1, CA3 y CA4: la base filtra por ubicación, radio, departamento, ciudad y tipo, y ordena
		FiltroBusqueda filtro = new FiltroBusqueda(criterios.latitud(), criterios.longitud(),
				conUbicacion ? radioKm * METROS_POR_KM : null, criterios.departamentoId(), criterios.ciudadId(),
				criterios.tipoCanchaId());
		List<Candidata> candidatas = repositorioBusqueda.buscar(filtro);

		Map<UUID, List<HorarioRecurrente>> horariosPorEscenario = repositorioHorarios
				.buscarPorEscenarios(candidatas.stream().map(c -> c.escenario().getId()).distinct().toList())
				.stream().collect(Collectors.groupingBy(HorarioRecurrente::getEscenarioId));

		// Política 11 de Cancha (RN-04, P7): DISPONIBLE, escenario ACTIVO y al menos un horario de cualquier día
		List<Candidata> publicables = candidatas.stream()
				.filter(c -> c.cancha().publicarEnBusqueda(c.escenario(),
						horariosPorEscenario.containsKey(c.escenario().getId())))
				.toList();

		if (criterios.fecha() != null) {
			publicables = conFranjaLibre(publicables, horariosPorEscenario, criterios.fecha(), criterios.hora(), ahora);
		}

		Map<UUID, BigDecimal> calificaciones = new HashMap<>();
		List<CanchaEncontrada> canchas = publicables.stream()
				.map(c -> encontrada(c, calificaciones.computeIfAbsent(c.escenario().getId(), this::calificacion)))
				.toList();

		LocalDate hoy = ahora.toLocalDate();
		return new ResultadoBusqueda(canchas, conUbicacion, conUbicacion ? radioKm : null, radioMinimoKm,
				radioMaximoKm, hoy, hoy.plusDays(ventanaReservaDias));
	}

	/**
	 * Orden: datos completos y legibles (ERR-GEN-01 y 02), rangos (ERR-GEN-04) y fecha (ERR-HOR-10).
	 *
	 * @return el radio que se aplica: el enviado o PAR-ESC-04 (CA10)
	 */
	private int validar(CriteriosBusqueda criterios, LocalDateTime ahora) {
		// CA2: la ubicación son dos datos; si llega uno solo, falta el otro
		if (criterios.latitud() != null && criterios.longitud() == null) {
			throw new ReglaNegocioException("ERR-GEN-01", new TextoCatalogo("campo.longitud"));
		}
		if (criterios.longitud() != null && criterios.latitud() == null) {
			throw new ReglaNegocioException("ERR-GEN-01", new TextoCatalogo("campo.latitud"));
		}
		// CA5: la hora sola no dice qué día buscar
		if (criterios.hora() != null && criterios.fecha() == null) {
			throw new ReglaNegocioException("ERR-GEN-01", new TextoCatalogo("campo.fecha"));
		}
		// CA5 y P5: una franja empieza en punto (PAR-HOR-02)
		if (criterios.hora() != null && !criterios.hora().equals(criterios.hora().withMinute(0).withSecond(0).withNano(0))) {
			throw new ReglaNegocioException("ERR-GEN-02", new TextoCatalogo("campo.hora"),
					new TextoCatalogo("formato.hora"));
		}

		if (criterios.latitud() != null) {
			validarRango(criterios.latitud(), LATITUD_MAXIMA, "campo.latitud");
			validarRango(criterios.longitud(), LONGITUD_MAXIMA, "campo.longitud");
		}
		// CA10 y CA11 (PAR-ESC-04 a PAR-ESC-06). P3: se valida aunque no haya ubicación
		int radioKm = criterios.radioKm() != null ? criterios.radioKm() : radioDefectoKm;
		if (radioKm < radioMinimoKm || radioKm > radioMaximoKm) {
			throw new ReglaNegocioException("ERR-GEN-04", new TextoCatalogo("campo.radioKm"), radioMinimoKm,
					radioMaximoKm);
		}

		// CA6: política 10 de HorarioRecurrente, aunque no haya ninguna cancha candidata
		if (criterios.fecha() != null) {
			HorarioRecurrente.validarFechaConsultable(criterios.fecha(), ahora, ventanaReservaDias);
		}
		return radioKm;
	}

	private static void validarRango(BigDecimal valor, int maximo, String campo) {
		if (valor.abs().compareTo(BigDecimal.valueOf(maximo)) > 0) {
			throw new ReglaNegocioException("ERR-GEN-04", new TextoCatalogo(campo), -maximo, maximo);
		}
	}

	/**
	 * CA5 y P2: deja las canchas con al menos una franja libre en la fecha; si hay hora, la franja que
	 * empieza a esa hora. Las reservas CONFIRMADAS y los bloqueos se piden de una vez para todas.
	 */
	private List<Candidata> conFranjaLibre(List<Candidata> candidatas,
			Map<UUID, List<HorarioRecurrente>> horariosPorEscenario, LocalDate fecha, LocalTime hora,
			LocalDateTime ahora) {
		if (candidatas.isEmpty()) {
			return candidatas;
		}
		List<UUID> canchaIds = candidatas.stream().map(c -> c.cancha().getId()).toList();
		Map<UUID, List<RangoOcupado>> reservas = consultaFranjasOcupadas.confirmadasDelDia(canchaIds, fecha);
		Map<UUID, List<RangoBloqueado>> bloqueos = repositorioBloqueos.rangosDelDia(canchaIds, fecha);

		Map<UUID, List<Franja>> franjasPorEscenario = new HashMap<>();
		return candidatas.stream().filter(c -> {
			// Políticas 11 y 12 de HorarioRecurrente: franjas del horario de ese día que todavía no empiezan
			List<Franja> franjas = franjasPorEscenario.computeIfAbsent(c.escenario().getId(),
					id -> HorarioRecurrente.calcularFranjas(horariosPorEscenario.getOrDefault(id, List.of()), fecha,
							ahora, ventanaReservaDias));
			List<RangoOcupado> reservasCancha = reservas.getOrDefault(c.cancha().getId(), List.of());
			List<RangoBloqueado> bloqueosCancha = bloqueos.getOrDefault(c.cancha().getId(), List.of());
			return franjas.stream()
					.filter(f -> hora == null || f.horaInicio().equals(hora))
					.anyMatch(f -> reservasCancha.stream().noneMatch(r -> f.seCruzaCon(fecha, r.horaInicio(), r.horaFin()))
							&& bloqueosCancha.stream().noneMatch(b -> f.seCruzaCon(fecha, b.horaInicio(), b.horaFin())));
		}).toList();
	}

	// D05 y AG-20, como en HU19: con todas las canchas del escenario; sin reseñas, vacía (null) y nunca cero
	private BigDecimal calificacion(UUID escenarioId) {
		List<Short> calificaciones = consultaCalificaciones
				.calificacionesDeCanchas(repositorioEscenarios.idsCanchas(escenarioId));
		return Escenario.calcularCalificacionPromedio(calificaciones).orElse(null);
	}

	private static CanchaEncontrada encontrada(Candidata candidata, BigDecimal calificacion) {
		Escenario escenario = candidata.escenario();
		// CA1: distancia en km con un decimal
		BigDecimal distanciaKm = candidata.distanciaMetros() == null ? null
				: BigDecimal.valueOf(candidata.distanciaMetros())
						.divide(BigDecimal.valueOf(METROS_POR_KM), 1, RoundingMode.HALF_UP);
		return new CanchaEncontrada(escenario.getId(), escenario.getNombre(), escenario.getLatitud(),
				escenario.getLongitud(), candidata.cancha().getId(), candidata.cancha().getNombre(),
				candidata.nombreTipoCancha(), candidata.cancha().getPrecioHora(), distanciaKm, calificacion);
	}

}
