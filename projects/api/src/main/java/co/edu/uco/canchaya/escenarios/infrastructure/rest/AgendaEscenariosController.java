package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.canchaya.escenarios.application.port.in.ListarEscenariosAgenda;
import co.edu.uco.canchaya.escenarios.application.port.in.ListarEscenariosAgenda.EscenarioAgenda;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

@RestController
class AgendaEscenariosController {

	private final ListarEscenariosAgenda listarEscenariosAgenda;

	AgendaEscenariosController(ListarEscenariosAgenda listarEscenariosAgenda) {
		this.listarEscenariosAgenda = listarEscenariosAgenda;
	}

	// HU29, CA4 y CA5: exige sesión (AG-11); el usuario sale del token. ERR-GEN-06 sale como 403 (P7)
	@GetMapping("/api/agenda/escenarios")
	List<RespuestaEscenarioAgenda> listar(Solicitante solicitante) {
		return listarEscenariosAgenda.ejecutar(solicitante).stream().map(RespuestaEscenarioAgenda::de).toList();
	}

	/** propietario es falso si el usuario es encargado asignado del escenario. */
	record RespuestaEscenarioAgenda(UUID id, String nombre, EstadoEscenario estado, boolean propietario) {

		static RespuestaEscenarioAgenda de(EscenarioAgenda escenario) {
			return new RespuestaEscenarioAgenda(escenario.id(), escenario.nombre(), escenario.estado(),
					escenario.propietario());
		}

	}

}
