package co.edu.uco.canchaya.transversal.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CatalogoMensajesTest {

	private final CatalogoMensajes catalogo = new CatalogoMensajes();

	@ParameterizedTest
	@ValueSource(strings = { "EXI-USU-01", "ERR-GEN-01", "ERR-GEN-02", "ERR-GEN-03", "ERR-GEN-04", "ERR-GEN-07",
			"ERR-USU-01", "ERR-USU-02", "ERR-USU-03", "ERR-USU-12", "ERR-USU-13", "ERR-USU-14", "ERR-USU-15" })
	void tieneTodosLosCodigosQueCitaLaHu1(String codigo) {
		assertThat(catalogo.texto(codigo)).isNotBlank();
	}

	@ParameterizedTest
	@ValueSource(strings = { "ERR-GEN-05", "ERR-USU-06", "ERR-USU-07", "ERR-USU-16" })
	void tieneLosCodigosNuevosQueCitaLaHu2(String codigo) {
		assertThat(catalogo.texto(codigo)).isNotBlank();
	}

	@ParameterizedTest
	@ValueSource(strings = { "EXI-USU-02", "ERR-USU-08", "ERR-USU-09", "ERR-USU-10", "ERR-USU-11" })
	void tieneLosCodigosNuevosQueCitaLaHu3(String codigo) {
		assertThat(catalogo.texto(codigo)).isNotBlank();
	}

	@Test
	void hu2Ca4ErrUsu06YErrUsu07TienenElMismoTextoParaNoRevelarSiElCorreoExiste() {
		assertThat(catalogo.texto("ERR-USU-07")).isEqualTo(catalogo.texto("ERR-USU-06"));
	}

	@Test
	void reemplazaLosParametrosEnOrdenYConservaLasTildes() {
		assertThat(catalogo.texto("ERR-USU-13", List.of("CC", "de 6 a 10 dígitos")))
				.isEqualTo("El número de documento no es válido para el tipo CC: de 6 a 10 dígitos.");
	}

	@Test
	void unCodigoQueNoExisteEsUnErrorDelPrograma() {
		assertThatThrownBy(() -> catalogo.texto("ERR-XXX-99")).isInstanceOf(IllegalStateException.class);
	}

}
