package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.util.UUID;

import co.edu.uco.canchaya.escenarios.domain.EstadoEscenario;

// HU13, CA2 y CA3: el mensaje EXI-ESC-03 y el nuevo estado, para que la pantalla actualice la fila
record RespuestaCambioEstado(String codigo, String mensaje, UUID id, EstadoEscenario estado) {
}
