# HU11 · Modificar un horario de atención

**Estado:** Implementada (24-nov-2026) · **HU:** [HU11](../../docs/historias/HU11.md) · **Módulo:** Escenarios y canchas (`co.edu.uco.canchaya.escenarios`)

## Fuentes

- Dominio: [HorarioRecurrente](../../docs/dominio/horariorecurrente.md), responsabilidad `modificar` (políticas 6, 2, 3, 7, 13 y 14) y su tabla «Uso de cada atributo» (el `id` y el escenario no se modifican; el día y las dos horas son obligatorios).
- Mensajes: `EXI-HOR-02`, `ERR-HOR-02`, `ERR-HOR-03`, `ERR-HOR-06`, `ERR-HOR-07`, `ERR-HOR-13` y `ERR-GEN-01` a `ERR-GEN-04` ([mensajes.md](../../docs/especificacion/mensajes.md)).
- Parámetros: `PAR-HOR-01` (franja de una hora) y `PAR-HOR-02` (horas en punto). Los dos son reglas fijas («No» configurables): ya están en `Franja.DURACION` y en la política 13 del dominio, como en HU7.
- Aspectos generales: AG-03 (todos los errores a la vez), AG-04 (después de modificar, el formulario muestra los datos guardados) y AG-12 (propiedad en el servidor).
- Decisiones: D09 (modificar un horario aplica hacia adelante; las reservas ya hechas se respetan), D27 (`America/Bogota`) y D39 (paquetes).
- Diseño: `HorarioRecurrente.modificar(escenario, diaSemana, horaInicio, horaFin, otrosHorariosDelDia, solicitante)` en [clases.md](../../docs/diseno/clases.md): método de instancia, `void`, lanza ERR-HOR-02, 03, 07 y 13; el caso de uso revisa ERR-HOR-06 antes. «La política 14 se cumple por diseño: el cambio aplica hacia adelante y no toca reservas ya hechas».
- Código que se reutiliza (HU7): `HorarioRecurrente.validarPropietario`, `enPunto`, `seCruzaCon`, `RepositorioHorarios.buscarPorEscenarioYDia`, `SolicitudDefinirHorario` (mismas reglas de datos), `RespuestaHorario`, la restricción `ex_horario_recurrente_sin_cruce` de la V1, `GET /api/escenarios/{escenarioId}/horarios` (la lista que se actualiza) y `GET /api/canchas/{canchaId}/disponibilidad` (HU20, que ya calcula las franjas en cada consulta).

## Qué se construye

El propietario del escenario cambia el día, la hora de inicio o la hora de fin de uno de sus horarios de atención. El escenario y el identificador no cambian. Como las franjas no se guardan y la disponibilidad (HU20) se calcula en cada consulta, el cambio se ve de inmediato sin tocar nada más. Las reservas confirmadas que queden por fuera del nuevo horario se conservan: modificar el horario no toca la tabla `reserva` (D09). El formulario se llena con la fila de la lista de horarios que ya existe (HU7), así que no hace falta otra consulta. La pantalla queda para el frontend.

## Contrato del servicio

`PUT /api/horarios/{id}` · con sesión · solo el propietario del escenario del horario · `application/json` (P1)

```json
{ "diaSemana": "MARTES", "horaInicio": "07:00", "horaFin": "13:00" }
```

Las mismas reglas de `SolicitudDefinirHorario` (HU7): los tres campos son obligatorios, el día es uno de `LUNES` a `DOMINGO` y las horas van en `hh:mm` de `00:00` a `23:59`. Que los minutos sean `00` lo revisa el dominio (ERR-HOR-13). Los tres campos se envían siempre: el formulario manda los datos actuales con los que cambiaron.

Respuesta `200`:

```json
{
  "codigo": "EXI-HOR-02",
  "mensaje": "El horario de atención se modificó correctamente.",
  "horario": { "id": "uuid", "diaSemana": "MARTES", "horaInicio": "07:00", "horaFin": "13:00" }
}
```

`horario` tiene la forma de `RespuestaHorario`: con ella el formulario muestra lo guardado (AG-04) y la pantalla actualiza la fila de la lista.

| HTTP | Código | Cuándo |
|---|---|---|
| 200 | `EXI-HOR-02` | Se guardó |
| 400 | `ERR-GEN-01` y `ERR-GEN-02` | Datos inválidos, todos a la vez (`ManejadorErrores`, AG-03) |
| 400 | `ERR-GEN-07` | El `id` no es UUID, o el JSON no se puede leer |
| 403 | `ERR-HOR-02` | No es el propietario, incluido el encargado de sede y el administrador del sistema (ya está en `ManejadorErrores`) |
| 404 | `ERR-HOR-06` | No existe el horario (**entrada nueva** en `ManejadorErrores`) |
| 422 | `ERR-HOR-03` | La hora de fin es igual o anterior a la de inicio |
| 422 | `ERR-HOR-07` | Se cruza con otro horario del mismo escenario y día; el texto lleva el día (`{diaSemana}`) |
| 422 | `ERR-HOR-13` | Alguna hora no es en punto |

`ERR-GEN-03` y `ERR-GEN-04` los nombra la HU (CA2) por la convención general, pero no aplican: ningún campo tiene longitud ni rango numérico; las horas fuera de `00:00`–`23:59` dan `ERR-GEN-02`, igual que en HU7.

## Diseño por capas

| Capa | Pieza | Qué hace |
|---|---|---|
| Dominio | `HorarioRecurrente.modificar(Escenario, DiaSemana, LocalTime horaInicio, LocalTime horaFin, List<HorarioRecurrente> otrosHorariosDelDia, Solicitante)` | En el orden de HU7: política 2 (`validarPropietario`, ERR-HOR-02) → 13 (`enPunto`, ERR-HOR-13) → 3 (ERR-HOR-03) → 7 (ERR-HOR-07 con el día: se cruza con alguno de la lista **cuyo id sea distinto**). Revisa todo antes de cambiar, así un error deja el objeto como estaba. `diaSemana`, `horaInicio` y `horaFin` dejan de ser `final`; `id` y `escenarioId` no se tocan. Si el escenario recibido no es el del horario, lanza `IllegalArgumentException` (error de programación, no de negocio), igual que `Cancha.modificar` |
| Dominio | `definir` | Se reorganiza por dentro para compartir con `modificar` las validaciones de 13 y 3 (un método privado), sin cambiar su comportamiento. Las pruebas de HU7 lo cuidan |
| Puerto de entrada | `ModificarHorario` con `ejecutar(UUID horarioId, DiaSemana, LocalTime horaInicio, LocalTime horaFin, Solicitante)` | Contrato del caso de uso; devuelve el `HorarioRecurrente` modificado. Mismo estilo de parámetros que `DefinirHorario` |
| Caso de uso | `ModificarHorarioCasoUso` | Política 6 (`buscarPorId`, ERR-HOR-06) → buscar el escenario del horario → `buscarPorEscenarioYDia` del **nuevo** día → `horario.modificar(...)` (2, 13, 3 y 7) → `actualizar`. La política 14 no tiene código: no se consulta ni se cambia ninguna reserva |
| Puerto de salida | `RepositorioHorarios.buscarPorId(UUID)` | Nuevo: `Optional<HorarioRecurrente>` |
| Puerto de salida | `RepositorioHorarios.actualizar(HorarioRecurrente)` | Nuevo: guarda día y horas. Si otra solicitud dejó al mismo tiempo un horario que se cruza, la base lo rechaza (`ex_horario_recurrente_sin_cruce`) y se traduce a ERR-HOR-07 con el día. `guardar` no sirve: hace INSERT (`isNew`) y traduce a ERR-HOR-04 |
| Persistencia | `RepositorioHorariosJpa` y `HorarioRecurrenteEntidad` | `actualizar` carga la entidad, cambia los tres campos con un método nuevo de la entidad y hace `saveAndFlush` para traducir aquí la violación (reutiliza `esCruce`). La columna `rango_horas` la recalcula la base sola |
| REST | `PUT /api/horarios/{id}` en `HorarioController` (P1), con `SolicitudDefinirHorario` (P2) y `RespuestaModificarHorario(codigo, mensaje, RespuestaHorario horario)` | El `@RequestMapping("/api/escenarios/{escenarioId}/horarios")` de la clase se quita y cada método lleva su ruta completa (la de HU7 queda en la constante `HORARIOS_ESCENARIO`); `GET` y `POST` de HU7 quedan con la misma URL. `ERR-HOR-06 → 404` en `ManejadorErrores` |
| Transversal | `EXI-HOR-02`, `ERR-HOR-06` y `ERR-HOR-07` en `catalogo-mensajes.properties` | Antes de HU11 no estaban. El comentario «Valores de {diaSemana} en ERR-HOR-04» pasa a decir «ERR-HOR-04 y ERR-HOR-07» |

Sin migraciones: la tabla `horario_recurrente` y su restricción de exclusión ya cubren el cruce también en un UPDATE.

**Concurrencia.** Dos modificaciones que se cruzan al mismo tiempo las frena la restricción de exclusión (ERR-HOR-07 a la segunda). Si un jugador reserva una franja mientras el propietario achica el horario, la reserva puede quedar por fuera del horario nuevo: es justo el caso de CA8 y D09 (se conserva), así que no hace falta tomar el turno de las canchas (D29) en esta HU. Dos modificaciones del mismo horario a la vez: gana la última, como en HU8 y HU9.

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1 | Cambia día y horas; conserva id y escenario; no se cruza consigo mismo (de 6–12 a 6–14 el mismo día sí se puede) | `HorarioRecurrenteModificarTest`: `ca1Modificar_cambiaDiaYHorasYConservaIdYEscenario` y `ca1Modificar_noSeCruzaConsigoMismo` | Dominio |
| CA1 | Se guarda sin crear otro horario; la política 7 usa los horarios del día nuevo; ampliar el mismo horario se puede | `ModificarHorarioCasoUsoTest`: `ca1Modificar_guardaLosCambios`, `ca1CambiarDeDia_revisaLosHorariosDelDiaNuevo` y `ca1AmpliarElMismoHorario_seGuarda` | Caso de uso |
| CA1 | `actualizar` cambia la fila (no agrega otra) y `buscarPorId` la lee | `ModificacionHorarioPersistenciaTest`: `ca1Actualizar_cambiaDiaYHoras` | Persistencia |
| CA1 | 200 con `EXI-HOR-02` y el horario guardado | `ModificarHorarioControllerTest`: `ca1ModificarHorario_200_EXI_HOR_02_conElHorarioGuardado` | REST |
| CA1 | Con token real: 200, la lista del escenario y la disponibilidad de una cancha (HU20) salen con el nuevo horario | `ModificarHorarioPuntaAPuntaTest`: `ca1ModificarHorario_200_EXI_HOR_02_listaYDisponibilidadActualizadas` | Punta a punta |
| CA2 | Sin datos: 400 con `ERR-GEN-01` por cada campo; día que no existe y horas mal escritas (`7:00`, `25:00`): 400 con los tres `ERR-GEN-02` a la vez; no se llama al caso de uso | `ModificarHorarioControllerTest`: `ca2SinDatos_400_ERR_GEN_01PorCadaCampo` y `ca2DatosInvalidos_400_conTodosLosErrores` | REST |
| CA3 | Horario inexistente: `ERR-HOR-06` y nada cambia | `ModificarHorarioCasoUsoTest`: `ca3HorarioInexistente_ERR_HOR_06`; `ModificacionHorarioPersistenciaTest`: `ca3BuscarPorId_inexistente_vacio` | Caso de uso + persistencia |
| CA3 | 404 `ERR-HOR-06` por HTTP | `ModificarHorarioControllerTest`: `ca3Ca4Ca5Ca7LasReglasIncumplidasRespondenSuMensaje`; `ModificarHorarioPuntaAPuntaTest`: `ca3HorarioInexistente_404_ERR_HOR_06` | REST + punta a punta |
| CA4 | Cualquier rol que no sea el dueño (otro administrador de cancha o encargado de sede, jugador, administrador del sistema): `ERR-HOR-02` y el horario queda como estaba | `HorarioRecurrenteModificarTest`: `ca4NoPropietario_ERR_HOR_02` (un caso por rol); `ModificarHorarioCasoUsoTest`: `ca4EncargadoDeSede_ERR_HOR_02` y `ca4AdministradorDelSistema_ERR_HOR_02` | Dominio + caso de uso |
| CA4 | 403 `ERR-HOR-02` por HTTP; con token real, nada cambia en la base | `ModificarHorarioControllerTest`: `ca3Ca4Ca5Ca7LasReglasIncumplidasRespondenSuMensaje`; `ModificarHorarioPuntaAPuntaTest`: `ca4NoPropietario_403_ERR_HOR_02` | REST + punta a punta |
| CA5 | Fin igual o anterior al inicio: `ERR-HOR-03` y el horario queda como estaba; 422 por HTTP | `HorarioRecurrenteModificarTest`: `ca5FinNoMayorQueInicio_ERR_HOR_03`; `ModificarHorarioControllerTest`: `ca3Ca4Ca5Ca7LasReglasIncumplidasRespondenSuMensaje` | Dominio + REST |
| CA6 | Se cruza con otro horario: `ERR-HOR-07` con el día (también el día nuevo al cambiar de día); quedar pegado (12–14 junto a 6–12) sí se puede | `HorarioRecurrenteModificarTest`: `ca6SeCruzaConOtro_ERR_HOR_07`, `ca6SeCruzaConOtroDelDiaNuevo_ERR_HOR_07ConEseDia` y `ca6HorarioPegado_seGuarda`; `ModificarHorarioCasoUsoTest`: `ca6UnHorarioDeOtroEscenarioNoCuenta` | Dominio + caso de uso |
| CA6 | Dos solicitudes al mismo tiempo: la base rechaza el cruce en el UPDATE y se responde `ERR-HOR-07`; el horario queda como estaba | `ModificacionHorarioPersistenciaTest`: `ca6Actualizar_cruceEnLaBase_ERR_HOR_07` | Persistencia |
| CA6 | 422 con el texto «…ya definido para el miércoles.» | `ModificarHorarioControllerTest`: `ca6SeCruza_422_ERR_HOR_07ConElDia` | REST |
| CA7 | Hora de inicio o de fin con minutos (`07:30`): `ERR-HOR-13`; el formato `hh:mm` deja pasar los minutos hasta el dominio | `HorarioRecurrenteModificarTest`: `ca7HoraNoEnPunto_ERR_HOR_13`; `ModificarHorarioControllerTest`: `ca7UnaHoraQueNoEsEnPuntoLlegaAlCasoDeUso` | Dominio + REST |
| CA8 | Una reserva confirmada que queda por fuera del nuevo horario sigue en la base igual que antes, en todas sus columnas (política 14, D09) | `ModificacionHorarioPersistenciaTest`: `ca8ReservaPorFueraDelNuevoHorario_seConserva` (inserta la reserva con SQL, P3) | Persistencia |
| CA8 | Con token real: la reserva sigue `CONFIRMADA` y la disponibilidad se calcula con el nuevo horario | `ModificarHorarioPuntaAPuntaTest`: `ca1ModificarHorario_200_EXI_HOR_02_listaYDisponibilidadActualizadas` | Punta a punta |

Además: sin sesión responde 401 `ERR-GEN-05` y un `id` que no es UUID responde 400 `ERR-GEN-07` (`ModificarHorarioControllerTest`). Las pruebas de HU7 (`HorarioRecurrenteTest`, `DefinirHorarioCasoUsoTest`, `HorarioPersistenciaTest`, `HorarioControllerTest` y `DefinirHorarioPuntaAPuntaTest`) siguen pasando con el cambio de `definir` y de las rutas de `HorarioController`.

Las pruebas siguen el estilo del módulo: `ca1Xxx`, con el criterio al principio del nombre.

## Decisiones tomadas

**P1 · Ruta del servicio (aprobada el 24-nov-2026: opción A, en el mismo `HorarioController`)**
`PUT /api/horarios/{id}`, igual que HU9 (`PUT /api/canchas/{id}`): el escenario sale del horario y en la ruta no aparece algo que no se puede cambiar. HU12 usaría `DELETE /api/horarios/{id}`. Va en `HorarioController`, por eso el prefijo de la clase pasa a cada método. Se descartó `PUT /api/escenarios/{escenarioId}/horarios/{id}` (B), que obliga a revisar que el horario sea de ese escenario.

**P2 · Datos de entrada (aprobada el 24-nov-2026: opción A)**
Se reutiliza `SolicitudDefinirHorario` tal cual: tiene los mismos tres campos con las mismas reglas y así no se repiten los patrones de validación. Se descartó un record nuevo `SolicitudModificarHorario` (B).

**P3 · Cómo probar CA8 (aprobada el 24-nov-2026: opción A)**
Prueba de persistencia que inserta una reserva confirmada con SQL (con un jugador y una cancha de prueba) dentro del horario, lo modifica para que la reserva quede por fuera y revisa que la fila de `reserva` siga igual, como P2 de HU9. Se descartó dejar CA8 solo «por diseño» (B).

## Fuera de alcance

- Cambiar el escenario de un horario (la hoja lo marca «No modificable»).
- Avisar al propietario o a los jugadores de las reservas que quedan por fuera del nuevo horario: la HU no lo pide.
- Una consulta aparte de un solo horario: la lista de HU7 ya trae todos los datos del formulario.
- Horarios que terminan a medianoche (`24:00`): el formato `hh:mm` de HU7 llega hasta `23:59` y esta HU no lo cambia.
- Eliminar un horario (HU12).
- La pantalla.
