package co.edu.uco.canchaya.escenarios.application.usecase;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultaEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBloqueos;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEncargados;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioHorarios;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.Franja;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

@Service
public class ConsultaEscenariosCasoUso implements ConsultaEscenarios {

	private final RepositorioEscenarios repositorioEscenarios;
	private final RepositorioEncargados repositorioEncargados;
	private final RepositorioCanchas repositorioCanchas;
	private final RepositorioHorarios repositorioHorarios;
	private final RepositorioBloqueos repositorioBloqueos;

	public ConsultaEscenariosCasoUso(RepositorioEscenarios repositorioEscenarios,
			RepositorioEncargados repositorioEncargados, RepositorioCanchas repositorioCanchas,
			RepositorioHorarios repositorioHorarios, RepositorioBloqueos repositorioBloqueos) {
		this.repositorioEscenarios = repositorioEscenarios;
		this.repositorioEncargados = repositorioEncargados;
		this.repositorioCanchas = repositorioCanchas;
		this.repositorioHorarios = repositorioHorarios;
		this.repositorioBloqueos = repositorioBloqueos;
	}

	@Override
	public DatosAgenda datosAgenda(UUID escenarioId, Solicitante solicitante, LocalDate desde, LocalDate hasta) {
		Objects.requireNonNull(escenarioId, "escenarioId");
		Objects.requireNonNull(desde, "desde");
		Objects.requireNonNull(hasta, "hasta");
		if (hasta.isBefore(desde)) {
			throw new IllegalArgumentException("hasta es anterior a desde: " + desde + " – " + hasta);
		}
		// P7: sin el rol no se consulta nada; quien no lo tiene no puede averiguar qué escenarios existen
		if (solicitante == null || !solicitante.tieneRol(Rol.ADMINISTRADOR_CANCHA)) {
			throw new ReglaNegocioException("ERR-GEN-06");
		}
		// P7: el que no existe responde igual que el ajeno
		Escenario escenario = repositorioEscenarios.buscarPorId(escenarioId)
				.orElseThrow(() -> new ReglaNegocioException("ERR-GEN-06"));
		// CA6 (RN-15): propietario o encargado asignado; antes de leer canchas, horarios o bloqueos
		boolean esEncargadoAsignado = repositorioEncargados.estaAsignado(escenarioId, solicitante.usuarioId());
		boolean propietario = escenario.validarAccesoAgenda(solicitante, esEncargadoAsignado);

		// CA1: todas las canchas, en cualquier estado, por nombre
		List<Cancha> canchas = repositorioCanchas.listarPorEscenario(escenarioId);
		List<CanchaAgenda> canchasAgenda = canchas.stream()
				.map(cancha -> new CanchaAgenda(cancha.getId(), cancha.getNombre(), cancha.getEstado().name()))
				.toList();

		// CA1 y CA2: los horarios se leen una vez y las franjas se calculan para cada fecha (P6)
		List<HorarioRecurrente> horarios = repositorioHorarios.buscarPorEscenario(escenarioId);
		Map<LocalDate, List<Franja>> franjasPorFecha = new LinkedHashMap<>();
		for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
			franjasPorFecha.put(fecha, HorarioRecurrente.franjasDeLaFecha(horarios, fecha));
		}

		// P2: una sola consulta para los bloqueos de todas las canchas
		List<BloqueoAgenda> bloqueos = repositorioBloqueos.deCanchasEntre(
				canchas.stream().map(Cancha::getId).toList(), desde, hasta);

		return new DatosAgenda(escenario.getId(), escenario.getNombre(), propietario, canchasAgenda,
				Collections.unmodifiableMap(franjasPorFecha), bloqueos);
	}

}
