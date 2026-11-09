package co.edu.uco.canchaya.identidad.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.identidad.application.secondaryports.RepositorioUsuarios;

class ConsultarCorreoDisponibleCasoUsoTest {

	private final RepositorioUsuarios repositorio = mock(RepositorioUsuarios.class);
	private final ConsultarCorreoDisponibleCasoUso casoUso = new ConsultarCorreoDisponibleCasoUso(repositorio);

	@Test
	void ca8ElCorreoRegistradoNoEstaDisponibleAunqueCambienLasMayusculas() {
		when(repositorio.existePorCorreo("ana@correo.com")).thenReturn(true);

		assertThat(casoUso.ejecutar(" ANA@Correo.com ")).isFalse();
	}

	@Test
	void ca8UnCorreoNuevoEstaDisponible() {
		assertThat(casoUso.ejecutar("nuevo@correo.com")).isTrue();
	}

}
