# HU29 · Consultar la agenda del escenario

**Estado:** Aprobada (25-nov-2026) · **HU:** [HU29](../../docs/historias/HU29.md) · **Módulo:** Reservas (`co.edu.uco.canchaya.reservas`) para la agenda, y Escenarios y canchas (`co.edu.uco.canchaya.escenarios`) para la lista de escenarios, los permisos, las franjas y los bloqueos

**Depende de** [HU7](../../docs/historias/HU7.md) (horarios), [HU20](../HU20-consultar-disponibilidad/spec.md) (`Franja`, `calcularFranjas`), [HU21](../HU21-reservar-cancha/spec.md) (`Reserva`, `RepositorioReservas`) y [HU23](../HU23-consultar-reservas/spec.md), todas implementadas. Toca tres HU que **no** están implementadas: HU30 (bloqueos, Release 2), HU32/HU33 (fuerza mayor e inasistencia, Release 2) y HU34 (encargados, Release 3). Ver P1 a P3. No cambia la base de datos: las tablas `bloqueo` y `encargado_escenario` ya están en la V1, y los índices `ix_reserva_cancha_fecha` y `ex_bloqueo_sin_cruce` sirven para estas consultas.

## Fuentes

- Dominio: [Reserva](../../docs/dominio/reserva.md) (atributos; para CA3 las políticas 15 y 17 de `cancelar` y 22, 23 y 32 de `marcarNoAsistio`), [Bloqueo](../../docs/dominio/bloqueo.md) (`tipoBloqueo`, `motivo`), [Cancha](../../docs/dominio/cancha.md), [HorarioRecurrente](../../docs/dominio/horariorecurrente.md) (políticas 11 y 12 de `calcularFranjas`) y [EncargadoEscenario](../../docs/dominio/encargadoescenario.md) (RN-15).
- Mensajes: `ERR-GEN-06` («No tiene permiso para realizar esta acción»). Ya está en `catalogo-mensajes.properties` y `ManejadorErrores` lo responde con 403. No hace falta ningún mensaje nuevo.
- Parámetros: PAR-HOR-01 (franja de una hora, regla fija: ya es la constante `Franja.DURACION`). Para CA3, PAR-RES-09 (24 horas para marcar la inasistencia, **Definido**; ya está en `ParametrosReserva.horasPlazoInasistencia`).
- Aspectos generales: AG-09 (lista vacía), AG-11 (exige sesión), AG-12 (permisos en el servidor), AG-14 (datos no visibles), AG-15 (el celular del jugador solo lo ven el propietario y los encargados, «agenda y correo de nueva reserva»), AG-17 y AG-18 (zona horaria y formatos).
- Decisiones: D02 (el encargado no cancela por fuerza mayor), D03 y D07 (encargado con rol ADMINISTRADOR_CANCHA limitado por EncargadoEscenario), D20 (inasistencia hasta 24 h), D27 (America/Bogota), D39 (paquetes).
- Arquitectura: [componentes.md](../../docs/diseno/componentes.md) dice que Escenarios ofrece `ConsultaEscenarios` y que Reservas tiene la agenda; [escenarios-calidad.md](../../docs/arquitectura/escenarios-calidad.md): ESC-USA-04 (abrir la agenda en el día actual), ESC-SEG-02 (la agenda exige token) y ESC-AUD-01 (la agenda muestra el origen y la justificación de las cancelaciones, ver P4).
- Prototipo «17 Agenda del escenario.png»: no está en el repositorio; el contrato no depende de él.

## Qué se construye

Dos servicios para el administrador de cancha (propietario o encargado de sede):

1. **La lista de escenarios con agenda** (CA4, CA5): el propietario ve todos los suyos; el encargado, solo los que tiene asignados.
2. **La agenda de un escenario** por día o por semana (CA1, CA2): para cada cancha y cada día, las franjas calculadas con los horarios del escenario marcadas como LIBRE, RESERVADA o BLOQUEADA, y el detalle de las reservas (nombre y celular del jugador, confirmación de asistencia y las opciones que tiene quien consulta, CA3) y de los bloqueos (tipo y motivo). Si el usuario no es el propietario ni un encargado asignado, responde ERR-GEN-06 (CA6).

Quedan para la pantalla: el selector de escenario (CA4), el cambio de vista día/semana (CA2), la grilla de colores, el detalle al seleccionar una reserva (CA3), mostrar u ocultar los botones de cancelar por fuerza mayor y de marcar inasistencia según los indicadores (las acciones son HU32 y HU33), y los formatos dd/mm/aaaa y hh:mm (AG-18).

## Contrato del servicio

### 1. `GET /api/agenda/escenarios` (CA4, CA5)

**Con sesión**, rol ADMINISTRADOR_CANCHA (P7). No recibe parámetros: el usuario sale del token.

Respuesta `200`, ordenada por nombre:

```json
[
  { "id": "uuid", "nombre": "Canchas La 70", "estado": "ACTIVO", "propietario": true },
  { "id": "uuid", "nombre": "Sede Belén",    "estado": "ACTIVO", "propietario": false }
]
```

- `propietario` es verdadero si el usuario es el propietario y falso si es encargado asignado. Si fuera las dos cosas no puede pasar (D22).
- Trae escenarios ACTIVOS e INACTIVOS (P8).
- Sin escenarios: lista vacía (P8).

| HTTP | Código | Cuándo |
|---|---|---|
| 200 | — | Siempre que el rol sea ADMINISTRADOR_CANCHA, aunque la lista esté vacía |
| 401 | `ERR-GEN-05` | Sin sesión o con la sesión vencida (ya lo hace la seguridad) |
| 403 | `ERR-GEN-06` | El usuario no tiene rol ADMINISTRADOR_CANCHA (P7) |

### 2. `GET /api/escenarios/{escenarioId}/agenda?fecha=AAAA-MM-DD&vista=DIA|SEMANA` (CA1, CA2, CA3, CA6)

**Con sesión.** La ruta no se agrega a las públicas (ESC-SEG-02).

- `fecha` es opcional: si no viene, es hoy en America/Bogota (D27, ESC-USA-04). Cualquier fecha, pasada o futura (P6).
- `vista` es opcional, por defecto `DIA`. `DIA` trae solo esa fecha; `SEMANA` trae de lunes a domingo de la semana de `fecha` (P5).

Respuesta `200`:

```json
{
  "escenarioId": "uuid",
  "escenario": "Canchas La 70",
  "propietario": true,
  "desde": "2026-11-23",
  "hasta": "2026-11-29",
  "canchas": [
    {
      "id": "uuid",
      "nombre": "Cancha 1",
      "estado": "DISPONIBLE",
      "dias": [
        {
          "fecha": "2026-11-25",
          "franjas": [
            { "horaInicio": "17:00", "horaFin": "18:00", "estado": "LIBRE",      "reservaId": null,   "bloqueoId": null },
            { "horaInicio": "18:00", "horaFin": "19:00", "estado": "RESERVADA",  "reservaId": "uuid", "bloqueoId": null },
            { "horaInicio": "19:00", "horaFin": "20:00", "estado": "RESERVADA",  "reservaId": "uuid", "bloqueoId": null },
            { "horaInicio": "20:00", "horaFin": "21:00", "estado": "BLOQUEADA",  "reservaId": null,   "bloqueoId": "uuid" }
          ],
          "reservas": [
            {
              "id": "uuid",
              "horaInicio": "18:00",
              "horaFin": "20:00",
              "estado": "CONFIRMADA",
              "jugador": "Ana Gómez",
              "celularJugador": "+573001112233",
              "confirmacionAsistencia": "NO_REQUERIDA",
              "origenCancelacion": null,
              "motivoCancelacion": null,
              "fechaCancelacion": null,
              "puedeCancelarPorFuerzaMayor": true,
              "puedeMarcarInasistencia": false
            }
          ],
          "bloqueos": [
            { "id": "uuid", "horaInicio": "20:00", "horaFin": "21:00", "tipoBloqueo": "MANTENIMIENTO", "motivo": "Cambio de malla" }
          ]
        }
      ]
    }
  ]
}
```

- `canchas`: **todas** las del escenario, en cualquier estado, ordenadas por nombre (CA1). `dias` trae un elemento por cada fecha de `desde` a `hasta`, aunque esté vacío.
- `franjas`: las del día de la semana según los horarios **actuales** del escenario (política 11 de `calcularFranjas`), **sin** quitar las que ya empezaron (la política 12 es para el jugador; el administrador necesita ver la operación de hoy completa) y sin la ventana de PAR-RES-01 (P6). Una franja es RESERVADA si la ocupa una reserva CONFIRMADA o NO_ASISTIO; BLOQUEADA si la cubre un bloqueo; si no, LIBRE. Una reserva CANCELADA **no** ocupa la franja (restricción de la HU).
- `reservas`: las de ese día y esa cancha, por hora de inicio. Incluye las CANCELADAS con su origen, justificación y fecha (P4). `motivoCancelacion` solo viene si el origen es `ADMINISTRADOR`. Una reserva o un bloqueo que quedó por fuera de los horarios actuales (porque el horario cambió después) sigue apareciendo en su lista aunque no tenga franja.
- `puedeCancelarPorFuerzaMayor` y `puedeMarcarInasistencia` son las «opciones disponibles» de CA3 (P3): dicen si hoy se podría, con la hora del servidor. Siempre son falsos para una reserva que no está CONFIRMADA.
- `celularJugador` es dato sensible: solo sale en este servicio, que exige ser propietario o encargado (AG-15). No van `jugadorId`, `precio`, `fechaCreacion` ni `fechaSolicitudConfirmacion` (AG-14: la HU no los pide). Los `id` van para que HU32, HU33 y HU31 puedan actuar; la pantalla no los muestra.
- `propietario` le dice a la pantalla si el usuario es propietario o encargado.

| HTTP | Código | Cuándo |
|---|---|---|
| 200 | — | El usuario es el propietario o un encargado asignado |
| 400 | — | `fecha` o `vista` con un formato que no se entiende (manejo actual de parámetros de `ManejadorErrores`) |
| 401 | `ERR-GEN-05` | Sin sesión o con la sesión vencida |
| 403 | `ERR-GEN-06` | El usuario no es el propietario ni un encargado asignado, o el escenario no existe (CA6, P7) |

## Diseño por capas

### Dominio de Escenarios

| Clase · método | Qué hace | Política |
|---|---|---|
| `HorarioRecurrente.franjasDeLaFecha(List<HorarioRecurrente>, LocalDate)` (nuevo, estático) | Franjas de una hora en punto de los horarios del día de la semana de la fecha, por hora de inicio, **sin** filtrar por la hora actual ni por la ventana. `calcularFranjas` pasa a usarlo y luego aplica las políticas 10 y 12, así no se repite el cálculo | Política 11 de `calcularFranjas`; PAR-HOR-01 |
| `Escenario.validarAccesoAgenda(Solicitante, boolean esEncargadoAsignado)` (nuevo) | Devuelve verdadero si el solicitante es el propietario y falso si es encargado asignado; si no es ninguno, lanza `ERR-GEN-06`. El caso de uso consulta la asignación y la pasa como valor lógico, como dice `clases.md` («esEncargadoAsignado») | RN-15, AG-12; CA6 |

### Dominio de Reservas

| Clase · método | Qué hace | Política |
|---|---|---|
| `Reserva.ocupa(Franja)` (nuevo) | Verdadero si la reserva no está CANCELADA y se cruza con la franja (`Franja.seCruzaCon`) | Restricción de la HU: «las canceladas no ocupan la franja»; CA1 |
| `Reserva.sePuedeCancelarPorFuerzaMayor(LocalDateTime ahora, boolean esPropietario)` (nuevo) | CONFIRMADA, el solicitante es el propietario (el encargado no, D02) e `inicio()` es posterior a `ahora` | Políticas 14, 15 y 17 de `cancelar`; CA3. Lo reutilizará `cancelar` en HU32 |
| `Reserva.sePuedeMarcarInasistencia(LocalDateTime ahora, ParametrosReserva)` (nuevo) | CONFIRMADA, `fin()` ya pasó (`fin() <= ahora`) y `ahora <= fin() + horasPlazoInasistencia` | Políticas 14, 22 y 32 de `marcarNoAsistio` (RN-09, D20, PAR-RES-09); CA3. La 23 (propietario o encargado) ya la garantiza el acceso a la agenda. Lo reutilizará `marcarNoAsistio` en HU33 |
| `EstadoFranja` (enum nuevo: `LIBRE`, `RESERVADA`, `BLOQUEADA`) con `de(Franja, List<Reserva> delDia, boolean bloqueada)` | RESERVADA si alguna reserva la ocupa; si no, BLOQUEADA si la cubre un bloqueo; si no, LIBRE | CA1 |
| `VistaAgenda` (enum nuevo: `DIA`, `SEMANA`) con `desde(LocalDate)` y `hasta(LocalDate)` | DIA: la misma fecha. SEMANA: el lunes y el domingo de la semana de la fecha | CA2, P5 |

Ninguno de estos métodos está en `clases.md`: van a «Pendientes en los Excel de origen».

### Puerto de entrada de Escenarios · `ListarEscenariosAgenda` (nuevo, `escenarios.application.port.in`)

```java
/** @throws ReglaNegocioException ERR-GEN-06 si el solicitante no tiene rol ADMINISTRADOR_CANCHA */
List<EscenarioAgenda> ejecutar(Solicitante solicitante);

record EscenarioAgenda(UUID id, String nombre, EstadoEscenario estado, boolean propietario) {}
```

`ListarEscenariosAgendaCasoUso`: revisa el rol (P7), trae los del propietario (`repositorioEscenarios.listarPorAdministrador`) y los asignados (`repositorioEncargados.escenariosAsignados` + `repositorioEscenarios.listarPorIds`), los une y los ordena por nombre. Solo lo usa el controlador de Escenarios: no sale a otro módulo, por eso puede usar `EstadoEscenario`.

### Puerto de entrada de Escenarios · `ConsultaEscenarios` (nuevo, el que nombra `componentes.md`)

Es lo que Escenarios le ofrece a Reservas para armar la agenda, en una sola llamada:

```java
/**
 * HU29: el escenario, sus canchas, sus franjas y sus bloqueos de desde a hasta (los dos incluidos).
 * @throws ReglaNegocioException ERR-GEN-06 si el escenario no existe o el solicitante no es su
 *         propietario ni un encargado asignado (CA6)
 */
DatosAgenda datosAgenda(UUID escenarioId, Solicitante solicitante, LocalDate desde, LocalDate hasta);

record DatosAgenda(UUID escenarioId, String escenario, boolean propietario, List<CanchaAgenda> canchas,
        Map<LocalDate, List<Franja>> franjasPorFecha, List<BloqueoAgenda> bloqueos) {}
record CanchaAgenda(UUID id, String nombre, String estado) {}
record BloqueoAgenda(UUID id, UUID canchaId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
        String tipoBloqueo, String motivo) {}
```

- Los estados y el tipo de bloqueo van como texto, no como los enum de Escenarios, porque Reservas no puede usar el dominio de otro módulo (solo `Franja` y `Solicitante` son excepción).
- `ConsultaEscenariosCasoUso`: busca el escenario (si no existe, `ERR-GEN-06`, P7), consulta `repositorioEncargados.estaAsignado`, llama `escenario.validarAccesoAgenda`, trae las canchas (`repositorioCanchas.listarPorEscenario`), los horarios (`repositorioHorarios.buscarPorEscenario`, una vez) y calcula `franjasDeLaFecha` para cada fecha, y trae los bloqueos (`repositorioBloqueos.deCanchasEntre`, una consulta).

### Puerto de entrada de Reservas · `ConsultarAgenda` (nuevo)

```java
Agenda ejecutar(UUID escenarioId, LocalDate fecha, VistaAgenda vista, Solicitante solicitante);
```

Con records de salida `Agenda`, `CanchaDeAgenda`, `DiaDeAgenda`, `FranjaDeAgenda`, `ReservaDeAgenda` y `BloqueoDeAgenda` que siguen el JSON del contrato.

### Caso de uso · `ConsultarAgendaCasoUso` (nuevo)

1. `fecha` vacía → hoy con `LocalDate.now(reloj)` (D27). `desde`/`hasta` con `vista` (P5).
2. `consultaEscenarios.datosAgenda(...)`: aquí se revisa el permiso (CA6) **antes** de leer cualquier reserva.
3. `repositorioReservas.deCanchasEntre(idsCanchas, desde, hasta)`: todas las reservas de esas canchas en esas fechas, en cualquier estado (P4), en una consulta.
4. `consultaUsuarios.buscarContactos(idsJugadores)`: nombre y celular en una consulta (CA3).
5. Para cada cancha y fecha: `EstadoFranja.de` en cada franja (CA1), las reservas y los bloqueos del día, y los dos indicadores de CA3 con `ahora = LocalDateTime.now(reloj)`.

Solo lee: no toma el turno de la cancha ni abre transacción de escritura (D29 y D36 no aplican). No envía correos (D15 no aplica).

### Persistencia

| Puerto (cambia o es nuevo) | Adaptador | Qué hace |
|---|---|---|
| `escenarios...port.out.RepositorioEncargados` (nuevo): `boolean estaAsignado(UUID escenarioId, UUID usuarioId)` y `List<UUID> escenariosAsignados(UUID usuarioId)` | `RepositorioEncargadosJdbc` (nuevo) | SQL directo sobre `encargado_escenario`, igual que `RepositorioBloqueosJdbc`: la clase `EncargadoEscenario` y su entidad JPA llegan con HU34 (P1) |
| `RepositorioEscenarios`: `listarPorAdministrador(UUID)` y `listarPorIds(Collection<UUID>)` (nuevos) | `RepositorioEscenariosJpa` | Devuelven `ResumenEscenario`, sin fotos, como `listar` de HU13 |
| `RepositorioBloqueos`: `deCanchasEntre(Collection<UUID> canchaIds, LocalDate desde, LocalDate hasta)` (nuevo) | `RepositorioBloqueosJdbc` | Con id, cancha, fecha, horas, tipo y motivo, por fecha y hora de inicio (P2) |
| `RepositorioReservas`: `deCanchasEntre(Collection<UUID> canchaIds, LocalDate desde, LocalDate hasta)` (nuevo) | `RepositorioReservasJpa` | Todas las reservas, en cualquier estado, por fecha y hora de inicio. Vacía si no se pide ninguna cancha |

### REST

- `AgendaEscenariosController` (nuevo, `escenarios.infrastructure.rest`): `GET /api/agenda/escenarios` → `RespuestaEscenarioAgenda`.
- `AgendaController` (nuevo, `reservas.infrastructure.rest`): `GET /api/escenarios/{escenarioId}/agenda` → `RespuestaAgenda`. Igual que `DisponibilidadController` (HU20), vive en Reservas aunque la ruta empiece por la cancha o el escenario.
- No cambian `ConfiguracionSeguridad` (las dos rutas exigen sesión por defecto) ni `ManejadorErrores` (`ERR-GEN-06` ya es 403).

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1 | Las franjas salen de los horarios del día, incluidas las que ya empezaron | `HorarioRecurrenteFranjasDeLaFechaTest` | Dominio |
| CA1 | Una franja con reserva CONFIRMADA o NO_ASISTIO es RESERVADA; con bloqueo, BLOQUEADA; con una reserva CANCELADA, LIBRE | `EstadoFranjaTest`, `ReservaAgendaTest.CA1_ocupa_*` | Dominio |
| CA1 | La vista por día trae todas las canchas del escenario con sus franjas LIBRE, RESERVADA y BLOQUEADA | `ConsultarAgendaCasoUsoTest.ca1_*`, `AgendaPuntaAPuntaTest.ca1_vistaDiaConTodasLasCanchas` | Caso de uso, punta a punta |
| CA2 | SEMANA va de lunes a domingo de la semana de la fecha | `VistaAgendaTest` | Dominio |
| CA2 | La vista por semana trae los siete días de cada cancha con sus reservas y bloqueos | `AgendaPuntaAPuntaTest.ca2_vistaSemanaConSieteDias` | Punta a punta |
| CA3 | Cancelar por fuerza mayor: solo el propietario, solo CONFIRMADA y antes del inicio | `ReservaAgendaTest.CA3_fuerzaMayor_*` | Dominio |
| CA3 | Marcar inasistencia: solo CONFIRMADA, después del fin y hasta 24 h después | `ReservaAgendaTest.CA3_inasistencia_*` | Dominio |
| CA3 | La reserva trae nombre y celular del jugador, la confirmación de asistencia y los indicadores; al encargado nunca le sale la fuerza mayor | `ConsultarAgendaCasoUsoTest.ca3_*`, `AgendaPuntaAPuntaTest.ca3_datosDelJugadorYOpciones` | Caso de uso, punta a punta |
| CA4 | El propietario con varios escenarios los ve todos para elegir | `ListarEscenariosAgendaCasoUsoTest.ca4_*`, `AgendaPuntaAPuntaTest.ca4_propietarioVeSusEscenarios` | Caso de uso, punta a punta |
| CA5 | El encargado solo ve los escenarios que tiene asignados | `ListarEscenariosAgendaCasoUsoTest.ca5_*`, `AgendaPuntaAPuntaTest.ca5_encargadoSoloVeLosAsignados` | Caso de uso, punta a punta |
| CA6 | Ni propietario ni encargado asignado → ERR-GEN-06 | `EscenarioAccesoAgendaTest` | Dominio |
| CA6 | Otro administrador, un jugador o el escenario que no existe → 403 ERR-GEN-06 y no sale ningún dato; sin sesión → 401 | `ConsultaEscenariosCasoUsoTest.ca6_*`, `AgendaPuntaAPuntaTest.ca6_*` | Caso de uso, punta a punta |
| — | Las consultas nuevas de persistencia filtran por canchas, fechas y usuario | `RepositorioEncargadosJdbcTest`, `RepositorioBloqueosJdbcTest`, `RepositorioReservasJpaTest` | Persistencia |
| — | Reservas solo usa los puertos de Escenarios e Identidad | `ArquitecturaTest` (ya existe) | Arquitectura |

Las pruebas punta a punta insertan bloqueos y encargados con SQL directo, porque todavía no hay servicios para crearlos (HU30 y HU34).

## Preguntas abiertas

No quedan preguntas abiertas. El desarrollador las resolvió el 25-nov-2026:

| # | Pregunta | Decisión |
|---|---|---|
| P1 | Encargados sin HU34 (Release 3) | **A.** Se lee la tabla `encargado_escenario` con SQL directo en `RepositorioEncargadosJdbc`. La clase `EncargadoEscenario` llega con HU34 |
| P2 | Bloqueos sin HU30 (Release 2) | **A.** Se muestran con tipo y motivo ampliando `RepositorioBloqueosJdbc`. Hoy vendrán vacíos en producción |
| P3 | Opciones de CA3 sin HU32 ni HU33 (Release 2) | **A.** Se devuelven ya `puedeCancelarPorFuerzaMayor` y `puedeMarcarInasistencia`, calculados en `Reserva` (políticas 15/17 con D02, y 22/32 con PAR-RES-09). La pantalla no muestra los botones hasta que existan las acciones |
| P4 | Reservas CANCELADAS | **A.** Se muestran en la lista del día con origen, justificación y fecha (ESC-AUD-01), pero no ocupan la franja |
| P5 | «La semana seleccionada» | **A.** De lunes a domingo de la semana de la fecha |
| P6 | Fechas que se pueden consultar | **A.** Cualquier fecha, pasada o futura. Las franjas salen siempre de los horarios actuales: en una fecha pasada pueden no coincidir con los de ese día, pero las reservas y los bloqueos se ven igual. Si más adelante se quiere un límite, se crea un `PAR-*` en el Excel; no se inventa el valor |
| P7 | Escenario inexistente o usuario sin rol ADMINISTRADOR_CANCHA | **A.** Todo responde `ERR-GEN-06` (403), también en el servicio 1 para jugadores y el administrador del sistema |
| P8 | Escenarios INACTIVOS y lista vacía | **A.** Incluye ACTIVOS e INACTIVOS con su estado. Sin escenarios, lista vacía; se reporta al Excel de mensajes que falta un INF para «no tiene escenarios» |

## Fuera de alcance

- Las acciones de cancelar por fuerza mayor (HU32), marcar inasistencia (HU33), crear y eliminar bloqueos (HU30, HU31) y asignar encargados (HU34): aquí solo se leen.
- La pantalla de la agenda (frontend).
- Ver la agenda de varios escenarios a la vez: se consulta uno por vez (CA4).

## Pendientes en los Excel de origen

- `clases.md`: agregar `HorarioRecurrente.franjasDeLaFecha`, `Escenario.validarAccesoAgenda`, `Reserva.ocupa`, `Reserva.sePuedeCancelarPorFuerzaMayor`, `Reserva.sePuedeMarcarInasistencia`, `EstadoFranja` y `VistaAgenda`.
- Catálogo de mensajes: falta un INF para «no tiene escenarios» en la lista de escenarios con agenda (P8, AG-09).
