# HU23 · Consultar mis reservas

**Estado:** Aprobada (24-nov-2026) · **HU:** [HU23](../../docs/historias/HU23.md) · **Módulo:** Reservas (`co.edu.uco.canchaya.reservas`), con `ConsultaCanchas` de Escenarios y canchas

**Depende de [HU21](../HU21-reservar-cancha/spec.md)** (implementada): reutiliza `Reserva`, `reconstruir`, `inicio`, `fin`, `ParametrosReserva`, `RepositorioReservas` y su adaptador, y `ConsultaCanchas`. No cambia la base de datos.

## Fuentes

- Dominio: [Reserva](../../docs/dominio/reserva.md), atributos `estado`, `confirmacionAsistencia`, `origenCancelacion` y `motivoCancelacion` (requisito de información de la HU). Para CA3, la política 16 de `cancelar` (RN-10).
- Mensajes: `INF-RES-01` y `ERR-GEN-06` ([mensajes.md](../../docs/especificacion/mensajes.md)). `ERR-GEN-06` («No tiene permiso para realizar esta acción») es el mensaje general de «sin permiso cuando no hay un código propio del objeto»; ya está en `catalogo-mensajes.properties` y `ManejadorErrores` lo responde con 403 (lo usa HU13 en `ListarEscenariosAdministracionCasoUso`).
- Parámetros: PAR-RES-05 (más de 3 horas para cancelar, **Definido**, límite estricto). Ya está en `ParametrosReserva.horasMinimasCancelacion` y en `canchaya.reserva.horas-minimas-cancelacion`.
- Aspectos generales: AG-09 (lista vacía con mensaje INF), AG-11 (exige sesión), AG-12 (permisos en el servidor), AG-14 (datos no visibles), AG-17 a AG-19 (zona horaria, formatos de fecha, hora y precio).
- Decisiones: D06 (más de 3 horas para cancelar), D27 (America/Bogota), D39 (paquetes).
- Diseño: en [entidad-relacion.md](../../docs/diseno/entidad-relacion.md), el índice `ix_reserva_jugador_fecha` («sirve para "mis reservas"»); en [componentes.md](../../docs/diseno/componentes.md), Reservas depende de Escenarios (`ConsultaCanchas`).

## Qué se construye

Un servicio, para el jugador con sesión iniciada, que devuelve **solo sus** reservas en dos listas: las vigentes (de la más próxima a la más lejana) y las pasadas (de la más reciente a la más antigua). Cada reserva trae escenario, cancha, fecha, horas, precio, estado y confirmación de asistencia; si está CANCELADA, quién la canceló y, si fue el administrador, la justificación; y un indicador `cancelable` que dice si hoy se puede cancelar (CONFIRMADA y faltan más de PAR-RES-05 horas).

Quedan para la pantalla: mostrar el botón de cancelar según `cancelable` (la cancelación es HU24), el formato `$ 80.000` del precio y dd/mm/aaaa de la fecha (AG-18, AG-19), las etiquetas de los estados y el enlace a buscar canchas de `INF-RES-01`.

## Contrato del servicio

`GET /api/reservas/mias` · **con sesión**, rol JUGADOR (la ruta no se agrega a las públicas; P2). No recibe parámetros: el jugador sale del token (CA5).

Respuesta `200`:

```json
{
  "vigentes": [
    {
      "id": "uuid",
      "escenario": "Canchas La 70",
      "cancha": "Cancha 1",
      "fecha": "2026-11-28",
      "horaInicio": "18:00",
      "horaFin": "20:00",
      "precio": 160000.00,
      "estado": "CONFIRMADA",
      "confirmacionAsistencia": "NO_REQUERIDA",
      "origenCancelacion": null,
      "motivoCancelacion": null,
      "cancelable": true
    }
  ],
  "pasadas": [
    {
      "id": "uuid",
      "escenario": "Canchas La 70",
      "cancha": "Cancha 2",
      "fecha": "2026-11-20",
      "horaInicio": "19:00",
      "horaFin": "20:00",
      "precio": 80000.00,
      "estado": "CANCELADA",
      "confirmacionAsistencia": "NO_REQUERIDA",
      "origenCancelacion": "ADMINISTRADOR",
      "motivoCancelacion": "Tormenta eléctrica",
      "cancelable": false
    }
  ],
  "codigo": null,
  "mensaje": null
}
```

- Sin ninguna reserva (CA2, AG-09): las dos listas vacías y `"codigo": "INF-RES-01"` con su texto en `mensaje`. Es el mismo patrón de `INF-HOR-01` (HU20) e `INF-ESC-01` (HU18).
- `motivoCancelacion` solo viene si `origenCancelacion` es `ADMINISTRADOR` (CA4 y regla del atributo); en los demás casos es `null`.
- `id` va para que HU24 pueda cancelar; la pantalla no lo muestra (AG-14). No van `canchaId`, `jugadorId`, `fechaCreacion`, `fechaCancelacion` ni `fechaSolicitudConfirmacion` (AG-14: la HU no los pide).
- `fecha` en ISO y horas `HH:mm`, como en `RespuestaReserva` de HU21.

| HTTP | Código | Cuándo |
|---|---|---|
| 200 | — | Tiene al menos una reserva |
| 200 | `INF-RES-01` | No tiene reservas (CA2) |
| 401 | `ERR-GEN-05` | Sin sesión o con la sesión vencida (ya lo hace la seguridad, AG-11) |
| 403 | `ERR-GEN-06` | El usuario no tiene rol JUGADOR (P2; ya mapeado en `ManejadorErrores`) |

## Diseño por capas

### Dominio · `Reserva` (cambia)

| Método | Qué hace | Política |
|---|---|---|
| `esVigente(LocalDateTime ahora)` | CONFIRMADA y `fin() > ahora` (P1). Es la misma idea de «activa» de HU21 (P4 de HU21). Una reserva en curso es vigente; una CANCELADA de mañana es pasada | P1 |
| `sePuedeCancelarPorJugador(LocalDateTime ahora, ParametrosReserva parametros)` | CONFIRMADA y faltan **más de** `horasMinimasCancelacion` horas para `inicio()`: con exactamente 3 horas, falso | Política 16 (RN-10, D06, PAR-RES-05); CA3 |

`sePuedeCancelarPorJugador` lo reutilizará `cancelar` en HU24 para lanzar `ERR-RES-16`; así la pantalla y el servidor usan la misma regla (AG-12). Ninguno de los dos métodos está en `clases.md`: va a «Pendientes en los Excel de origen».

### Puerto de entrada · `ConsultarMisReservas` (nuevo, `reservas.application.port.in`)

```java
MisReservas ejecutar(Solicitante solicitante);

record MisReservas(List<ReservaDelJugador> vigentes, List<ReservaDelJugador> pasadas) {
    boolean vacia();
}
record ReservaDelJugador(Reserva reserva, String cancha, String escenario, boolean cancelable) {}
```

### Caso de uso · `ConsultarMisReservasCasoUso` (nuevo)

0. Si el solicitante no tiene rol JUGADOR, lanza `ERR-GEN-06` (P2). Va en el caso de uso, como en `ListarEscenariosAdministracionCasoUso` de HU13: es un permiso de la consulta, no una política de `Reserva`.
1. `repositorioReservas.deJugador(solicitante.usuarioId())`: el id sale del token, nunca de la petición (CA5).
2. Los nombres de cancha y escenario, en una sola llamada: `consultaCanchas.nombres(idsDeCanchas)`.
3. `ahora = LocalDateTime.now(reloj)` (D27). Separa con `esVigente(ahora)`; ordena las vigentes por `inicio()` ascendente y las pasadas por `inicio()` descendente (restricción de diseño de la HU).
4. `cancelable = reserva.sePuedeCancelarPorJugador(ahora, parametros)` (CA3).

Solo lee: no toma el turno de la cancha ni usa transacción de escritura (D29 y D36 no aplican). No envía correos (D15 no aplica).

### Puerto de Escenarios · `ConsultaCanchas.nombres` (nuevo método)

```java
/** HU23: nombre de cada cancha y de su escenario, en cualquier estado. Las que no existen no vienen. */
Map<UUID, NombresCancha> nombres(Collection<UUID> canchaIds);

record NombresCancha(String cancha, String escenario) {}
```

No sirve `buscarReservable`: solo devuelve canchas DISPONIBLES de escenarios ACTIVOS, y una reserva pasada puede ser de una cancha o un escenario que hoy está inactivo. Se implementa en `ConsultaCanchasCasoUso` con un método nuevo de `RepositorioCanchas` (`nombresConEscenario`) que hace un solo `select ... join escenario ... where cancha.id in (...)`. Vacío si no se pide ninguna cancha. La dependencia Reservas → Escenarios ya está permitida.

### Puerto de salida · `RepositorioReservas.deJugador` (nuevo método)

`List<Reserva> deJugador(UUID jugadorId)`: todas las reservas del jugador, en cualquier estado. En `ReservaJpa`: `findByJugadorId`. Usa `ix_reserva_jugador_fecha`. El orden lo pone el caso de uso, porque depende de `ahora`.

### REST

- Un método nuevo en `ReservaController` (ya es el de `/api/reservas`): `GET /api/reservas/mias` con `Solicitante` del token. Si `vacia()`, responde con `INF-RES-01` del catálogo.
- `RespuestaMisReservas` y `RespuestaReservaDelJugador` (records), con `motivoCancelacion` solo si el origen es `ADMINISTRADOR`.
- `catalogo-mensajes.properties`: agregar `INF-RES-01` (hoy no está).

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1 | Vigentes por `inicio` ascendente y pasadas descendente, con nombres de cancha y escenario | `ConsultarMisReservasCasoUsoTest.CA1_separaVigentesYPasadas_yLasOrdena` | Caso de uso |
| CA1 | Reserva en curso (empezó y no terminó) es vigente; terminada o no CONFIRMADA es pasada (P1) | `ReservaConsultaTest.CA1_esVigente_*` | Dominio |
| CA1 | La respuesta trae escenario, cancha, fecha, horas, precio, estado y confirmación de asistencia | `ReservaControllerTest.CA1_devuelveLosDatosDeCadaReserva` | REST |
| CA1 | Se reserva con `POST /api/reservas` y aparece en `vigentes` de `GET /api/reservas/mias` | `MisReservasPuntaAPuntaTest.CA1_reservaHecha_apareceEnVigentes` | Punta a punta |
| CA1 | El nombre llega aunque la cancha esté INACTIVA | `ConsultaCanchasPersistenciaTest.CA1_canchaInactiva_tambienTraeNombres` | Persistencia |
| CA2 | Sin reservas: listas vacías y `INF-RES-01` con su texto | `ReservaControllerTest.CA2_sinReservas_INF_RES_01` | REST |
| CA3 | CONFIRMADA y faltan 3 h 1 min → `cancelable` verdadero | `ReservaConsultaTest.CA3_faltanMasDeTresHoras_sePuedeCancelar` | Dominio |
| CA3 | Faltan exactamente 3 h, o menos → falso (límite estricto de PAR-RES-05) | `ReservaConsultaTest.CA3_faltanTresHorasOMenos_noSePuedeCancelar` | Dominio |
| CA3 | CANCELADA o NO_ASISTIO → falso | `ReservaConsultaTest.CA3_noConfirmada_noSePuedeCancelar` | Dominio |
| CA3 | El caso de uso calcula `cancelable` con el reloj y PAR-RES-05 | `ConsultarMisReservasCasoUsoTest.CA3_marcaCancelableConElReloj` | Caso de uso |
| CA4 | CANCELADA por ADMINISTRADOR trae origen y justificación; por JUGADOR o SISTEMA_SIN_CONFIRMACION, origen y `motivoCancelacion` nulo | `ReservaControllerTest.CA4_cancelada_muestraOrigenYJustificacion` | REST |
| CA4 | Con una reserva cancelada guardada en la base (por SQL, porque HU24 aún no existe), llega su origen y su motivo | `MisReservasPuntaAPuntaTest.CA4_reservaCancelada_traeOrigenYMotivo` | Punta a punta |
| CA5 | El repositorio solo devuelve las del jugador pedido | `RepositorioReservasJpaTest.CA5_deJugador_soloLasSuyas` | Persistencia |
| CA5 | El caso de uso consulta con el id del token | `ConsultarMisReservasCasoUsoTest.CA5_consultaConElIdDelSolicitante` | Caso de uso |
| CA5 | Dos jugadores reservan; cada uno ve solo la suya. Sin token → 401 | `MisReservasPuntaAPuntaTest.CA5_cadaJugadorVeSoloLasSuyas` y `sinToken_401` | Punta a punta |
| P2 | Un solicitante sin rol JUGADOR recibe `ERR-GEN-06` y no se consulta el repositorio | `ConsultarMisReservasCasoUsoTest.P2_noJugador_ERR_GEN_06` | Caso de uso |
| P2 | Un administrador con token recibe 403 con `ERR-GEN-06` | `MisReservasPuntaAPuntaTest.P2_administrador_403_ERR_GEN_06` | Punta a punta |

## Decisiones tomadas (24-nov-2026)

Ya no quedan preguntas abiertas. El desarrollador eligió:

- **P1 · Qué es una reserva vigente → A.** Vigente = CONFIRMADA y todavía no termina (`fecha + horaFin > ahora`). Las terminadas, las CANCELADAS (aunque su fecha sea futura) y las NO_ASISTIO van a pasadas. Coincide con «reserva activa» de HU21 (P4) y con el límite de 5 reservas activas.
- **P2 · Solo el rol JUGADOR → B.** Un usuario sin rol JUGADOR recibe 403 con `ERR-GEN-06` («No tiene permiso para realizar esta acción»), el mensaje general cuando el objeto no tiene un código propio. Ya está en el catálogo y en `ManejadorErrores`, y HU13 lo usa igual. El borrador decía por error que no había mensaje para este caso.
- **P3 · Ruta → A.** `GET /api/reservas/mias`.
- **P4 · Forma de la respuesta → A.** Dos listas, `vigentes` y `pasadas`, ya ordenadas por el servidor con la hora de Bogotá (D27, AG-17).
- **P5 · Etiquetas de estado, confirmación y origen → A.** El backend devuelve los valores del enum y la pantalla pone la etiqueta. Se pide en el Excel agregar esas etiquetas al catálogo de mensajes. La opción B iría contra la regla 6 de `AGENTS.md`.

## Fuera de alcance

- Cancelar la reserva (HU24): aquí solo se dice si se puede, con `cancelable`.
- Paginar o limitar las pasadas: la HU no lo pide. Con 5 reservas activas como máximo, la lista crece despacio; si se vuelve larga, se pide en otra HU.
- Filtros por fecha o estado.
- Mostrar el celular, la dirección o la foto del escenario (la HU no los nombra).
- La agenda del administrador (HU29).

## Pendientes en los Excel de origen

- `clases.md` (Reserva): agregar `esVigente(ahora)` y `sePuedeCancelarPorJugador(ahora, parametros)`.
- `componentes.md` / `clases.md`: el método `nombres` de `ConsultaCanchas` y el record `NombresCancha`.
- Catálogo de mensajes: etiquetas para `EstadoReserva`, `ConfirmacionAsistencia` y `OrigenCancelacion` (P5).
- Catálogo de mensajes: agregar HU23 a la columna «HU» de `ERR-GEN-06` (hoy solo dice HU29; HU13 también lo usa en el código) (P2).
