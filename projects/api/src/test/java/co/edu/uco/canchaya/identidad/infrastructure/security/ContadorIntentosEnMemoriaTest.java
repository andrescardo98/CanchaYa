package co.edu.uco.canchaya.identidad.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// HU2 CA7, D28: 5 intentos fallidos (PAR-USU-10) en 5 minutos (PAR-USU-11) bloquean 15 minutos (PAR-USU-12)
class ContadorIntentosEnMemoriaTest {

	private static final String CORREO = "ana@correo.com";
	private static final String IP = "203.0.113.7";

	private RelojAjustable reloj;
	private ContadorIntentosEnMemoria contador;

	@BeforeEach
	void preparar() {
		reloj = new RelojAjustable(Instant.parse("2026-11-12T14:00:00Z"));
		contador = new ContadorIntentosEnMemoria(reloj, 5, 5, 15);
	}

	private void fallar(int veces, String correo, String origen) {
		for (int i = 0; i < veces; i++) {
			contador.registrarFallo(correo, origen);
		}
	}

	@Test
	void ca7CuatroFallosTodaviaNoBloquean() {
		fallar(4, CORREO, IP);

		assertThat(contador.estaBloqueado(CORREO, IP)).isFalse();
	}

	@Test
	void ca7ElQuintoFalloEnCincoMinutosBloqueaLaCuenta() {
		fallar(4, CORREO, IP);
		reloj.avanzar(Duration.ofMinutes(4));
		fallar(1, CORREO, IP);

		// Desde otro origen la cuenta sigue bloqueada
		assertThat(contador.estaBloqueado(CORREO, "198.51.100.1")).isTrue();
	}

	@Test
	void ca7CincoFallosDesdeUnMismoOrigenBloqueanElOrigenParaCualquierCuenta() {
		for (int i = 0; i < 5; i++) {
			contador.registrarFallo("cuenta" + i + "@correo.com", IP);
		}

		assertThat(contador.estaBloqueado("otra@correo.com", IP)).isTrue();
		assertThat(contador.estaBloqueado("otra@correo.com", "198.51.100.1")).isFalse();
	}

	@Test
	void ca7LosFallosDeHaceMasDeCincoMinutosYaNoCuentan() {
		fallar(4, CORREO, IP);
		reloj.avanzar(Duration.ofMinutes(5));
		fallar(1, CORREO, IP);

		assertThat(contador.estaBloqueado(CORREO, IP)).isFalse();
	}

	@Test
	void ca7ElBloqueoDuraQuinceMinutos() {
		fallar(5, CORREO, IP);

		reloj.avanzar(Duration.ofMinutes(15).minusSeconds(1));
		assertThat(contador.estaBloqueado(CORREO, IP)).isTrue();

		reloj.avanzar(Duration.ofSeconds(1));
		assertThat(contador.estaBloqueado(CORREO, IP)).isFalse();
	}

	@Test
	void ca7AlTerminarElBloqueoElConteoEmpiezaDeCero() {
		fallar(5, CORREO, IP);
		reloj.avanzar(Duration.ofMinutes(15));

		fallar(4, CORREO, IP);
		assertThat(contador.estaBloqueado(CORREO, IP)).isFalse();
	}

	private static final class RelojAjustable extends Clock {

		private Instant ahora;

		RelojAjustable(Instant inicio) {
			this.ahora = inicio;
		}

		void avanzar(Duration tiempo) {
			ahora = ahora.plus(tiempo);
		}

		@Override
		public Instant instant() {
			return ahora;
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.of("America/Bogota");
		}

		@Override
		public Clock withZone(ZoneId zona) {
			return this;
		}

	}

}
