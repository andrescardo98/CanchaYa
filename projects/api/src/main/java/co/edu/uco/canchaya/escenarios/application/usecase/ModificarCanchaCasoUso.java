package co.edu.uco.canchaya.escenarios.application.usecase;

import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ModificarCancha;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioTiposCancha;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/**
 * Mismo orden que el registro (HU6). La cancha modificada solo vive en memoria hasta
 * {@code actualizar}, así que un error en las políticas 4 u 8 no deja nada guardado.
 */
@Service
public class ModificarCanchaCasoUso implements ModificarCancha {

	private final RepositorioEscenarios repositorioEscenarios;
	private final RepositorioCanchas repositorioCanchas;
	private final RepositorioTiposCancha repositorioTipos;

	public ModificarCanchaCasoUso(RepositorioEscenarios repositorioEscenarios, RepositorioCanchas repositorioCanchas,
			RepositorioTiposCancha repositorioTipos) {
		this.repositorioEscenarios = repositorioEscenarios;
		this.repositorioCanchas = repositorioCanchas;
		this.repositorioTipos = repositorioTipos;
	}

	@Override
	public Cancha ejecutar(DatosModificacionCancha datos, Solicitante solicitante) {
		// Política 7
		Cancha cancha = repositorioCanchas.buscarPorId(datos.canchaId())
				.orElseThrow(() -> new ReglaNegocioException("ERR-CAN-07"));
		// La cancha no existe sin su escenario (llave foránea), así que no encontrarlo es un error de datos
		Escenario escenario = repositorioEscenarios.buscarPorId(cancha.getEscenarioId())
				.orElseThrow(() -> new IllegalStateException("La cancha " + cancha.getId() + " no tiene escenario"));

		// Políticas 2 y 5
		cancha.modificar(escenario, datos.tipoCanchaId(), datos.nombre(), datos.precioHora(), solicitante);

		// Política 4
		if (!repositorioTipos.existePorId(cancha.getTipoCanchaId())) {
			throw new ReglaNegocioException("ERR-CAN-04");
		}
		// Política 8
		if (repositorioCanchas.existeNombreEnOtraCancha(escenario.getId(), cancha.getNombre(), cancha.getId())) {
			throw new ReglaNegocioException("ERR-CAN-08", cancha.getNombre());
		}

		repositorioCanchas.actualizar(cancha);
		return cancha;
	}

}
