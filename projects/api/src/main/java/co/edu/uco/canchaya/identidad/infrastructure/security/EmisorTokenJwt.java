package co.edu.uco.canchaya.identidad.infrastructure.security;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import co.edu.uco.canchaya.identidad.application.port.out.EmisorToken;
import co.edu.uco.canchaya.identidad.domain.Usuario;

@Component
class EmisorTokenJwt implements EmisorToken {

	/** Nombre del dato del token que lleva el rol del usuario. */
	static final String ROL = "rol";

	private final JwtEncoder encoder;
	private final Clock reloj;
	private final int minutosVigencia;

	EmisorTokenJwt(JwtEncoder encoder, Clock reloj,
			@Value("${canchaya.usuario.vigencia-token-minutos}") int minutosVigencia) {
		this.encoder = encoder;
		this.reloj = reloj;
		this.minutosVigencia = minutosVigencia;
	}

	@Override
	public TokenEmitido generar(Usuario usuario) {
		Instant ahora = reloj.instant().truncatedTo(ChronoUnit.SECONDS);
		Instant venceEn = ahora.plus(minutosVigencia, ChronoUnit.MINUTES);
		// El token solo lleva el id y el rol (restricción de diseño de HU2), nada de datos personales
		JwtClaimsSet datos = JwtClaimsSet.builder()
				.subject(usuario.getId().toString())
				.claim(ROL, usuario.getRol().name())
				.issuedAt(ahora)
				.expiresAt(venceEn)
				.build();
		String valor = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), datos))
				.getTokenValue();
		return new TokenEmitido(valor, venceEn);
	}

}
