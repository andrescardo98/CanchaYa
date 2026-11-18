package co.edu.uco.canchaya.escenarios.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ConsultarPerfilEscenario;
import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaCalificaciones;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioHorarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioTiposCancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.TipoCancha;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.ubicacion.application.port.in.ConsultaUbicacion;
import co.edu.uco.canchaya.ubicacion.domain.Ciudad;
import co.edu.uco.canchaya.ubicacion.domain.Departamento;

/**
 * HU19. Solo lee: no guarda nada. Sin sesión (AG-11), así que no recibe el Solicitante.
 */
@Service
public class ConsultarPerfilEscenarioCasoUso implements ConsultarPerfilEscenario {

	private final RepositorioEscenarios escenarios;
	private final RepositorioCanchas canchas;
	private final RepositorioHorarios horarios;
	private final RepositorioTiposCancha tiposCancha;
	private final ConsultaUbicacion consultaUbicacion;
	private final ConsultaCalificaciones consultaCalificaciones;

	public ConsultarPerfilEscenarioCasoUso(RepositorioEscenarios escenarios, RepositorioCanchas canchas,
			RepositorioHorarios horarios, RepositorioTiposCancha tiposCancha, ConsultaUbicacion consultaUbicacion,
			ConsultaCalificaciones consultaCalificaciones) {
		this.escenarios = escenarios;
		this.canchas = canchas;
		this.horarios = horarios;
		this.tiposCancha = tiposCancha;
		this.consultaUbicacion = consultaUbicacion;
		this.consultaCalificaciones = consultaCalificaciones;
	}

	@Override
	public PerfilEscenario ejecutar(UUID escenarioId) {
		// CA4: si no existe (política 6) o está INACTIVO (política 10, RN-04) se responde lo mismo,
		// para que nadie pueda averiguar qué escenarios existen sin estar publicados
		Escenario escenario = escenarios.buscarPorId(escenarioId)
				.filter(Escenario::estaActivo)
				.orElseThrow(() -> new ReglaNegocioException("ERR-ESC-06"));

		Ciudad ciudad = ciudad(escenario);
		return new PerfilEscenario(escenario.getId(), escenario.getNombre(), escenario.getDescripcion(),
				escenario.getDireccion(), ciudad.getNombre(), nombreDepartamento(ciudad),
				escenario.getLatitud(), escenario.getLongitud(), escenario.getTelefonoContacto(),
				escenario.getCorreoContacto(), escenario.getFotos(), canchasPublicables(escenario),
				calificacionPromedio(escenario));
	}

	// CA1: política 11 de Cancha. Los horarios se consultan aquí y llegan al dominio como un valor simple
	private List<CanchaPerfil> canchasPublicables(Escenario escenario) {
		boolean escenarioTieneHorarios = !horarios.buscarPorEscenario(escenario.getId()).isEmpty();
		Map<UUID, String> nombresTipo = tiposCancha.listar().stream()
				.collect(Collectors.toMap(TipoCancha::getId, TipoCancha::getNombre));

		return canchas.listarPorEscenario(escenario.getId()).stream()
				.filter(cancha -> cancha.publicarEnBusqueda(escenario, escenarioTieneHorarios))
				.map(cancha -> new CanchaPerfil(cancha.getId(), cancha.getNombre(),
						nombresTipo.get(cancha.getTipoCanchaId()), cancha.getPrecioHora()))
				.toList();
	}

	// CA1 y CA2 (D05): con todas las canchas del escenario, también las inactivas, porque sus
	// reseñas pasadas siguen contando. Sin calificaciones queda vacío (null), nunca cero
	private BigDecimal calificacionPromedio(Escenario escenario) {
		List<Short> calificaciones = consultaCalificaciones
				.calificacionesDeCanchas(escenarios.idsCanchas(escenario.getId()));
		return Escenario.calcularCalificacionPromedio(calificaciones).orElse(null);
	}

	private String nombreDepartamento(Ciudad ciudad) {
		UUID departamentoId = ciudad.getDepartamentoId();
		return consultaUbicacion.listarDepartamentos().stream()
				.filter(departamento -> departamento.getId().equals(departamentoId))
				.map(Departamento::getNombre)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("No existe el departamento " + departamentoId));
	}

	// La base garantiza que la ciudad exista (llave foránea); si no, es un error inesperado (ERR-GEN-07)
	private Ciudad ciudad(Escenario escenario) {
		return consultaUbicacion.buscarCiudades(List.of(escenario.getCiudadId())).stream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("No existe la ciudad " + escenario.getCiudadId()));
	}

}
