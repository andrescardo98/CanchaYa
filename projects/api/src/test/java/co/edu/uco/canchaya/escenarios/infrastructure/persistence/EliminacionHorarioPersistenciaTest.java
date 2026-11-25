package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
import co.edu.uco.canchaya.escenarios.application.port.in.EliminarHorario;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioHorarios;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/** HU12 con la base real (PostGIS). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EliminacionHorarioPersistenciaTest {

	// Identificador de F5 en la V2
	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");

	@Autowired
	private RepositorioHorarios repositorio;

	@Autowired
	private EliminarHorario eliminarHorario;

	@Autowired
	private JdbcTemplate jdbc;

	private Solicitante propietario;
	private UUID escenarioId;

	@BeforeEach
	void preparar() {
		UUID usuarioId = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Ana Pérez', 'ana.hu12@correo.com', ?, 'CC', '1000000121', '+573001234567',
				  'ADMINISTRADOR_CANCHA', now())""", usuarioId, "x".repeat(60));
		propietario = new Solicitante(usuarioId, Rol.ADMINISTRADOR_CANCHA);
		escenarioId = UUID.randomUUID();
		jdbc.update("""
				insert into escenario (id, nombre, direccion, ciudad_id, latitud, longitud, telefono_contacto,
				  administrador_id, estado)
				values (?, 'Cancha La 80', 'Calle 80 # 45-10', (select id from ciudad where nombre = 'Rionegro'),
				  6.155123, -75.3737, '+573001234567', ?, 'ACTIVO')""", escenarioId, usuarioId);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("delete from cancha");
		jdbc.update("delete from horario_recurrente");
		jdbc.update("delete from escenario");
		jdbc.update("delete from usuario where rol <> 'ADMINISTRADOR_SISTEMA'");
	}

	private HorarioRecurrente guardado(DiaSemana dia, int inicio, int fin) {
		HorarioRecurrente horario = HorarioRecurrente.reconstruir(UUID.randomUUID(), escenarioId, dia,
				LocalTime.of(inicio, 0), LocalTime.of(fin, 0));
		repositorio.guardar(horario);
		return horario;
	}

	@Test
	void ca1Eliminar_borraLaFila() {
		HorarioRecurrente lunes = guardado(DiaSemana.LUNES, 6, 12);
		HorarioRecurrente martes = guardado(DiaSemana.MARTES, 6, 12);

		repositorio.eliminar(lunes.getId());

		assertThat(repositorio.buscarPorId(lunes.getId())).isEmpty();
		// Solo esa fila: los demás horarios del escenario siguen
		assertThat(repositorio.buscarPorEscenario(escenarioId)).extracting(HorarioRecurrente::getId)
				.containsExactly(martes.getId());
	}

	// D36: tomarTurno exige una transacción abierta (Propagation.MANDATORY); con la base real se
	// confirma que el caso de uso la abre antes de tomar los turnos de las canchas
	@Test
	void ca1EliminarConCanchas_tomaLosTurnosDentroDeLaTransaccionYBorra() {
		jdbc.update("insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora) values (?, ?, ?, ?, 80000)",
				UUID.randomUUID(), escenarioId, F5, "Cancha 1");
		HorarioRecurrente lunes = guardado(DiaSemana.LUNES, 6, 12);

		eliminarHorario.ejecutar(lunes.getId(), propietario);

		assertThat(repositorio.buscarPorId(lunes.getId())).isEmpty();
	}

}
