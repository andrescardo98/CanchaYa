package co.edu.uco.canchaya.identidad.application.usecase;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uco.canchaya.identidad.application.port.in.IniciarSesion;
import co.edu.uco.canchaya.identidad.application.port.out.CifradorContrasena;
import co.edu.uco.canchaya.identidad.application.port.out.ContadorIntentosFallidos;
import co.edu.uco.canchaya.identidad.application.port.out.EmisorToken;
import co.edu.uco.canchaya.identidad.application.port.out.EmisorToken.TokenEmitido;
import co.edu.uco.canchaya.identidad.application.port.out.RepositorioUsuarios;
import co.edu.uco.canchaya.identidad.domain.Usuario;
import co.edu.uco.canchaya.transversal.domain.ReglaNegocioException;

@Service
public class IniciarSesionCasoUso implements IniciarSesion {

	private final RepositorioUsuarios repositorio;
	private final CifradorContrasena cifrador;
	private final EmisorToken emisorToken;
	private final ContadorIntentosFallidos contador;
	private final int minutosBloqueo;

	public IniciarSesionCasoUso(RepositorioUsuarios repositorio, CifradorContrasena cifrador,
			EmisorToken emisorToken, ContadorIntentosFallidos contador,
			@Value("${canchaya.usuario.minutos-bloqueo-inicio-sesion}") int minutosBloqueo) {
		this.repositorio = repositorio;
		this.cifrador = cifrador;
		this.emisorToken = emisorToken;
		this.contador = contador;
		this.minutosBloqueo = minutosBloqueo;
	}

	@Override
	@Transactional(readOnly = true)
	public Sesion ejecutar(Credenciales credenciales, String origen) {
		String correo = Usuario.normalizarCorreo(credenciales.correoElectronico());

		// D28: se revisa antes que la contraseña, para rechazar aunque sea la correcta (CA7)
		if (contador.estaBloqueado(correo, origen)) {
			throw new ReglaNegocioException("ERR-USU-16", minutosBloqueo);
		}

		Optional<Usuario> encontrado = repositorio.buscarPorCorreo(correo);
		// Se compara siempre, aunque el correo no exista, para que la respuesta tarde lo mismo (ESC-SEG-03)
		boolean coincide = cifrador.coincide(credenciales.contrasena(),
				encontrado.map(Usuario::getContrasenaHash).orElse(null));

		// Política 6
		if (encontrado.isEmpty()) {
			contador.registrarFallo(correo, origen);
			throw new ReglaNegocioException("ERR-USU-06");
		}
		// Política 7
		if (!coincide) {
			contador.registrarFallo(correo, origen);
			throw new ReglaNegocioException("ERR-USU-07");
		}

		Usuario usuario = encontrado.get();
		TokenEmitido token = emisorToken.generar(usuario);
		return new Sesion(token.valor(), usuario.getRol(), token.venceEn());
	}

}
