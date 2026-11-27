---
name: especificar
description: Escribe la especificación técnica de una historia de usuario de CanchaYa (specs/HUnn-<nombre>/spec.md y tareas.md) a partir de la HU, el modelo de dominio enriquecido, los catálogos de mensajes y parámetros, las decisiones D01–D39, los prototipos de pantalla y el código existente. Cubre el backend, la pantalla o los dos; si el backend ya está implementado, especifica solo la pantalla y toma el contrato del código real. Úsalo cuando el usuario pida especificar, preparar o planear una HU antes de implementarla, o invoque /especificar HUnn.
---

# Skill: especificar

Prepara la especificación de una HU para que el desarrollador la apruebe **antes** de escribir código. No escribe código de la aplicación. Todo va en español de Colombia y nunca usa el término "MVP".

## Argumentos

- Número de la HU (obligatorio), por ejemplo `HU19`. Si no viene, pregúntalo.
- Alcance (opcional): `completa` (backend y pantalla), `backend` o `pantalla`. Si no viene, se decide en el paso 0.

## Pasos

### 0. Decidir el alcance

Revisa si el backend de la HU ya está implementado: la tabla «Estado de las especificaciones» de `specs/README.md`, una carpeta `specs/HUnn-*/` y el código (`projects/api/src/main/java/co/edu/uco/canchaya/<modulo>/`, el controlador y sus pruebas con `CAn` en el nombre).

| Situación | Alcance | Qué se escribe |
|---|---|---|
| Ni backend ni pantalla | `completa` | Todas las secciones de la plantilla |
| Backend implementado, con `spec.md` | `pantalla` | Se agrega la sección «Pantalla» al `spec.md` existente, las filas de capa «Pantalla» a la tabla de criterios y las tareas de pantalla a `tareas.md` |
| Backend implementado, sin `spec.md` (HU hechas antes de `specs/`) | `pantalla` | Un `spec.md` nuevo que dice en su encabezado «solo la pantalla», con el contrato **tomado del código** |
| La HU no tiene pantalla (solo correos o tareas programadas, ver `docs/prototipos/README.md`) | `backend` | Sin sección «Pantalla» |

Si no está claro, pregunta antes de seguir. En el chat, di qué alcance elegiste y por qué.

### 1. Leer las fuentes

Desde la raíz del repositorio:

1. `docs/historias/HUnn.md`: tarjeta, conversación y criterios de aceptación.
2. Las hojas de `docs/dominio/` que cita el «Requisito de información», sobre todo las responsabilidades y las políticas nombradas.
3. Cada código de mensaje de la HU en `docs/especificacion/mensajes.md` y cada `PAR-*` en `docs/especificacion/parametros.md`.
4. `docs/especificacion/aspectos-generales.md` (por ejemplo, AG-11 dice qué rutas no exigen sesión; AG-21, que todo funciona en celular y computador).
5. Las decisiones D01–D39 relacionadas (`docs/decisiones.md`).
6. Las clases y los componentes del módulo en `docs/diseno/clases.md` y `docs/diseno/componentes.md`, y las tablas en `docs/diseno/entidad-relacion.md`.
7. El código que ya existe en el módulo (`projects/api/src/main/java/co/edu/uco/canchaya/<modulo>/`): puertos, casos de uso, controladores, `ManejadorErrores` (códigos HTTP por mensaje), `ConfiguracionSeguridad` (rutas públicas) y `catalogo-mensajes.properties`. La especificación reutiliza lo que hay; no duplica.
8. Las especificaciones ya aprobadas en `specs/`, para mantener el mismo estilo de contrato.
9. Si `specs/README.md` tiene la HU en «Pendientes para especificaciones futuras», la propuesta es el punto de partida, sus preguntas pasan a las «Preguntas abiertas» y la fila se borra de ahí.

Si el alcance incluye la pantalla, además:

10. El prototipo en `docs/prototipos/README.md`: busca la fila de la HU y **mira las dos imágenes** (celular y computador) con la herramienta de lectura. Anota los campos, botones, mensajes (cada uno trae su código) y estados que muestra.
11. `projects/web/AGENTS.md` (reglas del frontend), `src/app/app.routes.ts` (rutas que ya existen), `src/app/core/` (`MensajesService`, `modelos.ts`) y las pantallas que ya existen, para reutilizarlas.

### 2. Revisar antes de diseñar

Anota como **pregunta abierta**, sin resolverla por tu cuenta:

- Toda contradicción entre documentos, o entre un documento y las decisiones (regla 3 de `AGENTS.md`).
- Todo parámetro «Por definir» (regla 7).
- Toda dependencia entre módulos que no permita la tabla de módulos de `AGENTS.md`.
- Todo paquete o carpeta que haría falta y no esté en la estructura de D39 o de `projects/web/AGENTS.md`.
- Todo mensaje que la HU usa y no está en `catalogo-mensajes.properties` (esto es una tarea, no una pregunta).

Si el alcance incluye la pantalla, también:

- Toda diferencia entre el prototipo y la HU, los mensajes o el contrato del backend (un campo que el prototipo muestra y el servicio no devuelve, un mensaje con otro código…). Mandan la HU y las decisiones; el prototipo se reporta.
- Toda regla de negocio que la pantalla tendría que decidir. No va en el frontend (RT-01): la decide el backend y la pantalla muestra lo que responde.
- Todo `{campo}` o `{formatoEsperado}` que la pantalla necesita para validar antes de enviar y no está en `catalogo-mensajes.json` (secciones `campos` y `formatos`).

Cada pregunta lleva opciones, una recomendación y si **bloquea** o no la implementación.

### 3. Escribir los archivos

Crea o completa `specs/HUnn-<nombre-corto>/spec.md` y `tareas.md` con las plantillas de `specs/README.md`, con estado **Borrador** (si el `spec.md` ya existe, el estado de la parte nueva va aparte, por ejemplo `Implementada (backend) · Borrador (pantalla)`):

- **Contrato**:
  - Si el backend se va a construir, sigue las convenciones del código existente (rutas `/api/...`, respuestas con `codigo` y `mensaje`, errores con `RespuestaError`, HTTP 404/403/422 según `ManejadorErrores`).
  - Si el backend **ya está implementado**, el contrato se copia del código real, no se inventa: ruta y método del controlador, campos de la solicitud y de la respuesta (records `Solicitud*` y `Respuesta*`), validaciones con su código (`@NotBlank(message = "ERR-GEN-01")`…), códigos HTTP de `ManejadorErrores` y si la ruta es pública en `ConfiguracionSeguridad`. Cita el archivo de cada dato (`ruta/Archivo.java`). Si el código no coincide con la HU, es una pregunta abierta, no una corrección.
- **Diseño por capas**: cada política del dominio se asigna a una clase y a una prueba. En alcance `pantalla` esta sección dice «No cambia: el backend ya está» y enlaza el código.
- **Pantalla** (si el alcance la incluye): prototipo que se sigue, ruta, componentes y servicio, cómo se muestran los mensajes y qué CA se verifica en la pantalla, con su prueba. Las validaciones del formulario solo guían al usuario (AC-03.1) y sus textos salen de `MensajesService`; los errores del backend se muestran con el `mensaje` de la `RespuestaError` tal como llega.
- **Criterios → pruebas**: cada `CAn` tiene al menos una fila. La capa puede ser Dominio, Caso de uso, Persistencia, REST, Punta a punta o **Pantalla**. Un criterio que el usuario ve (un mensaje, un botón deshabilitado, una lista) lleva también su fila «Pantalla» con la prueba de Vitest (`it('CAn: …')`, con `HttpTestingController` en lugar del backend). Si un criterio solo se puede verificar en la pantalla, dilo y señala qué dato del backend lo hace posible.
- **Tareas**: pequeñas, en el orden dominio → caso de uso → persistencia → REST → pantalla, y cada una cita su CA o su política. Las de pantalla van al final; en alcance `pantalla` son las únicas, más la verificación.
- Agrega o actualiza la fila de la HU en la tabla «Estado de las especificaciones» de `specs/README.md`.

### 4. Presentar

Resume en el chat, en lenguaje sencillo: qué alcance se especificó, qué se construye, el contrato en una línea (y, si se tomó del código, de qué archivos), la pantalla que se sigue y las preguntas abiertas que bloquean. Termina pidiendo que se respondan las preguntas y se apruebe la especificación. **No empieces a implementar.**
