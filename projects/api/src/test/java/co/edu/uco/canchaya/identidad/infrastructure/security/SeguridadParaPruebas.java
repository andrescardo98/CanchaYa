package co.edu.uco.canchaya.identidad.infrastructure.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Las pruebas de controladores (@WebMvcTest) no cargan las configuraciones de seguridad por su
 * cuenta; sin esto correrían con la seguridad por defecto de Spring y no con la de CanchaYa.
 */
@TestConfiguration
@Import({ ConfiguracionSeguridad.class, ConfiguracionJwt.class, RespuestaSinSesion.class })
public class SeguridadParaPruebas {
}
