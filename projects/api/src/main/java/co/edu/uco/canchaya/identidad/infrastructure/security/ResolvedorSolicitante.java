package co.edu.uco.canchaya.identidad.infrastructure.security;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import co.edu.uco.canchaya.identidad.domain.Rol;
import co.edu.uco.canchaya.identidad.domain.Solicitante;

/**
 * Un controlador de cualquier módulo recibe un parámetro Solicitante y aquí se llena con el id y
 * el rol del token (RT-06). Así ningún otro módulo tiene que saber cómo está armado el JWT.
 */
@Configuration(proxyBeanMethods = false)
class ResolvedorSolicitante implements HandlerMethodArgumentResolver, WebMvcConfigurer {

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvedores) {
		resolvedores.add(this);
	}

	@Override
	public boolean supportsParameter(MethodParameter parametro) {
		return Solicitante.class.equals(parametro.getParameterType());
	}

	// Sin token no se llega aquí: la seguridad ya respondió 401 (ERR-GEN-05)
	@Override
	public Solicitante resolveArgument(MethodParameter parametro, ModelAndViewContainer modelo,
			NativeWebRequest peticion, WebDataBinderFactory fabrica) {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion == null || !(autenticacion.getPrincipal() instanceof Jwt token)) {
			return null;
		}
		return new Solicitante(UUID.fromString(token.getSubject()),
				Rol.valueOf(token.getClaimAsString(EmisorTokenJwt.ROL)));
	}

}
