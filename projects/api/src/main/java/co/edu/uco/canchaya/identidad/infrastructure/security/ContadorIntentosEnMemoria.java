package co.edu.uco.canchaya.identidad.infrastructure.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import co.edu.uco.canchaya.identidad.application.port.out.ContadorIntentosFallidos;

/**
 * Límite de intentos de inicio de sesión (D28, táctica AC-04.3). El conteo vive en la memoria
 * del servidor porque hay una sola copia de la aplicación; si se reinicia, vuelve a cero.
 * Se cuenta aparte por cuenta y por origen: cualquiera de los dos que llegue al límite queda
 * bloqueado.
 */
@Component
class ContadorIntentosEnMemoria implements ContadorIntentosFallidos {

	// Por encima de este número de cuentas y orígenes se borran los que ya no cuentan, para que
	// alguien que pruebe muchos correos distintos no llene la memoria (plan de 512 MB de Render)
	private static final int LIMPIAR_DESDE = 10_000;

	private final Map<String, Registro> registros = new ConcurrentHashMap<>();
	private final Clock reloj;
	private final int intentosPermitidos;
	private final Duration ventana;
	private final Duration bloqueo;

	ContadorIntentosEnMemoria(Clock reloj,
			@Value("${canchaya.usuario.intentos-fallidos-permitidos}") int intentosPermitidos,
			@Value("${canchaya.usuario.minutos-ventana-intentos}") int minutosVentana,
			@Value("${canchaya.usuario.minutos-bloqueo-inicio-sesion}") int minutosBloqueo) {
		this.reloj = reloj;
		this.intentosPermitidos = intentosPermitidos;
		this.ventana = Duration.ofMinutes(minutosVentana);
		this.bloqueo = Duration.ofMinutes(minutosBloqueo);
	}

	@Override
	public boolean estaBloqueado(String correoElectronico, String origen) {
		Instant ahora = reloj.instant();
		return bloqueada(cuenta(correoElectronico), ahora) || bloqueada(origen(origen), ahora);
	}

	@Override
	public void registrarFallo(String correoElectronico, String origen) {
		Instant ahora = reloj.instant();
		fallo(cuenta(correoElectronico), ahora);
		fallo(origen(origen), ahora);
		if (registros.size() > LIMPIAR_DESDE) {
			registros.values().removeIf(registro -> registro.yaNoCuenta(ahora, ventana));
		}
	}

	private boolean bloqueada(String clave, Instant ahora) {
		Registro registro = registros.get(clave);
		if (registro == null) {
			return false;
		}
		synchronized (registro) {
			return registro.bloqueadoHasta != null && ahora.isBefore(registro.bloqueadoHasta);
		}
	}

	private void fallo(String clave, Instant ahora) {
		// compute bloquea la clave: dos fallos al mismo tiempo no se pisan
		registros.compute(clave, (k, registro) -> {
			Registro actual = registro == null ? new Registro() : registro;
			synchronized (actual) {
				if (actual.bloqueadoHasta != null && !ahora.isBefore(actual.bloqueadoHasta)) {
					actual.bloqueadoHasta = null; // terminó el bloqueo: se empieza de cero
				}
				// PAR-USU-11: solo cuentan los fallos de los últimos minutos
				while (!actual.fallos.isEmpty() && !actual.fallos.peekFirst().isAfter(ahora.minus(ventana))) {
					actual.fallos.pollFirst();
				}
				actual.fallos.addLast(ahora);
				// PAR-USU-10 y PAR-USU-12
				if (actual.fallos.size() >= intentosPermitidos) {
					actual.bloqueadoHasta = ahora.plus(bloqueo);
					actual.fallos.clear();
				}
			}
			return actual;
		});
	}

	private static String cuenta(String correoElectronico) {
		return "cuenta:" + correoElectronico;
	}

	private static String origen(String origen) {
		return "origen:" + origen;
	}

	private static final class Registro {

		private final Deque<Instant> fallos = new ArrayDeque<>();
		private Instant bloqueadoHasta;

		private synchronized boolean yaNoCuenta(Instant ahora, Duration ventana) {
			boolean bloqueado = bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
			boolean conFallosRecientes = !fallos.isEmpty() && fallos.peekLast().isAfter(ahora.minus(ventana));
			return !bloqueado && !conFallosRecientes;
		}

	}

}
