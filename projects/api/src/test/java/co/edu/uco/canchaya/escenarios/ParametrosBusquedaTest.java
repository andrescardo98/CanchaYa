package co.edu.uco.canchaya.escenarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * HU18 y regla 7 de AGENTS.md: los radios de búsqueda (PAR-ESC-04 a PAR-ESC-06) salen de
 * application.properties con los valores del catálogo, y en Render se cambian con su variable.
 */
class ParametrosBusquedaTest {

	private static final String DEFECTO = "canchaya.escenario.radio-busqueda-defecto-km";
	private static final String MAXIMO = "canchaya.escenario.radio-busqueda-maximo-km";
	private static final String MINIMO = "canchaya.escenario.radio-busqueda-minimo-km";

	// Lee application.properties como la aplicación, pero sin base de datos ni el resto de beans
	private final ApplicationContextRunner contexto = new ApplicationContextRunner()
			.withInitializer(new ConfigDataApplicationContextInitializer());

	// CA10 y CA11: 5 km por defecto (PAR-ESC-04), 30 de máximo (PAR-ESC-05) y 1 de mínimo (PAR-ESC-06), D11 y D26
	@Test
	void losValoresPorDefectoSonLosDelCatalogoDeParametros() {
		contexto.run(c -> {
			assertThat(c.getEnvironment().getProperty(DEFECTO, Integer.class)).isEqualTo(5);
			assertThat(c.getEnvironment().getProperty(MAXIMO, Integer.class)).isEqualTo(30);
			assertThat(c.getEnvironment().getProperty(MINIMO, Integer.class)).isEqualTo(1);
		});
	}

	// Las variables PAR_ESC_04 a PAR_ESC_06 cambian los radios sin tocar el código ni application.properties
	@Test
	void lasVariablesDeEntornoCambianLosRadios() {
		contexto.withInitializer(c -> c.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME + "-prueba",
				Map.of("PAR_ESC_04", "10", "PAR_ESC_05", "50", "PAR_ESC_06", "2"))))
				.run(c -> {
					assertThat(c.getEnvironment().getProperty(DEFECTO, Integer.class)).isEqualTo(10);
					assertThat(c.getEnvironment().getProperty(MAXIMO, Integer.class)).isEqualTo(50);
					assertThat(c.getEnvironment().getProperty(MINIMO, Integer.class)).isEqualTo(2);
				});
	}

}
