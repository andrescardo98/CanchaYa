package co.edu.uco.canchaya.escenarios.infrastructure.calificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.escenarios.application.port.out.ConsultaCalificaciones;

/**
 * HU19 (P6 de la especificación): la aplicación arranca con una sola implementación del puerto, la
 * temporal. Cuando Reputación la implemente (Release 3), esta prueba recuerda borrar el adaptador
 * temporal y cambiar la clase esperada.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConexionConsultaCalificacionesTest {

	@Autowired
	private ApplicationContext contexto;

	@Test
	void ca2LaAplicacionUsaElAdaptadorTemporalDeCalificaciones() {
		assertThat(contexto.getBeansOfType(ConsultaCalificaciones.class).values())
				.singleElement()
				.isInstanceOf(ConsultaCalificacionesSinResenas.class);
	}

}
