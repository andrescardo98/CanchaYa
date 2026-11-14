package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario.FotoAdjunta;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/escenarios")
class EscenarioController {

	private final RegistrarEscenario registrarEscenario;
	private final CatalogoMensajes catalogo;

	EscenarioController(RegistrarEscenario registrarEscenario, CatalogoMensajes catalogo) {
		this.registrarEscenario = registrarEscenario;
		this.catalogo = catalogo;
	}

	// HU5. Multipart: la parte «datos» en JSON y las fotos como archivos en la parte «fotos»
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<RespuestaRegistroEscenario> registrar(@Valid @RequestPart("datos") SolicitudRegistroEscenario datos,
			@RequestPart(name = "fotos", required = false) List<MultipartFile> fotos, Solicitante solicitante) {
		List<FotoAdjunta> adjuntas = fotos == null ? List.of() : fotos.stream().map(EscenarioController::aFoto).toList();
		Escenario escenario = registrarEscenario.ejecutar(datos.aDatos(adjuntas), solicitante);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(RespuestaRegistroEscenario.de(escenario, "EXI-ESC-01", catalogo.texto("EXI-ESC-01")));
	}

	private static FotoAdjunta aFoto(MultipartFile archivo) {
		try {
			return new FotoAdjunta(archivo.getOriginalFilename(), archivo.getBytes());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
