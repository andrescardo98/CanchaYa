package co.edu.uco.canchaya.identidad.application.port.out;

import java.time.Instant;

import co.edu.uco.canchaya.identidad.domain.Usuario;

public interface EmisorToken {

	/** Token de acceso (JWT) con el identificador y el rol del usuario. Dura PAR-USU-09 (D37). */
	TokenEmitido generar(Usuario usuario);

	record TokenEmitido(String valor, Instant venceEn) {
	}

}
