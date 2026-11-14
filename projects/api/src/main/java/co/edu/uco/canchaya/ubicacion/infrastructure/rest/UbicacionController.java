package co.edu.uco.canchaya.ubicacion.infrastructure.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uco.canchaya.ubicacion.application.port.in.ConsultaUbicacion;

/** Listas del selector de departamento y ciudad (HU5 CA6; también lo usará HU18). */
@RestController
@RequestMapping("/api/departamentos")
class UbicacionController {

	private final ConsultaUbicacion consultaUbicacion;

	UbicacionController(ConsultaUbicacion consultaUbicacion) {
		this.consultaUbicacion = consultaUbicacion;
	}

	record Opcion(UUID id, String nombre) {
	}

	@GetMapping
	List<Opcion> departamentos() {
		return consultaUbicacion.listarDepartamentos().stream()
				.map(departamento -> new Opcion(departamento.getId(), departamento.getNombre())).toList();
	}

	// HU5, CA6: solo las ciudades del departamento elegido
	@GetMapping("/{departamentoId}/ciudades")
	List<Opcion> ciudades(@PathVariable UUID departamentoId) {
		return consultaUbicacion.listarCiudades(departamentoId).stream()
				.map(ciudad -> new Opcion(ciudad.getId(), ciudad.getNombre())).toList();
	}

}
