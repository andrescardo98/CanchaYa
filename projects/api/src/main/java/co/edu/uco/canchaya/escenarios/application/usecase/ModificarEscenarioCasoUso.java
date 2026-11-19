package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.ModificarEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario.FotoAdjunta;
import co.edu.uco.canchaya.escenarios.application.port.out.AlmacenamientoFotos;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.usecase.ValidacionFotos.FotoRevisada;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.ubicacion.application.port.in.ConsultaUbicacion;

/**
 * Sin @Transactional a propósito, como el registro (HU5): subir las fotos tarda y no debe tener
 * ocupada una conexión de la base. El guardado sí es una sola transacción.
 */
@Service
public class ModificarEscenarioCasoUso implements ModificarEscenario {

	private static final Logger LOG = LoggerFactory.getLogger(ModificarEscenarioCasoUso.class);

	private final RepositorioEscenarios repositorio;
	private final ConsultaUbicacion consultaUbicacion;
	private final int maximoFotos;
	private final ValidacionFotos validacionFotos;

	public ModificarEscenarioCasoUso(RepositorioEscenarios repositorio, AlmacenamientoFotos almacenamiento,
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
	public Escenario ejecutar(UUID escenarioId, DatosModificacionEscenario datos, Solicitante solicitante) {
		List<String> conservadas = datos.fotosConservadas() == null ? List.of() : datos.fotosConservadas();
		List<FotoAdjunta> nuevas = datos.fotosNuevas() == null ? List.of() : datos.fotosNuevas();

		// Política 6
		Escenario escenario = repositorio.buscarPorId(escenarioId)
				.orElseThrow(() -> new ReglaNegocioException("ERR-ESC-06"));

		// Todo lo que puede fallar se revisa antes de subir las fotos, para no dejarlas sueltas.
		// La 7 va primero: quien no es el dueño no averigua nada más del escenario
		escenario.validarPropietario(solicitante); // Política 7
		Escenario.validarUbicacion(datos.latitud(), datos.longitud()); // Política 3
		// Política 2
		if (!consultaUbicacion.existeCiudad(datos.ciudadId())) {
			throw new ReglaNegocioException("ERR-ESC-02");
		}
		validarConservadas(escenario, conservadas);
		// Política 13
		Escenario.validarCantidadFotos(conservadas.size() + nuevas.size(), maximoFotos);
		List<FotoRevisada> revisadas = validacionFotos.revisar(nuevas);

		List<String> anteriores = escenario.getFotos();
		List<String> subidas = validacionFotos.subir(revisadas);
		try {
			List<String> fotos = new ArrayList<>(conservadas);
			fotos.addAll(subidas);
			escenario.modificar(datos.nombre(), datos.descripcion(), datos.direccion(), datos.ciudadId(),
					datos.latitud(), datos.longitud(), datos.telefonoContacto(), datos.correoContacto(), fotos,
					solicitante, maximoFotos);
			repositorio.actualizar(escenario);
		} catch (RuntimeException e) {
			// El cambio no quedó guardado: las fotos nuevas sobran y las anteriores siguen en uso
			validacionFotos.borrar(subidas);
			throw e;
		}

		// Ya quedó guardado: las fotos que se quitaron sobran. Si un borrado falla, solo se registra
		validacionFotos.borrar(anteriores.stream().filter(url -> !conservadas.contains(url)).toList());
		return escenario;
	}

	/**
	 * P1 de la especificación: el formulario solo envía las direcciones de las fotos actuales. Otra
	 * dirección, o una repetida, es una petición manipulada; se rechaza para que no se guarde una
	 * foto de otro escenario ni de otro sitio.
	 */
	private static void validarConservadas(Escenario escenario, List<String> conservadas) {
		Set<String> vistas = new HashSet<>();
		for (String url : conservadas) {
			if (!escenario.getFotos().contains(url) || !vistas.add(url)) {
				LOG.warn("Foto conservada que no es del escenario o repetida: escenarioId={} url={}",
						escenario.getId(), url);
				throw new ReglaNegocioException("ERR-GEN-07");
			}
		}
	}

}
