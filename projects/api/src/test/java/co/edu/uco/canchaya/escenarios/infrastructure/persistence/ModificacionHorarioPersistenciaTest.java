package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import co.edu.uco.canchaya.TestcontainersConfiguration;
import co.edu.uco.canchaya.escenarios.application.port.in.ModificarHorario;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioHorarios;
import co.edu.uco.canchaya.escenarios.domain.DiaSemana;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.HorarioRecurrente;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;

/** HU11 con la base real (PostGIS) y la restricción de exclusión de la V1. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ModificacionHorarioPersistenciaTest {

	// Identificador de F5 en la V2
	private static final UUID F5 = UUID.fromString("27d013e3-f5b3-5378-a7df-7c80429554fe");

	@Autowired
	private RepositorioHorarios repositorio;

	@Autowired
	private RepositorioEscenarios repositorioEscenarios;

	@Autowired
	private ModificarHorario modificarHorario;

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
				values (?, 'Ana Pérez', 'ana.hu11@correo.com', ?, 'CC', '1000000111', '+573001234567',
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
		jdbc.update("delete from reserva");
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

	private Escenario escenario() {
		return repositorioEscenarios.buscarPorId(escenarioId).orElseThrow();
	}

	@Test
	void ca1Actualizar_cambiaDiaYHoras() {
		HorarioRecurrente horario = guardado(DiaSemana.LUNES, 6, 12);

		HorarioRecurrente leido = repositorio.buscarPorId(horario.getId()).orElseThrow();
		leido.modificar(escenario(), DiaSemana.MARTES, LocalTime.of(7, 0), LocalTime.of(13, 0), List.of(),
				propietario);
		repositorio.actualizar(leido);

		HorarioRecurrente actualizado = repositorio.buscarPorId(horario.getId()).orElseThrow();
		assertThat(actualizado.getEscenarioId()).isEqualTo(escenarioId);
		assertThat(actualizado.getDiaSemana()).isEqualTo(DiaSemana.MARTES);
		assertThat(actualizado.getHoraInicio()).isEqualTo(LocalTime.of(7, 0));
		assertThat(actualizado.getHoraFin()).isEqualTo(LocalTime.of(13, 0));
		// Se cambió la fila; no se agregó otra
		assertThat(repositorio.buscarPorEscenario(escenarioId)).hasSize(1);
	}

	// CA3: la consulta de la política 6
	@Test
	void ca3BuscarPorId_inexistente_vacio() {
		assertThat(repositorio.buscarPorId(UUID.randomUUID())).isEmpty();
	}

	// CA6: si dos peticiones pasan la política 7 al mismo tiempo, la restricción de exclusión frena el UPDATE
	@Test
	void ca6Actualizar_cruceEnLaBase_ERR_HOR_07() {
		HorarioRecurrente manana = guardado(DiaSemana.LUNES, 6, 12);
		guardado(DiaSemana.LUNES, 14, 18);
		HorarioRecurrente leido = repositorio.buscarPorId(manana.getId()).orElseThrow();
		// Lista vacía: como si la otra petición aún no hubiera guardado su horario
		leido.modificar(escenario(), DiaSemana.LUNES, LocalTime.of(10, 0), LocalTime.of(16, 0), List.of(),
				propietario);

		ReglaNegocioException error = (ReglaNegocioException) assertThatThrownBy(() -> repositorio.actualizar(leido))
				.isInstanceOf(ReglaNegocioException.class).actual();

		assertThat(error.getCodigoMensaje()).isEqualTo("ERR-HOR-07");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("dia.LUNES"));
		HorarioRecurrente sinCambios = repositorio.buscarPorId(manana.getId()).orElseThrow();
		assertThat(sinCambios.getHoraInicio()).isEqualTo(LocalTime.of(6, 0));
		assertThat(sinCambios.getHoraFin()).isEqualTo(LocalTime.of(12, 0));
	}

	// CA8 y política 14 (RN-17, D09, P3 de la especificación): la reserva que queda por fuera se conserva
	@Test
	void ca8ReservaPorFueraDelNuevoHorario_seConserva() {
		HorarioRecurrente horario = guardado(DiaSemana.LUNES, 18, 22);
		UUID canchaId = UUID.randomUUID();
		jdbc.update("""
				insert into cancha (id, escenario_id, tipo_cancha_id, nombre, precio_hora, estado)
				values (?, ?, ?, 'Cancha 1', 80000, 'DISPONIBLE')""", canchaId, escenarioId, F5);
		UUID jugador = UUID.randomUUID();
		jdbc.update("""
				insert into usuario (id, nombre, correo_electronico, contrasena_hash, tipo_documento,
				  numero_documento, numero_celular, rol, fecha_registro)
				values (?, 'Luis Gómez', 'luis.hu11@correo.com', ?, 'CC', '1000000112', '+573001234567', 'JUGADOR', now())""",
				jugador, "x".repeat(60));
		LocalDate proximoLunes = LocalDate.now(ZoneId.of("America/Bogota"))
				.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
		UUID reserva = UUID.randomUUID();
		jdbc.update("""
				insert into reserva (id, cancha_id, jugador_id, fecha, hora_inicio, hora_fin, precio, estado, fecha_creacion)
				values (?, ?, ?, ?, ?, ?, 80000, 'CONFIRMADA', now())""",
				reserva, canchaId, jugador, proximoLunes, LocalTime.of(20, 0), LocalTime.of(21, 0));
		Map<String, Object> antes = jdbc.queryForMap("select * from reserva where id = ?", reserva);

		// De 18–22 a 6–12: la reserva de 20 a 21 queda por fuera
		modificarHorario.ejecutar(horario.getId(), DiaSemana.LUNES, LocalTime.of(6, 0), LocalTime.of(12, 0),
				propietario);

		assertThat(repositorio.buscarPorId(horario.getId())).map(HorarioRecurrente::getHoraFin)
				.contains(LocalTime.of(12, 0));
		assertThat(jdbc.queryForMap("select * from reserva where id = ?", reserva)).isEqualTo(antes);
	}

}
