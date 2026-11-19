package co.edu.uco.canchaya.escenarios.application.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario.FotoAdjunta;
import co.edu.uco.canchaya.escenarios.application.port.out.AlmacenamientoFotos;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

/**
 * Política 13 de Escenario, la parte que no puede revisar el dominio: formato (PAR-ESC-03) y
 * peso (PAR-ESC-02) de cada foto nueva, y su subida al almacenamiento. La usan el registro
 * (HU5) y la modificación (HU8). La cantidad (PAR-ESC-01) sí la revisa el dominio.
 */
final class ValidacionFotos {

	private static final long BYTES_POR_MB = 1024L * 1024L;

	private final AlmacenamientoFotos almacenamiento;
	private final int tamanoMaximoFotoMb;
	private final Set<FormatoFoto> formatosAdmitidos;

	ValidacionFotos(AlmacenamientoFotos almacenamiento, int tamanoMaximoFotoMb, List<String> formatosFoto) {
		this.almacenamiento = almacenamiento;
		this.tamanoMaximoFotoMb = tamanoMaximoFotoMb;
		this.formatosAdmitidos = formatosFoto.stream()
				.map(formato -> FormatoFoto.valueOf(formato.strip().toUpperCase(Locale.ROOT)))
				.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Revisa todas las fotos antes de subir la primera, para no dejar fotos sueltas.
	 *
	 * @throws ReglaNegocioException ERR-ESC-13 con la regla que incumple la primera foto inválida
	 */
	List<FotoRevisada> revisar(List<FotoAdjunta> fotos) {
		return fotos.stream().map(this::revisar).toList();
	}

	/**
	 * Sube las fotos en orden. Si una falla, borra las que ya había subido y lanza el error.
	 *
	 * @return las direcciones (URL), en el mismo orden
	 */
	List<String> subir(List<FotoRevisada> fotos) {
		List<String> urls = new ArrayList<>();
		try {
			for (FotoRevisada foto : fotos) {
				FormatoFoto formato = foto.formato();
				urls.add(almacenamiento.subir(UUID.randomUUID() + "." + formato.getExtension(),
						foto.adjunta().contenido(), formato.getTipoContenido()));
			}
			return List.copyOf(urls);
		} catch (RuntimeException e) {
			borrar(urls);
			throw e;
		}
	}

	/** Limpieza: si falla un borrado, el adaptador lo registra y no lanza error. */
	void borrar(List<String> urls) {
		urls.forEach(almacenamiento::borrar);
	}

	private FotoRevisada revisar(FotoAdjunta foto) {
		String archivo = foto.nombreArchivo() == null ? "" : foto.nombreArchivo();
		FormatoFoto formato = FormatoFoto.detectar(foto.contenido(), formatosAdmitidos)
				.orElseThrow(() -> new ReglaNegocioException("ERR-ESC-13",
						new TextoCatalogo("ERR-ESC-13.formato", archivo)));
		if (foto.contenido().length > tamanoMaximoFotoMb * BYTES_POR_MB) {
			throw new ReglaNegocioException("ERR-ESC-13",
					new TextoCatalogo("ERR-ESC-13.tamano", archivo, tamanoMaximoFotoMb));
		}
		return new FotoRevisada(foto, formato);
	}

	/** Una foto que ya pasó la revisión, con el formato que se le detectó. */
	record FotoRevisada(FotoAdjunta adjunta, FormatoFoto formato) {
	}

}
