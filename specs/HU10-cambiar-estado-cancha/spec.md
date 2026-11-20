# HU10 · Cambiar el estado de una cancha

**Estado:** Implementada (19-nov-2026) · **HU:** [HU10](../../docs/historias/HU10.md) · **Módulo:** Escenarios y canchas (`co.edu.uco.canchaya.escenarios`)

## Fuentes

- Dominio: [Cancha](../../docs/dominio/cancha.md), responsabilidad `cambiarEstado` (políticas 7, 2, 10 y 12) y `publicarEnBusqueda` (políticas 7 y 11). En la tabla «Uso de cada atributo», `estado` es «Requerido/Modificable» solo en `cambiarEstado`.
- Mensajes: `EXI-CAN-03`, `ERR-CAN-02`, `ERR-CAN-07`, `ERR-CAN-10`, `ERR-CAN-12`, `ERR-GEN-01` y `ERR-GEN-07` ([mensajes.md](../../docs/especificacion/mensajes.md)). `EXI-CAN-03` y `ERR-CAN-10` llevan `{estado}` con los textos «Disponible» o «Inactiva».
- Parámetros: no aplican.
- Aspectos generales: AG-12 (la propiedad se revisa en el servidor con el usuario del token).
- Decisiones: D08 (no se inactiva con reservas confirmadas futuras), D19 (solo se avisa por correo el cambio de estado del **escenario**, no el de la cancha), D27 (hora de Bogotá), D29 punto 2 y D36 (turno de la cancha) y D39 (paquetes).
- Diseño: `Cancha.cambiarEstado(escenario, nuevoEstado, solicitante, tieneReservasConfirmadasFuturas)` en [clases.md](../../docs/diseno/clases.md), que lanza ERR-CAN-02, ERR-CAN-10 y ERR-CAN-12; el caso de uso revisa ERR-CAN-07 antes de llamarlo.
- Código que se reutiliza:
  - `Cancha.validarPropietario` (política 2), `Cancha.publicarEnBusqueda` (política 11, HU19) y `RepositorioCanchas.buscarPorId` y `tomarTurno`.
  - `ConsultaReservasFuturas.existenConfirmadasFuturas(canchaIds)`, que ya implementa Reservas (HU13, D08).
  - El mismo esquema de `Escenario.cambiarEstado` y `CambiarEstadoEscenarioCasoUso` (HU13).
  - `ERR-CAN-02 → 403` y `ERR-CAN-07 → 404`, que ya están en `ManejadorErrores`.
  - Lo que ya hace que una cancha INACTIVA deje de verse y de reservarse:
    - `ConsultarPerfilEscenarioCasoUso` filtra las canchas con `publicarEnBusqueda`, así que sale de la ficha del escenario (HU19).
    - `ConsultaCanchasCasoUso.buscarReservable` filtra con `estaDisponible`, así que no admite reservas (HU20 y HU21).

## Qué se construye

Un servicio para que el propietario del escenario ponga una de sus canchas como DISPONIBLE o INACTIVA. Al inactivarla, la cancha deja de salir en la ficha del escenario y en la búsqueda, y ya no se puede reservar. Esto no necesita código nuevo, porque esas consultas ya miran el estado. No se puede inactivar una cancha que tiene reservas confirmadas que todavía no empiezan. La pantalla («07 Canchas del escenario») pone el botón en la fila de la lista de HU6 y actualiza esa fila con la respuesta. La pantalla no es parte de esta especificación.

## Contrato del servicio

`PATCH /api/canchas/{id}/estado` · con sesión · solo el propietario del escenario de la cancha · `application/json` (P1)

```json
{ "estado": "INACTIVA" }
```

`estado` es obligatorio (`ERR-GEN-01`) y solo acepta `DISPONIBLE` o `INACTIVA`. Cualquier otro valor no se puede leer y responde `ERR-GEN-07`, igual que `SolicitudCambioEstado` en HU13.

Respuesta `200`:

```json
{
  "codigo": "EXI-CAN-03",
  "mensaje": "La cancha Cancha 1 quedó en estado Inactiva.",
  "id": "uuid",
  "estado": "INACTIVA"
}
```

Tiene la misma forma que `RespuestaCambioEstado` de HU13. Con `estado`, la pantalla cambia la fila y el botón sin volver a consultar la lista (CA1 y CA2: «indicando el nuevo estado»).

| HTTP | Código | Cuándo |
|---|---|---|
| 200 | `EXI-CAN-03` | Se cambió el estado |
| 400 | `ERR-GEN-01` | Falta `estado` |
| 400 | `ERR-GEN-07` | El `id` no es UUID, el estado no existe o el JSON no se puede leer |
| 401 | — | Sin sesión (Spring Security, como en HU9) |
| 403 | `ERR-CAN-02` | No es el propietario: otro administrador de cancha, un encargado de sede o el administrador del sistema (P3). Ya está en `ManejadorErrores` |
| 404 | `ERR-CAN-07` | No existe la cancha. Ya está en `ManejadorErrores` |
| 422 | `ERR-CAN-10` | La cancha ya está en ese estado. Usa el valor por defecto de `ManejadorErrores` |
| 422 | `ERR-CAN-12` | Se quiere inactivar y tiene reservas confirmadas futuras. Usa el valor por defecto |

## Diseño por capas

| Capa | Pieza | Qué hace |
|---|---|---|
| Dominio | `Cancha.cambiarEstado(Escenario, EstadoCancha nuevoEstado, Solicitante, boolean tieneReservasConfirmadasFuturas)` | Si el escenario no es el de la cancha, lanza `IllegalArgumentException` (igual que `modificar`). Luego revisa, en orden: política 2 con `validarPropietario` (ERR-CAN-02); política 10, que el estado sea distinto (ERR-CAN-10 con `TextoCatalogo("estado." + nuevoEstado)`); y política 12, que no se inactive con reservas futuras (ERR-CAN-12; el dato solo importa al pasar a INACTIVA). Solo cambia el estado si pasa todo. `estado` deja de ser `final`. La política 11 no cambia: desde ese momento `publicarEnBusqueda` y `estaDisponible` responden con el nuevo estado |
| Puerto de entrada | `CambiarEstadoCancha` con `Cancha ejecutar(UUID canchaId, EstadoCancha nuevoEstado, Solicitante)` | Contrato del caso de uso |
| Caso de uso | `CambiarEstadoCanchaCasoUso`, `@Transactional` | En orden: (1) política 7, `buscarPorId` (ERR-CAN-07); (2) busca el escenario de la cancha (si falta, `IllegalStateException`, como en HU9); (3) política 2 con `Cancha.validarPropietario`, para que quien no es el propietario no tome el turno ni detenga las reservas; (4) `tomarTurno(canchaId)` solo si se va a inactivar (P2); (5) política 12, que pregunta a `existenConfirmadasFuturas(List.of(canchaId))` solo al inactivar, como HU13; (6) `cancha.cambiarEstado(...)` con las políticas 2, 10 y 12, que vuelve a revisar la 2 para no depender del orden del caso de uso; (7) `actualizarEstado`. No envía correo (D19). La política 7 va antes que la 2, igual que HU9: primero se responde 404 y después 403 |
| Puerto de salida | `RepositorioCanchas.actualizarEstado(Cancha)` | Nuevo. Guarda solo el estado |
| Puerto de salida | `ConsultaReservasFuturas` | Ya existe y no cambia. Reservas lo implementa y lee la tabla `reserva` en la misma transacción |
| Persistencia | `RepositorioCanchasJpa.actualizarEstado` y `CanchaJpa.actualizarEstado(id, estado)` | Un `update ... set estado` con `@Modifying`, igual que `EscenarioJpa.actualizarEstado` (HU13) |
| REST | `PATCH /api/canchas/{id}/estado` en `CanchaController`; `SolicitudCambioEstadoCancha(@NotNull EstadoCancha estado)` y `RespuestaCambioEstadoCancha(codigo, mensaje, id, estado)` | Arma `EXI-CAN-03` con el nombre de la cancha y `TextoCatalogo("estado." + estado)`. No se reutilizan los records de HU13 porque están tipados con `EstadoEscenario` |
| Transversal | `EXI-CAN-03`, `ERR-CAN-10`, `ERR-CAN-12`, `estado.DISPONIBLE=Disponible` y `estado.INACTIVA=Inactiva` en `catalogo-mensajes.properties` | Hoy no están. `estado.INACTIVO` (escenario) ya existe y es una clave distinta de `estado.INACTIVA` (cancha) |

No hace falta migración: la columna `cancha.estado` ya admite los dos valores.

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1 | DISPONIBLE → INACTIVA: cambia el estado y deja de `estaDisponible` y de `publicarEnBusqueda` | `ca1Inactivar_quedaInactivaYNoSePublica` | Dominio |
| CA1 | Se guarda INACTIVA; toma el turno antes de consultar las reservas (P2) | `ca1Inactivar_guardaElEstado` y `ca1Inactivar_tomaElTurnoAntesDeConsultarReservas` (el falso de Reservas anota si el turno ya estaba tomado) | Caso de uso |
| CA1 | `actualizarEstado` cambia la fila | `ca1ActualizarEstado_cambiaLaFila` | Persistencia |
| CA1 y CA2 | 200 con `EXI-CAN-03` («…quedó en estado Inactiva» o «…Disponible»), `id` y `estado`; el solicitante sale del token | `ca1Ca2CambiarEstado_200_EXI_CAN_03` (parametrizada) | REST |
| CA1 | Por HTTP de punta a punta: 200 con `EXI-CAN-03`. Después, la cancha no sale en el perfil del escenario (HU19) y `GET /api/canchas/{id}/disponibilidad` responde 404 `ERR-RES-01` (HU20) | `ca1Inactivar_200_EXI_CAN_03_yDejaDeVerseYDeReservarse` | Punta a punta |
| CA1 | Que no salga en la búsqueda: HU18 todavía no existe. Lo cubre `publicarEnBusqueda` (prueba de dominio de arriba), y HU18 lo verificará cuando use ese método | — | — |
| CA2 | INACTIVA → DISPONIBLE: vuelve a `publicarEnBusqueda` si el escenario está ACTIVO y tiene horarios; si el escenario está INACTIVO, se activa igual pero no se publica | `ca2Activar_vuelveAPublicarseSiCumpleRN04` y `ca2Activar_escenarioInactivo_seActivaPeroNoSePublica` | Dominio |
| CA2 | Activar no pregunta a Reservas ni toma el turno | `ca2Activar_noConsultaReservasNiTomaTurno` | Caso de uso |
| CA2 | 200 con `EXI-CAN-03` («…quedó en estado Disponible») y la cancha vuelve a salir en el perfil | `ca2Activar_200_EXI_CAN_03_yVuelveAlPerfil` | Punta a punta |
| CA3 | Cancha inexistente: `ERR-CAN-07`, no se guarda nada | `ca3CanchaInexistente_ERR_CAN_07` | Caso de uso |
| CA3, CA4 y CA6 | 404 `ERR-CAN-07`, 403 `ERR-CAN-02` y 422 `ERR-CAN-12` por HTTP, con su texto del catálogo | `ca3Ca4Ca6LasReglasIncumplidasRespondenSuMensaje` (parametrizada) | REST |
| CA4 | Otro administrador de cancha, un encargado de sede y el administrador del sistema: `ERR-CAN-02`, el estado no cambia y no se toma el turno | `ca4NoPropietario_ERR_CAN_02`, `ca4EncargadoDeSede_ERR_CAN_02` y `ca4AdministradorDelSistema_ERR_CAN_02` | Dominio + caso de uso |
| CA4 | 403 `ERR-CAN-02` por HTTP | `ca4NoPropietario_403_ERR_CAN_02` | Punta a punta |
| CA5 | Mismo estado (DISPONIBLE → DISPONIBLE e INACTIVA → INACTIVA): `ERR-CAN-10` con «Disponible» o «Inactiva» | `ca5MismoEstado_ERR_CAN_10` (parametrizada) | Dominio |
| CA5 | 422 `ERR-CAN-10` con el texto del estado | `ca5MismoEstado_422_ERR_CAN_10` | REST |
| CA6 | Inactivar con reservas confirmadas futuras: `ERR-CAN-12` y el estado no cambia; activar con reservas futuras sí se puede | `ca6ReservasFuturas_ERR_CAN_12` y `ca6ActivarConReservasFuturas_seActiva` | Dominio |
| CA6 | El caso de uso pregunta solo por esta cancha y no guarda nada | `ca6ReservasFuturas_noGuarda` | Caso de uso |
| CA6 | Con una reserva CONFIRMADA futura insertada por SQL: 422 `ERR-CAN-12`; con una pasada o cancelada, sí se inactiva | `ca6ReservaFutura_422_ERR_CAN_12` y `ca6ReservaPasadaOCancelada_seInactiva` | Punta a punta |
| — | Falta `estado`, el valor no existe (también un estado de escenario como `INACTIVO`) o el `id` no es UUID: 400 `ERR-GEN-01` o `ERR-GEN-07`; sin sesión: 401 `ERR-GEN-05`. En ninguno se llama al caso de uso | `estadoInvalido_400_*` y `sinSesion_401_ERR_GEN_05` | REST |

Las pruebas siguen el estilo del módulo: `ca1Xxx`, con el criterio al principio del nombre. Para el caso de uso se reutilizan `CanchasEnMemoria` (se le agregan `actualizarEstado` y un registro de llamadas a `tomarTurno`) y un falso de `ConsultaReservasFuturas` como `ReservasFalsas` de HU13.

## Decisiones tomadas

**P1 · Ruta del servicio (aprobada el 19-nov-2026: opción A)**
- A. `PATCH /api/canchas/{id}/estado`: es la ruta que ya se anticipó en la P1 de HU9 y sigue el patrón de HU13 (`PATCH /api/escenarios/{id}/estado`).
- B. `PUT /api/canchas/{id}/estado`.

**Recomendación: A.**

**P2 · Una reserva que entra mientras se inactiva la cancha (aprobada el 19-nov-2026: opción A)**
Sin control, puede pasar esto:
1. El caso de uso pregunta si hay reservas futuras, y no hay.
2. En ese mismo instante HU21 guarda una reserva.
3. La cancha queda INACTIVA con una reserva confirmada futura, y se rompe D08.

Opciones:
- A. Al inactivar, el caso de uso toma el turno de la cancha (`tomarTurno`, D29 punto 2 y D36) dentro de su transacción, antes de preguntar por las reservas. HU21 toma el mismo turno antes de revisar que la cancha sea reservable (paso 1 de su especificación). Así las dos operaciones van una después de la otra y la reserva que llega después ve la cancha INACTIVA (ERR-RES-01). Activar no lo necesita.
- B. No tomar el turno, como hace hoy HU13 con el escenario, y aceptar el riesgo.

**Recomendación: A.** Es una línea más y reutiliza lo que ya existe. Se reporta aparte que HU13 tiene el mismo riesgo con el escenario (todas sus canchas). Esta HU no lo corrige.

**P3 · El administrador del sistema no puede cambiar el estado de la cancha (aprobada el 19-nov-2026: opción A)**
- A. Igual que HU8 y HU9 (P3): la política 2 se aplica tal cual. Solo el propietario puede, y el administrador del sistema recibe `ERR-CAN-02`. El administrador del sistema solo cambia el estado del escenario (HU13).
- B. Dejar que el administrador del sistema también cambie el estado de la cancha.

**Recomendación: A.**

## Fuera de alcance

- Correo al propietario o a los jugadores por el cambio de estado de la cancha: D19 solo lo pide para el escenario.
- Cancelar o mover las reservas futuras para poder inactivar: la HU dice que primero deben cumplirse o cancelarse (HU24, HU25).
- La búsqueda de canchas (HU18): esta HU solo deja listo `publicarEnBusqueda`, que ya existe.
- Corregir el mismo riesgo de concurrencia en HU13 (P2): se reporta, no se hace.
- La pantalla.
