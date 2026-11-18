# HU19 · Consultar el perfil de un escenario

**Estado:** Implementada (17-nov-2026) · **HU:** [HU19](../../docs/historias/HU19.md) · **Módulo:** Escenarios y canchas (`co.edu.uco.canchaya.escenarios`)

## Fuentes

- Dominio: [Escenario](../../docs/dominio/escenario.md), con sus atributos y `calcularCalificacionPromedio` (políticas 6, 11 y 12), y [Cancha](../../docs/dominio/cancha.md), con `publicarEnBusqueda` (política 11, RN-04).
- Mensajes: `ERR-ESC-06` y `INF-RSE-01` ([mensajes.md](../../docs/especificacion/mensajes.md)). No aplican parámetros.
- Aspectos generales: AG-11 (ver la ficha no exige sesión) y AG-09 (una consulta sin datos muestra un mensaje INF).
- Decisiones: D05 (la calificación promedio se calcula al consultar), D13 (fotos) y D39 (paquetes).
- Diseño: `Escenario.estaActivo()`, `Escenario.calcularCalificacionPromedio(List<Short>)` y `Cancha.publicarEnBusqueda(Escenario, boolean escenarioTieneHorarios)` en [clases.md](../../docs/diseno/clases.md), y la nota «Datos de otros módulos» del mismo archivo.

## Qué se construye

Un servicio público, sin sesión, que entrega lo que muestra la ficha del escenario: fotos, nombre, descripción, dirección, ciudad y departamento, coordenadas para el mapa, contacto, las canchas publicables con su tipo y su precio, y la calificación promedio. El frontend todavía no existe, así que la imagen por defecto (CA3) y la navegación (CA1 «desde la búsqueda» y CA5) quedan para la pantalla. El backend entrega los datos que las hacen posibles.

## Contrato del servicio

`GET /api/escenarios/{id}/perfil` · **público** (se agrega a `ConfiguracionSeguridad` con `permitAll`, AG-11)

Respuesta `200`:

```json
{
  "id": "uuid",
  "nombre": "Canchas La 70",
  "descripcion": "texto o null",
  "direccion": "Cra 70 # 10-20",
  "ciudad": "Rionegro",
  "departamento": "Antioquia",
  "latitud": 6.155123,
  "longitud": -75.374456,
  "telefonoContacto": "+576045551234",
  "correoContacto": "contacto@la70.com o null",
  "fotos": ["https://.../1.jpg"],
  "canchas": [
    { "id": "uuid", "nombre": "Cancha 1", "tipoCancha": "Fútbol 5", "precioHora": 80000.00 }
  ],
  "calificacionPromedio": 4.3,
  "codigo": null,
  "mensaje": null
}
```

- Sin calificaciones: `calificacionPromedio` es `null` (nunca `0` ni un valor inventado), `codigo` es `"INF-RSE-01"` y `mensaje` trae su texto (CA2). Por ahora siempre es así (decisión de P1).
- Sin fotos: `fotos` es `[]` y la pantalla pone la imagen por defecto (CA3).
- Nunca se devuelve el administrador, su celular personal ni el `estado` (HU19: «solo el teléfono de contacto del escenario»).
- Canchas ordenadas por nombre, como en `RepositorioCanchas.listarPorEscenario`.

| HTTP | Código | Cuándo |
|---|---|---|
| 200 | — o `INF-RSE-01` | El escenario está ACTIVO |
| 404 | `ERR-ESC-06` | El escenario no existe o está INACTIVO (ya está en `ManejadorErrores`) |
| 400 | `ERR-GEN-07` | El `id` no es un UUID (ya lo maneja `ManejadorErrores`) |

## Revisión contra `clases.md` y el código

| Pieza | En `clases.md` | En el código | Qué se hace |
|---|---|---|---|
| `Escenario.estaActivo()` | Sí (RN-04) | Ya existe | Se usa para CA4. **Corrección:** el borrador anterior proponía `esVisibleParaJugadores()`, que era inventado y queda descartado |
| `Escenario.calcularCalificacionPromedio(List<Short>) → Optional<BigDecimal>`, estático | Sí (políticas 11 y 12, D05) | No existe | Se agrega con la firma exacta de `clases.md` |
| `Cancha.publicarEnBusqueda(Escenario, boolean escenarioTieneHorarios) → boolean` | Sí, con los dos parámetros | Agregado en T1 | Ver P5 |
| `Cancha.estaDisponible()` | Sí | Ya existe | Lo usa `publicarEnBusqueda` |
| ¿El escenario tiene horarios? | — | `RepositorioHorarios.buscarPorEscenario` ya existe | Se usa `!isEmpty()`; **no** se agrega un método nuevo (el borrador anterior proponía uno) |
| Nombre del departamento | — | `ConsultaUbicacion.listarDepartamentos()` ya existe | Se busca por `departamentoId`; no se agrega un método a Ubicación |
| Nombre del tipo de cancha | — | `RepositorioTiposCancha.listar()` ya existe | Se busca por `tipoCanchaId` |
| Identificadores de las canchas para las calificaciones | — | `RepositorioEscenarios.idsCanchas` ya existe | Incluye las canchas INACTIVAS: sus reseñas pasadas también cuentan |
| Modelo de datos | — | — | **Sin migraciones** ni cambios de tablas |

`clases.md` solo cubre el dominio (lo dice su nota «Alcance»). Los casos de uso, los puertos y los adaptadores siguen el patrón de las HU ya hechas (uno por caso de uso, D39). Las piezas de fuera del dominio que se crean son estas:

| Capa | Pieza | Por qué |
|---|---|---|
| Puerto de entrada | `ConsultarPerfilEscenario`, con los records anidados `PerfilEscenario` y `CanchaPerfil` | Mismo patrón que `ListarEscenariosAdministracion` con su record anidado |
| Puerto de salida | `ConsultaCalificaciones` (aprobado en P1) | Escenarios pide las calificaciones sin depender de Reputación |
| Adaptador temporal | `escenarios.infrastructure.calificaciones.ConsultaCalificacionesSinResenas`, que implementa `ConsultaCalificaciones` y devuelve una lista vacía | Aprobado en P1 y P6. Es un `@Component`, así que Spring lo conecta solo (lo encuentra en el escaneo de componentes) |
| Caso de uso | `ConsultarPerfilEscenarioCasoUso` | Arma el perfil |
| REST | `GET /{id}/perfil` en `EscenarioController` y `RespuestaPerfilEscenario` | Mismo patrón que las otras respuestas (`codigo` y `mensaje`) |
| Transversal | `INF-RSE-01` en `catalogo-mensajes.properties` | Hoy no está |

Firma propuesta del puerto, igual que `ConsultaReservasFuturas` (D08): Escenarios entrega los identificadores de sus canchas para que el otro módulo no lea la tabla `cancha`.

```java
/** Escenarios la define y Reputación la implementará en el Release 3 (decisión de P1). */
public interface ConsultaCalificaciones {
	/** Calificaciones (1 a 5) de las reseñas de reservas jugadas en esas canchas; vacía si no hay. */
	List<Short> calificacionesDeCanchas(Collection<UUID> canchaIds);
}
```

## Pruebas de arquitectura

`ArquitecturaTest` ya protege lo que pide la decisión de P1, así que **no hay que cambiarla**:

- `escenariosSoloUsaIdentidadYUbicacion` falla si Escenarios importa cualquier clase de `co.edu.uco.canchaya.reputacion..`.
- `reputacionSoloUsaIdentidadReservasYEscenarios` ya permite que, en el Release 3, Reputación implemente el puerto de Escenarios y llegue a las reservas por `ConsultaReservas` (punto 6 de P1).
- `sinDependenciasEnCirculo` revisa que no haya ciclos entre módulos.

T7 confirma con `./mvnw verify` que las tres siguen en verde.

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1 | Escenario ACTIVO: la respuesta trae fotos, nombre, descripción, dirección, ciudad, departamento, coordenadas, contacto, y canchas con tipo y precio | `CA1_escenarioActivo_devuelveElPerfilCompleto` | Caso de uso + punta a punta |
| CA1 | Solo salen las canchas publicables (DISPONIBLE, escenario ACTIVO y con horario); las INACTIVAS no | `CA1_canchaInactiva_noApareceEnElPerfil` y `CA1_escenarioSinHorarios_sinCanchas` | Dominio + caso de uso |
| CA1 | La respuesta no expone el administrador ni su celular | `CA1_perfil_noExponeDatosDelAdministrador` | REST |
| CA1 | Promedio con un decimal (4, 5, 4 → 4.3) | `CA1_calificaciones_promedioConUnDecimal` | Dominio |
| CA2 | Sin calificaciones: vacío, nunca cero | `CA2_sinCalificaciones_promedioVacio` | Dominio |
| CA2 | Sin calificaciones: `calificacionPromedio` null e `INF-RSE-01` | `CA2_sinResenas_sinPromedioYConMensajeINF_RSE_01` | REST |
| CA3 | Sin fotos: `fotos` es `[]` (la imagen por defecto la pone la pantalla) | `CA3_sinFotos_devuelveListaVacia` | REST |
| CA4 | No existe: 404 con `ERR-ESC-06` | `CA4_escenarioInexistente_404_ERR_ESC_06` | Caso de uso + REST |
| CA4 | INACTIVO: 404 con `ERR-ESC-06`, igual que si no existiera | `CA4_escenarioInactivo_404_ERR_ESC_06` | Caso de uso + REST |
| CA5 | Cada cancha trae su `id`, para que la pantalla lleve a su disponibilidad (HU20) | Se cubre con la prueba de CA1; la navegación se prueba en el frontend | REST |
| CA6 | Sin token: 200 | `CA6_sinSesion_escenarioActivo_200` | Punta a punta |

## Decisiones tomadas

**P1 · Quién entrega las calificaciones (aprobada el 17-nov-2026)**
1. Escenarios define el puerto de salida `ConsultaCalificaciones` en `application/port/out`.
2. Escenarios no depende de Reputación.
3. Mientras Reputación no exista, un adaptador temporal en la infraestructura de Escenarios devuelve un resultado vacío, nunca un promedio ficticio ni cero.
4. No se crea un módulo Reputación mínimo.
5. En el Release 3, Reputación implementa el puerto, respetando la dirección de dependencias de `AGENTS.md`, y el adaptador temporal se elimina.
6. Como `resena` solo tiene `reserva_id`, Reputación llega a las canchas por medio del módulo Reservas.
7. HU19 cubre el caso sin calificaciones con `INF-RSE-01`.

Conviene registrarla como una decisión nueva (D40) en el Excel de decisiones, como se hizo con D08.

**P2 · Qué canchas se muestran (aprobada el 17-nov-2026)**
Se aplica la política 11 completa: DISPONIBLE, escenario ACTIVO y con al menos un horario. Un escenario ACTIVO sin horarios muestra su ficha sin canchas.

**P5 · Firma de `Cancha.publicarEnBusqueda` (aprobada el 17-nov-2026)**
La firma recibe el `Escenario` y un booleano con los horarios. El caso de uso consulta antes los horarios y le pasa el resultado; el método no consulta otros módulos ni la base de datos.

*Corrección:* al revisar el Excel se vio que `clases.md` y `clases.mmd` **ya** tenían el segundo parámetro, con el nombre `escenarioTieneHorarios`. El borrador lo había pasado por alto, así que no había una firma anterior que corregir. El código usa el nombre documentado, `escenarioTieneHorarios`, y no `tieneHorarios`. En la hoja Cancha del Excel de clases solo se corrigió la descripción del parámetro `escenario` (celda K62): decía «para revisar quién es el propietario», copiada de `validarPropietario`, y ahora dice «para revisar que esté ACTIVO».

*Qué significa «tiene horarios»:* `HorarioRecurrente` no tiene estado ni vigencia. Un horario solo se guarda si pasó las validaciones de `definir` (horas en punto, fin mayor que inicio, sin cruces; la base también rechaza los cruces). Por eso todo horario guardado está publicado y es válido, y `escenarioTieneHorarios` es `!RepositorioHorarios.buscarPorEscenario(id).isEmpty()`.

**P6 · Paquete del adaptador temporal (aprobada el 17-nov-2026)**
Se crea el paquete `escenarios.infrastructure.calificaciones` con `ConsultaCalificacionesSinResenas`, que implementa el puerto `ConsultaCalificaciones` de Escenarios. Representa la ausencia de calificaciones con una lista vacía, nunca con un promedio de cero. No se crea el paquete `reputacion`. En el Release 3 se sustituye por la implementación real de Reputación y se elimina este paquete.

## Preguntas abiertas

**P3 · Inconsistencia entre documentos (no bloquea, se reporta)**
En `componentes.md`, Escenarios depende solo de Identidad y Reputación solo de Identidad. En `arquetipo.md` (y en `AGENTS.md`), Escenarios usa Identidad y Ubicación, y Reputación usa Identidad, Reservas y Escenarios. El código y `ArquitecturaTest` siguen a `arquetipo.md`, así que el error parece estar en el Excel de componentes.

**P4 · Ruta (no bloquea)**
Se propone `/api/escenarios/{id}/perfil` en lugar de `/api/escenarios/{id}`, para dejar `/{id}` libre al formulario de modificación del propietario (HU8), que sí exige sesión y devuelve otros datos.

## Pendientes en los Excel de origen

`docs/` no se edita a mano (regla 8): estos cambios los hace el desarrollador en el Excel y luego se regenera con `scripts/exportar_docs.py`.

| Excel | Cambio | Estado |
|---|---|---|
| Diagrama de clases, hoja Cancha, celda K62 | Descripción del parámetro `escenario` de `publicarEnBusqueda`: «para revisar que esté ACTIVO» (P5) | Hecho el 17-nov-2026 y exportado a `docs/diseno/clases.md` |
| Decisiones | Registrar la D40 con la decisión de P1 (puerto `ConsultaCalificaciones` definido por Escenarios, adaptador temporal sin reseñas y su reemplazo en el Release 3) | Pendiente |
| Componentes | Corregir de qué módulos depende Escenarios (Identidad y Ubicación) y Reputación (Identidad, Reservas y Escenarios), como en `arquetipo.md` (P3) | Pendiente |

Además, el Excel del Tallaje tiene un cambio en HU13 («fecha de registro» se quitó de la lista de administración) que nunca se exportó. No es de esta HU: va en su propio commit `docs: actualizar la documentación exportada`.

## Fuera de alcance

- La lista de reseñas (HU38), publicar reseñas (HU37) y la implementación real de `ConsultaCalificaciones` (Release 3).
- La disponibilidad de cada cancha (HU20).
- La pantalla, el mapa y la imagen por defecto: se hacen cuando empiece el frontend.
