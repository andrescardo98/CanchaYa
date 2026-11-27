---
name: pull-request
description: Llena la plantilla de pull request de CanchaYa (.github/pull_request_template.md) a partir de los cambios de la rama actual — contexto con la HU, tipo de cambio e instrucciones de prueba que verifican cada criterio de aceptación. Úsalo cuando el usuario pida llenar la plantilla, redactar o describir un pull request (PR), o invoque /pull-request.
---

# Skill: pull-request

Genera la descripción de un pull request de CanchaYa llenando **todas** las secciones de la plantilla `.github/pull_request_template.md`. El texto va en español de Colombia y nunca usa el término "MVP".

## Argumentos opcionales

- Rama base (por defecto `main`).
- Número de la HU, si no se puede deducir del nombre de la rama (p. ej. `HU21`).

## Pasos

### 1. Reunir el contexto del cambio

Ubica la raíz del repositorio con `git rev-parse --show-toplevel` y ejecuta desde allí:

```bash
git branch --show-current
git log --oneline <base>..HEAD
git diff --stat <base>...HEAD
git diff <base>...HEAD
```

Si hay cambios sin confirmar, avisa al usuario: el pull request solo incluye lo que está en commits.

Revisa que los mensajes de commit sigan Conventional Commits (sección «Mensajes de commit» de `AGENTS.md`). Si alguno no lo cumple, avísale al usuario y no lo corrijas por tu cuenta.

### 2. Identificar la historia de usuario

1. Busca el código `HU<nn>` en el nombre de la rama (`<tipo>/HU<nn>-<descripcion>`), luego en el cuerpo o el pie de los commits (`Refs: HU<nn>`).
2. Si no aparece, pregúntaselo al usuario. Si el cambio no corresponde a una HU (infraestructura, documentación), escribe `No aplica` y explica el motivo en el contexto.
3. Obtén el título, el release y los **criterios de aceptación** de la HU. Fuentes, en este orden:
   - Lo que el usuario indique en la conversación.
   - La hoja `HU<nn>` del Tallaje del producto (backlog oficial) o la especificación funcional, si el usuario da la ruta.
   - Si no tienes acceso a los criterios, pídelos. No los inventes.
4. El sprint sale del plan de liberaciones (decisión D25):
   - Sprint 1 (3–6 nov): HU1–HU7 y HU13.
   - Sprint 2 (9–13 nov): HU18–HU21.
   - Sprint 3 (17–20 nov): HU8–HU12, HU22–HU25 y HU29.
   - Las demás HU son Release 2 o Release 3, sin sprint asignado.

### 3. Llenar la plantilla

Lee `.github/pull_request_template.md` y respeta sus encabezados y su orden. Elimina los comentarios HTML (`<!-- -->`) en la versión final.

**Contexto**
- Línea de HU con número y título; línea de release/sprint.
- Uno o dos párrafos sobre el problema que se resuelve y por qué, en lenguaje de negocio.
- Resumen de cambios: viñetas agrupadas por capa o módulo (dominio, aplicación, infraestructura, API, frontend, base de datos). Cita archivos solo cuando aporte claridad.
- Si el cambio aplica una decisión (Dnn) o una regla de negocio (RN-nn), menciónala.

**Tipo de cambio**
- Marca con `[x]` cada opción cuyo tipo aparezca en los commits de la rama. Cada opción de la plantilla corresponde a un tipo de Conventional Commits (`feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `style`, `build`/`ci`/`chore`).
- Marca «Cambio que rompe compatibilidad» si algún commit lleva `!` o el pie `BREAKING CHANGE:`, y explícalo en el contexto.
- Contrasta los tipos con el diff: si un commit `docs` cambia código, o hay cambios en contratos de API o migraciones sin `!`, avísale al usuario.

**Instrucciones de prueba** — es la sección más importante: otro agente la va a seguir para verificar el pull request.
- **Preparación**: comandos exactos para levantar lo necesario (base de datos, backend, frontend), datos de prueba y variables de entorno. Usa los comandos reales del proyecto (revisa `pom.xml`, `package.json`, `docker-compose.yml` y «Cómo ejecutar en local» del `README.md`); no supongas.
  - Si el diff toca `projects/web/`, incluye levantar el frontend: `docker compose up -d` desde la raíz, el backend con `./mvnw spring-boot:run` desde `projects/api` (la pantalla lo llama por el proxy de `proxy.conf.json`) y, en otra terminal, desde `projects/web`, `npm install` (la primera vez o si cambió `package-lock.json`) y `npx ng serve`; la pantalla queda en <http://localhost:4200>. Di con qué usuario se entra si la pantalla exige sesión.
  - Si el cambio del frontend no tiene pantalla que se pueda abrir (un servicio, una configuración), basta con las pruebas automatizadas; dilo.
- **Verificación de criterios de aceptación**: una fila por criterio de la HU (`CA1`, `CA2`, …), con el texto del criterio, pasos concretos y reproducibles (pantalla, endpoint, petición HTTP o comando) y el resultado esperado observable (mensaje del catálogo por su código, código HTTP, dato persistido, correo enviado).
  - Incluye los caminos de error que exige el criterio, con el código del mensaje (`ERR-RES-nn`, etc.).
  - Si un criterio no queda cubierto por este pull request, dilo en la fila («No cubierto: se implementa en …»).
  - Si el pull request trae la pantalla, los pasos de cada criterio se hacen en ella: qué ruta abrir (`http://localhost:4200/...`), qué escribir o pulsar y qué debe verse (el texto del mensaje con su código). Pide revisarla en tamaño celular (390 px de ancho, con las herramientas de desarrollo del navegador) y en computador (AG-21), comparándola con su prototipo de `docs/prototipos/`.
- **Pruebas automatizadas**: los comandos que ejecutan las pruebas que agrega o afecta el cambio: `./mvnw verify` desde `projects/api` si toca el backend; `npx ng lint`, `npx ng test --watch=false` y `npx ng build` desde `projects/web` si toca el frontend.
- **Fuera del alcance de esta verificación**: lo que no se probó y por qué, o «Nada».

### 4. Verificar la plantilla llena

- No queda ninguna sección vacía ni ningún comentario de la plantilla.
- Cada criterio de aceptación tiene al menos una fila en la tabla.
- Los pasos se pueden ejecutar tal cual, sin conocimiento previo.
- Las reglas citadas coinciden con las decisiones D01–D36 de `docs/decisiones.md` (por ejemplo, franjas de 1 hora en punto, reservas de máximo 2 franjas, cancelación con más de 3 horas, zona horaria America/Bogota).

El resultado es la plantilla llena, en Markdown, lista para pegar en la descripción del pull request.
