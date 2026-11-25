package co.edu.uco.canchaya.identidad.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.identidad.application.port.in.ConsultaUsuarios.ContactoUsuario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.TipoDocumento;
import co.edu.uco.canchaya.identidad.domain.Usuario;

/** Lo que Identidad ofrece a los demás módulos sobre un usuario (HU13, HU22). */
class ConsultaUsuariosCasoUsoTest {

	private final UUID idJuan = UUID.randomUUID();
	private ConsultaUsuariosCasoUso consulta;

	@BeforeEach
	void preparar() {
		RepositorioPerfilesEnMemoria repositorio = new RepositorioPerfilesEnMemoria();
		repositorio.agregar(Usuario.reconstruir(idJuan, "Juan Gómez", "juan@correo.com", "hash-de-clave2026",
				TipoDocumento.CC, "1036123456", "+573001234567", Rol.JUGADOR,
				OffsetDateTime.now(ZoneId.of("America/Bogota"))));
		consulta = new ConsultaUsuariosCasoUso(repositorio);
	}

	// HU22 CA2: COR-RES-02 lleva el celular del jugador (P4)
	@Test
	void HU22_CA2_traeElCelular() {
		assertThat(consulta.buscarContactos(List.of(idJuan)))
				.containsExactly(new ContactoUsuario(idJuan, "Juan Gómez", "juan@correo.com", "+573001234567"));
	}

	@Test
	void losQueNoExistenNoVienen() {
		assertThat(consulta.buscarContactos(List.of(UUID.randomUUID()))).isEmpty();
		assertThat(consulta.buscarContactos(List.of())).isEmpty();
	}

}
