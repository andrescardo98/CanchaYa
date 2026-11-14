package co.edu.uco.canchaya.identidad.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

/** HU3: datos que llenan el formulario «Mi perfil». */
class ConsultarPerfilCasoUsoTest {

	private final UUID idAna = UUID.randomUUID();
	private RepositorioPerfilesEnMemoria repositorio;
	private ConsultarPerfilCasoUso casoUso;

	@BeforeEach
	void preparar() {
		repositorio = new RepositorioPerfilesEnMemoria();
		repositorio.agregar(Usuario.reconstruir(idAna, "Ana Pérez", "ana@correo.com", "hash-de-clave2026",
				TipoDocumento.CC, "1036123456", "+573001234567", Rol.JUGADOR,
				OffsetDateTime.now(ZoneId.of("America/Bogota"))));
		casoUso = new ConsultarPerfilCasoUso(repositorio);
	}

	@Test
	@DisplayName("Devuelve el perfil de su dueño, con documento y rol para mostrarlos como solo lectura")
	void devuelveElPerfilPropio() {
		Usuario perfil = casoUso.ejecutar(new Solicitante(idAna, Rol.JUGADOR), idAna);

		assertThat(perfil.getNombre()).isEqualTo("Ana Pérez");
		assertThat(perfil.getTipoDocumento()).isEqualTo(TipoDocumento.CC);
		assertThat(perfil.getRol()).isEqualTo(Rol.JUGADOR);
	}

	@Test
	@DisplayName("CA6: no deja ver el perfil de otro usuario (política 9, AG-15)")
	void ca6RechazaElPerfilDeOtroUsuario() {
		assertThatThrownBy(() -> casoUso.ejecutar(new Solicitante(UUID.randomUUID(), Rol.JUGADOR), idAna))
				.isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje())
				.isEqualTo("ERR-USU-09");
	}

	@Test
	@DisplayName("Política 8: rechaza un identificador que no existe")
	void politica8RechazaUnUsuarioQueNoExiste() {
		UUID inexistente = UUID.randomUUID();
		assertThatThrownBy(() -> casoUso.ejecutar(new Solicitante(inexistente, Rol.JUGADOR), inexistente))
				.isInstanceOf(ReglaNegocioException.class)
				.extracting(e -> ((ReglaNegocioException) e).getCodigoMensaje())
				.isEqualTo("ERR-USU-08");
	}

}
