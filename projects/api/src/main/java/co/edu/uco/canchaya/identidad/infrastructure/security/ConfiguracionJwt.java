package co.edu.uco.canchaya.identidad.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * El token se firma con una clave secreta que solo conoce el servidor (HS256). Si alguien
 * cambia el rol o la fecha de vencimiento del token, la firma deja de coincidir y se rechaza.
 */
@Configuration
class ConfiguracionJwt {

	// HS256 exige una clave de al menos 256 bits
	private static final int BYTES_MINIMOS = 32;

	@Bean
	SecretKey claveJwt(@Value("${canchaya.jwt.secreto}") String secreto) {
		byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < BYTES_MINIMOS) {
			throw new IllegalStateException("JWT_SECRETO debe tener al menos " + BYTES_MINIMOS + " caracteres");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey clave) {
		return NimbusJwtEncoder.withSecretKey(clave).algorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey clave, Clock reloj) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
		// Sin margen: un token vencido se rechaza en el minuto en que vence (PAR-USU-09, CA6).
		// Por defecto Spring lo acepta 60 segundos más.
		JwtTimestampValidator vigencia = new JwtTimestampValidator(Duration.ZERO);
		vigencia.setClock(reloj);
		decoder.setJwtValidator(vigencia);
		return decoder;
	}

}
