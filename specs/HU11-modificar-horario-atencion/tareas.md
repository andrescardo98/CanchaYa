# HU11 · Tareas

[spec.md](spec.md) está **Aprobada**. En cada tarea se escribe primero la prueba del criterio.

- [ ] T1 · Dominio · `HorarioRecurrente.modificar(escenario, diaSemana, horaInicio, horaFin, otrosHorariosDelDia, solicitante)`, compartiendo con `definir` las validaciones de 13 y 3, con `HorarioRecurrenteModificarTest` (CA1, CA4 a CA7; políticas 2, 13, 3 y 7). Las pruebas de HU7 siguen pasando
- [ ] T2 · Caso de uso · `ModificarHorario` y `ModificarHorarioCasoUso`, con `ModificarHorarioCasoUsoTest` (CA1, CA3, CA4; política 6). Agrega `buscarPorId` y `actualizar` a `RepositorioHorarios` y a `HorariosEnMemoria`; en `RepositorioHorariosJpa` quedan pendientes hasta T3
- [ ] T3 · Persistencia · `buscarPorId` y `actualizar` (ERR-HOR-07 si la base rechaza el cruce), con `ModificacionHorarioPersistenciaTest` (CA1, CA6 y CA8 con la reserva insertada por SQL, P3)
- [ ] T4 · REST · `PUT /api/horarios/{id}` en `HorarioController`, con el prefijo de la clase pasado a cada método (P1), `SolicitudDefinirHorario` como entrada (P2), `RespuestaModificarHorario`, `ERR-HOR-06 → 404`, y `EXI-HOR-02`, `ERR-HOR-06` y `ERR-HOR-07` en el catálogo, con `ModificarHorarioControllerTest` (CA1 a CA7)
- [ ] T5 · Punta a punta · `ModificarHorarioPuntaAPuntaTest`: modificar y ver el cambio en la lista y en la disponibilidad, inexistente y no propietario (CA1, CA3, CA4, CA8)
- [ ] T6 · Verificación · `./mvnw verify` y `/pull-request`
