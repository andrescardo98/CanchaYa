# HU24 · Tareas

[spec.md](spec.md) está **Aprobada**. En cada tarea se escribe primero la prueba del criterio.

- [x] T1 · Dominio · `Reserva.cancelar(solicitante, ahora, parametros)` (P4); `estado`, `origenCancelacion` y `fechaCancelacion` dejan de ser `final`. Con `ReservaCancelarTest` (CA1, CA3, CA4 y CA5; políticas 14, 15, 16 y 19)
- [x] T2 · Caso de uso · `CancelarReserva` y `CancelarReservaCasoUso` (`TransactionOperations`, actualización condicional, P3), y `buscarPorId` y `cancelarSiSigueConfirmada` en `RepositorioReservas` (en `RepositorioReservasJpa` quedan pendientes hasta T3; se ajustan los falsos que implementan el puerto). Con `CancelarReservaCasoUsoTest` (CA1 y CA4)
- [x] T3 · Persistencia · `RepositorioReservasJpa.buscarPorId` y `cancelarSiSigueConfirmada` (`update ... where estado = 'CONFIRMADA'`). Con pruebas en `RepositorioReservasJpaTest` (CA1, CA4 y política 20)
- [x] T4 · REST · `POST /api/reservas/{id}/cancelacion` (P1) y `RespuestaCancelacion`; `ERR-RES-14 → 404` y `ERR-RES-15 → 403` en `ManejadorErrores`; `ERR-RES-14`, `ERR-RES-15`, `ERR-RES-16` y `EXI-RES-02` en el catálogo. Con pruebas en `ReservaControllerTest` (CA1, CA3, CA4, CA5 y datos inválidos)
- [ ] T5 · Punta a punta · `CancelacionPuntaAPuntaTest`: cancelar y ver la franja libre, faltan 2 horas, cancelar dos veces y otro jugador (CA1, CA3, CA4 y CA5)
- [ ] T6 · Verificación · `./mvnw verify` y `/pull-request`
