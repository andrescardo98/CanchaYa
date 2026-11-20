# HU9 · Modificar una cancha

**Estado:** Implementada (19-nov-2026) · **HU:** [HU9](../../docs/historias/HU9.md) · **Módulo:** Escenarios y canchas (`co.edu.uco.canchaya.escenarios`)

## Fuentes

- Dominio: [Cancha](../../docs/dominio/cancha.md), responsabilidad `modificar` (políticas 7, 2, 8, 4, 5 y 9) y su tabla «Uso de cada atributo» (el escenario y el estado no se modifican).
- Mensajes: `EXI-CAN-02`, `ERR-CAN-02`, `ERR-CAN-04`, `ERR-CAN-05`, `ERR-CAN-07`, `ERR-CAN-08` y `ERR-GEN-01` a `ERR-GEN-04` ([mensajes.md](../../docs/especificacion/mensajes.md)).
- Parámetros: no aplican.
- Aspectos generales: AG-03 (todos los errores a la vez), AG-04 (después de modificar, el formulario muestra los datos actualizados), AG-06 (espacios sobrantes) y AG-12 (propiedad en el servidor).
- Decisiones: D10 (el precio de la reserva sale de las franjas) y D39 (paquetes).
- Diseño: `Cancha.modificar(escenario, tipoCanchaId, nombre, precioHora, solicitante)` en [clases.md](../../docs/diseno/clases.md), que lanza ERR-CAN-02 y ERR-CAN-05; el caso de uso revisa ERR-CAN-07, ERR-CAN-08 y ERR-CAN-04. «La política 9 se cumple por diseño: la reserva guarda su propio precio» (columna `reserva.precio` de la V1).
- Código que se reutiliza: `Cancha.validarPropietario`, `Cancha.validarPrecio`, `SolicitudRegistroCancha` (mismas reglas de datos), `RespuestaCancha`, `RepositorioCanchas.buscarPorId` (HU20), el índice único `uq_cancha_escenario_nombre` sobre `lower(nombre)` (V3) y `GET /api/escenarios/{escenarioId}/canchas` (la lista que se actualiza, CA1).

## Qué se construye

El propietario del escenario cambia el nombre, el tipo o el precio por hora de una de sus canchas. El escenario, el estado y el identificador no cambian. El formulario se llena con la fila de la lista de canchas que ya existe (HU6), que trae todos los datos, así que no hace falta otra consulta. Las reservas ya hechas no cambian de precio porque cada una guarda el suyo.

## Contrato del servicio

`PUT /api/canchas/{id}` · con sesión · solo el propietario del escenario de la cancha · `application/json` (P1)

```json
{ "nombre": "Cancha 1", "tipoCanchaId": "uuid", "precioHora": 90000 }
```

Las mismas reglas de `SolicitudRegistroCancha` (HU6): nombre obligatorio de 1 a 50 caracteres sin espacios en los extremos, tipo obligatorio y precio obligatorio con hasta 8 enteros y 2 decimales. Los tres campos se envían siempre: el formulario manda los datos actuales con los que cambiaron.

Respuesta `200`:

```json
{
  "codigo": "EXI-CAN-02",
  "mensaje": "La cancha se modificó correctamente.",
  "cancha": { "id": "uuid", "nombre": "Cancha 1", "tipoCanchaId": "uuid", "precioHora": 90000.00, "estado": "DISPONIBLE" }
}
```

`cancha` tiene la forma de `RespuestaCancha`: con ella el formulario muestra los datos guardados (AG-04) y la pantalla actualiza la fila de la lista sin volver a consultarla (CA1).

| HTTP | Código | Cuándo |
|---|---|---|
| 200 | `EXI-CAN-02` | Se guardó |
| 400 | `ERR-GEN-01` a `ERR-GEN-04` | Datos inválidos, todos a la vez (`ManejadorErrores`, AG-03) |
| 400 | `ERR-GEN-07` | El `id` no es UUID, o el JSON no se puede leer |
| 403 | `ERR-CAN-02` | No es el propietario, incluido el encargado de sede y el administrador del sistema (ya está en `ManejadorErrores`) |
| 404 | `ERR-CAN-07` | No existe la cancha (**entrada nueva** en `ManejadorErrores`) |
| 422 | `ERR-CAN-04` | El tipo de cancha no existe |
| 422 | `ERR-CAN-05` | Precio menor o igual a cero |
| 422 | `ERR-CAN-08` | Otra cancha del mismo escenario ya tiene ese nombre, sin importar mayúsculas |

## Diseño por capas

| Capa | Pieza | Qué hace |
|---|---|---|
| Dominio | `Cancha.modificar(Escenario, UUID tipoCanchaId, String nombre, BigDecimal precioHora, Solicitante)` | Política 2 (`validarPropietario`, ERR-CAN-02) y política 5 (`validarPrecio`, ERR-CAN-05, redondeo a 2 decimales). Recorta el nombre como `registrar`. Si el escenario recibido no es el de la cancha, lanza `IllegalArgumentException` (error de programación, no de negocio). `nombre`, `tipoCanchaId` y `precioHora` dejan de ser `final`; `id`, `escenarioId` y `estado` no se tocan |
| Puerto de entrada | `ModificarCancha` con el record `DatosModificacionCancha(canchaId, tipoCanchaId, nombre, precioHora)` | Contrato del caso de uso |
| Caso de uso | `ModificarCanchaCasoUso` | Orden, igual que HU6: política 7 (`buscarPorId`, ERR-CAN-07) → buscar el escenario de la cancha → `cancha.modificar(...)` (2 y 5) → política 4 (`existePorId` del tipo, ERR-CAN-04) → política 8 (`existeNombreEnOtraCancha`, ERR-CAN-08 con el nombre) → `actualizar`. Como la cancha modificada solo vive en memoria hasta `actualizar`, un error en 4 u 8 no deja nada guardado |
| Puerto de salida | `RepositorioCanchas.buscarPorId(UUID)` | Ya existe: lo agregó HU20 (su P7) con su implementación en `RepositorioCanchasJpa`. Se reutiliza tal cual |
| Puerto de salida | `RepositorioCanchas.existeNombreEnOtraCancha(escenarioId, nombre, canchaId)` | Política 8: compara sin distinguir mayúsculas y excluye la cancha que se modifica. Así, dejar el mismo nombre o cambiar solo mayúsculas («cancha 1» → «Cancha 1») sí se puede |
| Puerto de salida | `RepositorioCanchas.actualizar(Cancha)` | Guarda nombre, tipo y precio. Si otra solicitud dejó el mismo nombre al mismo tiempo, la base lo rechaza (`uq_cancha_escenario_nombre`) y se traduce a ERR-CAN-08. `guardar` no sirve: hace INSERT (`isNew`) |
| Persistencia | `RepositorioCanchasJpa` y `CanchaJpa` | `actualizar` carga la entidad, cambia los tres campos con un método nuevo de `CanchaEntidad` y hace `saveAndFlush` para traducir la violación del índice aquí. Consulta JPQL con `lower()` y `c.id <> :canchaId` |
| REST | `PUT /api/canchas/{id}` en `CanchaController`; `SolicitudModificarCancha` (mismas anotaciones que `SolicitudRegistroCancha`) y `RespuestaModificarCancha(codigo, mensaje, RespuestaCancha cancha)` | `ERR-CAN-07 → 404` en `ManejadorErrores` |
| Transversal | `EXI-CAN-02`, `ERR-CAN-07` y `ERR-CAN-08` en `catalogo-mensajes.properties` | Hoy no están |

Sin migraciones: la tabla `cancha` y el índice de la V3 ya tienen lo necesario. La política 9 no tiene código: la reserva copia el precio al crearse (HU21) y modificar la cancha no toca la tabla `reserva`.

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1 | Cambia nombre, tipo y precio; recorta el nombre; conserva id, escenario y estado | `ca1Modificar_cambiaLosDatosYConservaEscenarioYEstado` | Dominio |
| CA1 | Se guarda; dejar el mismo nombre o cambiar solo mayúsculas sí se puede | `ca1Modificar_guardaLosCambios` y `ca1Modificar_mismoNombreConOtrasMayusculas_seGuarda` | Caso de uso |
| CA1 | `actualizar` cambia la fila y `existeNombreEnOtraCancha` excluye la propia cancha | `ca1Actualizar_cambiaNombreTipoYPrecio` | Persistencia |
| CA1 | 200 con `EXI-CAN-02` y la cancha guardada; la lista del escenario muestra el cambio | `ca1ModificarCancha_200_EXI_CAN_02_yListaActualizada` | REST + punta a punta |
| CA2 | Nombre vacío o de 51 caracteres, sin tipo, sin precio o con 3 decimales: 400 con todos los ERR-GEN y no se llama al caso de uso | `ca2DatosInvalidos_400_conTodosLosErrores` | REST |
| CA3 | Cancha inexistente: 404 `ERR-CAN-07` y nada cambia | `ca3CanchaInexistente_ERR_CAN_07` | Caso de uso + REST |
| CA4 | Otro administrador de cancha, un encargado de sede y el administrador del sistema: `ERR-CAN-02`, nada cambia | `ca4NoPropietario_ERR_CAN_02`, `ca4EncargadoDeSede_ERR_CAN_02` y `ca4AdministradorDelSistema_ERR_CAN_02` | Dominio + caso de uso |
| CA4 | 403 `ERR-CAN-02` por HTTP | `ca4NoPropietario_403_ERR_CAN_02` | Punta a punta |
| CA5 | Nombre de otra cancha del escenario (también con otras mayúsculas): `ERR-CAN-08` con el nombre; el mismo nombre en otro escenario sí se puede | `ca5NombreDeOtraCancha_ERR_CAN_08` y `ca5MismoNombreEnOtroEscenario_seGuarda` | Caso de uso |
| CA5 | Dos solicitudes al mismo tiempo: la base rechaza el repetido y se responde ERR-CAN-08 | `ca5Actualizar_nombreRepetido_ERR_CAN_08` | Persistencia |
| CA6 | Tipo inexistente: `ERR-CAN-04` | `ca6TipoInexistente_ERR_CAN_04` | Caso de uso |
| CA7 | Precio 0, negativo o 0.004: `ERR-CAN-05` | `ca7PrecioMenorOIgualACero_ERR_CAN_05` | Dominio |
| CA8 | Una reserva hecha antes del cambio conserva su precio | `ca8CambiarPrecio_noCambiaElPrecioDeLasReservas` (inserta la reserva con SQL, P2) | Persistencia |
| CA8 | Las reservas nuevas toman el precio nuevo | Lo verifica HU21 al crear la reserva con `precioHora` de la cancha (P2) | — |

Las pruebas siguen el estilo del módulo: `ca1Xxx`, con el criterio al principio del nombre.

## Decisiones tomadas

**P1 · Ruta del servicio (aprobada el 19-nov-2026: opción A)**
`PUT /api/canchas/{id}`: el escenario sale de la cancha y en la ruta no aparece algo que no se puede cambiar. HU10 usaría `PATCH /api/canchas/{id}/estado`. Se descartó `PUT /api/escenarios/{escenarioId}/canchas/{id}` (B), que obliga a revisar que la cancha sea de ese escenario.

**P2 · Cómo probar CA8 sin reservas (aprobada el 19-nov-2026: opción A)**
Prueba de persistencia que inserta una fila en `reserva` con SQL (con un jugador de prueba), cambia el precio de la cancha y revisa que `reserva.precio` siga igual. La parte «las reservas nuevas toman el precio actualizado» queda como fila de la tabla CA → pruebas de HU21. Se descartó dejar CA8 solo «por diseño» (B).

**P3 · El administrador del sistema no puede modificar (aprobada el 19-nov-2026)**
Igual que HU8 (P3): la política 2 se aplica tal cual, solo el propietario, y el administrador del sistema recibe `ERR-CAN-02`.

## Fuera de alcance

- Revisar al escribir si el nombre está libre al modificar (HU6 tiene `nombre-disponible` para el registro; HU9 no pide validar al perder el foco).
- Cambiar el estado de la cancha (HU10) o moverla a otro escenario.
- Una consulta aparte de una sola cancha: la lista de HU6 ya trae todos los datos del formulario.
- La pantalla.
