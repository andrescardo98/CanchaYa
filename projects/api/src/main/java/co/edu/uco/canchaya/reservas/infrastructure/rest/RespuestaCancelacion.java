package co.edu.uco.canchaya.reservas.infrastructure.rest;

import java.util.UUID;

import co.edu.uco.canchaya.reservas.domain.EstadoReserva;
import co.edu.uco.canchaya.reservas.domain.Reserva;
import co.edu.uco.canchaya.transversal.infrastructure.rest.CatalogoMensajes;

/**
 * HU24, CA1: la reserva cancelada con EXI-RES-02. Con {@code id} y {@code estado}, «Mis reservas»
 * (HU23) mueve la reserva sin volver a consultar la lista. No lleva el origen ni la fecha de
 * cancelación (AG-14).
 */
record RespuestaCancelacion(String codigo, String mensaje, UUID id, EstadoReserva estado) {

	private static final String CODIGO = "EXI-RES-02";

	static RespuestaCancelacion de(Reserva reserva, CatalogoMensajes catalogo) {
		return new RespuestaCancelacion(CODIGO, catalogo.texto(CODIGO), reserva.getId(), reserva.getEstado());
	}

}
