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
| 400 | `ERR-GEN-04` | Radio fuera de 1 a 30 km (CA11), o latitud o longitud fuera de rango. Se agrega `ERR-GEN-04 → 400` en `ManejadorErrores` (también `ERR-GEN-01` y `ERR-GEN-02`), igual que los de la validación de datos |
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
| `RepositorioBusquedaCanchas` (nuevo) | `application.port.out` | `List<Candidata> buscar(FiltroBusqueda)`: canchas con su escenario, filtradas por punto y radio (`ST_DWithin`), departamento, ciudad y tipo, con la distancia en metros (`ST_Distance`) si hay punto. `Candidata` trae lo necesario para reconstruir `Cancha` y `Escenario` (sin fotos) y el nombre del tipo (P7) |
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
| `ManejadorErrores` | `ERR-GEN-01`, `ERR-GEN-02` y `ERR-GEN-04 → 400`, para que respondan igual que los de la validación de datos. Ningún otro caso de uso los lanza, así que no cambia otras HU. El texto de `ERR-GEN-04` ya recibe `{minimo}` y `{maximo}` como parámetros de la `ReglaNegocioException` |
| `catalogo-mensajes.properties` | `INF-ESC-01`, `INF-ESC-03`, los nombres de campo `campo.radioKm` y `campo.hora` y el formato esperado `formato.hora` para `ERR-GEN-02`. `campo.fecha`, `campo.latitud`, `campo.longitud`, `ERR-GEN-04` y `ERR-HOR-10` ya estaban |

### Arquitectura

`ArquitecturaTest` ya permite que Reservas implemente un puerto de Escenarios (lo hace con `ConsultaReservasFuturas`) y prohíbe que Escenarios use Reservas, así que **no hay que cambiarla**.

## Spike de PostGIS

La HU pide un spike antes de implementar. Va como T1: una prueba con Testcontainers (`postgis/postgis:16-3.4`, la misma imagen de las demás pruebas) que guarda escenarios con coordenadas conocidas de Antioquia y confirma que:

1. `ST_DWithin` con `geography` mide en metros sobre la Tierra (no en grados) y respeta el borde del radio.
2. `ST_Distance` ordena bien y da una distancia razonable contra un valor calculado aparte (por ejemplo, Medellín – Rionegro).
3. La columna generada `ubicacion` se llena sola al guardar latitud y longitud.

Si el spike muestra algo distinto, se corrige esta especificación antes de seguir.

## Criterios de aceptación → pruebas

Clases de prueba: **Dom** = `HorarioRecurrenteValidarFechaConsultableTest`; **CU** = `BuscarCanchasCasoUsoTest` (dobles en memoria); **Spike** = `BusquedaPostgisSpikeTest`; **Per** = `BusquedaCanchasPersistenciaTest` (`@DataJpaTest` con PostGIS real); **Res** = `ConsultaFranjasOcupadasPersistenciaTest` (Reservas); **REST** = `BusquedaCanchasControllerTest`; **P2P** = `BusquedaCanchasPuntaAPuntaTest` (aplicación completa, sin token, reloj real); **Conf** = `ParametrosBusquedaTest`; **Cat** = `CatalogoMensajesTest`.

| CA | Qué se verifica | Pruebas | Clase |
|---|---|---|---|
| CA1 | Spike: columna generada, `ST_Distance` en metros (Rionegro – Medellín, unos 24 km), borde del radio y radio en metros y no en grados | `spike_columnaGenerada_seLlenaYSeRecalcula`, `spike_distancia_enMetrosSobreLaTierra`, `spike_radio_respetaElBordeEnMetros`, `spike_radio_noEsEnGrados` | Spike |
| CA1 | Dentro del radio, de la más cercana a la más lejana (nombres al revés de la distancia), con su distancia; borde del radio | `ca1ConUbicacion_dentroDelRadioYOrdenadasPorDistancia`, `ca1ElRadioRespetaElBorde` | Per |
| CA1 | Distancia en km con un decimal, radio aplicado y filtro en metros | `ca1ConUbicacion_dentroDelRadioYOrdenadasPorDistancia` | CU |
| CA1, CA8, P9 | Cada resultado trae `escenarioId` (para ir al perfil, HU19), nombre, coordenadas del escenario, cancha, tipo, precio, distancia y calificación | `ca1CadaResultadoTraeLoQueMuestraLaPantalla` (CU), `CA1_conUbicacion_devuelveResultadosConDistancia` (REST) | CU + REST |
| CA1 (RN-04, P7) | No salen la cancha INACTIVA, el escenario INACTIVO ni el escenario sin horarios; un horario de cualquier día basta; la base no filtra por estado | `ca1SoloCanchasQueCumplenRN04`, `ca1UnHorarioDeOtroDiaBastaParaPublicar` (CU), `p7NoFiltraPorEstado_reconstruyeCanchaYEscenario` (Per) | CU + Per |
| CA1 (D05) | Calificación vacía sin reseñas, nunca cero; con reseñas, un decimal | `ca1CalificacionPromedioDelEscenario` | CU |
| CA1, CA9, CA10 | Sin token, con ubicación y sin radio: 5 km, orden por distancia; con 10 km salen los dos | `CA1_CA9_CA10_sinSesionConUbicacionOrdenadasPorDistanciaYRadioPorDefecto` | P2P |
| CA2 | Sin ubicación: sin radio ni distancia, orden por nombre | `ca2SinUbicacion_sinDistanciaYOrdenadasPorNombre` (CU y Per) | CU + Per |
| CA2 (P6) | Sin ubicación y con resultados: `INF-ESC-03` | `CA2_sinUbicacion_INF_ESC_03_sinOrdenarPorDistancia` (REST), `CA2_CA3_sinUbicacionPorDepartamento_INF_ESC_03` (P2P) | REST + P2P |
| CA2 | `latitud` sin `longitud` (o al revés) → 400 con `ERR-GEN-01`; coordenadas fuera de rango → `ERR-GEN-04` | `ca2LatitudSinLongitud_ERR_GEN_01`, `coordenadasFueraDeRango_ERR_GEN_04` (CU), `CA2_latitudSinLongitud_ERR_GEN_01` (REST) | CU + REST |
| CA3, CA4 | Departamento, ciudad (también de otro departamento) y tipo; los filtros se combinan | `ca3PorDepartamentoYCiudad_soloLosDeAhi`, `ca4PorTipo_soloLasDeEseTipo`, `losFiltrosSeCombinan` (Per); `ca3Ca4LosFiltrosLleganAlRepositorio` y las de CA3 y CA4 (CU) | Per + CU |
| CA5 | Fecha y hora: no salen la cancha con reserva CONFIRMADA ni la bloqueada ni la de un escenario que no atiende a esa hora; la de la CANCELADA sí | `ca5FechaYHora_soloConLaFranjaLibre` (CU), `CA5_fechaYHora_soloConLaFranjaLibre` (P2P) | CU + P2P |
| CA5 (P2) | Solo fecha: sale la cancha con al menos una franja libre; no la que está llena ni la de un escenario que no atiende ese día | `ca5SoloFecha_alMenosUnaFranjaLibre` (CU), `CA5_fechaYHora_soloConLaFranjaLibre` (P2P) | CU + P2P |
| CA5 (pol. 12) | Hoy, una franja que ya empezó no sale | `ca5HoyHoraQueYaEmpezo_noSale` | CU |
| CA5 | Sin fecha ni hora no se filtra ni se consultan reservas | `ca5SinFechaNiHora_noFiltraPorDisponibilidad` | CU |
| CA5 (P5) | `hora` sin `fecha` → `ERR-GEN-01`; `14:30` → `ERR-GEN-02` con el formato esperado; ambos 400 | `ca5HoraSinFecha_ERR_GEN_01`, `ca5HoraNoEnPunto_ERR_GEN_02` (CU), `CA5_horaSinFecha_ERR_GEN_01`, `CA5_horaNoEnPunto_ERR_GEN_02` (REST) | CU + REST |
| CA5 | Bloqueos de varias canchas por cancha y fecha; horarios de varios escenarios | `ca5LosBloqueosDeVariasCanchasPorCancha`, `losHorariosDeVariosEscenarios` | Per |
| CA5 (P1) | Reservas: solo las CONFIRMADAS de esas canchas en esa fecha, por cancha; sin canchas no consulta | `ca5SoloConfirmadasDeLaFecha`, `sinCanchas_mapaVacio` | Res |
| CA6 | Ayer y hoy + 31 → `ERR-HOR-10`; hoy y hoy + 30 se aceptan; la ventana sale de PAR-RES-01; `calcularFranjas` usa la misma validación | `ca6FechaFueraDeLaVentana_ErrHor10`, `ca6BordesDeLaVentana_seAceptan`, `ca6LaVentanaSaleDelParametro`, `ca6CalcularFranjasUsaLaMismaValidacion` | Dom |
| CA6 | Se rechaza antes de buscar, aunque no haya canchas; la respuesta trae `fechaMinima` y `fechaMaxima` | `ca6FechaFueraDeVentana_ERR_HOR_10`, `ca6LaRespuestaTraeLasFechasMinimaYMaxima` | CU |
| CA6 | 422 con `ERR-HOR-10` | `CA6_fechaFueraDeVentana_422_ERR_HOR_10` (REST y P2P) | REST + P2P |
| CA7 | Sin resultados → `[]` e `INF-ESC-01`, que gana sobre `INF-ESC-03` (P6) | `ca7SinResultados_listaVacia` (CU), `CA7_sinResultados_INF_ESC_01` (REST y P2P) | CU + REST + P2P |
| CA9 | Sin token → 200; solo el GET es público | `CA9_sinSesion_200_soloElGet` (REST) y la de CA1, CA9 y CA10 (P2P) | REST + P2P |
| CA9 (P8) | `GET /api/departamentos`, sus ciudades y `GET /api/tipos-cancha` responden sin token (sin las rutas públicas, 401) | `CA9_filtrosSinSesion_200` | P2P |
| CA10 | Con ubicación y sin radio: 5 km; el escenario a 7 km no sale | `ca10SinRadio_usaElRadioPorDefecto` | CU |
| CA11 | Radio 0, 31, 100 y -5 → `ERR-GEN-04` con 1 y 30, también sin ubicación (P3); 1 y 30 se aceptan | `ca11RadioFueraDeRango_ERR_GEN_04`, `ca11BordesDelRadio_seAceptan`, `p3RadioSinUbicacion_noFiltra` | CU |
| CA11 | 400 con «El valor de radio de búsqueda debe estar entre 1 y 30.» | `CA11_radioFueraDeRango_ERR_GEN_04` (REST y P2P), `hu18ArmaErrGen04ConElCampoYLosLimitesDelRadio` (Cat) | REST + P2P + Cat |
| P4 | Radio `2.5`, UUID, fecha u hora ilegibles → 400 con `ERR-GEN-07`, sin buscar | `datosIlegibles_400_ERR_GEN_07` | REST |
| Regla 7 | PAR-ESC-04 a 06 salen de `application.properties` (5, 30 y 1) y sus variables los cambian; un máximo de 50 llega al caso de uso | `losValoresPorDefectoSonLosDelCatalogoDeParametros`, `lasVariablesDeEntornoCambianLosRadios` (Conf), `parametrosDeRadio_seLeenDeLaConfiguracion` (CU) | Conf + CU |
| Regla 6 | `INF-ESC-01`, `INF-ESC-03`, `ERR-HOR-10` y `ERR-GEN-04` están en el catálogo | `tieneLosCodigosQueCitaLaHu18` | Cat |
| AG-14 | La respuesta no expone estados ni datos del administrador | `respuesta_noExponeEstadosNiAdministrador` | REST |
| — | Los parámetros de la URL llegan completos al caso de uso | `losParametrosLleganAlCasoDeUso` | REST |

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
| Catálogo de mensajes, columna de las HU que usan cada código | Agregar HU18 a `ERR-GEN-01` (latitud sin longitud, hora sin fecha) y a `ERR-GEN-02` (hora que no está en punto, P5) | Pendiente |
| Tallaje, hoja HU18, «Catálogo de mensajes» | Agregar `ERR-GEN-01` y `ERR-GEN-02` a los códigos usados en esta HU | Pendiente |
| Aspectos generales, AG-11 | Aclarar que los selectores de departamento, ciudad y tipo de cancha tampoco exigen sesión, porque son parte de buscar canchas (P8) | Pendiente |

## Fuera de alcance

- La pantalla: el permiso de ubicación del navegador, el mapa, el control del radio, el selector de fecha y la navegación al perfil (CA8).
- Paginar los resultados: la HU no lo pide y el volumen es pequeño (D16).
- La disponibilidad completa de una cancha (HU20) y la reserva (HU21).
- La calificación real: llega con Reputación en el Release 3 (P1 de HU19); por ahora siempre `null`.
