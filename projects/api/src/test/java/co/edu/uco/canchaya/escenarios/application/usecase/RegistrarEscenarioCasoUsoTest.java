package co.edu.uco.canchaya.escenarios.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uco.canchaya.escenarios.application.port.in.RegistrarEscenario.DatosRegistroEscenario;
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

class RegistrarEscenarioCasoUsoTest {

	private static final UUID RIONEGRO = UUID.randomUUID();
	private static final Solicitante PROPIETARIO = new Solicitante(UUID.randomUUID(), Rol.ADMINISTRADOR_CANCHA);
	private static final int MB = 1024 * 1024;

	private RepositorioEnMemoria repositorio;
	private AlmacenamientoEnMemoria almacenamiento;
	private RegistrarEscenarioCasoUso casoUso;

	@BeforeEach
	void preparar() {
		repositorio = new RepositorioEnMemoria();
		almacenamiento = new AlmacenamientoEnMemoria();
		// PAR-ESC-01: 5 fotos, PAR-ESC-02: 2 MB, PAR-ESC-03: JPG y PNG
		casoUso = new RegistrarEscenarioCasoUso(repositorio, almacenamiento, new UbicacionFalsa(), 5, 2,
				List.of("JPG", " png "));
	}

	private static DatosRegistroEscenario datos(UUID ciudad, String latitud, List<FotoAdjunta> fotos) {
		return new DatosRegistroEscenario("Cancha La 80", "Dos canchas de F5", "Calle 80 # 45-10", ciudad,
				new BigDecimal(latitud), new BigDecimal("-75.3737"), "+573001234567", "contacto@la80.com", fotos);
	}

	private static FotoAdjunta jpg(String nombre, int bytes) {
		byte[] contenido = new byte[bytes];
		contenido[0] = (byte) 0xFF;
		contenido[1] = (byte) 0xD8;
		contenido[2] = (byte) 0xFF;
		return new FotoAdjunta(nombre, contenido);
	}

	private static FotoAdjunta png(String nombre) {
		return new FotoAdjunta(nombre, new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0 });
	}

	private ReglaNegocioException esperarError(DatosRegistroEscenario datos, Solicitante solicitante, String codigo) {
		var error = (ReglaNegocioException) assertThatThrownBy(() -> casoUso.ejecutar(datos, solicitante))
				.isInstanceOf(ReglaNegocioException.class).actual();
		assertThat(error.getCodigoMensaje()).isEqualTo(codigo);
		// Ningún error deja datos guardados ni fotos subidas
		assertThat(repositorio.guardados).isEmpty();
		assertThat(almacenamiento.subidas).isEmpty();
		return error;
	}

	@Test
	void ca1GuardaElEscenarioInactivoDelPropietarioConSusFotos() {
		Escenario escenario = casoUso.ejecutar(datos(RIONEGRO, "6.155", List.of(jpg("a.jpg", 10), png("b.png"))),
				PROPIETARIO);

		assertThat(repositorio.guardados).containsExactly(escenario);
		assertThat(escenario.getEstado()).isEqualTo(EstadoEscenario.INACTIVO);
		assertThat(escenario.getAdministradorId()).isEqualTo(PROPIETARIO.usuarioId());
		assertThat(escenario.getFotos()).hasSize(2).containsExactlyElementsOf(almacenamiento.subidas);
		assertThat(almacenamiento.tipos).containsExactly("image/jpeg", "image/png");
		assertThat(almacenamiento.subidas.get(0)).endsWith(".jpg");
		assertThat(almacenamiento.subidas.get(1)).endsWith(".png");
	}

	@Test
	void ca1GuardaSinFotos() {
		Escenario escenario = casoUso.ejecutar(datos(RIONEGRO, "6.155", null), PROPIETARIO);
		assertThat(escenario.getFotos()).isEmpty();
		assertThat(repositorio.guardados).hasSize(1);
	}

	@Test
	void politica4CambiaElIdentificadorSiYaExiste() {
		repositorio.idRepetidos = 2;
		Escenario escenario = casoUso.ejecutar(datos(RIONEGRO, "6.155", List.of()), PROPIETARIO);
		assertThat(repositorio.consultasDeId).isEqualTo(3);
		assertThat(repositorio.guardados).containsExactly(escenario);
	}

	@Test
	void ca3RechazaAUnJugadorSinSubirFotos() {
		esperarError(datos(RIONEGRO, "6.155", List.of(jpg("a.jpg", 10))),
				new Solicitante(UUID.randomUUID(), Rol.JUGADOR), "ERR-ESC-01");
	}

	@Test
	void ca4RechazaUnaCiudadQueNoExiste() {
		esperarError(datos(UUID.randomUUID(), "6.155", List.of(jpg("a.jpg", 10))), PROPIETARIO, "ERR-ESC-02");
	}

	@Test
	void ca5RechazaUnaLatitudFueraDeRango() {
		esperarError(datos(RIONEGRO, "90.5", List.of(jpg("a.jpg", 10))), PROPIETARIO, "ERR-ESC-03");
	}

	@Test
	void ca9RechazaMasDeCincoFotos() {
		List<FotoAdjunta> seis = List.of(jpg("1.jpg", 10), jpg("2.jpg", 10), jpg("3.jpg", 10), jpg("4.jpg", 10),
				jpg("5.jpg", 10), jpg("6.jpg", 10));
		ReglaNegocioException error = esperarError(datos(RIONEGRO, "6.155", seis), PROPIETARIO, "ERR-ESC-13");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("ERR-ESC-13.cantidad", 5));
	}

	@Test
	void ca10RechazaUnaFotoQueNoEsJpgNiPngAunqueSeLlameAsi() {
		// Un GIF renombrado: lo que cuenta es el contenido, no la extensión
		FotoAdjunta gif = new FotoAdjunta("falsa.jpg", "GIF89a....".getBytes());
		ReglaNegocioException error = esperarError(datos(RIONEGRO, "6.155", List.of(jpg("a.jpg", 10), gif)),
				PROPIETARIO, "ERR-ESC-13");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("ERR-ESC-13.formato", "falsa.jpg"));
	}

	@Test
	void ca10RechazaUnFormatoQueElParametroNoAdmite() {
		casoUso = new RegistrarEscenarioCasoUso(repositorio, almacenamiento, new UbicacionFalsa(), 5, 2,
				List.of("JPG"));
		esperarError(datos(RIONEGRO, "6.155", List.of(png("b.png"))), PROPIETARIO, "ERR-ESC-13");
	}

	@Test
	void ca11RechazaUnaFotoDeMasDeDosMb() {
		ReglaNegocioException error = esperarError(datos(RIONEGRO, "6.155", List.of(jpg("grande.jpg", 2 * MB + 1))),
				PROPIETARIO, "ERR-ESC-13");
		assertThat(error.getParametros()).containsExactly(new TextoCatalogo("ERR-ESC-13.tamano", "grande.jpg", 2));
	}

	@Test
	void ca11AceptaUnaFotoDeExactamenteDosMb() {
		casoUso.ejecutar(datos(RIONEGRO, "6.155", List.of(jpg("justa.jpg", 2 * MB))), PROPIETARIO);
		assertThat(repositorio.guardados).hasSize(1);
	}

	@Test
	void siFallaElGuardadoBorraLasFotosSubidas() {
		repositorio.fallar = true;
		assertThatThrownBy(() -> casoUso.ejecutar(datos(RIONEGRO, "6.155", List.of(jpg("a.jpg", 10))), PROPIETARIO))
				.isInstanceOf(IllegalStateException.class);
		assertThat(almacenamiento.borradas).containsExactlyElementsOf(almacenamiento.subidas).hasSize(1);
	}

	private static final class RepositorioEnMemoria implements RepositorioEscenarios {

		final List<Escenario> guardados = new ArrayList<>();
		int idRepetidos;
		int consultasDeId;
		boolean fallar;

		@Override
		public boolean existePorId(UUID id) {
			consultasDeId++;
			return idRepetidos-- > 0;
		}

		@Override
		public void guardar(Escenario escenario) {
			if (fallar) {
				throw new IllegalStateException("base caída");
			}
			guardados.add(escenario);
		}

	}

	private static final class AlmacenamientoEnMemoria implements AlmacenamientoFotos {

		final List<String> subidas = new ArrayList<>();
		final List<String> tipos = new ArrayList<>();
		final List<String> borradas = new ArrayList<>();

		@Override
		public String subir(String nombre, byte[] contenido, String tipoContenido) {
			String url = "https://fotos.test/" + nombre;
			subidas.add(url);
			tipos.add(tipoContenido);
			return url;
		}

		@Override
		public void borrar(String url) {
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

	}

}
