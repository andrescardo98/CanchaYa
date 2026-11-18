---
name: especificar
description: Escribe la especificación técnica de una historia de usuario de CanchaYa (specs/HUnn-<nombre>/spec.md y tareas.md) a partir de la HU, el modelo de dominio enriquecido, los catálogos de mensajes y parámetros, las decisiones D01–D39 y el código existente. Úsalo cuando el usuario pida especificar, preparar o planear una HU antes de implementarla, o invoque /especificar HUnn.
---

# Skill: especificar

Prepara la especificación de una HU para que el desarrollador la apruebe **antes** de escribir código. No escribe código de la aplicación. Todo va en español de Colombia y nunca usa el término "MVP".

## Argumentos

- Número de la HU (obligatorio), por ejemplo `HU19`. Si no viene, pregúntalo.

## Pasos

### 1. Leer las fuentes

Desde la raíz del repositorio:

1. `docs/historias/HUnn.md`: tarjeta, conversación y criterios de aceptación.
2. Las hojas de `docs/dominio/` que cita el «Requisito de información», sobre todo las responsabilidades y las políticas nombradas.
3. Cada código de mensaje de la HU en `docs/especificacion/mensajes.md` y cada `PAR-*` en `docs/especificacion/parametros.md`.
4. `docs/especificacion/aspectos-generales.md` (por ejemplo, AG-11 dice qué rutas no exigen sesión).
5. Las decisiones D01–D39 relacionadas (`docs/decisiones.md`).
6. Las clases y los componentes del módulo en `docs/diseno/clases.md` y `docs/diseno/componentes.md`, y las tablas en `docs/diseno/entidad-relacion.md`.
7. El código que ya existe en el módulo (`projects/api/src/main/java/co/edu/uco/canchaya/<modulo>/`): puertos, casos de uso, controladores, `ManejadorErrores` (códigos HTTP por mensaje), `ConfiguracionSeguridad` (rutas públicas) y `catalogo-mensajes.properties`. La especificación reutiliza lo que hay; no duplica.
8. Las especificaciones ya aprobadas en `specs/`, para mantener el mismo estilo de contrato.

### 2. Revisar antes de diseñar

Anota como **pregunta abierta**, sin resolverla por tu cuenta:

- Toda contradicción entre documentos, o entre un documento y las decisiones (regla 3 de `AGENTS.md`).
- Todo parámetro «Por definir» (regla 7).
- Toda dependencia entre módulos que no permita la tabla de módulos de `AGENTS.md`.
- Todo paquete que haría falta y no esté en la estructura de D39.
- Todo mensaje que la HU usa y no está en `catalogo-mensajes.properties` (esto es una tarea, no una pregunta).

Cada pregunta lleva opciones, una recomendación y si **bloquea** o no la implementación.

### 3. Escribir los archivos

Crea `specs/HUnn-<nombre-corto>/spec.md` y `tareas.md` con las plantillas de `specs/README.md`, con estado **Borrador**:

- **Contrato**: sigue las convenciones del código existente (rutas `/api/...`, respuestas con `codigo` y `mensaje`, errores con `RespuestaError`, HTTP 404/403/422 según `ManejadorErrores`).
- **Criterios → pruebas**: cada `CAn` tiene al menos una fila. Si un criterio solo se puede verificar en la pantalla, dilo y señala qué dato del backend lo hace posible.
- **Diseño por capas**: cada política del dominio se asigna a una clase y a una prueba.
- **Tareas**: pequeñas, en el orden dominio → caso de uso → persistencia → REST, y cada una cita su CA o su política.
- Agrega la fila de la HU en la tabla «Estado de las especificaciones» de `specs/README.md`.

### 4. Presentar

Resume en el chat, en lenguaje sencillo: qué se construye, el contrato en una línea y las preguntas abiertas que bloquean. Termina pidiendo que se respondan las preguntas y se apruebe la especificación. **No empieces a implementar.**
