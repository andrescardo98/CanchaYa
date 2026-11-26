# HU24 · Cancelar una reserva

**Estado:** Implementada (25-nov-2026) · **HU:** [HU24](../../docs/historias/HU24.md) · **Módulo:** Reservas (`co.edu.uco.canchaya.reservas`)

## Fuentes

- Dominio: [Reserva](../../docs/dominio/reserva.md), responsabilidad `cancelar` (políticas 14, 15, 16, 19, 20 y 21). En la tabla «Uso de cada atributo», `id` es «Requerido/No modificable»; `estado`, `origenCancelacion` y `fechaCancelacion` se asignan solos; `motivoCancelacion` es opcional y solo lo usa el propietario (HU32).
- Mensajes: `EXI-RES-02`, `CNF-RES-01`, `ERR-RES-14`, `ERR-RES-15` y `ERR-RES-16` ([mensajes.md](../../docs/especificacion/mensajes.md)). También `ERR-GEN-05` (sin sesión) y `ERR-GEN-07` (id que no es UUID).
- Parámetros: `PAR-GEN-01` (America/Bogota) y `PAR-RES-05` (más de 3 horas, límite estricto: con exactamente 3 horas ya no se cancela) ([parametros.md](../../docs/especificacion/parametros.md)). Los dos ya están en la configuración: el reloj de la aplicación y `ParametrosReserva.horasMinimasCancelacion` (`canchaya.reserva.horas-minimas-cancelacion`, variable `PAR_RES_05`).
- Aspectos generales: AG-11 (exige sesión), AG-12 (el permiso se revisa en el servidor con el usuario del token) y AG-14 (datos no visibles).
- Decisiones: D06 (más de 3 horas), D15 (el correo no revierte la operación), D27 (hora de Bogotá), D29 punto 3 (actualización condicional) y D39 (paquetes).
- Diseño: `Reserva.cancelar(solicitante, esPropietarioDelEscenario, motivoCancelacion, ahora, parametros)` en [clases.md](../../docs/diseno/clases.md), que devuelve `void` y lanza ERR-RES-14 a ERR-RES-18; el caso de uso revisa ERR-RES-14 antes de llamarlo. «No hay métodos set: el estado solo cambia con los métodos de negocio».
- Código que se reutiliza:
  - `Reserva.inicio()`, `estaConfirmada()` y `reconstruir(...)` (HU20 y HU21), `Reserva.sePuedeCancelarPorJugador(ahora, parametros)` (HU23, la misma política 16 con la que «Mis reservas» muestra la opción de cancelar) y `OrigenCancelacion.JUGADOR`, que ya existe.
  - El envío de correos después de la transacción de `ReservarCanchaCasoUso` (HU22), que servirá de modelo a HU25.
  - `RepositorioReservas` y `RepositorioReservasJpa` (HU21), el `Clock` de la aplicación y `TransactionOperations`, igual que `ReservarCanchaCasoUso`.
  - `ReservaController` y `ManejadorErrores`, que ya tienen el patrón de códigos HTTP por mensaje.
  - La tabla `reserva` (V1) ya tiene `origen_cancelacion`, `fecha_cancelacion`, el `CHECK` `ck_reserva_cancelacion` y la restricción `ex_reserva_sin_cruce ... WHERE (estado = 'CONFIRMADA')`. Por eso, en cuanto la reserva queda CANCELADA, la base deja de contarla y la franja vuelve a estar libre (política 20) sin código adicional: la disponibilidad (HU20) y la reserva (HU21) ya solo miran las CONFIRMADAS.

## Qué se construye

Un servicio para que el jugador cancele una de sus reservas confirmadas cuando faltan más de 3 horas para que empiece. La reserva queda CANCELADA con origen JUGADOR y la fecha de cancelación, y la franja vuelve a salir libre para los demás. La hora se toma del servidor, no del celular. La ventana de confirmación (CNF-RES-01) y el botón en «Mis reservas» son de la pantalla; esta especificación solo cubre el backend. Los correos de cancelación los agrega HU25 (P2).

## Contrato del servicio

`POST /api/reservas/{id}/cancelacion` · con sesión · solo el jugador que hizo la reserva · sin cuerpo (P1)

Respuesta `200`:

```json
{
  "codigo": "EXI-RES-02",
  "mensaje": "La reserva se canceló correctamente.",
  "id": "uuid",
  "estado": "CANCELADA"
}
```

Con `id` y `estado`, la pantalla «Mis reservas» (HU23) mueve la reserva sin volver a consultar la lista, como en HU10. No lleva el origen ni la fecha de cancelación (AG-14).

| HTTP | Código | Cuándo |
|---|---|---|
| 200 | `EXI-RES-02` | Se canceló |
| 400 | `ERR-GEN-07` | El `id` no es UUID (ya lo hace `ManejadorErrores`) |
| 401 | `ERR-GEN-05` | Sin sesión (Spring Security, como en HU21) |
| 403 | `ERR-RES-15` | El usuario del token no es el jugador de la reserva. Nuevo en `ManejadorErrores` |
| 404 | `ERR-RES-14` | No existe la reserva, ya no está CONFIRMADA, o otra operación la cambió al mismo tiempo (P3). Nuevo en `ManejadorErrores`, igual que `ERR-RES-01` («no existe o no está disponible») |
| 422 | `ERR-RES-16` | Faltan 3 horas o menos para el inicio, o ya empezó. Usa el valor por defecto |

## Diseño por capas

| Capa | Pieza | Qué hace |
|---|---|---|
| Dominio | `Reserva.cancelar(Solicitante solicitante, LocalDateTime ahora, ParametrosReserva parametros)` (P4) | Revisa, en orden: política 14, que siga CONFIRMADA (ERR-RES-14; el caso de uso ya lo revisó, pero así el método no depende de quién lo llame); política 15, que `solicitante.usuarioId()` sea `jugadorId` (ERR-RES-15); política 16 (RN-10, D06, PAR-RES-05) con `sePuedeCancelarPorJugador`, que faltan **más** de `horasMinimasCancelacion` horas (ERR-RES-16; con exactamente 3 horas o con la reserva ya empezada, falla). Solo si pasa todo, política 19: `estado = CANCELADA`, `origenCancelacion = JUGADOR`, `fechaCancelacion = ahora` en Bogotá. `estado`, `origenCancelacion` y `fechaCancelacion` dejan de ser `final`, como `Cancha.estado` en HU10 |
| Puerto de entrada | `CancelarReserva` con `Reserva ejecutar(UUID reservaId, Solicitante solicitante)` | Contrato del caso de uso |
| Caso de uso | `CancelarReservaCasoUso`, con `TransactionOperations` como HU21 | En una transacción: (1) `ahora = LocalDateTime.now(reloj)`; (2) política 14, `buscarPorId` (si no existe, ERR-RES-14); (3) `reserva.cancelar(...)` con las políticas 14, 15, 16 y 19; (4) `cancelarSiSigueConfirmada(reserva)`; si devuelve falso, otra operación la cambió entre la lectura y la escritura y se lanza ERR-RES-14 (P3). No toma el turno de la cancha: cancelar libera una franja, no la ocupa, así que no puede crear un cruce. No envía correos (P2): HU25 los agrega después de la transacción (D15) |
| Puerto de salida | `RepositorioReservas.buscarPorId(UUID)` → `Optional<Reserva>` y `boolean cancelarSiSigueConfirmada(Reserva)` | Nuevos |
| Persistencia | `RepositorioReservasJpa` | `buscarPorId` con `jpa.findById(...).map(ReservaEntidad::aDominio)`. `cancelarSiSigueConfirmada` con un `update reserva set estado = 'CANCELADA', origen_cancelacion = :origen, fecha_cancelacion = :fecha where id = :id and estado = 'CONFIRMADA'` (actualización condicional, D29 punto 3) y devuelve si cambió una fila. Va en SQL con `NamedParameterJdbcTemplate`, como las otras consultas del adaptador |
| REST | `POST /api/reservas/{id}/cancelacion` en `ReservaController`; `RespuestaCancelacion(codigo, mensaje, id, estado)` | El solicitante sale del token. Arma `EXI-RES-02` del catálogo |
| Transversal | `ERR-RES-14 → 404` y `ERR-RES-15 → 403` en `ManejadorErrores`. `ERR-RES-14`, `ERR-RES-15`, `ERR-RES-16` y `EXI-RES-02` en `catalogo-mensajes.properties` | Hoy no están |

No hace falta migración: la tabla ya tiene las columnas, los `CHECK` y la restricción de exclusión que solo cuenta las CONFIRMADAS.

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1 | Faltan más de 3 horas (por ejemplo, 3 horas y 1 minuto): queda CANCELADA, con origen JUGADOR, `fechaCancelacion` = ahora en Bogotá y sin motivo | `ca1MasDeTresHoras_quedaCanceladaPorElJugador` | Dominio |
| CA1 | Busca por id, cancela con la hora del reloj y guarda con la actualización condicional | `ca1ReservaConfirmada_seCancelaYSeGuarda` | Caso de uso |
| CA1 | `buscarPorId` arma la reserva guardada; `cancelarSiSigueConfirmada` cambia la fila y devuelve verdadero; después se puede guardar otra reserva que se cruza con ella (la restricción de exclusión ya no la cuenta, política 20) | `ca1BuscarPorId_devuelveLaReserva` y `ca1CancelarSiSigueConfirmada_cambiaLaFilaYLiberaLaFranja` | Persistencia |
| CA1 | 200 con `EXI-RES-02`, `id` y `estado`; el solicitante sale del token | `ca1Cancelar_200_EXI_RES_02` | REST |
| CA1 | Por HTTP de punta a punta: reserva con `POST /api/reservas`, cancela, y `GET /api/canchas/{id}/disponibilidad` vuelve a mostrar la franja libre | `ca1Cancelar_200_yLaFranjaVuelveAEstarDisponible` | Punta a punta |
| CA1 | Los correos de cancelación | Los verifica HU25 (P2) | — |
| CA2 | Si el jugador cancela la ventana de confirmación, la pantalla no llama al servicio y la reserva sigue CONFIRMADA. Solo se verifica en la pantalla. El backend aporta los datos de CNF-RES-01 (`fecha`, `horaInicio`, `horaFin`, `escenario`) en la lista de «Mis reservas» (HU23) | — | Pantalla |
| CA3 | Con exactamente 3 horas, con 2 horas y 59 minutos y con la reserva ya empezada: ERR-RES-16 y sigue CONFIRMADA | `ca3TresHorasOMenos_ERR_RES_16` (parametrizada) | Dominio |
| CA3 | PAR-RES-05 se lee de los parámetros: con 5 horas configuradas, faltando 4, ERR-RES-16 | `ca3ElLimiteSaleDelParametro` | Dominio |
| CA3 | 422 `ERR-RES-16` con su texto del catálogo | `ca3Ca4Ca5LasReglasIncumplidasRespondenSuMensaje` (parametrizada) | REST |
| CA3 | Con una reserva que empieza en 2 horas insertada por SQL: 422 `ERR-RES-16` y la fila sigue CONFIRMADA | `ca3FaltanDosHoras_422_ERR_RES_16` | Punta a punta |
| CA4 | Reserva CANCELADA o NO_ASISTIO: ERR-RES-14 | `ca4NoConfirmada_ERR_RES_14` (parametrizada) | Dominio |
| CA3 y CA5 | Si falla la política 15 o la 16, no se llama a `cancelarSiSigueConfirmada` | `ca3TresHorasOMenos_noSeGuarda` y `ca5OtroUsuario_noSeGuarda` | Caso de uso |
| CA4 | No existe: ERR-RES-14 y no se actualiza nada. La actualización condicional devuelve falso (otra operación la cambió): ERR-RES-14 | `ca4NoExiste_ERR_RES_14` y `ca4CambiadaAlMismoTiempo_ERR_RES_14` | Caso de uso |
| CA4 | Sobre una reserva ya CANCELADA, `cancelarSiSigueConfirmada` devuelve falso y no cambia el origen ni la fecha | `ca4CancelarSiSigueConfirmada_yaCancelada_devuelveFalso` | Persistencia |
| CA4 | 404 `ERR-RES-14` por HTTP | `ca3Ca4Ca5LasReglasIncumplidasRespondenSuMensaje` | REST |
| CA4 | Cancelar dos veces la misma reserva: la segunda responde 404 `ERR-RES-14` | `ca4CancelarDosVeces_404_ERR_RES_14` | Punta a punta |
| CA5 | Otro jugador o un administrador de cancha: ERR-RES-15 y sigue CONFIRMADA. Si además faltan 3 horas o menos, sigue siendo ERR-RES-15 (la política 15 va antes que la 16) | `ca5OtroUsuario_ERR_RES_15` y `ca5OtroUsuarioConMenosDeTresHoras_ERR_RES_15` | Dominio |
| CA5 | 403 `ERR-RES-15` por HTTP | `ca3Ca4Ca5LasReglasIncumplidasRespondenSuMensaje` | REST |
| CA5 | Otro jugador con su propio token: 403 `ERR-RES-15` | `ca5OtroJugador_403_ERR_RES_15` | Punta a punta |
| — | El `id` no es UUID: 400 `ERR-GEN-07`; sin sesión: 401 `ERR-GEN-05`. En ninguno se llama al caso de uso | `idInvalido_400_ERR_GEN_07` y `sinSesion_401_ERR_GEN_05` | REST |

Las pruebas siguen el estilo del módulo (`CA1_...` o `ca1...`, con el criterio al principio del nombre). El dominio va en `ReservaCancelarTest`, el caso de uso en `CancelarReservaCasoUsoTest` (con Mockito, como `ReservarCanchaCasoUsoTest`), la persistencia en `RepositorioReservasJpaTest`, el REST en `ReservaControllerTest` y la punta a punta en `CancelacionPuntaAPuntaTest`.

## Decisiones tomadas

**P1 · Ruta del servicio (aprobada el 24-nov-2026: opción A)**
- A. `POST /api/reservas/{id}/cancelacion` sin cuerpo. Se lee como «registrar la cancelación de la reserva». HU32 usa la misma ruta y le agrega un cuerpo con `motivoCancelacion`.
- B. `PATCH /api/reservas/{id}/estado` con `{"estado": "CANCELADA"}`, como HU10 y HU13. Pero el único valor válido sería CANCELADA, y NO_ASISTIO (HU33) tiene reglas y quién la hace distintos, así que el campo sobra.
- C. `DELETE /api/reservas/{id}`. No conviene: la reserva no se borra, queda guardada como CANCELADA.

**Recomendación: A.**

**P2 · Los correos de cancelación (aprobada el 24-nov-2026: opción A)**
CA1 dice «se envían los correos de cancelación (HU25)», y HU25 es la HU de esos correos (COR-RES-03, con su propio CA4 de fallo del envío).
- A. HU24 no envía correos. HU25 los agrega después de la transacción de este caso de uso (D15), igual que HU21 dejó los suyos a HU22 (P6 de HU21). La parte de los correos de CA1 se verifica en HU25.
- B. Hacer HU25 (solo COR-RES-03) dentro de esta HU.

**Recomendación: A.** Mantiene cada HU con sus propios criterios y no mezcla el diseño de las plantillas de correo (HU22 tampoco está hecha todavía).

**P3 · Dos operaciones que cambian la misma reserva a la vez (aprobada el 24-nov-2026: opción A)**
Sin control, puede pasar esto: el jugador cancela mientras la tarea programada (HU28) o el propietario (HU32) también la cancelan, o el jugador toca dos veces el botón. Las dos leen la reserva CONFIRMADA y la segunda escritura pisa el origen y la fecha de la primera.
- A. Actualización condicional: el `update` solo cambia la fila si sigue CONFIRMADA (`where id = :id and estado = 'CONFIRMADA'`). Si no cambió ninguna fila, se responde ERR-RES-14. Es lo que pide D29 punto 3 para la confirmación de asistencia y la cancelación automática, y así las tres cancelaciones usan la misma regla.
- B. Bloquear la fila de la reserva al leerla (`select ... for update`).
- C. No hacer nada.

**Recomendación: A.** No hace falta el turno de la cancha: cancelar solo libera una franja.

**P4 · La firma de `Reserva.cancelar` (aprobada el 24-nov-2026: opción A)**
`clases.md` la define con `esPropietarioDelEscenario` y `motivoCancelacion`, que solo usa el propietario (HU32, Release 2).
- A. En HU24, `cancelar(solicitante, ahora, parametros)` solo con la parte del jugador. HU32 agrega los dos parámetros y las políticas 17 y 18. Queda anotado en los pendientes del Excel de clases.
- B. Ponerle ya la firma completa de `clases.md`, aunque los dos parámetros no se usen hasta HU32.

**Recomendación: A** (regla 5: no agregar lo que la HU no pide). Mientras no esté HU32, el propietario que llame este servicio recibe ERR-RES-15.

## Pendientes para los Excel

Para corregir en el Excel de origen (no se edita `docs/`):

- `clases.md`: `Reserva.cancelar` recibe por ahora `(solicitante, ahora, parametros)` (P4); `RepositorioReservas` gana `buscarPorId` y `cancelarSiSigueConfirmada`.
- `clases.md` describe la actualización condicional (D29 punto 3) para `confirmarAsistencia` y `cancelarPorFaltaDeConfirmacion`, pero no para `cancelar`. Con P3 aprobada, hay que agregarla también ahí.

## Fuera de alcance

- Los correos de cancelación al jugador y al propietario (HU25, P2).
- La cancelación del propietario por fuerza mayor, con justificación y hasta que empiece la reserva (HU32, políticas 17 y 18).
- Cancelar desde el enlace del correo de confirmación de asistencia (HU26, `{enlaceCancelar}`).
- La lista «Mis reservas» que trae los datos de CNF-RES-01 y el indicador `cancelable` (HU23, ya en `main`).
- El texto de CNF-RES-01 en `catalogo-mensajes.properties`: el backend no lo usa; lo arma la pantalla.
- La pantalla.
