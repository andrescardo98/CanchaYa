# HU21 · Reservar una cancha

**Estado:** Implementada (19-nov-2026; aprobada el 18-nov-2026) · **HU:** [HU21](../../docs/historias/HU21.md) · **Módulo:** Reservas (`co.edu.uco.canchaya.reservas`), con `ConsultaCanchas` de Escenarios y canchas

**Depende de [HU20](../HU20-consultar-disponibilidad/spec.md).** Esta especificación **no vuelve a definir** `Franja`, `HorarioRecurrente.calcularFranjas`, `Reserva` (atributos, `reconstruir`, `verificarDisponibilidad`), `ParametrosReserva`, el puerto `ConsultaCanchas` (`buscarReservable`, `franjasDelDia`, `bloqueosDelDia`, `tomarTurno`) ni `RepositorioReservas.confirmadasDeCanchaEnFecha`: están en la sección «Piezas compartidas con HU21» de HU20 y se construyen en sus tareas T1 a T6.

## Fuentes

- Dominio: [Reserva](../../docs/dominio/reserva.md), con sus atributos, la combinación `ReservaUnicaPorFranja` y `confirmar` (políticas 1, 2, 5 a 13 y 31).
- Mensajes: `EXI-RES-01`, `ERR-RES-01`, `ERR-RES-02`, `ERR-RES-05` a `ERR-RES-09`, `ERR-RES-31` e `INF-RES-02` ([mensajes.md](../../docs/especificacion/mensajes.md)). También `ERR-RES-10` (solo en el log).
- Parámetros, todos **Definidos**: PAR-GEN-01, PAR-RES-01 (30 días), PAR-RES-02 (2 franjas), PAR-RES-03 (2 horas al día), PAR-RES-04 (5 reservas activas), PAR-RES-06 (5 horas), PAR-RES-07 (12:00 m.) y PAR-RES-12 (confirmación de asistencia activa: «No»).
- Aspectos generales: AG-11 y AG-12 (reservar exige sesión y rol), AG-14 (datos no visibles), AG-17 a AG-19 (zona horaria y formatos de fecha, hora y precio).
- Decisiones: D10, D15 (correo después de guardar), D27, D29 (concurrencia), D33 (PAR-RES-12), D36 (turno de la cancha entre módulos).
- Diseño: en [clases.md](../../docs/diseno/clases.md), los bloques completos de `Reserva.confirmar` (13 parámetros), `calcularConfirmacionInicial` (4 parámetros), `horas` y la nota «Cero dobles reservas»; en `V1__estructura_inicial.sql`, la tabla `reserva` con `ex_reserva_sin_cruce` y `ck_reserva_duracion`.

## Qué se construye

Un servicio, solo para jugadores con sesión iniciada, que crea una reserva de 1 o 2 franjas seguidas de una cancha. La reserva queda CONFIRMADA de inmediato, con su precio (precio por hora × horas) y su confirmación de asistencia inicial. Mientras PAR-RES-12 esté en «No», la confirmación siempre es NO_REQUERIDA. Si dos jugadores piden la misma franja al mismo tiempo, solo uno la obtiene: el turno por cancha lo evita y la restricción de exclusión de la base de datos es el respaldo.

Quedan para la pantalla el resumen antes de confirmar, volver a cargar la disponibilidad tras el 409 con `ERR-RES-06` (CA3) y el paso al inicio de sesión con `INF-RES-02` (CA15). Los correos de la reserva son de HU22 (P6).

## Contrato del servicio

`POST /api/reservas` · **con sesión**, rol JUGADOR (la ruta no se agrega a las públicas)

```json
{ "canchaId": "uuid", "fecha": "2026-11-20", "horaInicio": "18:00", "horaFin": "20:00" }
```

Los cuatro campos son obligatorios (`ERR-GEN-01` con `@NotNull`, todos a la vez, AG-03).

Respuesta `201`:

```json
{
  "id": "uuid",
  "canchaId": "uuid",
  "cancha": "Cancha 1",
  "escenario": "Canchas La 70",
  "fecha": "2026-11-20",
  "horaInicio": "18:00",
  "horaFin": "20:00",
  "precio": 160000.00,
  "codigo": "EXI-RES-01",
  "mensaje": "¡Reserva confirmada! Canchas La 70, Cancha 1, 20/11/2026 de 18:00 a 20:00. Valor: $ 160.000. Le enviamos los datos a su correo."
}
```

No se devuelven `estado`, `confirmacionAsistencia`, `origenCancelacion`, `fechaCreacion` ni `fechaCancelacion` (conversación de HU21 y AG-14). El `id` sí, como en las demás respuestas, porque lo necesitan HU23 y HU24; la pantalla no lo muestra. En `EXI-RES-01`, `{fecha}` va en dd/mm/aaaa (AG-18) y `{precio}` como `$ 160.000` (AG-19).

| HTTP | Código | Cuándo |
|---|---|---|
| 201 | `EXI-RES-01` | Reserva CONFIRMADA |
| 400 | `ERR-GEN-01` | Falta un campo (ya lo maneja `ManejadorErrores`) |
| 400 | `ERR-GEN-07` | Un campo no se puede leer, por ejemplo `"25:00"` (ya lo maneja) |
| 401 | `ERR-GEN-05` | Sin sesión o con la sesión vencida (ya lo hace la seguridad; la pantalla muestra `INF-RES-02`, CA15) |
| 403 | `ERR-RES-05` | El usuario no es JUGADOR (se agrega a `ManejadorErrores`) |
| 404 | `ERR-RES-01` | La cancha no existe, no está DISPONIBLE o su escenario no está ACTIVO (lo agrega HU20, P2 de HU20) |
| 422 | `ERR-RES-02` | Fecha anterior a hoy o posterior a hoy + 30 |
| 422 | `ERR-RES-31` | Las horas no son en punto o no son 1 o 2 horas seguidas (P9) |
| 409 | `ERR-RES-06` | La franja la tomó otra reserva, o está fuera del horario (P8) |
| 422 | `ERR-RES-07` | Un bloqueo cubre alguna franja |
| 422 | `ERR-RES-08` | El jugador ya tiene 5 reservas activas |
| 422 | `ERR-RES-09` | Pasaría de 2 horas en esa fecha; `{fecha}` en dd/mm/aaaa |

## Diseño por capas

### Lo que se reutiliza de HU20

| Pieza | Para qué en HU21 | Tarea de HU20 |
|---|---|---|
| `Franja` y `Franja.seCruzaCon` | `franjasLibres` y `cubiertaPorBloqueo` | T1 |
| `HorarioRecurrente.calcularFranjas` (por `ConsultaCanchas.franjasDelDia`) | Política 6: que el rango esté dentro del horario y no haya empezado | T1, T3 |
| `Reserva` (atributos, `reconstruir`, `inicio`, `fin`, `horas`) y los tres enum | Base de `confirmar` | T2 |
| `ParametrosReserva` y su `@Bean` | PAR-RES-01 a 04, 06, 07 y 12 (`confirmacionAsistenciaActiva`, P2) | T2, T6 |
| `ConsultaCanchas.buscarReservable` | Política 1, `precioHora` (política 11) y los nombres de `EXI-RES-01` | T3 |
| `ConsultaCanchas.tomarTurno` | D29 punto 2 (CA4) | T3, T4 |
| `ConsultaCanchas.bloqueosDelDia` | Política 7 (CA5) | T3, T4 |
| `RepositorioReservas.confirmadasDeCanchaEnFecha` | Política 6 (CA3, CA13) | T5 |
| `ERR-RES-01 → 404` y su texto | CA9 | T8 |

### Dominio (`reservas.domain.Reserva`)

**`confirmar`**, con los 13 parámetros de `clases.md`:

```java
/**
 * Políticas 5, 1, 2, 31, 6, 7, 8, 9, 11 y 12, en el orden de la columna Excepciones de clases.md.
 * La 10 (id repetido) la revisa el caso de uso; la 13 (correo) es de HU22.
 */
public static Reserva confirmar(UUID canchaId, Solicitante jugador, LocalDate fecha, LocalTime horaInicio,
		LocalTime horaFin, BigDecimal precioHora, boolean canchaDisponible, List<Franja> franjasLibres,
		boolean cubiertaPorBloqueo, int reservasActivasJugador, int horasReservadasEnElDia,
		LocalDateTime ahora, ParametrosReserva parametros);
```

| Orden | Política | Regla | Error |
|---|---|---|---|
| 1 | 5 | `jugador` tiene rol JUGADOR | `ERR-RES-05` |
| 2 | 1 | `canchaDisponible` | `ERR-RES-01` |
| 3 | 2 | `hoy ≤ fecha ≤ hoy + ventanaReservaDias`, incluido hoy + 30 (P6 de HU20) | `ERR-RES-02` |
| 4 | 31 | Horas en punto y `horaFin − horaInicio` entre 1 y `franjasMaximasPorReserva` horas (P9). No se exige que las dos franjas sean del mismo horario recurrente (P3, D10) | `ERR-RES-31` |
| 5 | 6 | Cada franja de `[horaInicio, horaFin)` está en `franjasLibres`, que ya quita las que empezaron y las reservadas. Dos franjas seguidas de horarios contiguos (06–12 y 12–14) sirven (P3) | `ERR-RES-06` |
| 6 | 7 | `!cubiertaPorBloqueo` | `ERR-RES-07` |
| 7 | 8 | `reservasActivasJugador < reservasActivasMaximas` | `ERR-RES-08` |
| 8 | 9 | `horasReservadasEnElDia + horas ≤ horasMaximasPorDia` | `ERR-RES-09` con la fecha |
| — | 11 | `precio = precioHora × horas`, con 2 decimales | — |
| — | 12 | `calcularConfirmacionInicial` | — |

La reserva nace con `estado = CONFIRMADA`, `fechaCreacion = ahora` en America/Bogota como `OffsetDateTime` y sin datos de cancelación.

**`calcularConfirmacionInicial(fecha, horaInicio, ahora, parametros)`** (privado, firma de `clases.md`), política 12 y D33:
- Si `parametros.confirmacionAsistenciaActiva()` es falso (PAR-RES-12 en «No», P2) → `NO_REQUERIDA`.
- Si no, `PENDIENTE` cuando faltan **5 horas o más** (PAR-RES-06) **y** la reserva empieza a las 12:00 o después (PAR-RES-07). En cualquier otro caso, `NO_REQUERIDA`.

**`conNuevoId()`** (nuevo, para la política 10, como `HorarioRecurrente.conNuevoId`). No está en `clases.md`; ver «Pendientes en los Excel».

### Puertos y caso de uso

| Capa | Pieza | Qué hace |
|---|---|---|
| Puerto de entrada | `reservas.application.port.in.ReservarCancha`, con el record `ReservaConfirmada` (la reserva y los nombres de cancha y escenario) | Contrato del caso de uso |
| Puerto de salida | `RepositorioReservas` (ya existe): `existePorId`, `guardar`, `contarActivasDeJugador(jugadorId, ahora)` y `horasConfirmadasDeJugadorEnFecha(jugadorId, fecha)` | Políticas 10, 8 (activa = CONFIRMADA y no terminada, P4) y 9 |
| Caso de uso | `reservas.application.usecase.ReservarCanchaCasoUso` | Ver los pasos abajo |

Pasos de `ReservarCanchaCasoUso.ejecutar(canchaId, fecha, horaInicio, horaFin, solicitante)`. Todo va dentro de **una** transacción (`TransactionTemplate`), porque el turno de la cancha dura hasta que la transacción termina (D36):

1. `consultaCanchas.tomarTurno(canchaId)`: desde aquí, otra reserva o un bloqueo de esa misma cancha esperan (D29 punto 2). Se toma **antes** de leer franjas y reservas, como pide `clases.md`.
2. `consultaCanchas.buscarReservable(canchaId)`. Si está vacío (incluye el escenario INACTIVO), no se piden franjas y `confirmar` recibe `canchaDisponible = false`: lanza `ERR-RES-05` o `ERR-RES-01` según el rol.
3. `consultaCanchas.franjasDelDia(canchaId, fecha)`. Si responde `ERR-HOR-10`, se sigue con franjas vacías y `confirmar` lanza `ERR-RES-02` con su propia regla (P1). Cualquier otro error se deja pasar.
4. `bloqueosDelDia` y `confirmadasDeCanchaEnFecha`: `franjasLibres` = franjas que no se cruzan con ninguna reserva; `cubiertaPorBloqueo` = algún bloqueo se cruza con `[horaInicio, horaFin)`. Las reservas y los bloqueos van por separado para distinguir `ERR-RES-06` de `ERR-RES-07`.
5. `contarActivasDeJugador` y `horasConfirmadasDeJugadorEnFecha`.
6. `Reserva.confirmar(...)`.
7. Política 10: mientras `existePorId`, se registra `ERR-RES-10` en el log y `conNuevoId()`.
8. `guardar`: si la restricción `ex_reserva_sin_cruce` la rechaza, el adaptador lanza `ERR-RES-06` (respaldo de D29 punto 1, igual que `RepositorioHorariosJpa` con `ERR-HOR-04`).
9. Al confirmar la transacción se libera el turno. HU21 no envía correos (P6): HU22 los agrega en este punto, **fuera** de la transacción (D15).

Riesgo conocido (P7): el turno es por cancha. Si un jugador manda en el mismo instante dos reservas en canchas distintas, las dos pueden pasar los límites de PAR-RES-03 y PAR-RES-04. Se acepta y se anota en el pull request.

Una reserva de 2 horas es **una sola fila** con un solo rango (D29 punto 1): se guardan las dos franjas o ninguna (CA11, CA13) sin lógica adicional.

### Persistencia (`reservas.infrastructure.persistence`)

| Pieza | Cambio |
|---|---|
| `ReservaEntidad` y `ReservaJpa` (nuevos) | La entidad JPA de la tabla `reserva` que anunciaba el comentario de `RepositorioReservasJdbc`. `rango` es columna generada: no se mapea. `aDominio()` usa `Reserva.reconstruir` |
| `RepositorioReservasJpa` (nuevo) o `RepositorioReservasJdbc` (existente) | Implementan entre los dos `RepositorioReservas`. Se propone un solo adaptador, `RepositorioReservasJpa`, que absorbe las dos consultas JDBC existentes con `NamedParameterJdbcTemplate`, para no tener dos clases que implementen el mismo puerto |
| `guardar` | `saveAndFlush` y traducción a `ERR-RES-06` de la violación de `ex_reserva_sin_cruce` (`23P01`) y del bloqueo mutuo (`40P01`, con advertencia en el log, P10) |
| `contarActivasDeJugador` | CONFIRMADAS del jugador con `fecha + hora_fin > ahora` (P4); usa `ix_reserva_jugador_fecha` |
| `horasConfirmadasDeJugadorEnFecha` | Suma de `hora_fin − hora_inicio` de sus CONFIRMADAS de esa fecha, en cualquier cancha |

### REST y transversal

| Pieza | Cambio |
|---|---|
| `reservas.infrastructure.rest.ReservaController` | `POST /api/reservas`; arma el `Solicitante` del token como los otros controladores |
| `SolicitudReserva` y `RespuestaReserva` | Entrada con `@NotNull` y salida con `codigo` y `mensaje` |
| `ManejadorErrores` | `ERR-RES-05 → 403` y `ERR-RES-06 → 409` (P8) |
| `catalogo-mensajes.properties` | `EXI-RES-01`, `ERR-RES-02`, `05`, `06`, `07`, `08`, `09` y `31`. `INF-RES-02` no: lo muestra la pantalla y ningún servicio lo devuelve. `ERR-RES-10` tampoco: solo va al log, como `ERR-HOR-05`. También `campo.canchaId` y `campo.fecha`, que usa `ERR-GEN-01` para nombrar el campo que falta |
| `application.properties` | Nada nuevo: PAR-RES-12 (`canchaya.reserva.confirmacion-asistencia-activa=${PAR_RES_12:false}`) ya lo agrega HU20 T6 (P2) |

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1 | Franja libre: CONFIRMADA con su hora de inicio, su hora de fin y precio = `precioHora` × 1 | `CA1_franjaLibre_reservaConfirmadaConPrecio` | Dominio (`ReservaConfirmarTest`) |
| CA1 | Se guarda y la franja sale ocupada en la disponibilidad de HU20 | `CA1_reservar_franjaDejaDeOfrecerse` | Punta a punta (`ReservaPuntaAPuntaTest`) |
| CA1 | 201 con `EXI-RES-01` y los datos de la reserva; fecha y precio con el formato de AG-18 y AG-19; sin `estado` ni `confirmacionAsistencia` | `CA1_reservaConfirmada_201_EXI_RES_01` | REST |
| CA1 | Los correos | Se prueban en HU22 (P6) | — |
| CA2 | PAR-RES-12 en «No» → siempre `NO_REQUERIDA` | `CA2_confirmacionInactiva_siempreNoRequerida` | Dominio |
| CA2 | Activa: 5 h o más y desde las 12:00 → `PENDIENTE`; 4 h 59 min, o a las 11:00 → `NO_REQUERIDA`; borde de exactamente 5 h y a las 12:00 → `PENDIENTE` | `CA2_confirmacionActiva_pendienteONoRequerida` | Dominio |
| CA3 | Una reserva confirmada cubre la franja → 409 con `ERR-RES-06` y no se guarda | `CA3_franjaYaReservada_ERR_RES_06` y `CA3_franjaYaReservada_409` | Dominio + caso de uso + REST |
| CA3 | La base rechaza un cruce que se escapó del turno → `ERR-RES-06` | `CA3_restriccionDeExclusion_ERR_RES_06` | Persistencia (Testcontainers) |
| CA4 | Dos hilos reservan la misma franja a la vez: una 201 y otra 409 con `ERR-RES-06`; en la base queda una sola. Es el spike RT-03 (P5) | `CA4_dosSolicitudesSimultaneas_soloUnaSeConfirma` | Persistencia y punta a punta (Testcontainers) |
| CA5 | Bloqueo sobre la franja → `ERR-RES-07` | `CA5_franjaBloqueada_ERR_RES_07` | Dominio + caso de uso |
| CA6 | Con 5 activas → `ERR-RES-08`; con 4, y con 5 de las que una ya terminó → se confirma | `CA6_cincoActivas_ERR_RES_08` y `CA6_reservaTerminada_noCuentaComoActiva` | Dominio + persistencia |
| CA7 | 1 h ese día + 2 nuevas → `ERR-RES-09`; 1 + 1 → se confirma; en otra cancha también cuenta | `CA7_superaDosHorasEnElDia_ERR_RES_09` | Dominio + persistencia |
| CA8 | Ayer y hoy + 31 → `ERR-RES-02` (no `ERR-HOR-10`); hoy + 30 → se confirma | `CA8_fueraDeVentana_ERR_RES_02` | Dominio + caso de uso |
| CA9 | Cancha inexistente, cancha INACTIVA o escenario INACTIVO → 404 `ERR-RES-01` | `CA9_canchaNoDisponible_404_ERR_RES_01` | Caso de uso + REST |
| CA10 | ADMINISTRADOR_CANCHA y ADMINISTRADOR_SISTEMA → 403 `ERR-RES-05`, aunque la cancha no exista | `CA10_noJugador_403_ERR_RES_05` | Dominio + REST |
| CA11 | Dos franjas libres y seguidas → una sola reserva, de la primera a la segunda, precio × 2 | `CA11_dosFranjas_unaReservaConPrecioDoble` | Dominio + punta a punta |
| CA11 | Con horarios 06–12 y 12–14, la reserva de 11 a 13 se confirma (P3) | `CA11_franjasSeguidasDeHorariosContiguos_seConfirma` | Dominio + caso de uso |
| CA12 | 3 horas, 0 horas, `horaFin` antes de `horaInicio` y 18:30–19:30 → `ERR-RES-31` | `CA12_duracionInvalida_ERR_RES_31` | Dominio |
| CA13 | Segunda franja reservada → `ERR-RES-06`; bloqueada → `ERR-RES-07`; fuera del horario → `ERR-RES-06`. En los tres casos no queda ninguna fila | `CA13_segundaFranjaNoDisponible_noSeGuardaNinguna` | Dominio + caso de uso |
| CA14 | Ya tiene una de 2 horas esa fecha → `ERR-RES-09` | `CA14_yaTieneDosHorasEseDia_ERR_RES_09` | Dominio |
| CA15 | Sin token → 401 `ERR-GEN-05`. `INF-RES-02` y volver al resumen son de la pantalla | `CA15_sinSesion_401` | Punta a punta |
| Pol. 10 | Id repetido → se registra `ERR-RES-10` y se reintenta | `politica10_idRepetido_reintentaConOtro` | Caso de uso |
| HU9 CA8 | El propietario cambia el precio con `PUT /api/canchas/{id}`; la reserva nueva sale con el precio nuevo y una anterior conserva el suyo | `HU9_CA8_reservaNueva_tomaElPrecioActualizado` | Punta a punta |
| D36 | El caso de uso toma el turno antes de leer las franjas | `turno_seTomaAntesDeLeerFranjas` (orden de llamadas con `InOrder`) | Caso de uso |

## Decisiones tomadas (18-nov-2026)

**P1 · Fuera de la ventana sale `ERR-RES-02`.** Si `franjasDelDia` responde `ERR-HOR-10`, el caso de uso sigue con franjas vacías y `confirmar` lanza `ERR-RES-02` con su propia regla (política 2). No cambian `clases.md` ni los mensajes.

**P2 · PAR-RES-12 va en `ParametrosReserva`.** Es el undécimo componente, `confirmacionAsistenciaActiva` (boolean). Lo crea HU20 T2 y su valor lo lee HU20 T6, con `false` por defecto (D33). Las firmas de `confirmar` y `calcularConfirmacionInicial` no cambian.

**P3 · Dos franjas seguidas sirven aunque sean de horarios distintos.** Si las dos están libres y seguidas, la reserva se acepta aunque salgan de horarios contiguos del mismo día (06–12 y 12–14): es lo que dice D10, «1 o 2 franjas consecutivas de la misma cancha». Se corrigen en el Excel la política 31 y la regla especial de `horaFin` del enriquecido, que pedían «un mismo horario recurrente».

**P4 · Activa = CONFIRMADA y que no haya terminado** (`fecha + horaFin > ahora`).

**P5 · La prueba de CA4 es el spike RT-03.** Dos hilos con Testcontainers y `CountDownLatch`, con y sin turno. Va primero en T3 y su resultado se anota en el pull request y en `docs/proyecto.md`.

**P6 · Los correos van en HU22.** HU21 no envía correos. La parte de CA1 sobre los correos se prueba en HU22.

**P7 · Riesgo conocido.** Dos solicitudes simultáneas del mismo jugador en canchas distintas pueden pasar sus límites. Se acepta y se anota en el pull request.

**P8 · `ERR-RES-06` responde 409.**

**P9 · `ERR-RES-31`** cuando las horas no son en punto o la duración no es de 1 o 2 horas. `ERR-RES-06` cuando el rango sí es de horas en punto pero alguna franja no está libre o está fuera del horario.

## Resultado del spike RT-03 (19-nov-2026)

`ReservaConcurrenciaTest`, 20 repeticiones de cada caso, con dos hilos que salen a la vez de un `CountDownLatch`:

| Caso | Reservas que quedan | Respuesta del que pierde |
|---|---|---|
| Con turno (caso de uso completo) | 1 en las 20 | `ERR-RES-06` en las 20 |
| Sin turno, antes de P10 (solo la restricción `ex_reserva_sin_cruce`) | 1 en las 20 | `ERR-RES-06` en 13; **bloqueo mutuo** de PostgreSQL (`40P01`) en 7, que salía como 500 |
| Sin turno, con P10 | 1 en las 20 | `ERR-RES-06` en las 20 (en la corrida de verificación, 11 por la restricción y 9 por bloqueo mutuo traducido) |

Conclusión: la restricción de exclusión sola ya impide la doble reserva, pero cuando las dos inserciones se cruzan en el índice GiST, cada una espera a la otra y PostgreSQL aborta una por bloqueo mutuo. El turno por cancha (D29 punto 2) es la defensa principal y la restricción es el respaldo; con P10, el respaldo también responde `ERR-RES-06`.

**P10 · El bloqueo mutuo al guardar una reserva responde `ERR-RES-06` (decidido el 19-nov-2026).** `RepositorioReservasJpa.guardar` traduce `40P01` a `ERR-RES-06` (409), igual que la violación de exclusión (`23P01`). Lo registra en el log como advertencia, con la cancha, la fecha y la franja. Las dos se capturan alrededor de `saveAndFlush`, que es donde se ejecuta el insert y la base responde; si llegaran al confirmar la transacción, ya no se podrían traducir. La prueba sin turno exige 20 de 20 `ERR-RES-06`.

## Preguntas abiertas

Ninguna.

## Para el pull request

- Resultado del spike RT-03: la tabla de arriba, que también está en [docs/proyecto.md](../../docs/proyecto.md) (P5).
- Riesgo conocido (P7): dos solicitudes simultáneas del mismo jugador en canchas distintas pueden pasar los límites de PAR-RES-03 y PAR-RES-04, porque el turno es por cancha.

## Tareas que dependen de HU20

Las tareas de [tareas.md](tareas.md) indican cuáles de HU20 necesitan. En resumen: T1 de HU21 depende de T1, T2 y T6 de HU20; T2 de T3 y T5; T3 de T4 y T5; y T5 (punta a punta) de T8 de HU20, porque comprueba en el servicio de disponibilidad que la franja reservada sale ocupada. T4, T6 y T7 no dependen de HU20.

## Pendientes en los Excel de origen

| Excel | Cambio | Estado |
|---|---|---|
| Diagrama de clases, hoja ParametrosReserva | Agregar `confirmacionAsistenciaActiva` (PAR-RES-12), por P2 (también está en la lista de HU20) | Pendiente |
| Diagrama de clases, hoja Reserva | Agregar `conNuevoId()` (política 10), como en las demás clases con reintento de UUID | Pendiente |
| Modelo de dominio enriquecido, hoja Reserva, política 31 | Quitar «dentro de un mismo horario recurrente del escenario»: «una reserva abarca una o dos franjas consecutivas de la misma cancha (máximo 2 horas)», según D10 (P3) | Pendiente |
| Modelo de dominio enriquecido, hoja Reserva, regla especial de `horaFin` | Quitar «dentro de un mismo horario recurrente del escenario», según D10 (P3) | Pendiente |
| Modelo de dominio enriquecido, hoja Reserva, política 8 | Precisar que «activa» es CONFIRMADA y que todavía no termina (P4) | Pendiente |
| Decisiones, D29 | Registrar que el spike RT-03 se hizo con la prueba de CA4 (`ReservaConcurrenciaTest`) y su resultado (P5): cero dobles reservas con y sin turno; sin turno, PostgreSQL puede abortar una inserción por bloqueo mutuo (`40P01`), que se traduce a `ERR-RES-06` como la violación de exclusión (P10). `docs/proyecto.md` ya se actualizó a mano | Pendiente |

## Fuera de alcance

- Los correos COR-RES-01 y COR-RES-02 (HU22, según P6).
- Ver y cancelar las reservas propias (HU23 y HU24), la confirmación de asistencia (HU26 a HU28) y la agenda del administrador.
- `ConsultaReservas`: ningún otro módulo la necesita todavía.
- La pantalla: el resumen, volver a cargar la disponibilidad tras `ERR-RES-06` y el inicio de sesión con `INF-RES-02`.
