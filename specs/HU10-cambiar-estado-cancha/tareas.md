# HU10 · Tareas

[spec.md](spec.md) está **Implementada**. En cada tarea se escribe primero la prueba del criterio.

- [x] T1 · Dominio · `Cancha.cambiarEstado(escenario, nuevoEstado, solicitante, tieneReservasConfirmadasFuturas)`, con `CanchaCambiarEstadoTest` (CA1, CA2, CA4, CA5 y CA6; políticas 2, 10, 12 y 11)
- [x] T2 · Caso de uso · `CambiarEstadoCancha` y `CambiarEstadoCanchaCasoUso` (`@Transactional`; el turno y la consulta de reservas solo al inactivar, P2), con `CambiarEstadoCanchaCasoUsoTest` (CA1 a CA6; políticas 7, 2, 10 y 12). Agrega `actualizarEstado` a `RepositorioCanchas`, a `CanchasEnMemoria` y a los demás falsos que implementan el puerto. En `RepositorioCanchasJpa` queda pendiente hasta T3
- [x] T3 · Persistencia · `RepositorioCanchasJpa.actualizarEstado` y `CanchaJpa.actualizarEstado`, con `CambioEstadoCanchaPersistenciaTest` (CA1)
- [x] T4 · REST · `PATCH /api/canchas/{id}/estado` (P1), `SolicitudCambioEstadoCancha`, `RespuestaCambioEstadoCancha`, y `EXI-CAN-03`, `ERR-CAN-10`, `ERR-CAN-12`, `estado.DISPONIBLE` y `estado.INACTIVA` en el catálogo, con `EstadoCanchaControllerTest` (CA1 a CA6 y datos inválidos). Los otros `@WebMvcTest` de `CanchaController` reciben el nuevo `@MockitoBean`
- [x] T5 · Punta a punta · `CambioEstadoCanchaPuntaAPuntaTest`: inactivar y dejar de verse en el perfil y en la disponibilidad, activar y volver, no propietario, y reserva futura insertada por SQL (CA1, CA2, CA4 y CA6)
- [x] T6 · Verificación · `./mvnw verify` y `/pull-request`
