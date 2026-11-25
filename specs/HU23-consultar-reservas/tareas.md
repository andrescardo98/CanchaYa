# HU23 · Tareas

[spec.md](spec.md) está **Aprobada** (24-nov-2026). En cada tarea se escribe primero la prueba del criterio.

- [ ] T1 · Dominio · `Reserva.esVigente(ahora)` y `Reserva.sePuedeCancelarPorJugador(ahora, parametros)`, con `ReservaConsultaTest` (CA1 según P1; CA3 y política 16: más de 3 h, exactamente 3 h, menos, no CONFIRMADA)
- [ ] T2 · Caso de uso · `ConsultarMisReservas` (con `MisReservas` y `ReservaDelJugador`), `ConsultaCanchas.nombres` (solo la firma y `NombresCancha`) y `ConsultarMisReservasCasoUso`, con `ConsultarMisReservasCasoUsoTest` (CA1 orden y nombres, CA3 con reloj fijo, CA5 id del solicitante, P2 `ERR-GEN-06` si no es JUGADOR)
- [ ] T3 · Persistencia · `RepositorioReservas.deJugador` en `RepositorioReservasJpa` (`ReservaJpa.findByJugadorId`) y `ConsultaCanchas.nombres` en `ConsultaCanchasCasoUso` con `RepositorioCanchas.nombresConEscenario` (un solo `select ... join`). Pruebas: `RepositorioReservasJpaTest.CA5_deJugador_soloLasSuyas` y `ConsultaCanchasNombresPersistenciaTest.CA1_canchaInactiva_tambienTraeNombres`
- [ ] T4 · REST · `GET /api/reservas/mias` en `ReservaController`, `RespuestaMisReservas`, `RespuestaReservaDelJugador` (motivo solo con ADMINISTRADOR) e `INF-RES-01` en `catalogo-mensajes.properties`, con `ReservaControllerTest` (CA1, CA2, CA4)
- [ ] T5 · Punta a punta · `MisReservasPuntaAPuntaTest`: reservar y verla en vigentes (CA1), reserva cancelada insertada por SQL (CA4), dos jugadores (CA5), administrador → 403 `ERR-GEN-06` (P2) y sin token → 401
- [ ] T6 · Verificación · `./mvnw verify` en verde (incluye `ArquitecturaTest`), estado de `spec.md` en **Implementada** y `/pull-request`
