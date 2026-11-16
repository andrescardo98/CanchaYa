package co.edu.uco.canchaya.escenarios.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarCancha;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioCanchas;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioTiposCancha;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

@Service
public class RegistrarCanchaCasoUso implements RegistrarCancha {

	private static final Logger LOG = LoggerFactory.getLogger(RegistrarCanchaCasoUso.class);

	private final RepositorioEscenarios repositorioEscenarios;
	private final RepositorioCanchas repositorioCanchas;
	private final RepositorioTiposCancha repositorioTipos;

	public RegistrarCanchaCasoUso(RepositorioEscenarios repositorioEscenarios, RepositorioCanchas repositorioCanchas,
			RepositorioTiposCancha repositorioTipos) {
		this.repositorioEscenarios = repositorioEscenarios;
		this.repositorioCanchas = repositorioCanchas;
		this.repositorioTipos = repositorioTipos;
	}

	@Override
	public Cancha ejecutar(DatosRegistroCancha datos, Solicitante solicitante) {
		// Política 1
		Escenario escenario = repositorioEscenarios.buscarPorId(datos.escenarioId())
				.orElseThrow(() -> new ReglaNegocioException("ERR-CAN-01"));

		// Políticas 2 y 5
		Cancha cancha = Cancha.registrar(escenario, datos.tipoCanchaId(), datos.nombre(), datos.precioHora(),
				solicitante);

		// Política 4
		if (!repositorioTipos.existePorId(cancha.getTipoCanchaId())) {
			throw new ReglaNegocioException("ERR-CAN-04");
		}
		// Política 3
		if (repositorioCanchas.existeNombreEnEscenario(escenario.getId(), cancha.getNombre())) {
			throw new ReglaNegocioException("ERR-CAN-03", cancha.getNombre());
		}
		// Política 6. ERR-CAN-06 solo va al log; el usuario no lo ve.
		while (repositorioCanchas.existePorId(cancha.getId())) {
			LOG.warn("ERR-CAN-06 objeto=Cancha id={}", cancha.getId());
			cancha = cancha.conNuevoId();
		}

		repositorioCanchas.guardar(cancha);
		return cancha;
	}

}
