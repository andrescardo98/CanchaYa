---
name: pull-request
description: Llena la plantilla de pull request de CanchaYa (.github/pull_request_template.md) a partir de los cambios de la rama actual — contexto con la HU, tipo de cambio e instrucciones de prueba que verifican cada criterio de aceptación. Úsalo cuando el usuario pida crear, preparar, redactar o describir un pull request (PR), o invoque /pull-request.
---

# Skill: pull-request

Genera la descripción de un pull request de CanchaYa llenando **todas** las secciones de la plantilla `.github/pull_request_template.md`. El texto va en español de Colombia y nunca usa el término "MVP".

## Argumentos opcionales

- Rama base (por defecto `main`).
- Número de la HU, si no se puede deducir del nombre de la rama (p. ej. `HU21`).
- `--crear`: además de redactar, crea el pull request con `gh` (solo si el usuario lo pide).

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

### 2. Identificar la historia de usuario

1. Busca el código `HU<nn>` en el nombre de la rama, luego en los mensajes de commit.
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
- Marca con `[x]` cada opción que corresponda según el diff. Pistas:
  - Archivos nuevos de funcionalidad → Nueva funcionalidad.
  - Solo pruebas → Pruebas.
  - Solo `.md` o documentación → Documentación.
  - `Dockerfile`, `docker-compose`, `.github/workflows`, configuración → Infraestructura.
  - Cambios en contratos de API, migraciones que alteran columnas o datos existentes → Cambio que rompe compatibilidad (explícalo en el contexto).

**Instrucciones de prueba** — es la sección más importante: otro agente la va a seguir para verificar el pull request.
- **Preparación**: comandos exactos para levantar lo necesario (base de datos, backend, frontend), datos de prueba y variables de entorno. Usa los comandos reales del proyecto (revisa `pom.xml`, `package.json`, `docker-compose.yml`); no supongas.
- **Verificación de criterios de aceptación**: una fila por criterio de la HU (`CA1`, `CA2`, …), con el texto del criterio, pasos concretos y reproducibles (pantalla, endpoint, petición HTTP o comando) y el resultado esperado observable (mensaje del catálogo por su código, código HTTP, dato persistido, correo enviado).
  - Incluye los caminos de error que exige el criterio, con el código del mensaje (`ERR-RES-nn`, etc.).
  - Si un criterio no queda cubierto por este pull request, dilo en la fila («No cubierto: se implementa en …»).
- **Pruebas automatizadas**: los comandos que ejecutan las pruebas que agrega o afecta el cambio.
- **Fuera del alcance de esta verificación**: lo que no se probó y por qué, o «Nada».

### 4. Verificar antes de entregar

- No queda ninguna sección vacía ni ningún comentario de la plantilla.
- Cada criterio de aceptación tiene al menos una fila en la tabla.
- Los pasos se pueden ejecutar tal cual, sin conocimiento previo.
- Las reglas citadas coinciden con las decisiones D01–D27 (por ejemplo, franjas de 1 hora en punto, reservas de máximo 2 franjas, cancelación con más de 3 horas, zona horaria America/Bogota).

### 5. Entregar

- Por defecto, escribe la descripción en `.git/PR_BODY.md` (fuera del control de versiones) y muéstrala al usuario.
- Propón un título con el formato `HU<nn>: <descripción en infinitivo>` (p. ej. `HU21: Reservar una o dos franjas con control de concurrencia`).
- Solo si el usuario lo pide (o pasa `--crear`), crea el pull request como borrador:

```bash
git push -u origin HEAD
gh pr create --draft --base <base> --title "<título>" --body-file .git/PR_BODY.md
```

Nunca hagas push a `main` ni fuerces el push.
