# HU8 · Modificar un escenario

**Estado:** Implementada (18-nov-2026) · **HU:** [HU8](../../docs/historias/HU8.md) · **Módulo:** Escenarios y canchas (`co.edu.uco.canchaya.escenarios`)

## Fuentes

- Dominio: [Escenario](../../docs/dominio/escenario.md), responsabilidad `modificar` (políticas 2, 3, 6, 7 y 13) y su tabla «Uso de cada atributo» (estado y administrador no se modifican).
- Mensajes: `EXI-ESC-02`, `ERR-ESC-02`, `ERR-ESC-03`, `ERR-ESC-06`, `ERR-ESC-07`, `ERR-ESC-13` y `ERR-GEN-01` a `ERR-GEN-04` ([mensajes.md](../../docs/especificacion/mensajes.md)).
- Parámetros: `PAR-ESC-01` (5 fotos), `PAR-ESC-02` (2 MB) y `PAR-ESC-03` (JPG, PNG), ya leídos de `canchaya.escenario.*` en `application.properties`.
- Aspectos generales: AG-03 (todos los errores a la vez), AG-04 (después de modificar, el formulario muestra los datos actualizados), AG-06 (espacios sobrantes) y AG-12 (propiedad en el servidor).
- Decisiones: D13 (fotos), D14 (ubicación en el mapa), D31 (Supabase Storage) y D39 (paquetes).
- Diseño: `Escenario.modificar(nombre, descripcion, direccion, ciudadId, latitud, longitud, telefonoContacto, correoContacto, fotos, solicitante, maximoFotos)` en [clases.md](../../docs/diseno/clases.md), que lanza ERR-ESC-07, ERR-ESC-03 y ERR-ESC-13; el caso de uso revisa antes ERR-ESC-06 y ERR-ESC-02.

## Qué se construye

El propietario cambia los datos de su escenario: nombre, descripción, dirección, ciudad, ubicación, contacto y fotos. El estado y el propietario no cambian. Para que el formulario se pueda llenar con los datos actuales, se agrega también la consulta del escenario para su propietario. La validación al perder el foco (CA7) es de la pantalla: usa las mismas reglas y los mismos códigos que devuelve el servidor.

## Contrato del servicio

### Consultar el escenario para editarlo

`GET /api/escenarios/{id}` · con sesión · solo el propietario

Respuesta `200`:

```json
{
  "id": "uuid",
  "nombre": "Canchas La 70",
  "descripcion": "texto o null",
  "direccion": "Cra 70 # 10-20",
  "ciudadId": "uuid",
  "latitud": 6.155123,
  "longitud": -75.374456,
  "telefonoContacto": "+576045551234",
  "correoContacto": "contacto@la70.com o null",
  "fotos": ["https://.../1.jpg"],
  "estado": "INACTIVO"
}
```

`estado` se devuelve solo para mostrarlo; no se puede cambiar desde aquí. No se devuelve `administradorId`.

### Modificar

`PUT /api/escenarios/{id}` · con sesión · solo el propietario · `multipart/form-data`, igual que el registro (HU5):

- Parte `datos` (JSON): los mismos campos y reglas de `SolicitudRegistroEscenario` (nombre 1–100, descripción 0–500, dirección 1–200, ciudad, latitud, longitud, teléfono hasta 15 con indicativo, correo opcional 5–150) más `fotosConservadas`: la lista de URL de las fotos actuales que se quedan, en el orden en que se muestran. Es obligatoria: si no viene, 400 `ERR-GEN-01` y no se guarda nada (así un descuido no borra las fotos). La lista vacía `[]` sí es válida y quita todas las fotos actuales. Ver P1.
- Parte `fotos` (archivos, opcional): las fotos nuevas. Van después de las conservadas.

Respuesta `200`: `{ "codigo": "EXI-ESC-02", "mensaje": "El escenario se modificó correctamente.", "escenario": { ...la misma forma de GET... } }`. Con el escenario completo la pantalla muestra los datos guardados (AG-04).

| HTTP | Código | Cuándo |
|---|---|---|
| 200 | `EXI-ESC-02` | Se guardó |
| 400 | `ERR-GEN-01` a `ERR-GEN-04` | Datos inválidos, todos a la vez (`ManejadorErrores`, AG-03) |
| 400 | `ERR-GEN-07` | El `id` no es UUID, o el JSON no se puede leer |
| 403 | `ERR-ESC-07` | No es el propietario, incluido el encargado de sede (**entrada nueva** en `ManejadorErrores`) |
| 404 | `ERR-ESC-06` | No existe el escenario (ya está en `ManejadorErrores`) |
| 422 | `ERR-ESC-02` | La ciudad no existe |
| 422 | `ERR-ESC-03` | Latitud o longitud por fuera de rango |
| 422 | `ERR-ESC-13` | Más de 5 fotos en total, o una foto nueva que no es JPG ni PNG o pesa más de 2 MB |

El GET usa las mismas filas 400, 403 y 404.

## Diseño por capas

| Capa | Pieza | Qué hace |
|---|---|---|
| Dominio | `Escenario.modificar(...)` con la firma de `clases.md` | Política 7 (`ERR-ESC-07`: rol ADMINISTRADOR_CANCHA y `esPropietario`), política 3 (`validarUbicacion`), política 13 (`validarCantidadFotos`). Recorta textos y redondea coordenadas igual que `registrar`. Los atributos que cambia dejan de ser `final`; `id`, `administradorId` y `estado` no los toca |
| Dominio | `Escenario.validarPropietario(Solicitante)` (nuevo, público) | La política 7 sola, para que el caso de uso la revise **antes** de subir fotos, igual que `validarSolicitante` en HU5. La usa también la consulta del GET |
| Puerto de entrada | `ModificarEscenario` con el record `DatosModificacionEscenario` (campos + `fotosConservadas` + `fotosNuevas: List<FotoAdjunta>`, reutilizando `RegistrarEscenario.FotoAdjunta`) | Contrato del caso de uso |
| Puerto de entrada | `ConsultarEscenarioPropietario` | El GET |
| Caso de uso | `ModificarEscenarioCasoUso` (sin `@Transactional`, como HU5) | Orden: política 6 (`buscarPorId`, ERR-ESC-06) → 7 → 3 → 2 (`ConsultaUbicacion.existeCiudad`) → que cada `fotosConservadas` sea una foto actual (P1) → 13 (cantidad total, y formato y peso de cada nueva) → subir las nuevas → `escenario.modificar(...)` → `actualizar`. Si algo falla después de subir, borra las nuevas. Después de guardar, borra del almacenamiento las fotos que se quitaron (si falla se registra y no se revierte, como `AlmacenamientoFotos.borrar`) |
| Caso de uso | `ValidacionFotos` (nuevo, del paquete `usecase`) | Se saca de `RegistrarEscenarioCasoUso` la revisión de formato y peso y la subida, para que HU5 y HU8 usen la misma. `RegistrarEscenarioCasoUso` queda igual por fuera; sus pruebas no cambian |
| Caso de uso | `ConsultarEscenarioPropietarioCasoUso` | ERR-ESC-06 → `validarPropietario` → devuelve el `Escenario` |
| Puerto de salida | `RepositorioEscenarios.actualizar(Escenario)` | Guarda los datos y reemplaza las fotos en una transacción. `guardar` no sirve: hace INSERT (`isNew`) |
| Persistencia | `RepositorioEscenariosJpa.actualizar` | Carga la entidad, cambia los campos, borra las fotos y hace `flush` **antes** de agregar las nuevas: Hibernate hace los INSERT antes que los DELETE y chocaría con `uq_foto_escenario_orden (escenario_id, orden)` |
| REST | `GET /{id}` y `PUT /{id}` en `EscenarioController`; `SolicitudModificarEscenario`, `RespuestaEscenario` y `RespuestaModificarEscenario` | `ERR-ESC-07 → 403` en `ManejadorErrores` |
| Transversal | `EXI-ESC-02` y `ERR-ESC-07` en `catalogo-mensajes.properties` | Hoy no están |

Sin migraciones: las tablas `escenario` y `foto_escenario` ya tienen lo necesario.

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1 | Cambia los datos, recorta espacios y conserva id, propietario y estado | `CA1_modificar_cambiaLosDatosYConservaEstadoYPropietario` | Dominio |
| CA1 | Se guarda; las fotos conservadas siguen, las nuevas se suben y las quitadas se borran después de guardar | `CA1_modificar_guardaYReemplazaLasFotos` | Caso de uso |
| CA1 | Al reemplazar fotos no choca el orden (`uq_foto_escenario_orden`) | `CA1_actualizar_reemplazaFotosSinChocarElOrden` | Persistencia |
| CA1 | 200 con `EXI-ESC-02` y el escenario guardado; el perfil público (HU19) muestra el cambio | `CA1_modificarEscenario_200_EXI_ESC_02_yPerfilActualizado` | Punta a punta |
| CA1 | El GET devuelve los datos actuales para llenar el formulario | `CA1_consultarParaEditar_devuelveLosDatosActuales` | REST + caso de uso |
| CA2 | Campos vacíos, mal escritos o muy largos: 400 con todos los ERR-GEN y nada cambia | `CA2_datosInvalidos_400_conTodosLosErrores` | REST |
| CA3 | No existe: 404 `ERR-ESC-06`, no se sube ninguna foto | `CA3_escenarioInexistente_404_ERR_ESC_06` | Caso de uso + REST |
| CA4 | Otro administrador de cancha y un encargado de sede: 403 `ERR-ESC-07`, sin subir fotos | `CA4_noPropietario_ERR_ESC_07` y `CA4_encargadoDeSede_ERR_ESC_07` | Dominio + caso de uso |
| CA4 | El GET de otro usuario: 403 `ERR-ESC-07` | `CA4_consultarParaEditar_noPropietario_403` | REST |
| CA5 | Ciudad inexistente: `ERR-ESC-02` | `CA5_ciudadInexistente_ERR_ESC_02` | Caso de uso |
| CA6 | Latitud 91 o longitud -181: `ERR-ESC-03` | `CA6_ubicacionFueraDeRango_ERR_ESC_03` | Dominio |
| CA7 | Validación al perder el foco | Es de la pantalla. El backend devuelve el `campo` y el código de cada error (CA2) | Frontend |
| CA8 | 3 conservadas + 3 nuevas = 6: `ERR-ESC-13` «se adjuntaron más de 5 fotos», sin subir | `CA8_masDeCincoFotosEnTotal_ERR_ESC_13` | Dominio + caso de uso |
| CA9 | Foto nueva que no es JPG ni PNG: `ERR-ESC-13` con el archivo | `CA9_fotoNoJpgNiPng_ERR_ESC_13` | Caso de uso |
| CA10 | Foto nueva de más de 2 MB: `ERR-ESC-13` con el archivo | `CA10_fotoDeMasDe2MB_ERR_ESC_13` | Caso de uso |

En el código las pruebas siguen el estilo del módulo, `ca1Xxx` en lugar de `CA1_xxx`: el criterio va al principio del nombre. En el dominio quedaron en `EscenarioModificarTest` (T1).

## Decisiones tomadas

**P1 · Cómo se modifican las fotos (aprobada el 18-nov-2026: opción A)**
La parte `datos` trae `fotosConservadas` (URL que se quedan, en orden) y la parte `fotos` trae las nuevas. Resultado: conservadas + nuevas, máximo 5 en total. Las que no vienen en `fotosConservadas` se borran del almacenamiento después de guardar. Una URL de `fotosConservadas` que no es foto actual del escenario, o que viene repetida, es una petición manipulada (el formulario nunca la envía): se registra en el log y se responde 400 `ERR-GEN-07` sin guardar nada. El caso de uso lanza `ReglaNegocioException("ERR-GEN-07")` y `ManejadorErrores` lo responde con 400 (entrada nueva en T6). Se descartaron reenviar todas las fotos como archivos (B) y los servicios aparte para agregar y quitar (C).

**P2 · Consulta del escenario para el formulario (aprobada el 18-nov-2026)**
Se incluye `GET /api/escenarios/{id}` en esta HU (la ruta que HU19 dejó libre, su P4): `/perfil` solo sirve escenarios ACTIVOS y un escenario recién registrado está INACTIVO.

**P3 · El administrador del sistema no puede modificar (aprobada el 18-nov-2026)**
La política 7 se aplica tal cual: solo el propietario. El administrador del sistema recibe `ERR-ESC-07`.

## Preguntas abiertas

Ninguna.

## Fuera de alcance

- La lista «mis escenarios» del propietario (para llegar al formulario): no la pide esta HU.
- Cambiar el estado (HU13) o el propietario.
- Volver a revisar las fotos ya conservadas (ya cumplieron las reglas al subirse).
- La pantalla y la validación al perder el foco (CA7).
