# HU12 · Tareas

[spec.md](spec.md) está **Aprobada**. En cada tarea se escribe primero la prueba del criterio. De T2 en adelante se espera a que HU11 esté en `main` y se rebasa la rama (P1).

- [ ] T1 · Dominio · `HorarioRecurrente.validarEliminacion(escenario, solicitante, tieneReservasConfirmadasFuturas)` y `DiaSemana.aDayOfWeek()`, con `HorarioRecurrenteEliminarTest` (CA1, CA5, CA6; políticas 2 y 15)
- [ ] T2 · Reservas · `ConsultaReservasFuturas.existenConfirmadasFuturasEnHorario`, su implementación en `ConsultaReservasFuturasCasoUso` y `RepositorioReservas.existenConfirmadasEnHorarioQueEmpiezanDespuesDe` en `RepositorioReservasJpa`, con pruebas de persistencia que insertan reservas con SQL (CA6, P2)
- [ ] T3 · Caso de uso · `EliminarHorario` y `EliminarHorarioCasoUso` (turnos en orden dentro de `transaccion.execute`), y `eliminar` en `RepositorioHorarios` y `HorariosEnMemoria`, con `EliminarHorarioCasoUsoTest` (CA1, CA4, CA5, CA6; política 6)
- [ ] T4 · Persistencia · `RepositorioHorariosJpa.eliminar`, con su prueba (CA1)
- [ ] T5 · REST · `DELETE /api/horarios/{id}` en `HorarioController`, `RespuestaEliminarHorario`, y `EXI-HOR-03` y `ERR-HOR-15` en el catálogo, con `EliminarHorarioControllerTest` (CA1, CA4, CA6)
- [ ] T6 · Punta a punta · `EliminarHorarioPuntaAPuntaTest`: eliminar y ver el cambio en la lista y en la disponibilidad, no propietario y con reservas futuras (CA1, CA5, CA6)
- [ ] T7 · Verificación · `./mvnw verify` y `/pull-request`
