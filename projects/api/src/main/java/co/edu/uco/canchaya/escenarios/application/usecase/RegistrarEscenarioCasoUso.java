package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario;
import co.edu.uco.canchaya.escenarios.application.port.out.AlmacenamientoFotos;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;
import co.edu.uco.canchaya.ubicacion.application.port.in.ConsultaUbicacion;

/**
 * Sin @Transactional a propósito: subir las fotos tarda, y no debe tener ocupada una conexión
 * de la base mientras tanto (solo hay 5, AC-05.3). El guardado sí es una sola transacción.
 */
@Service
public class RegistrarEscenarioCasoUso implements RegistrarEscenario {

	private static final Logger LOG = LoggerFactory.getLogger(RegistrarEscenarioCasoUso.class);
	private static final long BYTES_POR_MB = 1024L * 1024L;

	private final RepositorioEscenarios repositorio;
	private final AlmacenamientoFotos almacenamiento;
	private final ConsultaUbicacion consultaUbicacion;
	private final int maximoFotos;
	private final int tamanoMaximoFotoMb;
	private final Set<FormatoFoto> formatosAdmitidos;

	public RegistrarEscenarioCasoUso(RepositorioEscenarios repositorio, AlmacenamientoFotos almacenamiento,
			ConsultaUbicacion consultaUbicacion,
			@Value("${canchaya.escenario.maximo-fotos}") int maximoFotos,
			@Value("${canchaya.escenario.tamano-maximo-foto-mb}") int tamanoMaximoFotoMb,
			@Value("${canchaya.escenario.formatos-foto}") List<String> formatosFoto) {
		this.repositorio = repositorio;
		this.almacenamiento = almacenamiento;
		this.consultaUbicacion = consultaUbicacion;
		this.maximoFotos = maximoFotos;
		this.tamanoMaximoFotoMb = tamanoMaximoFotoMb;
		this.formatosAdmitidos = formatosFoto.stream()
				.map(formato -> FormatoFoto.valueOf(formato.strip().toUpperCase(Locale.ROOT)))
				.collect(Collectors.toUnmodifiableSet());
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
		List<FormatoFoto> formatos = fotos.stream().map(this::validarFoto).toList();

		List<String> urls = new ArrayList<>();
		try {
			for (int i = 0; i < fotos.size(); i++) {
				FormatoFoto formato = formatos.get(i);
				urls.add(almacenamiento.subir(UUID.randomUUID() + "." + formato.getExtension(),
						fotos.get(i).contenido(), formato.getTipoContenido()));
			}

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
			urls.forEach(almacenamiento::borrar);
			throw e;
		}
	}

	// Política 13: formato (PAR-ESC-03) y peso (PAR-ESC-02) de cada foto
	private FormatoFoto validarFoto(FotoAdjunta foto) {
		String archivo = foto.nombreArchivo() == null ? "" : foto.nombreArchivo();
		FormatoFoto formato = FormatoFoto.detectar(foto.contenido(), formatosAdmitidos)
				.orElseThrow(() -> new ReglaNegocioException("ERR-ESC-13",
						new TextoCatalogo("ERR-ESC-13.formato", archivo)));
		if (foto.contenido().length > tamanoMaximoFotoMb * BYTES_POR_MB) {
			throw new ReglaNegocioException("ERR-ESC-13",
					new TextoCatalogo("ERR-ESC-13.tamano", archivo, tamanoMaximoFotoMb));
		}
		return formato;
	}

}
