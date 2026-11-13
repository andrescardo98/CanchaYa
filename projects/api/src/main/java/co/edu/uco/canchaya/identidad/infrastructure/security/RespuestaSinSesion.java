package co.edu.uco.canchaya.identidad.infrastructure.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;
import co.edu.uco.canchaya.transversal.infrastructure.rest.RespuestaError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Responde 401 con ERR-GEN-05 y el mismo formato de los demás errores. Spring Security
 * rechaza la petición antes de llegar a los controladores, por eso ManejadorErrores no la ve.
 * El frontend, al recibir este 401, borra el token y lleva al inicio de sesión (CA5, CA6).
 */
@Component
class RespuestaSinSesion implements AuthenticationEntryPoint {

	private final CatalogoMensajes catalogo;
	private final JsonMapper json;

	RespuestaSinSesion(CatalogoMensajes catalogo, JsonMapper json) {
		this.catalogo = catalogo;
		this.json = json;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
			throws IOException {
		RespuestaError cuerpo = new RespuestaError(
				List.of(new RespuestaError.Mensaje("ERR-GEN-05", null, catalogo.texto("ERR-GEN-05"))));
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		json.writeValue(response.getOutputStream(), cuerpo);
	}

}
