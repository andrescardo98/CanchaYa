package co.edu.uco.canchaya.escenarios.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBloqueos;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioHorarios;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.ubicacion.application.port.in.ConsultaUbicacion;

@Service
public class ConsultaCanchasCasoUso implements ConsultaCanchas {

	private final RepositorioCanchas repositorioCanchas;
	private final RepositorioEscenarios repositorioEscenarios;
	private final RepositorioHorarios repositorioHorarios;
	private final RepositorioBloqueos repositorioBloqueos;
	private final ConsultaUbicacion consultaUbicacion;
	private final Clock reloj;
	private final int ventanaReservaDias;

	public ConsultaCanchasCasoUso(RepositorioCanchas repositorioCanchas, RepositorioEscenarios repositorioEscenarios,
			RepositorioHorarios repositorioHorarios, RepositorioBloqueos repositorioBloqueos,
			ConsultaUbicacion consultaUbicacion, Clock reloj,
			@Value("${canchaya.reserva.ventana-dias}") int ventanaReservaDias) {
		this.repositorioCanchas = repositorioCanchas;
		this.repositorioEscenarios = repositorioEscenarios;
		this.repositorioHorarios = repositorioHorarios;
		this.repositorioBloqueos = repositorioBloqueos;
		this.consultaUbicacion = consultaUbicacion;
		this.reloj = reloj;
		this.ventanaReservaDias = ventanaReservaDias;
	}

	@Override
	public Optional<CanchaReservable> buscarReservable(UUID canchaId) {
		// Política 1 de Reserva y P2: cancha DISPONIBLE en un escenario ACTIVO
		return repositorioCanchas.buscarPorId(canchaId)
				.filter(Cancha::estaDisponible)
				.flatMap(cancha -> repositorioEscenarios.buscarPorId(cancha.getEscenarioId())
						.filter(escenario -> escenario.estaActivo())
						.map(escenario -> new CanchaReservable(cancha.getId(), escenario.getId(), cancha.getNombre(),
								escenario.getNombre(), cancha.getPrecioHora())));
	}

	@Override
	public Optional<DatosCorreoCancha> buscarDatosCorreo(UUID canchaId) {
		// HU22, P2: sin filtrar por estado, a diferencia de buscarReservable
		return repositorioCanchas.buscarPorId(canchaId)
				.flatMap(cancha -> repositorioEscenarios.buscarPorId(cancha.getEscenarioId())
						.flatMap(escenario -> consultaUbicacion.buscarCiudades(List.of(escenario.getCiudadId()))
								.stream().findFirst()
								.map(ciudad -> new DatosCorreoCancha(cancha.getNombre(), escenario.getNombre(),
										escenario.getDireccion(), ciudad.getNombre(), escenario.getAdministradorId()))));
	}

	@Override
	public List<Franja> franjasDelDia(UUID canchaId, LocalDate fecha) {
		// Política 9 de HorarioRecurrente. Es el respaldo: quien llama ya revisó ERR-RES-01 con
		// buscarReservable (nota de ERR-HOR-09 en el catálogo de mensajes)
		Cancha cancha = repositorioCanchas.buscarPorId(canchaId)
				.orElseThrow(() -> new ReglaNegocioException("ERR-HOR-09"));
		List<HorarioRecurrente> horarios = repositorioHorarios.buscarPorEscenarioYDia(cancha.getEscenarioId(),
				DiaSemana.de(fecha.getDayOfWeek()));
		return HorarioRecurrente.calcularFranjas(horarios, fecha, LocalDateTime.now(reloj), ventanaReservaDias);
	}

	@Override
	public List<RangoBloqueado> bloqueosDelDia(UUID canchaId, LocalDate fecha) {
		return repositorioBloqueos.rangosDelDia(canchaId, fecha);
	}

	@Override
	public void tomarTurno(UUID canchaId) {
		// La transacción la abre quien llama (HU21); el repositorio exige que exista
		repositorioCanchas.tomarTurno(canchaId);
	}

	@Override
	public Map<UUID, NombresCancha> nombres(Collection<UUID> canchaIds) {
		// HU23: sin filtrar por estado; una reserva pasada puede ser de una cancha que hoy está inactiva
		return repositorioCanchas.nombresConEscenario(canchaIds);
	}

}
