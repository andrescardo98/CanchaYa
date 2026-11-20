package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.EstadoCancha;

// HU10, CA1 y CA2: el mensaje EXI-CAN-03 y el nuevo estado, para que la pantalla actualice la fila
record RespuestaCambioEstadoCancha(String codigo, String mensaje, UUID id, EstadoCancha estado) {
}
