package co.edu.uco.canchaya.escenarios.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioBusquedaCanchas;
import co.edu.uco.canchaya.escenarios.domain.Cancha;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;

/**
 * HU18. Consulta directa con SQL porque usa funciones de PostGIS que JPA no mapea (comprobadas en el
 * spike, T1). ST_DWithin sobre geography mide en metros y usa el índice espacial
 * ix_escenario_ubicacion; ST_Distance da la distancia para ordenar. No filtra por estado ni por
 * horarios: RN-04 la decide Cancha.publicarEnBusqueda (P7).
 */
@Repository
class RepositorioBusquedaCanchasJdbc implements RepositorioBusquedaCanchas {

	private static final String COLUMNAS = """
			select c.id as cancha_id, c.tipo_cancha_id, c.nombre as cancha_nombre, c.precio_hora,
			  c.estado as cancha_estado, t.nombre as tipo_nombre,
			  e.id as escenario_id, e.nombre as escenario_nombre, e.descripcion, e.direccion, e.ciudad_id,
			  e.latitud, e.longitud, e.telefono_contacto, e.correo_contacto, e.administrador_id,
			  e.estado as escenario_estado""";

	private static final String TABLAS = """
			from cancha c
			join escenario e on e.id = c.escenario_id
			join tipo_cancha t on t.id = c.tipo_cancha_id
			join ciudad ci on ci.id = e.ciudad_id""";

	/** El punto del jugador; PostGIS recibe primero la longitud y después la latitud. */
	private static final String PUNTO = "ST_MakePoint(:longitud, :latitud)::geography";

	private final NamedParameterJdbcTemplate jdbc;

	RepositorioBusquedaCanchasJdbc(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<Candidata> buscar(FiltroBusqueda filtro) {
		MapSqlParameterSource parametros = new MapSqlParameterSource();
		List<String> condiciones = new ArrayList<>();
		StringBuilder sql = new StringBuilder(COLUMNAS);

		if (filtro.conUbicacion()) {
			sql.append(", ST_Distance(e.ubicacion, ").append(PUNTO).append(") as distancia_metros");
			parametros.addValue("latitud", filtro.latitud().doubleValue())
					.addValue("longitud", filtro.longitud().doubleValue())
					.addValue("radio", filtro.radioMetros().doubleValue());
			// CA1: dentro del radio, en metros
			condiciones.add("ST_DWithin(e.ubicacion, " + PUNTO + ", :radio)");
		}
		// Solo se agregan los filtros que llegan: PostgreSQL no sabe el tipo de un parámetro vacío en «:x is null»
		if (filtro.departamentoId() != null) {
			condiciones.add("ci.departamento_id = :departamento"); // CA3
			parametros.addValue("departamento", filtro.departamentoId());
		}
		if (filtro.ciudadId() != null) {
			condiciones.add("e.ciudad_id = :ciudad"); // CA3
			parametros.addValue("ciudad", filtro.ciudadId());
		}
		if (filtro.tipoCanchaId() != null) {
			condiciones.add("c.tipo_cancha_id = :tipo"); // CA4
			parametros.addValue("tipo", filtro.tipoCanchaId());
		}

		sql.append('\n').append(TABLAS);
		if (!condiciones.isEmpty()) {
			sql.append("\nwhere ").append(String.join("\n  and ", condiciones));
		}
		// CA1: de la más cercana a la más lejana. CA2: sin ubicación, por nombre
		sql.append(filtro.conUbicacion()
				? "\norder by distancia_metros, e.nombre, c.nombre"
				: "\norder by e.nombre, c.nombre");

		return jdbc.query(sql.toString(), parametros, (fila, numero) -> candidata(fila, filtro.conUbicacion()));
	}

	private static Candidata candidata(ResultSet fila, boolean conUbicacion) throws SQLException {
		UUID escenarioId = fila.getObject("escenario_id", UUID.class);
		Cancha cancha = Cancha.reconstruir(fila.getObject("cancha_id", UUID.class), escenarioId,
				fila.getObject("tipo_cancha_id", UUID.class), fila.getString("cancha_nombre"),
				fila.getBigDecimal("precio_hora"), EstadoCancha.valueOf(fila.getString("cancha_estado")));
		// Sin fotos: la búsqueda no las muestra
		Escenario escenario = Escenario.reconstruir(escenarioId, fila.getString("escenario_nombre"),
				fila.getString("descripcion"), fila.getString("direccion"), fila.getObject("ciudad_id", UUID.class),
				fila.getBigDecimal("latitud"), fila.getBigDecimal("longitud"), fila.getString("telefono_contacto"),
				fila.getString("correo_contacto"), List.of(), fila.getObject("administrador_id", UUID.class),
				EstadoEscenario.valueOf(fila.getString("escenario_estado")));
		Double distancia = conUbicacion ? fila.getDouble("distancia_metros") : null;
		return new Candidata(cancha, escenario, fila.getString("tipo_nombre"), distancia);
	}

}
