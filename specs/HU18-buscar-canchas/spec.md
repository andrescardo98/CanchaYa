# HU18 · Buscar canchas cercanas

**Estado:** Aprobada (24-nov-2026) · **HU:** [HU18](../../docs/historias/HU18.md) · **Módulos:** Escenarios y canchas (`co.edu.uco.canchaya.escenarios`), que hace la búsqueda, y Reservas (`co.edu.uco.canchaya.reservas`), que solo dice qué canchas tienen una reserva confirmada en la franja pedida

## Fuentes

- Dominio: [Cancha](../../docs/dominio/cancha.md), `publicarEnBusqueda` (políticas 7 y 11, RN-04); [HorarioRecurrente](../../docs/dominio/horariorecurrente.md), `calcularFranjas` (políticas 10 a 12); [Escenario](../../docs/dominio/escenario.md) (latitud, longitud, ciudad y calificación promedio, D05); [Departamento](../../docs/dominio/departamento.md) y [Ciudad](../../docs/dominio/ciudad.md).
- Mensajes ([mensajes.md](../../docs/especificacion/mensajes.md)): `INF-ESC-01`, `INF-ESC-03`, `ERR-HOR-10` y `ERR-GEN-04`. `ERR-GEN-01` y `ERR-GEN-02` para datos incompletos o ilegibles (convención general).
- Parámetros ([parametros.md](../../docs/especificacion/parametros.md)), todos **Definidos**: PAR-ESC-04 (radio por defecto, 5 km), PAR-ESC-05 (radio máximo, 30 km), PAR-ESC-06 (radio mínimo, 1 km) y PAR-RES-01 (ventana de 30 días). También PAR-GEN-01 (America/Bogota) y PAR-HOR-01 (franja de 1 hora).
- Aspectos generales: AG-09 (búsqueda vacía con mensaje INF), AG-11 (buscar no exige sesión), AG-14 (no se muestran identificadores ni estados), AG-17 (hora del servidor), AG-19 (precios) y AG-20 (calificación con un decimal y ausente si no hay reseñas).
- Decisiones: D05 (promedio al consultar), D11 y D26 (radio de 1 a 30 km, 5 por defecto; sin ubicación se busca por ciudad), D08 (patrón del puerto que define Escenarios e implementa Reservas), D16 (solo Antioquia), D27 (zona horaria) y D39 (paquetes).
- Diseño: `Cancha.publicarEnBusqueda(Escenario, boolean escenarioTieneHorarios)` y `HorarioRecurrente.calcularFranjas` en [clases.md](../../docs/diseno/clases.md); en [componentes.md](../../docs/diseno/componentes.md), «Escenarios y canchas … fotos y búsqueda por radio»; en el [modelo entidad-relación](../../docs/diseno/entidad-relacion.md), la columna generada `escenario.ubicacion GEOGRAPHY(POINT, 4326)` con su índice espacial `ix_escenario_ubicacion` (ya está en `V1__estructura_inicial.sql`).
- Especificaciones que se reutilizan: [HU19](../HU19-perfil-escenario/spec.md) (`publicarEnBusqueda`, `ConsultaCalificaciones`) y [HU20](../HU20-consultar-disponibilidad/spec.md) (`calcularFranjas`, `Franja`, `RepositorioBloqueos`, ventana inclusiva hoy ≤ fecha ≤ hoy + 30).

## Qué se construye

Un servicio público, sin sesión, que devuelve las canchas publicables (RN-04) que cumplen los filtros que lleguen: cercanía a un punto (con radio), departamento, ciudad, tipo de cancha y, si se indican fecha y hora, que la franja de esa hora esté libre. Si llega la ubicación del jugador, cada resultado trae su distancia y la lista va de la más cercana a la más lejana. Pedir el permiso de ubicación al navegador, el mapa, el control del radio y la navegación al perfil (CA8) son de la pantalla; el backend le entrega el radio mínimo, máximo y por defecto, y las fechas mínima y máxima, para que no los escriba fijos.

La búsqueda la hace **Escenarios**, como dice `componentes.md`: allí están las canchas, los escenarios, los horarios y los bloqueos. Lo único que no tiene son las reservas. Para no depender de Reservas (tabla de módulos de `AGENTS.md`), Escenarios define un puerto de salida que Reservas implementa, igual que `ConsultaReservasFuturas` (D08). Ver P1.

## Contrato del servicio

`GET /api/canchas/busqueda` · **público** (se agrega a `ConfiguracionSeguridad` con `permitAll`, solo GET, AG-11)

Parámetros de consulta, todos opcionales:

| Parámetro | Formato | Regla |
|---|---|---|
| `latitud`, `longitud` | Decimal | Van juntos: si llega uno sin el otro, `ERR-GEN-01` del que falta. Latitud de -90 a 90 y longitud de -180 a 180 (`ERR-GEN-04`) |
| `radioKm` | Entero (P4) | De PAR-ESC-06 a PAR-ESC-05 (1 a 30), si no `ERR-GEN-04` (CA11). Sin radio se usa PAR-ESC-04, 5 km (CA10). Sin ubicación no filtra (P3) |
| `departamentoId` | UUID | CA3 |
| `ciudadId` | UUID | CA3. Si no es del departamento enviado, no hay resultados (`INF-ESC-01`), sin error |
| `tipoCanchaId` | UUID | CA4 |
| `fecha` | `aaaa-mm-dd` | Hoy ≤ fecha ≤ hoy + PAR-RES-01, si no `ERR-HOR-10` (CA6). Sin `hora`: salen las canchas con al menos una franja libre ese día (P2) |
| `hora` | `hh:mm`, en punto | Exige `fecha` (`ERR-GEN-01` de fecha). Minutos distintos de 00: `ERR-GEN-02` (P5). Con `fecha`: salen las canchas con libre la franja que empieza a esa hora (CA5) |

Respuesta `200`:

```json
{
  "resultados": [
    {
      "escenarioId": "uuid",
      "nombreEscenario": "Canchas La 70",
      "latitud": 6.155123,
      "longitud": -75.374456,
      "canchaId": "uuid",
      "nombreCancha": "Cancha 1",
      "tipoCancha": "Fútbol 5",
      "precioHora": 80000.00,
      "distanciaKm": 2.4,
      "calificacionPromedio": null
    }
  ],
  "radioKm": 5,
  "radioMinimoKm": 1,
  "radioMaximoKm": 30,
  "fechaMinima": "2026-11-22",
  "fechaMaxima": "2026-12-22",
  "codigo": null,
  "mensaje": null
}
```

- Un resultado por cancha. `escenarioId` sirve para ir al perfil del escenario (CA8, HU19); los identificadores no se muestran en la pantalla (AG-14).
- `latitud` y `longitud` del escenario, para el marcador de cada escenario en el mapa del prototipo «11 Búsqueda de canchas» (P9). El círculo del radio lo dibuja la pantalla con el punto del jugador y `radioKm`.
- Con ubicación: ordenados por distancia, y a igual distancia por nombre del escenario y de la cancha (CA1). `distanciaKm` va con un decimal y `radioKm` es el radio aplicado.
- Sin ubicación: `distanciaKm` y `radioKm` son `null` y el orden es por nombre del escenario y de la cancha (CA2).
- `calificacionPromedio` sale de `ConsultaCalificaciones` (HU19): por ahora siempre `null`, nunca `0` (AG-20, D05).
- Nunca se devuelven estados, el administrador ni el celular.
- Códigos informativos (P6): sin resultados → `INF-ESC-01` (CA7); con resultados y sin ubicación → `INF-ESC-03` (CA2); con resultados y con ubicación → `null`.

| HTTP | Código | Cuándo |
|---|---|---|
| 200 | —, `INF-ESC-01` o `INF-ESC-03` | La búsqueda se ejecutó |
| 400 | `ERR-GEN-04` | Radio fuera de 1 a 30 km (CA11), o latitud o longitud fuera de rango. Se agrega `ERR-GEN-04 → 400` en `ManejadorErrores`, igual que los demás `ERR-GEN-01` a `04` |
| 400 | `ERR-GEN-01` | Llega `latitud` sin `longitud` (o al revés), o `hora` sin `fecha` |
| 400 | `ERR-GEN-02` | `hora` no está en punto (P5) |
| 400 | `ERR-GEN-07` | Un UUID, una fecha o una hora que no se pueden leer (ya lo maneja `ManejadorErrores`) |
| 422 | `ERR-HOR-10` | Fecha anterior a hoy o posterior a hoy + 30 (CA6; por defecto en `ManejadorErrores`) |

Orden de las validaciones, todas **antes** de consultar la base (CA6 y CA11: «no se ejecuta la búsqueda»): datos completos y legibles → radio y coordenadas → fecha.

## Diseño por capas

### Dominio (`escenarios.domain`)

| Pieza | Cambio | Política |
|---|---|---|
| `Cancha.publicarEnBusqueda` | Ya existe (HU19). Se reutiliza sin cambios | Política 11 (RN-04) |
| `HorarioRecurrente.validarFechaConsultable(fecha, ahora, ventanaReservaDias)` | **Nuevo**, estático. Sale de `calcularFranjas`, que lo sigue llamando. Hace falta porque CA6 exige rechazar la fecha aunque no haya ninguna cancha candidata, y sin candidatas `calcularFranjas` nunca se llamaría | Política 10 (RN-11) |
| `HorarioRecurrente.calcularFranjas` | Ya existe (HU20). Se usa por escenario para saber qué franjas del día están dentro del horario y no han empezado | Políticas 11 y 12 |
| `Franja.seCruzaCon` | Ya existe (HU20). Dice si una reserva o un bloqueo ocupa la franja | Política 11 de HorarioRecurrente |

No se crea un objeto de dominio «Búsqueda»: el modelo enriquecido no lo tiene. El radio y las coordenadas del jugador son validaciones de datos de entrada (ERR-GEN), no políticas de un objeto.

### Puertos de Escenarios

| Pieza | Paquete | Qué hace |
|---|---|---|
| `BuscarCanchas` (nuevo), con los records `CriteriosBusqueda` y `ResultadoBusqueda` (lista de `CanchaEncontrada`, radio aplicado y límites) | `application.port.in` | Contrato del caso de uso |
| `RepositorioBusquedaCanchas` (nuevo) | `application.port.out` | `List<Candidata> buscar(FiltroUbicacion)`: canchas con su escenario, filtradas por punto y radio (`ST_DWithin`), departamento, ciudad y tipo, con la distancia en metros (`ST_Distance`) si hay punto. `Candidata` trae lo necesario para reconstruir `Cancha` y `Escenario` (sin fotos) y el nombre del tipo (P7) |
| `ConsultaFranjasOcupadas` (nuevo, P1) | `application.port.out` | `Map<UUID, List<RangoOcupado>> confirmadasDelDia(Collection<UUID> canchaIds, LocalDate fecha)`: las horas de las reservas CONFIRMADAS de esas canchas en la fecha, por cancha, sin el jugador. La define Escenarios y la implementa Reservas. Devuelve las horas y no solo «ocupada sí o no» porque con fecha sin hora hay que revisar todas las franjas del día (P2) |
| `RepositorioBloqueos.rangosDelDia(Collection<UUID> canchaIds, LocalDate fecha)` | `application.port.out` (ya existe) | Sobrecarga nueva del método de HU20: los bloqueos de varias canchas en una sola consulta, por cancha |
| `RepositorioHorarios.buscarPorEscenarios(Collection<UUID> escenarioIds)` | `application.port.out` (ya existe) | Método nuevo: los horarios de varios escenarios en una sola consulta, para no hacer una por escenario |
| `ConsultaCalificaciones` | `application.port.out` (ya existe, HU19) | Se reutiliza por escenario |

### Caso de uso: `escenarios.application.usecase.BuscarCanchasCasoUso`

Solo lee, sin transacción de escritura. Pasos de `ejecutar(criterios)`:

1. Radio: si no llega, PAR-ESC-04; si está fuera de PAR-ESC-06 a PAR-ESC-05 → `ERR-GEN-04` con `radio de búsqueda`, 1 y 30 (CA10, CA11). Coordenadas fuera de rango → `ERR-GEN-04`.
2. Si hay `fecha`: `HorarioRecurrente.validarFechaConsultable(fecha, ahora, PAR-RES-01)` → `ERR-HOR-10` (CA6).
3. `repositorioBusqueda.buscar(...)`: candidatas por ubicación, radio, departamento, ciudad y tipo (CA1, CA3, CA4).
4. `repositorioHorarios.buscarPorEscenarios` y, para cada candidata, `cancha.publicarEnBusqueda(escenario, escenarioTieneHorarios)`; las que no cumplen se quitan (RN-04, P7). Aquí se usan todos los horarios del escenario, no solo los del día: RN-04 pide «al menos un horario».
5. Si hay `fecha` (CA5, P2): `consultaFranjasOcupadas.confirmadasDelDia(...)` y `repositorioBloqueos.rangosDelDia(...)`, cada una en una sola llamada con todas las canchas que quedan. Para cada cancha, sus franjas son `HorarioRecurrente.calcularFranjas(horariosDelEscenario, fecha, ahora, PAR-RES-01)`; si hay `hora`, solo la que empieza a esa hora. Una franja está libre si ninguna reserva ni bloqueo de la cancha se cruza con ella (`Franja.seCruzaCon`). La cancha se queda si tiene al menos una franja libre.
6. `calificacionPromedio` por escenario con `ConsultaCalificaciones` y `Escenario.calcularCalificacionPromedio` (HU19).
7. El orden ya viene de la base (paso 3). El controlador agrega `INF-ESC-01` o `INF-ESC-03` (P6), igual que `INF-HOR-01` en HU20.

### Persistencia

| Pieza | Módulo | Qué hace |
|---|---|---|
| `RepositorioBusquedaCanchasJdbc` | Escenarios (`infrastructure.persistence`) | SQL con PostGIS: `ST_DWithin(e.ubicacion, ST_MakePoint(:lng, :lat)::geography, :radioMetros)` (usa `ix_escenario_ubicacion`) y `ST_Distance` para ordenar. Join con `ciudad` para el departamento y con `tipo_cancha` para el nombre. JDBC, como `RepositorioBloqueosJdbc`, porque es una consulta de lectura con funciones de PostGIS que JPA no mapea |
| `RepositorioBloqueosJdbc.rangosDelDia(canchaIds, fecha)` | Escenarios | `select cancha_id, hora_inicio, hora_fin from bloqueo where cancha_id in (:ids) and fecha = :fecha`, solo las horas (P5 de HU20) |
| `RepositorioHorariosJpa.buscarPorEscenarios(escenarioIds)` | Escenarios | Todos los horarios de varios escenarios en una consulta. Sirven para RN-04 («al menos un horario») y, filtrados por día dentro de `calcularFranjas`, para las franjas. |
| `ConsultaFranjasOcupadasCasoUso` (implementa el puerto de Escenarios) y `RepositorioReservas.confirmadasDeCanchasEnFecha(canchaIds, fecha)` | Reservas (`application.usecase` y `infrastructure.persistence`) | Solo reservas CONFIRMADAS, usando `ix_reserva_cancha_fecha`. Mismo patrón que `ConsultaReservasFuturasCasoUso` (D08) |

Sin migraciones: `escenario.ubicacion` y su índice ya existen en V1.

### Configuración y REST

| Pieza | Qué hace |
|---|---|
| `application.properties` | PAR-ESC-04 a PAR-ESC-06: `canchaya.escenario.radio-busqueda-defecto-km=${PAR_ESC_04:5}`, `…-maximo-km=${PAR_ESC_05:30}` y `…-minimo-km=${PAR_ESC_06:1}`. PAR-RES-01 ya existe (`canchaya.reserva.ventana-dias`). Las variables se agregan a la tabla de `projects/api/AGENTS.md` |
| `escenarios.infrastructure.rest.BusquedaCanchasController` y `RespuestaBusquedaCanchas` | `GET /api/canchas/busqueda`. Un controlador propio para no mezclar lo público con `CanchaController`, que es del propietario |
| `ConfiguracionSeguridad` | `GET /api/canchas/busqueda` público. También `GET /api/departamentos/**` y `GET /api/tipos-cancha`, que hoy exigen sesión y sin ellos un visitante no puede llenar los filtros (CA9, P8) |
| `ManejadorErrores` | `ERR-GEN-04 → 400`, y que el texto de `ERR-GEN-04` reciba `{minimo}` y `{maximo}` (hoy el manejador solo le pasa `{campo}`) |
| `catalogo-mensajes.properties` | `INF-ESC-01`, `INF-ESC-03` y los nombres de campo `campo.radioKm`, `campo.fecha`, `campo.hora`, `campo.departamentoId`; el formato esperado de `hora` para `ERR-GEN-02`. `ERR-GEN-04` y `ERR-HOR-10` ya están |

### Arquitectura

`ArquitecturaTest` ya permite que Reservas implemente un puerto de Escenarios (lo hace con `ConsultaReservasFuturas`) y prohíbe que Escenarios use Reservas, así que **no hay que cambiarla**.

## Spike de PostGIS

La HU pide un spike antes de implementar. Va como T1: una prueba con Testcontainers (`postgis/postgis:16-3.4`, la misma imagen de las demás pruebas) que guarda escenarios con coordenadas conocidas de Antioquia y confirma que:

1. `ST_DWithin` con `geography` mide en metros sobre la Tierra (no en grados) y respeta el borde del radio.
2. `ST_Distance` ordena bien y da una distancia razonable contra un valor calculado aparte (por ejemplo, Medellín – Rionegro).
3. La columna generada `ubicacion` se llena sola al guardar latitud y longitud.

Si el spike muestra algo distinto, se corrige esta especificación antes de seguir.

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1 | Con punto y radio de 10 km: salen los escenarios a 2 y 8 km, no el de 15 km; ordenados por distancia y con `distanciaKm` | `CA1_conUbicacion_dentroDelRadioYOrdenadasPorDistancia` | Persistencia (Testcontainers) + REST |
| CA1 | Solo salen las canchas publicables: cancha INACTIVA, escenario INACTIVO y escenario sin horarios no salen | `CA1_soloCanchasQueCumplenRN04` | Caso de uso + persistencia |
| CA1 | Spike: columna generada, `ST_Distance` en metros (Rionegro – Medellín, unos 24 km), borde del radio y radio en metros y no en grados | `spike_columnaGenerada_seLlenaYSeRecalcula`, `spike_distancia_enMetrosSobreLaTierra`, `spike_radio_respetaElBordeEnMetros` y `spike_radio_noEsEnGrados` (`BusquedaPostgisSpikeTest`) | Persistencia |
| CA2 | Sin ubicación: busca por departamento y ciudad, `distanciaKm` null, orden por nombre e `INF-ESC-03` | `CA2_sinUbicacion_INF_ESC_03_sinOrdenarPorDistancia` | Caso de uso + REST |
| CA2 | `latitud` sin `longitud` → 400 con `ERR-GEN-01` | `CA2_latitudSinLongitud_ERR_GEN_01` | REST |
| CA3 | Departamento: solo sus escenarios. Departamento y ciudad: solo los de la ciudad. Ciudad de otro departamento: sin resultados | `CA3_porDepartamentoYCiudad_soloLosDeAhi` | Persistencia + caso de uso |
| CA4 | Tipo de cancha: solo las de ese tipo, aunque el escenario tenga de otros | `CA4_porTipo_soloLasDeEseTipo` | Persistencia |
| CA5 | Fecha y hora: no sale la cancha con reserva CONFIRMADA en esa franja, ni la bloqueada, ni la de un escenario que no atiende a esa hora; sí sale la que tiene una reserva CANCELADA | `CA5_fechaYHora_soloConLaFranjaLibre` | Caso de uso + punta a punta |
| CA5 | Hoy a una hora que ya empezó: no sale (política 12) | `CA5_hoyHoraQueYaEmpezo_noSale` | Caso de uso |
| CA5 | Sin fecha ni hora no se filtra por disponibilidad | `CA5_sinFechaNiHora_noFiltraPorDisponibilidad` | Caso de uso |
| CA5 (P2) | Solo fecha: sale la cancha con una franja libre ese día; no sale la que tiene todas ocupadas (reservas y bloqueos) ni la de un escenario que no atiende ese día | `CA5_soloFecha_alMenosUnaFranjaLibre` | Caso de uso |
| CA5 | `hora` sin `fecha` → `ERR-GEN-01`; `hora` 14:30 → `ERR-GEN-02` | `CA5_horaSinFecha_ERR_GEN_01` y `CA5_horaNoEnPunto_ERR_GEN_02` | REST |
| CA5 | `ConsultaFranjasOcupadasCasoUso`: solo cuentan las CONFIRMADAS de esas canchas en esa fecha, agrupadas por cancha | `CA5_soloConfirmadasDeLaFecha` | Persistencia de Reservas |
| CA6 | Ayer y hoy + 31 → 422 con `ERR-HOR-10`, aunque no haya canchas; hoy y hoy + 30 se aceptan | `CA6_fechaFueraDeVentana_ERR_HOR_10` y `CA6_bordesDeLaVentana_seAceptan` | Dominio (`validarFechaConsultable`) + REST |
| CA7 | Ninguna cancha cumple → `resultados` `[]` e `INF-ESC-01` | `CA7_sinResultados_INF_ESC_01` | Caso de uso + REST |
| CA8 | Cada resultado trae `escenarioId` para ir al perfil (HU19); la navegación se prueba en el frontend | Se cubre con la prueba de CA1 | REST |
| CA1 (P9) | Cada resultado trae la `latitud` y la `longitud` de su escenario, para el mapa | Se cubre con la prueba de CA1 | REST |
| CA9 | Sin token → 200. `GET /api/departamentos`, sus ciudades y `GET /api/tipos-cancha` también responden sin token | `CA9_sinSesion_200` y `CA9_filtrosSinSesion_200` | Punta a punta |
| CA10 | Con ubicación y sin radio: se aplica 5 km (`radioKm` = 5 en la respuesta; el escenario a 7 km no sale) | `CA10_sinRadio_usaElRadioPorDefecto` | Caso de uso |
| CA11 | Radio 31 → 400 con `ERR-GEN-04` y el texto «entre 1 y 30»; radio 0 también (PAR-ESC-06); 1 y 30 se aceptan | `CA11_radioFueraDeRango_ERR_GEN_04` y `CA11_bordesDelRadio_seAceptan` | Caso de uso + REST |
| — | Cambiar PAR-ESC-05 en la configuración cambia el máximo sin tocar el código (regla 7) | `parametrosDeRadio_seLeenDeLaConfiguracion` | Caso de uso |
| — | La respuesta no expone estados ni datos del administrador (AG-14) | `respuesta_noExponeEstadosNiAdministrador` | REST |

## Decisiones tomadas (24-nov-2026)

**P1 · La búsqueda queda en Escenarios.** Escenarios define el puerto de salida `ConsultaFranjasOcupadas`, que implementa Reservas, con el mismo patrón de `ConsultaReservasFuturas` (D08). La tabla de módulos no cambia y `ArquitecturaTest` ya lo permite. Conviene registrarlo en el Excel de decisiones y en `componentes.md`.

**P2 · Fecha sin hora.** Salen las canchas que tienen al menos una franja libre ese día. La fecha se valida igual con `ERR-HOR-10` (CA6). Por eso `ConsultaFranjasOcupadas` y `RepositorioBloqueos` devuelven las horas ocupadas del día por cancha, y no solo si una franja está ocupada.

**P3 · Radio sin ubicación.** Se valida igual (CA11) pero no filtra, y `radioKm` sale `null`.

**P4 · Radio entero** en km, de 1 a 30. Un valor como `2.5` responde `ERR-GEN-07`.

**P5 · Hora que no está en punto.** `14:30` responde `ERR-GEN-02` con el formato esperado «hh:00».

**P6 · Un solo código informativo.** Sin resultados gana `INF-ESC-01`; con resultados y sin ubicación, `INF-ESC-03`.

**P7 · RN-04 en el dominio.** El SQL filtra solo por ubicación, radio, departamento, ciudad y tipo, y la política 11 la decide `Cancha.publicarEnBusqueda` en el caso de uso, como en HU19.

**P8 · Selectores públicos.** `GET /api/departamentos`, `GET /api/departamentos/{id}/ciudades` y `GET /api/tipos-cancha` pasan a ser públicos (solo GET).

**P9 · Coordenadas en el resultado.** El prototipo «11 Búsqueda de canchas» muestra un mapa al lado de los resultados, con un marcador por escenario y el círculo del radio. Cada resultado trae la `latitud` y la `longitud` de su escenario.

## Preguntas abiertas

Ninguna.

## Pendientes en los Excel de origen

| Excel | Cambio | Estado |
|---|---|---|
| Diagrama de clases, hoja HorarioRecurrente | Agregar `validarFechaConsultable(fecha, ahora, ventanaReservaDias)` (política 10), que ahora usan `calcularFranjas` y la búsqueda | Pendiente |
| Decisiones y componentes | Puerto `ConsultaFranjasOcupadas`, que define Escenarios e implementa Reservas (P1) | Pendiente |

## Fuera de alcance

- La pantalla: el permiso de ubicación del navegador, el mapa, el control del radio, el selector de fecha y la navegación al perfil (CA8).
- Paginar los resultados: la HU no lo pide y el volumen es pequeño (D16).
- La disponibilidad completa de una cancha (HU20) y la reserva (HU21).
- La calificación real: llega con Reputación en el Release 3 (P1 de HU19); por ahora siempre `null`.
