package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioHorarios;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

/** HU7: lo que el caso de uso le pide a la base de horarios, con PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class HorarioPersistenciaTest {

	@Autowired
	private RepositorioHorarios repositorio;

	@Autowired
	private JdbcTemplate jdbc;

	private UUID escenario;

	@BeforeEach
	void preparar() {
		UUID propietario = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Ana Pérez', 'ana@correo.com', ?, 'CC', '1000000071', '+573001234567',
				  'ADMINISTRADOR_CANCHA', now())""", propietario, "x".repeat(60));
		UUID rionegro = jdbc.queryForObject("select id from ciudad where nombre = 'Rionegro'", UUID.class);
		escenario = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Cancha La 80', 'Calle 80 # 45-10', ?, 6.155123, -75.3737, '+573001234567', ?, 'ACTIVO')""",
				escenario, rionegro, propietario);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from horario_recurrente");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private HorarioRecurrente horario(DiaSemana dia, int inicio, int fin) {
		return HorarioRecurrente.reconstruir(UUID.randomUUID(), escenario, dia, LocalTime.of(inicio, 0),
				LocalTime.of(fin, 0));
	}

	@Test
	void ca1GuardaYLeeElHorario() {
		HorarioRecurrente horario = horario(DiaSemana.MIERCOLES, 6, 12);

		repositorio.guardar(horario);

		assertThat(repositorio.existePorId(horario.getId())).isTrue();
		HorarioRecurrente leido = repositorio.buscarPorEscenarioYDia(escenario, DiaSemana.MIERCOLES).getFirst();
		assertThat(leido.getId()).isEqualTo(horario.getId());
		assertThat(leido.getEscenarioId()).isEqualTo(escenario);
		assertThat(leido.getHoraInicio()).isEqualTo(LocalTime.of(6, 0));
		assertThat(leido.getHoraFin()).isEqualTo(LocalTime.of(12, 0));
	}

	@Test
	void ca1LaListaSaleDeLunesADomingoYPorHora() {
		HorarioRecurrente domingo = horario(DiaSemana.DOMINGO, 8, 12);
		HorarioRecurrente juevesTarde = horario(DiaSemana.JUEVES, 14, 23);
		HorarioRecurrente lunes = horario(DiaSemana.LUNES, 6, 12);
		HorarioRecurrente juevesManana = horario(DiaSemana.JUEVES, 6, 12);
		repositorio.guardar(domingo);
		repositorio.guardar(juevesTarde);
		repositorio.guardar(lunes);
		repositorio.guardar(juevesManana);

		assertThat(repositorio.buscarPorEscenario(escenario)).extracting(HorarioRecurrente::getId)
				.containsExactly(lunes.getId(), juevesManana.getId(), juevesTarde.getId(), domingo.getId());
	}

	// CA6: si dos peticiones pasan la política 4 al mismo tiempo, la restricción de exclusión frena la segunda
	@Test
	void ca6LaBaseRechazaUnCruceYSaleComoErrHor04() {
		repositorio.guardar(horario(DiaSemana.LUNES, 6, 12));

		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(
				() -> repositorio.guardar(horario(DiaSemana.LUNES, 10, 14)))
				.isInstanceOf(ReglaNegocioException.class).actual();

		assertThat(error.getCodigoMensaje()).isEqualTo("ERR-HOR-04");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("dia.LUNES"));
		assertThat(repositorio.buscarPorEscenarioYDia(escenario, DiaSemana.LUNES)).hasSize(1);
	}

	// CA7: la base también deja horarios seguidos y el mismo rango en otro día
	@Test
	void ca7LaBaseAceptaHorariosQueNoSeCruzan() {
		repositorio.guardar(horario(DiaSemana.LUNES, 6, 12));
		repositorio.guardar(horario(DiaSemana.LUNES, 12, 14));
		repositorio.guardar(horario(DiaSemana.LUNES, 14, 23));
		repositorio.guardar(horario(DiaSemana.MARTES, 6, 12));

		assertThat(repositorio.buscarPorEscenarioYDia(escenario, DiaSemana.LUNES)).hasSize(3);
		assertThat(repositorio.buscarPorEscenario(escenario)).hasSize(4);
	}

}
