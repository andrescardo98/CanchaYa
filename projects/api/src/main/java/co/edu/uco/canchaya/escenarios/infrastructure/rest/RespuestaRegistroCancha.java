package co.edu.uco.canchaya.escenarios.infrastructure.rest;

import java.util.UUID;

// Solo el id: el estado no lo ve el usuario en el formulario (restricción de la HU)
record RespuestaRegistroCancha(String codigo, String mensaje, UUID id) {
}
