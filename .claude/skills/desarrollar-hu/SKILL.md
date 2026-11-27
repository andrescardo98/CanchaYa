---
name: desarrollar-hu
description: Lleva una historia de usuario de CanchaYa de principio a fin — rama, especificación (spec.md y tareas.md con el skill especificar), aprobación, implementación por tareas con pruebas de cada CA (backend y pantalla), verificación y pull request — con puntos de parada para que el desarrollador apruebe cada fase. Úsalo cuando el usuario pida desarrollar, implementar o trabajar una HU completa o su pantalla, o invoque /desarrollar-hu HUnn.
---

# Skill: desarrollar-hu

Guía el desarrollo completo de una HU siguiendo el `AGENTS.md` de la raíz, el de `projects/api`, el de `projects/web` y el flujo de `specs/README.md`. Todo va en español de Colombia y nunca usa el término "MVP".

Trabaja por fases y **detente en cada punto de parada (⛔)** hasta que el desarrollador responda. El desarrollador debe entender y poder sustentar todo el código: por eso las paradas no se saltan aunque el cambio parezca pequeño.

## Argumentos

- Número de la HU (obligatorio), por ejemplo `HU22`. Si no viene, pregúntalo.
- Alcance (opcional): `completa`, `backend` o `pantalla`, como en el skill `especificar`.

## Fase 0 · Preparación

1. Revisa si ya existe `specs/HUnn-*/`. Si existe, di en qué estado está cada parte (Borrador, Aprobada o Implementada, del backend y de la pantalla) y qué tareas de `tareas.md` faltan, y sigue desde la fase que corresponda. Si ya hay una rama de la HU, cámbiate a ella.
2. Si no hay rama: verifica que estás en `main` actualizado y sin cambios pendientes, y crea la rama `feat/HUnn-<descripcion-corta>` (o `fix/...` si es una corrección). Si solo se hace la pantalla, la descripción lo dice (`feat/HU1-pantalla-registro`).
3. Revisa si la HU depende de otras HU que aún no están implementadas (la tabla «Estado de las especificaciones» de `specs/README.md` y el código). Una pantalla depende de que su backend esté implementado. Si depende de algo que falta, avisa antes de seguir.

## Fase 1 · Especificación (no se escribe código de la aplicación)

4. Sigue los pasos del skill `especificar` (`.claude/skills/especificar/SKILL.md`) para HUnn: decide el alcance, lee todas sus fuentes y escribe o completa `specs/HUnn-<nombre>/spec.md` y `tareas.md` con estado **Borrador**, y actualiza la fila en `specs/README.md`.
5. Además de lo que pide ese skill, revisa en especial:
   - Que cada `CAn` tenga al menos una prueba en la tabla «Criterios → pruebas», y que cada política del dominio tenga su clase y su prueba.
   - Que las dependencias entre módulos estén permitidas y que no hagan falta paquetes fuera de D39.
   - Que los mensajes salgan del catálogo y los parámetros de la configuración. Si falta un mensaje en `catalogo-mensajes.properties`, es una tarea; si un `PAR-*` está «Por definir», es una pregunta.
   - Si la HU cambia la base: la migración nueva `V<n>__<descripcion>.sql` (nunca se edita una que ya existe).
   - Si la HU toca reservas o estados que pueden cambiar al mismo tiempo: cómo se cumplen D29 y D36.
   - Si la HU envía correos: que se envíen después de guardar y que, si fallan, no se revierta nada (D15).
   - Que fechas y horas se calculen en `America/Bogota` (D27), también al mostrarlas en la pantalla.
   - Si hay pantalla: que siga el prototipo en celular y en computador (AG-21), que no decida reglas de negocio (RT-01) y que el contrato que usa coincida con el del backend (si ya está implementado, con el código real).
6. Resume en lenguaje sencillo: qué se construye, el contrato en una línea, la pantalla, las tareas y las **preguntas abiertas** (cada una con opciones, tu recomendación y si bloquea o no).

⛔ **Parada 1 · Preguntas.** Espera las respuestas. Con ellas, actualiza `spec.md` (quita las preguntas resueltas o anota lo que se decidió) y muestra qué cambió. Las contradicciones con `docs/` se reportan para corregirlas en el Excel de origen: no se edita `docs/`.

⛔ **Parada 2 · Aprobación.** No implementes nada hasta que el desarrollador diga que la especificación está aprobada. Entonces cambia el estado a **Aprobada** en `spec.md` y en `specs/README.md`.

## Fase 2 · Implementación (una tarea de `tareas.md` a la vez)

Para cada tarea, en orden (dominio → caso de uso → persistencia → REST → pantalla; las de pantalla siempre van al final, cuando el servicio REST ya existe):

7. Primero escribe la prueba del CA o de la política, con su código (por ejemplo `CA3`) en el nombre o en un comentario. Luego escribe el código mínimo que la hace pasar.
8. Corre las pruebas que correspondan y muestra el resultado real. Si fallan, dilo y corrige; no lo ocultes.
   - Backend, desde `projects/api`: `./mvnw test -Dtest=...`.
   - Pantalla, desde `projects/web`: `npx ng test --watch=false`.
9. Marca la tarea `[x]` en `tareas.md` y explica en lenguaje sencillo qué hace, qué problema evita (el término técnico entre paréntesis), qué archivos tocaste y por qué. En las tareas de pantalla, compara cada pieza de Angular con su equivalente en Spring cuando ayude a entenderla.
10. Haz un commit por tarea con Conventional Commits en español y el pie `Refs: HUnn`.

⛔ **Parada 3 · Siguiente tarea.** Después de cada tarea, espera que el desarrollador diga que sigas antes de pasar a la otra.

Reglas durante la implementación:

- Si algo no coincide con la especificación, detente: primero propón el cambio a `spec.md` y, cuando el desarrollador lo acepte, corrige el código.
- No agregues nada que la HU no pida. Si ves algo que falta o sobra, coméntalo y anótalo en «Fuera de alcance».
- El dominio no depende de Spring ni de JPA. Entre módulos solo se usan los puertos de entrada. Si ArchUnit falla, se corrige el código, no la regla.
- No escribas textos para el usuario ni valores de parámetros fijos en el código. En la pantalla, los textos de mensajes salen de `MensajesService` o del `mensaje` que responde el backend, y las direcciones de `environment`.
- Ninguna regla de negocio va en el frontend (RT-01): la pantalla solo muestra lo que responde el backend.

## Fase 3 · Verificación y pull request

11. Corre la verificación completa de cada proyecto que tocó la rama y muestra el resultado:
    - Si tocó `projects/api`: `./mvnw verify` (con Docker encendido).
    - Si tocó `projects/web`: desde `projects/web`, `npx ng lint`, `npx ng test --watch=false` y `npx ng build`. Son los mismos pasos de la CI (`.github/workflows/frontend.yml`).
12. Revisa la tabla «Criterios → pruebas»: confirma que cada CA tiene una prueba que existe y pasa, y que `spec.md` describe lo que el código hace de verdad.
13. Cambia el estado a **Implementada** en `spec.md` y en `specs/README.md` (si la especificación tiene dos partes, el de la parte que se terminó).
14. Sigue el skill `pull-request` para llenar la plantilla, con el título en Conventional Commits.

⛔ **Parada 4 · Pull request.** Muestra el título y el cuerpo del PR. No hagas push ni abras el PR hasta que el desarrollador lo pida.
