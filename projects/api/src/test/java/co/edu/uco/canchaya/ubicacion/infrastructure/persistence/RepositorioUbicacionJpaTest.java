package co.edu.uco.canchaya.ubicacion.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.ubicacion.application.port.in.ConsultaUbicacion;
import co.edu.uco.canchaya.ubicacion.domain.Ciudad;
import co.edu.uco.canchaya.ubicacion.domain.Departamento;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RepositorioUbicacionJpaTest {

	@Autowired
	private ConsultaUbicacion consultaUbicacion;

	@Test
	void listaLosDepartamentosDeLaV2() {
		assertThat(consultaUbicacion.listarDepartamentos()).extracting(Departamento::getNombre)
				.containsExactly("Antioquia");
	}

	@Test
	void ca6ListaSoloLasCiudadesDelDepartamentoOrdenadasPorNombre() {
		Departamento antioquia = consultaUbicacion.listarDepartamentos().get(0);

		List<Ciudad> ciudades = consultaUbicacion.listarCiudades(antioquia.getId());

		assertThat(ciudades).hasSize(125).allMatch(ciudad -> ciudad.getDepartamentoId().equals(antioquia.getId()));
		// El orden lo da la base (su intercalación), no String.compareTo
		assertThat(ciudades).extracting(Ciudad::getNombre).startsWith("Abejorral", "Abriaquí");
		assertThat(consultaUbicacion.listarCiudades(UUID.randomUUID())).isEmpty();
	}

	@Test
	void ca4SabeSiUnaCiudadExiste() {
		Departamento antioquia = consultaUbicacion.listarDepartamentos().get(0);
		UUID rionegro = consultaUbicacion.listarCiudades(antioquia.getId()).stream()
				.filter(ciudad -> ciudad.getNombre().equals("Rionegro")).findFirst().orElseThrow().getId();

		assertThat(consultaUbicacion.existeCiudad(rionegro)).isTrue();
		assertThat(consultaUbicacion.existeCiudad(UUID.randomUUID())).isFalse();
		assertThat(consultaUbicacion.existeCiudad(null)).isFalse();
	}

}
