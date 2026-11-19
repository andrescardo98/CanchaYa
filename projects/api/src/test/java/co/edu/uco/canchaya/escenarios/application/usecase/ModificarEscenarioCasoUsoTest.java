package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.escenarios.application.port.in.ModificarEscenario.DatosModificacionEscenario;
import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario.FotoAdjunta;
import co.edu.uco.canchaya.escenarios.application.port.out.AlmacenamientoFotos;
import co.edu.uco.canchaya.escenarios.application.port.out.RepositorioEscenarios;
import co.edu.uco.canchaya.escenarios.domain.Escenario;
import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;
import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;
import co.edu.uco.canchaya.transversal.domain.TextoCatalogo;
import co.edu.uco.canchaya.ubicacion.application.port.in.ConsultaUbicacion;
import co.edu.uco.canchaya.ubicacion.domain.Ciudad;
import co.edu.uco.canchaya.ubicacion.domain.Departamento;

/** HU8: caso de uso «modificar» de Escenario (políticas 2, 3, 6, 7 y 13; P1 de la especificación). */
class ModificarEscenarioCasoUsoTest {

	private static final UUID RIONEGRO = UUID.randomUUID();
	private static final UUID PROPIETARIO_ID = UUID.randomUUID();
	private static final Solicitante PROPIETARIO = new Solicitante(PROPIETARIO_ID, Rol.ADMINISTRADOR_CANCHA);
	private static final String FOTO_1 = "https://fotos.test/1.jpg";
	private static final String FOTO_2 = "https://fotos.test/2.jpg";
	private static final String FOTO_3 = "https://fotos.test/3.jpg";
	private static final int MB = 1024 * 1024;

	private RepositorioEnMemoria repositorio;
	private AlmacenamientoEnMemoria almacenamiento;
	private ModificarEscenarioCasoUso casoUso;
	private Escenario guardado;

	@BeforeEach
	void preparar() {
		repositorio = new RepositorioEnMemoria();
		almacenamiento = new AlmacenamientoEnMemoria();
		// PAR-ESC-01: 5 fotos, PAR-ESC-02: 2 MB, PAR-ESC-03: JPG y PNG
		casoUso = new ModificarEscenarioCasoUso(repositorio, almacenamiento, new UbicacionFalsa(), 5, 2,
				List.of("JPG", "PNG"));
		guardado = Escenario.reconstruir(UUID.randomUUID(), "Cancha La 80", null, "Calle 80 # 45-10", RIONEGRO,
				new BigDecimal("6.155123"), new BigDecimal("-75.373700"), "+573001234567", null,
				List.of(FOTO_1, FOTO_2, FOTO_3), PROPIETARIO_ID, EstadoEscenario.ACTIVO);
		repositorio.escenarios.put(guardado.getId(), guardado);
	}

	private static DatosModificacionEscenario datos(UUID ciudad, String latitud, List<String> conservadas,
			List<FotoAdjunta> nuevas) {
		return new DatosModificacionEscenario("Canchas La 70", "Tres canchas de F5", "Cra 70 # 10-20", ciudad,
				new BigDecimal(latitud), new BigDecimal("-75.5635"), "+576045551234", "contacto@la70.com",
				conservadas, nuevas);
	}

	private static FotoAdjunta jpg(String nombre, int bytes) {
		byte[] contenido = new byte[bytes];
		contenido[0] = (byte) 0xFF;
		contenido[1] = (byte) 0xD8;
		contenido[2] = (byte) 0xFF;
		return new FotoAdjunta(nombre, contenido);
	}

	private static FotoAdjunta gif(String nombre) {
		return new FotoAdjunta(nombre, "GIF89a....".getBytes());
	}

	private ReglaNegocioException esperarError(UUID escenarioId, DatosModificacionEscenario datos,
			Solicitante solicitante, String codigo) {
		var error = assertThatThrownBy(() -> casoUso.ejecutar(escenarioId, datos, solicitante))
				.isInstanceOf(ReglaNegocioException.class).actual();
		ReglaNegocioException regla = (ReglaNegocioException) error;
		assertThat(regla.getCodigoMensaje()).isEqualTo(codigo);
		// Ninguna regla incumplida deja algo guardado ni fotos sueltas
		assertThat(repositorio.actualizados).isEmpty();
		assertThat(almacenamiento.subidas).isEmpty();
		assertThat(almacenamiento.borradas).isEmpty();
		return regla;
	}

	@Test
	void ca1GuardaLosDatosNuevos() {
		Escenario escenario = casoUso.ejecutar(guardado.getId(),
				datos(RIONEGRO, "6.25", List.of(FOTO_1, FOTO_2, FOTO_3), List.of()), PROPIETARIO);

		assertThat(repositorio.actualizados).containsExactly(escenario);
		assertThat(escenario.getNombre()).isEqualTo("Canchas La 70");
		assertThat(escenario.getLatitud()).isEqualByComparingTo("6.25");
		assertThat(escenario.getEstado()).isEqualTo(EstadoEscenario.ACTIVO);
		// Sin cambios en las fotos, no se sube ni se borra nada
		assertThat(almacenamiento.subidas).isEmpty();
		assertThat(almacenamiento.borradas).isEmpty();
	}

	@Test
	void ca1ConservaLasFotosElegidasAgregaLasNuevasYBorraLasQuitadas() {
		Escenario escenario = casoUso.ejecutar(guardado.getId(),
				datos(RIONEGRO, "6.25", List.of(FOTO_3, FOTO_1), List.of(jpg("nueva.jpg", 10))), PROPIETARIO);

		assertThat(almacenamiento.subidas).hasSize(1);
		String nueva = almacenamiento.subidas.getFirst();
		// Primero las conservadas, en el orden enviado, y después las nuevas
		assertThat(escenario.getFotos()).containsExactly(FOTO_3, FOTO_1, nueva);
		// La quitada se borra solo después de guardar
		assertThat(almacenamiento.borradas).containsExactly(FOTO_2);
		assertThat(almacenamiento.borradoAntesDeGuardar).isFalse();
	}

	@Test
	void ca1SiFallaElGuardadoBorraLasNuevasYConservaLasAnteriores() {
		repositorio.fallar = true;

		assertThatThrownBy(() -> casoUso.ejecutar(guardado.getId(),
				datos(RIONEGRO, "6.25", List.of(FOTO_1), List.of(jpg("nueva.jpg", 10))), PROPIETARIO))
				.isInstanceOf(IllegalStateException.class);

		// Solo se borra la foto recién subida; las anteriores siguen en uso
		assertThat(almacenamiento.borradas).containsExactlyElementsOf(almacenamiento.subidas);
		assertThat(almacenamiento.borradas).doesNotContain(FOTO_2, FOTO_3);
	}

	@Test
	void ca3ElEscenarioNoExiste() {
		esperarError(UUID.randomUUID(), datos(RIONEGRO, "6", List.of(), List.of(jpg("a.jpg", 10))), PROPIETARIO,
				"ERR-ESC-06");
	}

	@Test
	void ca4OtroAdministradorDeCanchaNoPuedeModificar() {
		Solicitante otro = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(guardado.getId(), datos(RIONEGRO, "6", List.of(), List.of(jpg("a.jpg", 10))), otro,
				"ERR-ESC-07");
	}

	@Test
	void ca4ElPermisoSeRevisaAntesQueLosDatos() {
		// Quien no es el dueño no averigua si la ciudad o la ubicación son válidas
		Solicitante otro = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);

		esperarError(guardado.getId(), datos(UUID.randomUUID(), "91", List.of(), List.of()), otro, "ERR-ESC-07");
	}

	@Test
	void ca5LaCiudadNoExiste() {
		esperarError(guardado.getId(), datos(UUID.randomUUID(), "6", List.of(), List.of(jpg("a.jpg", 10))),
				PROPIETARIO, "ERR-ESC-02");
	}

	@Test
	void ca6LaUbicacionNoEsValida() {
		esperarError(guardado.getId(), datos(RIONEGRO, "90.5", List.of(), List.of(jpg("a.jpg", 10))), PROPIETARIO,
				"ERR-ESC-03");
	}

	@Test
	void ca8ConservadasMasNuevasNoPuedenPasarDeCinco() {
		List<FotoAdjunta> tres = List.of(jpg("a.jpg", 10), jpg("b.jpg", 10), jpg("c.jpg", 10));

		ReglaNegocioException error = esperarError(guardado.getId(),
				datos(RIONEGRO, "6", List.of(FOTO_1, FOTO_2, FOTO_3), tres), PROPIETARIO, "ERR-ESC-13");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("ERR-ESC-13.cantidad", 5));
	}

	@Test
	void ca8CincoEnTotalSePermiten() {
		Escenario escenario = casoUso.ejecutar(guardado.getId(),
				datos(RIONEGRO, "6", List.of(FOTO_1, FOTO_2, FOTO_3), List.of(jpg("a.jpg", 10), jpg("b.jpg", 10))),
				PROPIETARIO);

		assertThat(escenario.getFotos()).hasSize(5);
	}

	@Test
	void ca9UnaFotoNuevaQueNoEsJpgNiPng() {
		ReglaNegocioException error = esperarError(guardado.getId(),
				datos(RIONEGRO, "6", List.of(FOTO_1), List.of(jpg("a.jpg", 10), gif("animada.gif"))), PROPIETARIO,
				"ERR-ESC-13");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("ERR-ESC-13.formato", "animada.gif"));
	}

	@Test
	void ca10UnaFotoNuevaDeMasDeDosMb() {
		ReglaNegocioException error = esperarError(guardado.getId(),
				datos(RIONEGRO, "6", List.of(), List.of(jpg("grande.jpg", 2 * MB + 1))), PROPIETARIO, "ERR-ESC-13");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("ERR-ESC-13.tamano", "grande.jpg", 2));
	}

	@Test
	void p1UnaFotoConservadaQueNoEsDelEscenarioEsErrGen07() {
		esperarError(guardado.getId(), datos(RIONEGRO, "6", List.of(FOTO_1, "https://otro.sitio/x.jpg"), List.of()),
				PROPIETARIO, "ERR-GEN-07");
	}

	@Test
	void p1UnaFotoConservadaRepetidaEsErrGen07() {
		esperarError(guardado.getId(), datos(RIONEGRO, "6", List.of(FOTO_1, FOTO_1), List.of()), PROPIETARIO,
				"ERR-GEN-07");
	}

	private static final class RepositorioEnMemoria implements RepositorioEscenarios {

		final Map<UUID, Escenario> escenarios = new HashMap<>();
		final List<Escenario> actualizados = new ArrayList<>();
		boolean fallar;

		@Override
		public Optional<Escenario> buscarPorId(UUID id) {
			return Optional.ofNullable(escenarios.get(id));
		}

		@Override
		public void actualizar(Escenario escenario) {
			if (fallar) {
				throw new IllegalStateException("base caída");
			}
			actualizados.add(escenario);
		}

		// Lo que sigue es de otras HU; la HU8 no lo usa

		@Override
		public boolean existePorId(UUID id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void guardar(Escenario escenario) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void actualizarEstado(Escenario escenario) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<ResumenEscenario> listar(EstadoEscenario estado) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<UUID> idsCanchas(UUID escenarioId) {
			throw new UnsupportedOperationException();
		}

	}

	private final class AlmacenamientoEnMemoria implements AlmacenamientoFotos {

		final List<String> subidas = new ArrayList<>();
		final List<String> borradas = new ArrayList<>();
		boolean borradoAntesDeGuardar;

		@Override
		public String subir(String nombre, byte[] contenido, String tipoContenido) {
			String url = "https://fotos.test/" + nombre;
			subidas.add(url);
			return url;
		}

		@Override
		public void borrar(String url) {
			if (repositorio.actualizados.isEmpty() && !subidas.contains(url)) {
				borradoAntesDeGuardar = true;
			}
			borradas.add(url);
		}

	}

	private static final class UbicacionFalsa implements ConsultaUbicacion {

		@Override
		public boolean existeCiudad(UUID ciudadId) {
			return RIONEGRO.equals(ciudadId);
		}

		@Override
		public List<Departamento> listarDepartamentos() {
			return List.of();
		}

		@Override
		public List<Ciudad> listarCiudades(UUID departamentoId) {
			return List.of();
		}

		@Override
		public List<Ciudad> buscarCiudades(Collection<UUID> ciudadIds) {
			return List.of();
		}

	}

}
