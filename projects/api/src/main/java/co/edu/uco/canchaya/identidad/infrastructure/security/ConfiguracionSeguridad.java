package co.edu.uco.canchaya.identidad.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Qué se puede usar sin sesión y qué exige el token (AG-11, D32). Todo lo que no esté en la
 * lista de rutas abiertas pide un token válido; así una ruta nueva nace protegida.
 */
@Configuration
class ConfiguracionSeguridad {

	@Bean
	SecurityFilterChain seguridad(HttpSecurity http, RespuestaSinSesion respuestaSinSesion) throws Exception {
		return http
				// El token viaja en la cabecera Authorization y no en una cookie, así que no aplica CSRF
				.csrf(AbstractHttpConfigurer::disable)
				// El servidor no guarda sesiones: cada petición trae su token (D37)
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(rutas -> rutas
						.requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll() // HU1
						.requestMatchers(HttpMethod.GET, "/api/usuarios/correo-disponible").permitAll() // HU1
						.requestMatchers(HttpMethod.POST, "/api/sesiones").permitAll() // HU2
						.requestMatchers("/actuator/health").permitAll() // monitor de UptimeRobot (D31)
						.requestMatchers("/error").permitAll()
						.anyRequest().authenticated())
				// Sin token o con el token vencido o alterado: 401 con ERR-GEN-05 (CA5, CA6)
				.oauth2ResourceServer(recurso -> recurso
						.jwt(jwt -> {
						})
						.authenticationEntryPoint(respuestaSinSesion))
				.exceptionHandling(errores -> errores.authenticationEntryPoint(respuestaSinSesion))
				.build();
	}

}
