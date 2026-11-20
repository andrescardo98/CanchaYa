package co.edu.uco.canchaya.transversal.infrastructure.rest;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.infrastructure.rest.RespuestaError.Mensaje;
import jakarta.validation.ConstraintViolation;

@RestControllerAdvice
class ManejadorErrores {

	private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

	private final CatalogoMensajes catalogo;

	ManejadorErrores(CatalogoMensajes catalogo) {
		this.catalogo = catalogo;
	}

	// ERR-GEN-01 a 04: se devuelven todos los campos con error a la vez (AG-03)
	@ExceptionHandler(BindException.class)
	ResponseEntity<RespuestaError> camposInvalidos(BindException ex) {
		// Un mensaje por campo; al ordenar por código gana la obligatoriedad (01)
		Map<String, FieldError> unoPorCampo = new LinkedHashMap<>();
		ex.getFieldErrors().stream()
				.sorted(Comparator.comparing(FieldError::getDefaultMessage))
				.forEach(error -> unoPorCampo.putIfAbsent(error.getField(), error));

		List<Mensaje> errores = unoPorCampo.values().stream().map(this::mensajeDeCampo).toList();
		return ResponseEntity.badRequest().body(new RespuestaError(errores));
	}

	// Las reglas que no son de datos inválidos llevan su propio código HTTP; las demás, 422.
	// Con Map.ofEntries no hay límite de entradas (Map.of solo admite 10)
	private static final Map<String, HttpStatus> ESTADO_POR_CODIGO = Map.ofEntries(
			Map.entry("ERR-USU-08", HttpStatus.NOT_FOUND), // HU3: no existe el usuario
			Map.entry("ERR-USU-09", HttpStatus.FORBIDDEN), // HU3: el perfil es de otro usuario
			Map.entry("ERR-ESC-01", HttpStatus.FORBIDDEN), // HU5: solo un administrador de cancha registra escenarios
			Map.entry("ERR-ESC-06", HttpStatus.NOT_FOUND), // HU13: no existe el escenario
			Map.entry("ERR-ESC-07", HttpStatus.FORBIDDEN), // HU8: solo el propietario modifica el escenario
			Map.entry("ERR-ESC-08", HttpStatus.FORBIDDEN), // HU13: solo el administrador del sistema cambia el estado
			Map.entry("ERR-CAN-01", HttpStatus.NOT_FOUND), // HU6: no existe el escenario de la cancha
			Map.entry("ERR-CAN-02", HttpStatus.FORBIDDEN), // HU6: no es el propietario del escenario
			Map.entry("ERR-CAN-07", HttpStatus.NOT_FOUND), // HU9: no existe la cancha
			Map.entry("ERR-GEN-06", HttpStatus.FORBIDDEN), // HU13: sin permiso para ver la administración de escenarios
			Map.entry("ERR-GEN-07", HttpStatus.BAD_REQUEST), // HU8 (P1): petición manipulada, por ejemplo una foto conservada que no es del escenario
			Map.entry("ERR-HOR-01", HttpStatus.NOT_FOUND), // HU7: no existe el escenario del horario
			Map.entry("ERR-HOR-02", HttpStatus.FORBIDDEN), // HU7: solo el propietario gestiona los horarios
			Map.entry("ERR-RES-01", HttpStatus.NOT_FOUND), // HU20: la cancha no existe o no se puede reservar
			Map.entry("ERR-RES-05", HttpStatus.FORBIDDEN), // HU21: solo los jugadores reservan
			Map.entry("ERR-RES-06", HttpStatus.CONFLICT)); // HU21 (P8): otra reserva tomó la franja, o está fuera del horario

	@ExceptionHandler(ReglaNegocioException.class)
	ResponseEntity<RespuestaError> reglaIncumplida(ReglaNegocioException ex) {
		String codigo = ex.getCodigoMensaje();
		return ResponseEntity.status(ESTADO_POR_CODIGO.getOrDefault(codigo, HttpStatus.UNPROCESSABLE_CONTENT))
				.body(unError(codigo, catalogo.texto(codigo, ex.getParametros())));
	}

	// JSON mal armado o un valor que no se puede leer, por ejemplo "si" en un campo booleano
	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<RespuestaError> cuerpoIlegible(HttpMessageNotReadableException ex) {
		LOG.warn("No se pudo leer el cuerpo de la petición: {}", ex.getMessage());
		return ResponseEntity.badRequest().body(unError("ERR-GEN-07", catalogo.texto("ERR-GEN-07")));
	}

	// Un valor de la URL que no se puede leer, por ejemplo un identificador que no es UUID o un estado que no existe
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<RespuestaError> parametroIlegible(MethodArgumentTypeMismatchException ex) {
		LOG.warn("No se pudo leer el parámetro {} de la petición: {}", ex.getName(), ex.getMessage());
		return ResponseEntity.badRequest().body(unError("ERR-GEN-07", catalogo.texto("ERR-GEN-07")));
	}

	// AG-10: el usuario nunca ve la traza; el detalle queda solo en el log
	@ExceptionHandler(Exception.class)
	ResponseEntity<RespuestaError> errorInesperado(Exception ex) {
		HttpStatusCode estado = ex instanceof ErrorResponse respuesta
				? respuesta.getStatusCode()
				: HttpStatus.INTERNAL_SERVER_ERROR;
		if (estado.is5xxServerError()) {
			LOG.error("ERR-GEN-07", ex);
		}
		return ResponseEntity.status(estado).body(unError("ERR-GEN-07", catalogo.texto("ERR-GEN-07")));
	}

	private Mensaje mensajeDeCampo(FieldError error) {
		String codigo = error.getDefaultMessage();
		String atributo = error.getField();
		String campo = catalogo.nombreCampo(atributo);

		String texto = switch (codigo) {
			case "ERR-GEN-02" -> catalogo.texto(codigo, List.of(campo, catalogo.formatoEsperado(atributo)));
			case "ERR-GEN-03" -> textoDeLongitud(error, campo);
			default -> catalogo.texto(codigo, List.of(campo));
		};
		return new Mensaje(codigo, atributo, texto);
	}

	private String textoDeLongitud(FieldError error, String campo) {
		Map<String, Object> limites = error.unwrap(ConstraintViolation.class).getConstraintDescriptor().getAttributes();
		Object minimo = limites.get("min");
		Object maximo = limites.get("max");
		// Así lo pide la nota de ERR-GEN-03 en el catálogo
		if (Integer.valueOf(0).equals(minimo)) {
			return catalogo.texto("ERR-GEN-03.sin-minimo", List.of(campo, maximo));
		}
		return catalogo.texto("ERR-GEN-03", List.of(campo, minimo, maximo));
	}

	private static RespuestaError unError(String codigo, String texto) {
		return new RespuestaError(List.of(new Mensaje(codigo, null, texto)));
	}

}
