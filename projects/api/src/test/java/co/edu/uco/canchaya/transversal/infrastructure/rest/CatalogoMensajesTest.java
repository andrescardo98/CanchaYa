package co.edu.uco.canchaya.transversal.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

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

	@ParameterizedTest
	@ValueSource(strings = { "EXI-ESC-01", "ERR-ESC-01", "ERR-ESC-02", "ERR-ESC-03", "ERR-ESC-13" })
	void tieneLosCodigosNuevosQueCitaLaHu5(String codigo) {
		assertThat(catalogo.texto(codigo)).isNotBlank();
	}

	@ParameterizedTest
	@ValueSource(strings = { "EXI-CAN-01", "ERR-CAN-01", "ERR-CAN-02", "ERR-CAN-03", "ERR-CAN-04", "ERR-CAN-05" })
	void tieneLosCodigosNuevosQueCitaLaHu6(String codigo) {
		assertThat(catalogo.texto(codigo)).isNotBlank();
	}

	@ParameterizedTest
	@ValueSource(strings = { "EXI-HOR-01", "ERR-HOR-01", "ERR-HOR-02", "ERR-HOR-03", "ERR-HOR-04", "ERR-HOR-13" })
	void tieneLosCodigosNuevosQueCitaLaHu7(String codigo) {
		assertThat(catalogo.texto(codigo)).isNotBlank();
	}

	@ParameterizedTest
	@ValueSource(strings = { "INF-ESC-01", "INF-ESC-03", "ERR-HOR-10", "ERR-GEN-04" })
	void tieneLosCodigosQueCitaLaHu18(String codigo) {
		assertThat(catalogo.texto(codigo)).isNotBlank();
	}

	@Test
	void hu18ArmaErrGen04ConElCampoYLosLimitesDelRadio() {
		assertThat(catalogo.texto("ERR-GEN-04", List.of(new TextoCatalogo("campo.radioKm"), 1, 30)))
				.isEqualTo("El valor de radio de búsqueda debe estar entre 1 y 30.");
	}

	@Test
	void hu7ArmaErrHor04ConElDiaQueTambienSaleDelCatalogo() {
		assertThat(catalogo.texto("ERR-HOR-04", List.of(new TextoCatalogo("dia.MIERCOLES"))))
				.isEqualTo("El horario se cruza con otro horario ya definido para el miércoles.");
	}

	@Test
	void hu5ArmaErrEsc13ConLaReglaIncumplidaQueTambienSaleDelCatalogo() {
		assertThat(catalogo.texto("ERR-ESC-13", List.of(new TextoCatalogo("ERR-ESC-13.tamano", "cancha.jpg", 2))))
				.isEqualTo("Las fotos no cumplen las condiciones: la foto cancha.jpg pesa más de 2 MB. Se permiten "
						+ "máximo 5 fotos por escenario, en formato JPG o PNG y de hasta 2 MB cada una.");
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
