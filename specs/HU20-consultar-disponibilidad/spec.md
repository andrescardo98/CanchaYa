# HU20 · Consultar la disponibilidad de una cancha

**Estado:** Implementada (18-nov-2026) · **HU:** [HU20](../../docs/historias/HU20.md) · **Módulos:** Reservas (`co.edu.uco.canchaya.reservas`), que arma la disponibilidad, y Escenarios y canchas (`co.edu.uco.canchaya.escenarios`), que entrega las franjas, los bloqueos y el turno de la cancha

Esta especificación define **una sola vez** las piezas que comparte con [HU21](../HU21-reservar-cancha/spec.md): el record `Franja`, `HorarioRecurrente.calcularFranjas`, `Reserva.verificarDisponibilidad`, el record `ParametrosReserva`, el puerto `ConsultaCanchas` (con `bloqueosDelDia` y `tomarTurno`, D36) y la lectura de las reservas confirmadas de una cancha. HU21 las reutiliza y no las vuelve a definir.

## Fuentes

- Dominio: [HorarioRecurrente](../../docs/dominio/horariorecurrente.md), `calcularFranjas` (políticas 9 a 12), y [Reserva](../../docs/dominio/reserva.md), `verificarDisponibilidad` (políticas 1 a 4). También [Bloqueo](../../docs/dominio/bloqueo.md) (atributos: un bloqueo es de un día y en horas en punto, D18 y D23).
- Mensajes: `INF-HOR-01`, `ERR-HOR-10` y `ERR-RES-01` ([mensajes.md](../../docs/especificacion/mensajes.md)). `ERR-HOR-09` queda como respaldo: su nota dice que HU20 usa `ERR-RES-01`.
- Parámetros ([parametros.md](../../docs/especificacion/parametros.md)), todos **Definidos**: PAR-GEN-01 (America/Bogota), PAR-HOR-01 (franja de 1 hora, regla fija), PAR-RES-01 (30 días) y PAR-RES-02 (2 franjas).
- Aspectos generales: AG-09 (consulta vacía con mensaje INF), AG-11 (consultar disponibilidad no exige sesión), AG-17 (hora del servidor, no la del dispositivo) y AG-18 (formatos).
- Decisiones: D10 (franjas de 1 hora, reservas de 1 o 2 franjas), D18 y D23 (bloqueos), D27 (zona horaria), D29 y D36 (turno de la cancha), D39 (paquetes).
- Diseño: en [clases.md](../../docs/diseno/clases.md), los bloques completos de `HorarioRecurrente.calcularFranjas`, `Franja`, `ParametrosReserva`, `Reserva` (atributos, `verificarDisponibilidad`, `reconstruir`, `inicio`, `fin`, `horas`, `estaConfirmada`) y `Bloqueo.seCruzaCon`; en [componentes.md](../../docs/diseno/componentes.md), la nota sobre `ConsultaCanchas` (`tomarTurno` y `bloqueosDelDia`); en el modelo entidad-relación, las tablas `reserva` y `bloqueo` de `V1__estructura_inicial.sql`.

## Qué se construye

Un servicio público, sin sesión, que recibe una cancha y una fecha y devuelve las franjas de una hora del horario del escenario para ese día, cada una marcada como libre u ocupada. Una franja está ocupada si tiene una reserva CONFIRMADA o un bloqueo, sin decir cuál de las dos ni quién reservó. En la fecha de hoy no aparecen las franjas que ya empezaron. La pantalla (selector de fecha, selección de 1 o 2 franjas seguidas en CA7) queda para el frontend; el backend le entrega la fecha de hoy y la fecha máxima según el servidor, y el máximo de franjas por reserva, para que no los escriba fijos.

La disponibilidad la arma **Reservas**, porque es el único módulo que puede ver al mismo tiempo las reservas (suyas) y las franjas y bloqueos (de Escenarios, por `ConsultaCanchas`). Escenarios no puede usar Reservas (tabla de módulos de `AGENTS.md`).

## Contrato del servicio

`GET /api/canchas/{canchaId}/disponibilidad?fecha=2026-11-20` · **público** (se agrega a `ConfiguracionSeguridad` con `permitAll`, solo GET, AG-11)

- `fecha` es opcional, en formato ISO `aaaa-mm-dd`. Si no llega, se usa la fecha de hoy en America/Bogota (restricción de diseño de HU20 «por defecto se muestra la fecha de hoy» y AG-17: la fecha la pone el servidor, no el dispositivo).

Respuesta `200`:

```json
{
  "canchaId": "uuid",
  "fecha": "2026-11-20",
  "fechaMinima": "2026-11-18",
  "fechaMaxima": "2026-12-18",
  "franjasMaximasPorReserva": 2,
  "franjas": [
    { "horaInicio": "06:00", "horaFin": "07:00", "libre": true },
    { "horaInicio": "07:00", "horaFin": "08:00", "libre": false }
  ],
  "codigo": null,
  "mensaje": null
}
```

- `franjas` va ordenada por `horaInicio`. Nunca trae el jugador, el tipo de bloqueo ni el motivo (restricción de diseño de HU20).
- Sin franjas para ese día (el escenario no atiende ese día de la semana, o ya pasaron todas las de hoy): `franjas` es `[]`, `codigo` es `"INF-HOR-01"` y `mensaje` trae su texto (CA4, AG-09).
- `fechaMinima` y `fechaMaxima` son hoy y hoy + PAR-RES-01, para que el selector de fecha no deje elegir otras (restricción de diseño de HU20). `franjasMaximasPorReserva` es PAR-RES-02, para CA7.

| HTTP | Código | Cuándo |
|---|---|---|
| 200 | — o `INF-HOR-01` | La cancha existe y está DISPONIBLE, y la fecha está dentro de la ventana |
| 404 | `ERR-RES-01` | La cancha no existe, no está DISPONIBLE o su escenario no está ACTIVO (se agrega a `ManejadorErrores`; P2 y P4) |
| 422 | `ERR-HOR-10` | La fecha es anterior a hoy o posterior a hoy + 30 días (por defecto en `ManejadorErrores`) |
| 400 | `ERR-GEN-07` | `canchaId` no es un UUID o `fecha` no se puede leer (ya lo maneja `ManejadorErrores`) |

Orden de las validaciones: primero la cancha (`ERR-RES-01`) y después la fecha (`ERR-HOR-10`), porque `calcularFranjas` necesita el escenario de la cancha (así lo dice su bloque en `clases.md`: «antes de llamarlo, el caso de uso revisa ERR-HOR-09»).

## Piezas compartidas con HU21

### Dominio de Escenarios (`escenarios.domain`)

**`Franja`** (record, nuevo). Firma exacta de `clases.md`:

```java
/** Bloque de una hora en punto de una fecha (PAR-HOR-01). No se guarda (RN-02). */
public record Franja(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
	public Franja { /* IllegalArgumentException si no empieza en punto o no dura exactamente 1 hora */ }
	public LocalDateTime inicio();
	/** Rango de la misma fecha; fin abierto, igual que el tsrange '[)' de la base. */
	public boolean seCruzaCon(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin);
}
```

La duración de 1 hora va como constante con el comentario `PAR-HOR-01`: el catálogo la marca como «No (regla fija)» y dice que cambiarla exige rediseñar el cálculo, igual que «en punto» (PAR-HOR-02) ya está fijo en `HorarioRecurrente`.

**`HorarioRecurrente.calcularFranjas`** (método estático nuevo). Firma exacta de `clases.md`, con sus cuatro parámetros:

```java
/**
 * Políticas 10, 11 y 12. La política 9 (que exista la cancha) la revisa el caso de uso.
 * @throws ReglaNegocioException ERR-HOR-10 si fecha < hoy o fecha > hoy + ventanaReservaDias
 */
public static List<Franja> calcularFranjas(List<HorarioRecurrente> horariosEscenario, LocalDate fecha,
		LocalDateTime ahora, int ventanaReservaDias);
```

- Toma solo los horarios cuyo `diaSemana` corresponde a `fecha.getDayOfWeek()`. Se agrega `DiaSemana.de(DayOfWeek)` para no depender del orden del enum.
- Parte cada horario en franjas de una hora: de 06:00 a 09:00 salen 06–07, 07–08 y 08–09. Las ordena por hora de inicio.
- Política 12: quita las franjas cuyo inicio no es posterior a `ahora` (una franja que empieza a las 14:00 ya no se ofrece a las 14:00).
- Política 10: ventana **inclusiva** `hoy ≤ fecha ≤ hoy + ventanaReservaDias`: hoy + 30 sí se puede consultar (P6). `hoy` es `ahora.toLocalDate()`.

**`DiaSemana.de(DayOfWeek)`** (método nuevo en el enum existente). Pequeño; no está en `clases.md` (ver «Pendientes en los Excel»).

### Dominio de Reservas (`reservas.domain`)

Hoy el paquete no existe. Se crean:

- **`EstadoReserva`**, **`ConfirmacionAsistencia`** y **`OrigenCancelacion`**: los enum de `clases.md`, con los mismos valores que los `CHECK` de la tabla `reserva`.
- **`ParametrosReserva`**: el record de `clases.md` con sus diez componentes (PAR-RES-01 a PAR-RES-10) **más** `confirmacionAsistenciaActiva` (PAR-RES-12), el undécimo, que se decidió en P2 de [HU21](../HU21-reservar-cancha/spec.md). Se crea completo desde ahora para no cambiar el record en HU21. Todos los valores están Definidos y se llenan desde `application.properties` (T6). HU20 solo usa `ventanaReservaDias` y `franjasMaximasPorReserva`.
- **`Reserva`**, con lo que HU20 necesita y HU21 reutiliza:
  - Los atributos, el constructor privado, los getters y `reconstruir` con los parámetros de `clases.md`. **Diferencia encontrada:** `reconstruir` en `clases.md` no recibe `fechaSolicitudConfirmacion`, aunque el constructor y la tabla sí la tienen. Se agrega como último parámetro (ver «Pendientes en los Excel»).
  - `inicio()`, `fin()`, `horas()` y `estaConfirmada()`, como en `clases.md`.
  - **`verificarDisponibilidad`**, con la firma exacta de `clases.md`:

```java
/**
 * Políticas 1 a 4. Verdadero si todas las franjas de [horaInicio, horaFin) están en franjasLibres
 * y ninguna empezó.
 * @throws ReglaNegocioException ERR-RES-01 si !canchaDisponible; ERR-RES-02 si la fecha está fuera de la ventana
 */
public static boolean verificarDisponibilidad(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
		boolean canchaDisponible, List<Franja> franjasLibres, LocalDateTime ahora, ParametrosReserva parametros);
```

  Devuelve falso, sin lanzar error, si el rango no cubre franjas enteras de `franjasLibres`. Esto incluye un rango que no empieza en punto, que no dura 1 o 2 horas (PAR-RES-02) o que empezó antes de `ahora`. Decir *por qué* no se puede reservar (`ERR-RES-31`, `ERR-RES-06`, `ERR-RES-07`) es trabajo de `confirmar` en HU21.

`Reserva` (de Reservas) recibe `Franja` (de Escenarios), como lo dibuja `clases.md`. Por la decisión de P1, `Franja` se queda en `escenarios.domain`, igual que `Solicitante` en `identidad.domain`, y `AGENTS.md` anota que los records de valor compartidos son la excepción a la regla de puertos.

### Puerto de entrada de Escenarios: `ConsultaCanchas`

Nuevo, en `escenarios.application.port.in`. Lo implementa un caso de uso nuevo, `ConsultaCanchasCasoUso`, en `escenarios.application.usecase`. No se toca ningún caso de uso de HU8 a HU12.

```java
/** Lo que Escenarios ofrece a Reservas sobre una cancha (componentes.md, D36). */
public interface ConsultaCanchas {

	/** Vacío si la cancha no existe, no está DISPONIBLE o su escenario no está ACTIVO (política 1 de Reserva, P2). */
	Optional<CanchaReservable> buscarReservable(UUID canchaId);

	/** HorarioRecurrente.calcularFranjas con los horarios del escenario de la cancha. @throws ERR-HOR-09 (no existe, política 9; respaldo de ERR-RES-01, que ya revisó quien llama) o ERR-HOR-10 */
	List<Franja> franjasDelDia(UUID canchaId, LocalDate fecha);

	/** D36. Solo las horas: el jugador no ve el tipo ni el motivo del bloqueo. */
	List<RangoBloqueado> bloqueosDelDia(UUID canchaId, LocalDate fecha);

	/**
	 * D29 punto 2 y D36: bloquea la fila de la cancha (select ... for update) hasta que termina la
	 * transacción de quien llama. Exige una transacción abierta (Propagation.MANDATORY).
	 */
	void tomarTurno(UUID canchaId);

	/** nombre y nombreEscenario los usa EXI-RES-01 (HU21); precioHora, la política 11 de confirmar. */
	record CanchaReservable(UUID id, UUID escenarioId, String nombre, String nombreEscenario, BigDecimal precioHora) {
	}

	record RangoBloqueado(LocalTime horaInicio, LocalTime horaFin) {
	}
}
```

- `franjasDelDia` lee PAR-RES-01 de la configuración, igual que Reservas (una sola propiedad, `canchaya.reserva.ventana-dias`), y la hora del `Clock` del servidor (D27).
- HU20 usa `buscarReservable`, `franjasDelDia` y `bloqueosDelDia`. `tomarTurno` se construye y se prueba aquí porque es parte del puerto (lo pediste así), pero solo lo llama HU21.

Puertos de salida y persistencia de Escenarios que hacen falta:

| Pieza | Cambio | Por qué |
|---|---|---|
| `RepositorioCanchas.buscarPorId(UUID)` | Método nuevo | Hoy no existe. HU9 (en el otro worktree) seguramente lo agrega también: se reutiliza el de la rama que se una primero (P7) |
| `RepositorioCanchas.tomarTurno(UUID)` | Método nuevo | `select id from cancha where id = :id for update` |
| `RepositorioHorarios.buscarPorEscenarioYDia` | Ya existe | Se reutiliza para `franjasDelDia` |
| `RepositorioEscenarios.buscarPorId` | Ya existe | Para revisar que el escenario esté ACTIVO con `Escenario.estaActivo()` (P2) y para su nombre (HU21) |
| `RepositorioBloqueos.rangosDelDia(canchaId, fecha)` | Puerto de salida nuevo y adaptador JDBC `RepositorioBloqueosJdbc` que devuelve solo las horas | Todavía no hay clase `Bloqueo` ni entidad JPA (llegan con HU30). Mismo patrón que `RepositorioReservasJdbc` (P5) |
| `CanchasEnMemoria` (pruebas) | Implementa los métodos nuevos | Es el doble de prueba de `RepositorioCanchas` |

### Lectura de las reservas confirmadas (Reservas)

`RepositorioReservas` (ya existe) recibe un método nuevo, implementado en `RepositorioReservasJdbc` con el índice `ix_reserva_cancha_fecha`:

```java
/** Reservas CONFIRMADAS de la cancha en esa fecha, por hora de inicio. HU20 y HU21 (dentro del turno). */
List<Reserva> confirmadasDeCanchaEnFecha(UUID canchaId, LocalDate fecha);
```

**`ConsultaReservas` no cambia en esta HU ni en HU21.** Reservas lee sus propias reservas con su repositorio; `ConsultaReservas` es lo que Reservas ofrece a otros módulos (Reputación, Release 3) y hoy ningún otro módulo lo necesita para HU20 o HU21. Crearlo vacío sería agregar algo que la HU no pide.

## Diseño de HU20 (lo que solo usa esta HU)

| Capa | Pieza | Qué hace |
|---|---|---|
| Puerto de entrada | `reservas.application.port.in.ConsultarDisponibilidad`, con los records `Disponibilidad` y `FranjaDisponible(horaInicio, horaFin, libre)` | Contrato del caso de uso |
| Caso de uso | `reservas.application.usecase.ConsultarDisponibilidadCasoUso` | Ver los pasos abajo |
| Configuración | `reservas.infrastructure.configuracion.ConfiguracionReservas`, con el `@Bean ParametrosReserva` que lee PAR-RES-01 a PAR-RES-10 y PAR-RES-12 de `application.properties` (P8) | Que ningún valor quede escrito en el código (regla 7) |
| REST | `reservas.infrastructure.rest.DisponibilidadController` y `RespuestaDisponibilidad` | `GET /api/canchas/{canchaId}/disponibilidad` |
| Transversal | `INF-HOR-01`, `ERR-HOR-10` y `ERR-RES-01` en `catalogo-mensajes.properties`; `ERR-RES-01 → 404` en `ManejadorErrores`; la ruta pública en `ConfiguracionSeguridad` | Hoy no están |

Pasos de `ConsultarDisponibilidadCasoUso.ejecutar(canchaId, fecha)`, sin transacción de escritura:

1. `fecha` vacía → hoy en America/Bogota (`LocalDate.now(reloj)`).
2. `consultaCanchas.buscarReservable(canchaId)`; si está vacío (no existe, no está DISPONIBLE o su escenario no está ACTIVO) → `ERR-RES-01` (CA6, P2). Lo lanza el caso de uso porque sin la cancha no se pueden calcular las franjas. `verificarDisponibilidad` también lo revisa (política 1) con `canchaDisponible`, como respaldo.
3. `consultaCanchas.franjasDelDia(canchaId, fecha)` → lanza `ERR-HOR-10` si la fecha está fuera de la ventana (CA5) y ya quita las franjas que empezaron (CA3).
4. `consultaCanchas.bloqueosDelDia` y `repositorioReservas.confirmadasDeCanchaEnFecha`.
5. `franjasLibres` = las franjas que no se cruzan (`Franja.seCruzaCon`) con ninguna reserva ni bloqueo (política 11 de HorarioRecurrente y 3 de Reserva).
6. Cada franja sale con `libre = Reserva.verificarDisponibilidad(fecha, f.horaInicio(), f.horaFin(), true, franjasLibres, ahora, parametros)` (CA1, CA2).
7. Sin franjas, `franjas` sale vacía; el controlador agrega `INF-HOR-01` (CA4), igual que `INF-RSE-01` en HU19. Por eso `Disponibilidad` no lleva código ni mensaje.

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1 | Horario de 06:00 a 09:00 → franjas 06–07, 07–08 y 08–09, en orden | `CA1_horarioDeTresHoras_tresFranjasDeUnaHora` | Dominio (`HorarioRecurrenteCalcularFranjasTest`) |
| CA1 | Dos horarios el mismo día (06–09 y 14–16) → cinco franjas; los de otro día no cuentan | `CA1_variosHorarios_soloLosDelDiaDeLaFecha` | Dominio |
| CA1 | Respuesta con franjas libres y ocupadas, fecha mínima y máxima y franjas máximas | `CA1_canchaDisponible_devuelveFranjasLibresYOcupadas` | Caso de uso + REST |
| CA1 | Sin `fecha` se usa la de hoy en Bogotá | `CA1_sinFecha_usaLaFechaDeHoy` | Caso de uso |
| CA1 | `Franja` rechaza una franja que no es de una hora en punto | `franja_noEnPuntoONoDeUnaHora_lanzaIllegalArgument` | Dominio (`FranjaTest`) |
| CA2 | Una reserva CONFIRMADA de 2 horas ocupa sus dos franjas | `CA2_reservaDeDosHoras_ocupaDosFranjas` | Caso de uso |
| CA2 | Un bloqueo ocupa sus franjas; una reserva CANCELADA no ocupa | `CA2_bloqueoOcupa_reservaCanceladaNo` | Caso de uso + persistencia |
| CA2 | `verificarDisponibilidad`: rango de 1 o 2 franjas libres → verdadero; si una no está libre → falso; 3 franjas → falso | `CA2_verificarDisponibilidad_unaODosFranjasLibres` y `CA2_verificarDisponibilidad_rangoConFranjaOcupada_falso` | Dominio (`ReservaVerificarDisponibilidadTest`) |
| CA2 | La respuesta no expone jugador, tipo ni motivo del bloqueo | `CA2_respuesta_noExponeQuienNiPorQue` | REST |
| CA3 | Hoy a las 07:30: no salen 06–07 ni 07–08; sí 08–09. A las 08:00 exactas tampoco sale 08–09 | `CA3_hoy_noOfreceFranjasQueYaEmpezaron` | Dominio |
| CA3 | Mañana salen todas, aunque la hora de hoy sea mayor | `CA3_otraFecha_noFiltraPorHora` | Dominio |
| CA4 | Sin horario ese día de la semana → `[]` e `INF-HOR-01` | `CA4_sinHorarioEseDia_INF_HOR_01` | Caso de uso + REST |
| CA5 | Ayer y hoy + 31 → `ERR-HOR-10`; hoy y hoy + 30 → se calculan | `CA5_fueraDeLaVentana_ERR_HOR_10` y `CA5_bordesDeLaVentana_seCalculan` | Dominio |
| CA5 | 422 con `ERR-HOR-10` | `CA5_fechaFueraDeVentana_422_ERR_HOR_10` | REST |
| CA6 | Cancha inexistente, cancha INACTIVA y cancha DISPONIBLE de un escenario INACTIVO → 404 con `ERR-RES-01`, sin calcular franjas | `CA6_canchaInexistenteOInactiva_404_ERR_RES_01` y `CA6_escenarioInactivo_404_ERR_RES_01` | Caso de uso + REST |
| CA4 | Cancha DISPONIBLE de un escenario ACTIVO sin horarios → `[]` e `INF-HOR-01` (P3) | `CA4_escenarioSinHorarios_INF_HOR_01` | Caso de uso |
| CA6 | `verificarDisponibilidad` con `canchaDisponible = false` → `ERR-RES-01` | `CA6_verificarDisponibilidad_canchaNoDisponible_ERR_RES_01` | Dominio |
| CA7 | Se arma en la pantalla. El backend entrega `franjasMaximasPorReserva` (PAR-RES-02) y franjas ordenadas con `horaFin` de una igual a `horaInicio` de la siguiente | `CA7_respuesta_traeFranjasMaximasYFranjasSeguidas` | REST |
| CA8 | Sin token → 200 | `CA8_sinSesion_200` | Punta a punta (`DisponibilidadPuntaAPuntaTest`) |
| D36 | `tomarTurno` sin transacción falla; con transacción, una segunda transacción espera hasta que la primera termina | `tomarTurno_sinTransaccion_falla` y `tomarTurno_segundaTransaccionEspera` | Persistencia (Testcontainers) |

## Decisiones tomadas (18-nov-2026)

**P1 · `Franja` se queda en Escenarios.** Sigue en `escenarios.domain`, como dice `clases.md`, y la usan `Reserva` y `ConsultaCanchas.franjasDelDia`, igual que `identidad.domain.Solicitante`. `AGENTS.md` anota que los records de valor compartidos son la excepción a la regla de puertos (T10). ArchUnit ya lo permite: no hay que cambiar `ArquitecturaTest`.

**P2 · La cancha se puede reservar si el escenario está ACTIVO.** `buscarReservable` devuelve vacío si la cancha no existe, no está DISPONIBLE o su escenario no está ACTIVO, y en los tres casos sale `ERR-RES-01`. Vale para HU20 y HU21. Hay que ajustar la política 1 de Reserva en el Excel.

**P3 · Escenario sin horarios.** Confirmado: una cancha DISPONIBLE de un escenario ACTIVO sin horarios responde `[]` con `INF-HOR-01`. Aquí no se usa `publicarEnBusqueda`.

**P4 · `ERR-RES-01` responde 404**, como `ERR-ESC-06`.

**P5 · Los bloqueos se leen con JDBC, solo las horas.** `RepositorioBloqueosJdbc.rangosDelDia`. `Bloqueo`, `TipoBloqueo` y la entidad JPA llegan con HU30.

**P6 · La ventana incluye hoy + 30**: `hoy ≤ fecha ≤ hoy + 30`.

**P7 · `RepositorioCanchas.buscarPorId`.** Se reutiliza el de la rama que se una primero (esta o la de HU8 a HU12). Los demás archivos compartidos (`ManejadorErrores`, `catalogo-mensajes.properties`, `application.properties`, `ConfiguracionSeguridad`, `CanchasEnMemoria` y `specs/README.md`) se resuelven al unir. No se toca ningún caso de uso de HU8 a HU12.

**P8 · Paquete `reservas.infrastructure.configuracion`** con `ConfiguracionReservas`, que publica el `@Bean ParametrosReserva`. Se agrega a la estructura de paquetes de `AGENTS.md` en el mismo cambio (T6), y conviene registrarlo en D39 del Excel de decisiones.

## Preguntas abiertas

Ninguna.

## Pendientes en los Excel de origen

| Excel | Cambio | Estado |
|---|---|---|
| Diagrama de clases, hoja Reserva | `reconstruir` no recibe `fechaSolicitudConfirmacion` (el constructor y la tabla sí la tienen) | Pendiente |
| Diagrama de clases, hoja DiaSemana | Agregar `de(DayOfWeek)`, que usa `calcularFranjas` | Pendiente |
| Diagrama de clases, hoja ParametrosReserva | Agregar `confirmacionAsistenciaActiva` (PAR-RES-12), por P2 de HU21 | Pendiente |
| Modelo de dominio enriquecido, hoja Reserva, política 1 | «Debe existir la cancha, estar en estado DISPONIBLE y su escenario estar ACTIVO» (P2) | Pendiente |
| Decisiones, D39 | Paquete `infrastructure.configuracion` en Reservas (P8) y la excepción de los records de valor compartidos (P1) | Pendiente |

## Fuera de alcance

- Reservar (HU21), crear y eliminar bloqueos (HU30 y HU31) y la agenda del administrador.
- La búsqueda de canchas (HU18), aunque también usa PAR-RES-01 y `ERR-HOR-10`.
- `ConsultaReservas`: no la necesita ninguna HU de esta especificación.
- La pantalla: selector de fecha, selección de 1 o 2 franjas (CA7) y el paso al inicio de sesión al reservar (CA8).
