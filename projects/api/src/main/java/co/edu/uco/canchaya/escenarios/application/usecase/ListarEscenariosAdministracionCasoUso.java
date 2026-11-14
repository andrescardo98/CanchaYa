package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ListarEscenariosAdministracion;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios.ResumenEscenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios;
import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios.ContactoUsuario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.ubicacion.application.port.in.ConsultaUbicacion;
import co.edu.uco.canchaya.ubicacion.domain.Ciudad;

@Service
public class ListarEscenariosAdministracionCasoUso implements ListarEscenariosAdministracion {

	private final RepositorioEscenarios repositorio;
	private final ConsultaUbicacion consultaUbicacion;
	private final ConsultaUsuarios consultaUsuarios;

	public ListarEscenariosAdministracionCasoUso(RepositorioEscenarios repositorio,
			ConsultaUbicacion consultaUbicacion, ConsultaUsuarios consultaUsuarios) {
		this.repositorio = repositorio;
		this.consultaUbicacion = consultaUbicacion;
		this.consultaUsuarios = consultaUsuarios;
	}

	@Override
	public List<EscenarioAdministracion> ejecutar(EstadoEscenario estado, Solicitante solicitante) {
		// Restricción de diseño de la HU13: solo el administrador del sistema entra a esta opción
		if (solicitante == null || !solicitante.tieneRol(Rol.ADMINISTRADOR_SISTEMA)) {
			throw new ReglaNegocioException("ERR-GEN-06");
		}

		List<ResumenEscenario> escenarios = repositorio.listar(estado);

		// Una consulta por módulo para toda la lista, no una por escenario
		Map<UUID, String> ciudades = consultaUbicacion
				.buscarCiudades(distintos(escenarios, ResumenEscenario::ciudadId)).stream()
				.collect(Collectors.toMap(Ciudad::getId, Ciudad::getNombre));
		Map<UUID, String> propietarios = consultaUsuarios
				.buscarContactos(distintos(escenarios, ResumenEscenario::administradorId)).stream()
				.collect(Collectors.toMap(ContactoUsuario::id, ContactoUsuario::nombre));

		return escenarios.stream()
				.map(escenario -> new EscenarioAdministracion(escenario.id(), escenario.nombre(),
						ciudades.get(escenario.ciudadId()), propietarios.get(escenario.administradorId()),
						escenario.estado()))
				.toList();
	}

	private static Set<UUID> distintos(List<ResumenEscenario> escenarios, Function<ResumenEscenario, UUID> campo) {
		return escenarios.stream().map(campo).collect(Collectors.toSet());
	}

}
