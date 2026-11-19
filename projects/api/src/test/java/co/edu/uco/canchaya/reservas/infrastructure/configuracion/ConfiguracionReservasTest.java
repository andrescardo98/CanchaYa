package co.edu.uco.canchaya.reservas.infrastructure.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import co.edu.uco.canchaya.reservas.domain.ParametrosReserva;

/** P8 de HU20 y regla 7 de AGENTS.md: ParametrosReserva sale de application.properties. */
class ConfiguracionReservasTest {

	// Lee application.properties como la aplicación, pero sin base de datos ni el resto de beans
	private final ApplicationContextRunner contexto = new ApplicationContextRunner()
			.withInitializer(new ConfigDataApplicationContextInitializer())
			.withUserConfiguration(ConfiguracionReservas.class);

	@Test
	void losValoresPorDefectoSonLosDelCatalogoDeParametros() {
		contexto.run(c -> assertThat(c.getBean(ParametrosReserva.class)).isEqualTo(new ParametrosReserva(
				30, // PAR-RES-01
				2, // PAR-RES-02
				2, // PAR-RES-03
				5, // PAR-RES-04
				3, // PAR-RES-05
				5, // PAR-RES-06
				LocalTime.NOON, // PAR-RES-07
				3, // PAR-RES-08
				24, // PAR-RES-09
				250, // PAR-RES-10
				false))); // PAR-RES-12 en «No» (D33)
	}

	@Test
	void unCambioEnLaConfiguracionLlegaSinTocarElCodigo() {
		contexto.withPropertyValues("canchaya.reserva.ventana-dias=15", "canchaya.reserva.franjas-maximas=3",
				"canchaya.reserva.hora-minima-confirmacion=14:00",
				"canchaya.reserva.confirmacion-asistencia-activa=true")
				.run(c -> {
					ParametrosReserva parametros = c.getBean(ParametrosReserva.class);
					assertThat(parametros.ventanaReservaDias()).isEqualTo(15);
					assertThat(parametros.franjasMaximasPorReserva()).isEqualTo(3);
					assertThat(parametros.horaMinimaConfirmacion()).isEqualTo(LocalTime.of(14, 0));
					assertThat(parametros.confirmacionAsistenciaActiva()).isTrue();
				});
	}

}
