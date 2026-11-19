package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario;
import co.edu.uco.canchaya.escenarios.application.port.out.AlmacenamientoFotos;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.usecase.ValidacionFotos.FotoRevisada;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.ubicacion.application.port.in.ConsultaUbicacion;

/**
 * Sin @Transactional a propósito: subir las fotos tarda, y no debe tener ocupada una conexión
 * de la base mientras tanto (solo hay 5, AC-05.3). El guardado sí es una sola transacción.
 */
@Service
public class RegistrarEscenarioCasoUso implements RegistrarEscenario {

	private static final Logger LOG = LoggerFactory.getLogger(RegistrarEscenarioCasoUso.class);

	private final RepositorioEscenarios repositorio;
	private final ConsultaUbicacion consultaUbicacion;
	private final int maximoFotos;
	private final ValidacionFotos validacionFotos;

	public RegistrarEscenarioCasoUso(RepositorioEscenarios repositorio, AlmacenamientoFotos almacenamiento,
			ConsultaUbicacion consultaUbicacion,
			@Value("${canchaya.escenario.maximo-fotos}") int maximoFotos,
			@Value("${canchaya.escenario.tamano-maximo-foto-mb}") int tamanoMaximoFotoMb,
			@Value("${canchaya.escenario.formatos-foto}") List<String> formatosFoto) {
		this.repositorio = repositorio;
		this.consultaUbicacion = consultaUbicacion;
		this.maximoFotos = maximoFotos;
		this.validacionFotos = new ValidacionFotos(almacenamiento, tamanoMaximoFotoMb, formatosFoto);
	}

	@Override
	public Escenario ejecutar(DatosRegistroEscenario datos, Solicitante solicitante) {
		List<FotoAdjunta> fotos = datos.fotos() == null ? List.of() : datos.fotos();

		// Todo lo que puede fallar se revisa antes de subir las fotos, para no dejarlas sueltas
		Escenario.validarSolicitante(solicitante); // Política 1
		Escenario.validarUbicacion(datos.latitud(), datos.longitud()); // Política 3
		// Política 2
		if (!consultaUbicacion.existeCiudad(datos.ciudadId())) {
			throw new ReglaNegocioException("ERR-ESC-02");
		}
		// Política 13
		Escenario.validarCantidadFotos(fotos.size(), maximoFotos);
		List<FotoRevisada> revisadas = validacionFotos.revisar(fotos);

		List<String> urls = validacionFotos.subir(revisadas);
		try {
			Escenario escenario = Escenario.registrar(datos.nombre(), datos.descripcion(), datos.direccion(),
					datos.ciudadId(), datos.latitud(), datos.longitud(), datos.telefonoContacto(),
					datos.correoContacto(), urls, solicitante, maximoFotos);

			// Política 4. ERR-ESC-04 solo va al log; el usuario no lo ve.
			while (repositorio.existePorId(escenario.getId())) {
				LOG.warn("ERR-ESC-04 objeto=Escenario id={}", escenario.getId());
				escenario = escenario.conNuevoId();
			}

			repositorio.guardar(escenario);
			return escenario;
		} catch (RuntimeException e) {
			// El escenario no quedó guardado: sus fotos sobran
			validacionFotos.borrar(urls);
			throw e;
		}
	}

}
