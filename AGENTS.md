# AGENTS.md

Guía para los agentes de IA (Claude Code u otros) que trabajan en el repositorio de **CanchaYa**. Para el contexto del producto, lee el [README.md](README.md).

## Estructura del repositorio

Es un monorepo. Por ahora tiene esta estructura:

```
CanchaYa/
├── README.md                       # Descripción del proyecto
├── AGENTS.md                       # Este archivo: guía para agentes
├── CLAUDE.md                       # Remite a AGENTS.md
├── .claude/
│   └── skills/
│       └── pull-request/
│           └── SKILL.md            # Skill que llena la plantilla de pull request
├── .github/
│   └── pull_request_template.md    # Plantilla de pull request
└── architecture/
    └── projects/                   # Proyectos de la aplicación (vacío por ahora)
```

| Ruta | Para qué sirve |
|---|---|
| `README.md` | Visión, alcance, roles, stack y cronograma del proyecto. |
| `AGENTS.md` | Reglas y convenciones para agentes. Se actualiza cada vez que cambia la estructura del repositorio. |
| `CLAUDE.md` | Solo importa `AGENTS.md` con `@AGENTS.md`; no se le agrega contenido propio. |
| `.claude/` | Configuración de Claude Code del repositorio: skills, y más adelante agentes y comandos. |
| `.github/pull_request_template.md` | Plantilla de los pull request: contexto, tipo de cambio e instrucciones de prueba. |
| `architecture/projects/` | Aquí van los proyectos de código (backend, frontend, infraestructura). Todavía no se ha creado ninguno. |

## Reglas para agentes

1. **Idioma**: la documentación, los mensajes al usuario, los pull request y los mensajes de commit van en español de Colombia.
2. **Terminología**: no se usa el término "MVP" ni "Producto Mínimo Viable". Se habla de *el proyecto*, *la aplicación*, *el software* o *la plataforma*.
3. **Fuente de verdad**: las reglas de negocio están en las decisiones D01–D27 (`decisiones.md`, en la carpeta de documentos del trabajo de grado). Si el código o un artefacto las contradice, mandan las decisiones. Si encuentras una contradicción, repórtala en lugar de resolverla por tu cuenta.
4. **Modelo de dominio**: la referencia oficial es el modelo de dominio enriquecido. Toda entidad lleva un `id` de tipo UUID que genera el servidor, y los atributos se escriben en camelCase.
5. **Historias de usuario**: el backlog oficial es el del Tallaje del producto (38 HU). Todo cambio funcional debe poder trazarse a una HU y a sus criterios de aceptación.
6. **Mensajes**: los textos que ve el usuario salen del catálogo de mensajes por su código (`ERR/EXI/INF/CNF-<OBJ>-nn`). No se escriben textos sueltos en el código fuente.
7. **Estructura**: si agregas, mueves o eliminas carpetas de primer o segundo nivel, actualiza la sección «Estructura del repositorio» de este archivo en el mismo cambio.

## Mensajes de commit

Los mensajes de commit siguen [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/):

```
<tipo>(<alcance opcional>): <descripción>

[cuerpo opcional]

[pie opcional]
```

- **Tipos**: `feat` (nueva funcionalidad), `fix` (corrección de error), `refactor`, `test`, `docs`, `style`, `perf`, `build`, `ci`, `chore`, `revert`.
- **Alcance**: el módulo o contexto afectado, en minúsculas, por ejemplo `reservas`, `escenarios`, `identidad`, `ubicacion`, `frontend`, `infra`.
- **Descripción**: en español, en imperativo, en minúscula y sin punto final, de máximo 72 caracteres. Por ejemplo: `feat(reservas): validar el límite de 2 horas por día`.
- **Cuerpo**: explica el qué y el porqué. Cita la HU o la decisión cuando aplique (`HU21`, `D10`).
- **Cambios que rompen compatibilidad**: agrega `!` después del tipo o del alcance (`feat(api)!: ...`) y un pie `BREAKING CHANGE: <detalle>`.

## Pull requests

- Todo pull request usa la plantilla `.github/pull_request_template.md`.
- Para llenarla, usa el skill `pull-request` (`.claude/skills/pull-request/SKILL.md`). El skill lee el diff, identifica la HU y escribe instrucciones de prueba que permiten verificar cada criterio de aceptación.
- Nombre de rama sugerido: `<tipo>/HU<nn>-<descripcion-corta>`, por ejemplo `feature/HU21-reservar-franja`.
