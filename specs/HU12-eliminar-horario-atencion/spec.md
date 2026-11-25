# HU12 · Eliminar un horario de atención

**Estado:** Implementada (24-nov-2026) · **HU:** [HU12](../../docs/historias/HU12.md) · **Módulo:** Escenarios y canchas (`co.edu.uco.canchaya.escenarios`), con un cambio pequeño en Reservas (implementa `ConsultaReservasFuturas`)

## Fuentes

- Dominio: [HorarioRecurrente](../../docs/dominio/horariorecurrente.md), responsabilidad `eliminar` (políticas 6, 2, 8 y 15) y su tabla «Uso de cada atributo» (solo se necesita el `id`).
- Mensajes: `EXI-HOR-03`, `CNF-HOR-01`, `CNF-HOR-02`, `ERR-HOR-02`, `ERR-HOR-06` y `ERR-HOR-15` ([mensajes.md](../../docs/especificacion/mensajes.md)).
- Parámetros: no aplican.
- Aspectos generales: AG-12 (la propiedad se revisa en el servidor).
- Decisiones: D08 (no se elimina un horario con reservas confirmadas futuras afectadas), D27 (`America/Bogota`), D29 punto 2 y D36 (turno de la cancha) y D39 (paquetes).
- Diseño: `HorarioRecurrente.validarEliminacion(escenario, solicitante, tieneReservasConfirmadasFuturas)` en [clases.md](../../docs/diseno/clases.md): método de instancia, `void`, lanza ERR-HOR-02 y ERR-HOR-15; el caso de uso revisa ERR-HOR-06 antes y hace el borrado. `ConsultaReservasFuturas` la define Escenarios y la implementa Reservas ([componentes.md](../../docs/diseno/componentes.md)).
- Código que se reutiliza: `HorarioRecurrente.validarPropietario` (HU7), `RepositorioEscenarios.buscarPorId` e `idsCanchas`, `RepositorioCanchas.tomarTurno` y el patrón de `CambiarEstadoEscenarioCasoUso` (HU13: turnos en orden dentro de `TransactionOperations`), `ConsultaReservasFuturasCasoUso` y `RepositorioReservasJpa` (HU13), `GET /api/escenarios/{escenarioId}/horarios` (la lista que se actualiza, HU7) y `GET /api/canchas/{canchaId}/disponibilidad` (HU20). De HU11 (se espera a que esté en `main`, P1): `RepositorioHorarios.buscarPorId`, `ERR-HOR-06 → 404` y la ruta `/api/horarios/{id}` en `HorarioController`.

## Qué se construye

El propietario del escenario borra uno de sus horarios de atención. El registro se elimina de la base (borrado definitivo) y, como las franjas se calculan en cada consulta (HU20), esas horas dejan de ofrecerse de inmediato. No se puede eliminar si alguna cancha del escenario tiene una reserva confirmada futura en ese día de la semana y rango de horas (D08). La confirmación antes de borrar (CNF-HOR-01) y la advertencia del último horario (CNF-HOR-02) son de la pantalla: el frontend ya sabe cuántos horarios hay con la lista de HU7 (P4).

## Contrato del servicio

`DELETE /api/horarios/{id}` · con sesión · solo el propietario del escenario del horario · sin cuerpo

Respuesta `200` (P3):

```json
{ "codigo": "EXI-HOR-03", "mensaje": "El horario de atención se eliminó correctamente." }
```

| HTTP | Código | Cuándo |
|---|---|---|
| 200 | `EXI-HOR-03` | Se eliminó |
| 400 | `ERR-GEN-07` | El `id` no es UUID (`ManejadorErrores`) |
| 401 | `ERR-GEN-05` | Sin sesión (Spring Security, como el resto de rutas privadas) |
| 403 | `ERR-HOR-02` | No es el propietario, incluido el encargado de sede y el administrador del sistema (ya está en `ManejadorErrores`) |
| 404 | `ERR-HOR-06` | No existe el horario (la entrada la agrega HU11, P1) |
| 422 | `ERR-HOR-15` | Hay reservas confirmadas futuras dentro del horario. Va con el 422 por defecto, igual que no inactivar con reservas futuras en HU10 y HU13 |

## Diseño por capas

| Capa | Pieza | Qué hace |
|---|---|---|
| Dominio | `HorarioRecurrente.validarEliminacion(Escenario, Solicitante, boolean tieneReservasConfirmadasFuturas)` | Política 2 (`validarPropietario`, ERR-HOR-02) → política 15 (ERR-HOR-15 si `tieneReservasConfirmadasFuturas`). El permiso va primero: quien no es el dueño no se entera de si hay reservas. Si el escenario recibido no es el del horario, `IllegalArgumentException` (error de programación), como en HU11. La política 8 (RN-04) no tiene código aquí: la cumple la búsqueda (HU18) al no mostrar canchas de escenarios sin horarios |
| Puerto de entrada | `EliminarHorario` con `ejecutar(UUID horarioId, Solicitante)` | Contrato del caso de uso; no devuelve nada |
| Caso de uso | `EliminarHorarioCasoUso` | Sin `@Transactional`; dentro de `transaccion.execute`, como HU13: política 6 (`horarios.buscarPorId`, ERR-HOR-06) → `escenarios.buscarPorId(horario.getEscenarioId())` → política 2 (`HorarioRecurrente.validarPropietario`, ERR-HOR-02, P5) → tomar el turno de cada cancha del escenario (`idsCanchas`, ordenadas por id) → `consultaReservasFuturas.existenConfirmadasFuturasEnHorario(...)` solo si el escenario tiene canchas → `horario.validarEliminacion(...)` → `horarios.eliminar(id)` |
| Puerto de salida (Escenarios) | `RepositorioHorarios.buscarPorId(UUID)` | `Optional<HorarioRecurrente>`. Lo agrega HU11; HU12 lo reutiliza (P1) |
| Puerto de salida (Escenarios) | `RepositorioHorarios.eliminar(UUID)` | Nuevo: borra la fila |
| Puerto de salida (Escenarios) | `ConsultaReservasFuturas.existenConfirmadasFuturasEnHorario(Collection<UUID> canchaIds, DayOfWeek dia, LocalTime horaInicio, LocalTime horaFin)` | Nuevo. Usa `java.time.DayOfWeek` y no `DiaSemana`, para que Reservas no dependa del dominio de Escenarios. `DiaSemana` gana el método inverso `aDayOfWeek()` |
| Reservas · caso de uso | `ConsultaReservasFuturasCasoUso` | Implementa el método nuevo con `LocalDateTime.now(reloj)` (D27); lista vacía → `false` sin ir a la base |
| Reservas · puerto de salida | `RepositorioReservas.existenConfirmadasEnHorarioQueEmpiezanDespuesDe(canchaIds, DayOfWeek, horaInicio, horaFin, momento)` | Nuevo |
| Reservas · persistencia | `RepositorioReservasJpa` | `estado = 'CONFIRMADA'`, `extract(isodow from fecha) = :dia` (1 = lunes … 7 = domingo, igual que `DayOfWeek.getValue()`), `hora_inicio < :horaFin and hora_fin > :horaInicio` (se cruza, P2) y `fecha + hora_inicio > :momento` (futura, mismo criterio que HU13) |
| Persistencia (Escenarios) | `RepositorioHorariosJpa.eliminar` | `deleteById` con `flush`. Ninguna tabla tiene llave foránea a `horario_recurrente`, así que el borrado no choca con nada |
| REST | `DELETE /api/horarios/{id}` en `HorarioController` y `RespuestaEliminarHorario(codigo, mensaje)` | Misma ruta base que HU11 (P1 de HU11) |
| Transversal | `EXI-HOR-03` y `ERR-HOR-15` en `catalogo-mensajes.properties` (`ERR-HOR-06` lo agrega HU11) | Hoy no están. `CNF-HOR-01` y `CNF-HOR-02` no se agregan: los muestra la pantalla (P4) |

Sin migraciones.

**Concurrencia (D29 punto 2, D36).** Si un jugador reserva una franja de ese horario mientras el propietario lo elimina, podría quedar una reserva confirmada sin horario, justo lo que la política 15 prohíbe. Para evitarlo, el caso de uso toma el turno de todas las canchas del escenario antes de preguntar por reservas y lo suelta al terminar el borrado, igual que inactivar un escenario (HU13). Reservar (HU21) toma el turno de su cancha y recalcula las franjas dentro de él, así que una reserva que llegue al mismo tiempo espera y después ya no encuentra el horario (ERR-RES-06). Los turnos se toman en orden por id para que dos operaciones no se queden esperando la una a la otra. Dos eliminaciones del mismo horario a la vez: las dos pueden encontrarlo antes de borrar; la segunda no borra nada y responde 200 igual. No deja datos malos, así que no se agrega más control.

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1 | El propietario sin reservas futuras puede eliminar | `ca1ValidarEliminacion_propietarioSinReservas_noLanza` | Dominio |
| CA1 | Toma los turnos en orden, consulta las reservas con el día y las horas del horario y lo borra | `ca1Eliminar_tomaTurnosConsultaYBorra` | Caso de uso |
| CA1 | Escenario sin canchas: borra sin preguntar a Reservas | `ca1EscenarioSinCanchas_borraSinConsultarReservas` | Caso de uso |
| CA1 | Solo se borra el horario pedido; los demás del escenario siguen | `ca1Eliminar_noTocaLosOtrosHorarios` | Caso de uso |
| CA1 | `eliminar` borra la fila y `buscarPorId` ya no la encuentra; los demás horarios siguen | `ca1Eliminar_borraLaFila` | Persistencia |
| CA1 | Con la base real, el caso de uso abre la transacción antes de tomar los turnos (`tomarTurno` la exige, D36) y borra | `ca1EliminarConCanchas_tomaLosTurnosDentroDeLaTransaccionYBorra` | Persistencia + caso de uso |
| CA1 | 200 con `EXI-HOR-03` y su texto del catálogo; llama al caso de uso con el id y el solicitante del token | `ca1EliminarHorario_200_EXI_HOR_03` | REST |
| CA1 | 200 con `EXI-HOR-03`; la lista del escenario ya no trae el horario y la disponibilidad de una cancha (HU20) ya no ofrece esas franjas | `ca1EliminarHorario_200_EXI_HOR_03_listaYDisponibilidadActualizadas` | Punta a punta |
| CA2 | Cancelar la confirmación no envía la petición | Solo en la pantalla; el backend no borra nada sin un `DELETE` | Frontend |
| CA3 | Advertencia CNF-HOR-02 si es el último horario | Solo en la pantalla, con el tamaño de la lista de `GET /api/escenarios/{escenarioId}/horarios` (P4) | Frontend |
| CA4 | Horario inexistente: `ERR-HOR-06` y no se borra ni se consulta nada | `ca4HorarioInexistente_ERR_HOR_06` | Caso de uso |
| CA4, CA5, CA6 | Por HTTP: 404 `ERR-HOR-06`, 403 `ERR-HOR-02` y 422 `ERR-HOR-15`, cada uno con su texto del catálogo; además 401 `ERR-GEN-05` sin sesión y 400 `ERR-GEN-07` si el id no es UUID | `ca4Ca5Ca6LasReglasIncumplidasRespondenSuMensaje` (una vez por código), `sinSesion_401_ERR_GEN_05` y `unIdentificadorQueNoEsUuid_400_ERR_GEN_07` | REST |
| CA5 | Quien no es el dueño, con cualquier rol (otro administrador de cancha o un encargado de sede, que tienen el mismo rol, el administrador del sistema y el jugador), o sin sesión: `ERR-HOR-02`, aunque haya reservas (el permiso va primero) | `ca5NoPropietario_ERR_HOR_02` (una vez por rol, como en HU11), `ca5SinSolicitante_ERR_HOR_02` y `ca5NoPropietarioConReservas_ERR_HOR_02` | Dominio |
| CA5 | El caso de uso no borra, ni toma turnos, ni pregunta a Reservas (P5) | `ca5NoPropietario_noBorraNiTomaTurnos` | Caso de uso |
| CA4 | 404 `ERR-HOR-06` por HTTP con token real | `ca4HorarioInexistente_404_ERR_HOR_06` | Punta a punta |
| CA5 | 403 `ERR-HOR-02` por HTTP con token real y el horario sigue en la base | `ca5NoPropietario_403_ERR_HOR_02` | Punta a punta |
| CA6 | Con reservas futuras: `ERR-HOR-15` | `ca6ConReservasFuturas_ERR_HOR_15` | Dominio |
| CA6 | El caso de uso no borra | `ca6ConReservasFuturas_noBorra` | Caso de uso |
| CA6 | La consulta de Reservas cuenta solo las CONFIRMADAS futuras de esas canchas en ese día que se cruzan con el rango; no cuentan: otro día, fuera del rango, pegada al borde (termina a la hora de inicio), cancelada, ya empezada, otra cancha | `ca6ExistenConfirmadasEnHorario_*` (una por caso, reservas insertadas con SQL) | Persistencia (Reservas) |
| CA6 | Reservas pregunta con la hora de Bogotá (D27) y, sin canchas, responde `false` sin ir a la base | `ca6EnHorario_preguntaConElDiaLasHorasYLaHoraDeBogota`, `ca6EnHorario_sinReservasResponde_false` y `ca6EnHorario_sinCanchasNoVaALaBase` | Caso de uso (Reservas) |
| CA6 | 422 `ERR-HOR-15` por HTTP y el horario sigue en la lista | `ca6ConReservasFuturas_422_ERR_HOR_15` | Punta a punta |

Las pruebas siguen el estilo del módulo: `caNXxx`, con el criterio al principio del nombre.

## Decisiones tomadas

**P1 · Punto de partida de la rama (decidida el 24-nov-2026: opción A, esperar a HU11)**
HU11 agrega `RepositorioHorarios.buscarPorId`, `ERR-HOR-06 → 404`, `ERR-HOR-06` en el catálogo y la ruta `/api/horarios/{id}` en `HorarioController`. HU12 las reutiliza y no las vuelve a escribir. T1 (dominio) se puede hacer ya; de T2 en adelante se espera a que HU11 esté en `main` y se rebasa esta rama sobre ella. Se descartaron partir de la rama de HU11 (B) y repetir las piezas desde `main` (C).

**P2 · Qué reservas cuentan como «dentro del horario» (decidida el 24-nov-2026: opción A)**
Cuenta toda reserva CONFIRMADA futura que **se cruce** con el rango `[horaInicio, horaFin)` ese día de la semana, aunque sea en parte. Después de modificar un horario (HU11, D09) puede quedar una reserva a medias dentro del rango; si solo contaran las que caben completas (B), esa reserva quedaría sin horario, que es lo que D08 quiere evitar. Una reserva pegada al borde (termina justo a la hora de inicio) no se cruza.

**P3 · Respuesta exitosa (decidida el 24-nov-2026: opción A)**
`200` con `codigo` y `mensaje`: EXI-HOR-03 sale del catálogo del backend (regla 6 de `AGENTS.md`), como en HU7, HU11 y HU13. Se descartó `204` sin cuerpo (B).

**P4 · Confirmaciones CNF-HOR-01 y CNF-HOR-02 (decidida el 24-nov-2026: opción A)**
Son de la pantalla: con la lista de HU7 sabe el día y las horas (CNF-HOR-01) y si es el último horario (CNF-HOR-02). El backend no cambia y no se agregan al `catalogo-mensajes.properties`. Se descartó un servicio que diga si es el último horario (B).

**P5 · El permiso antes de los turnos (decidida el 24-nov-2026 al implementar la T3)**
El caso de uso revisa la política 2 con `HorarioRecurrente.validarPropietario` antes de tomar los turnos de las canchas y de preguntar a Reservas. Así quien no es el dueño no detiene las reservas de nadie, como en HU13. `validarEliminacion` vuelve a revisar el permiso: es la regla completa del dominio y no cuesta nada.

## Fuera de alcance

- Que las canchas de un escenario sin horarios dejen de aparecer en la búsqueda (política 8, RN-04): lo hace la búsqueda (HU18) al filtrar; esta HU no toca la búsqueda.
- Avisar a alguien por correo: la HU no lo pide.
- Borrado lógico o papelera: la HU pide eliminación definitiva.
- La pantalla (confirmaciones CA2 y CA3).
