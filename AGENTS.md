# AGENTS.md

Guía para los agentes de IA (Claude Code u otros) que trabajan en el repositorio de **CanchaYa**. Para el contexto del producto, lee el [README.md](README.md). Para la documentación de análisis y diseño, lee [docs/README.md](docs/README.md): ahí dice qué archivo consultar en cada caso.

## Estructura del repositorio

Es un monorepo. Por ahora tiene esta estructura:

```
CanchaYa/
├── README.md                       # Descripción del proyecto
├── AGENTS.md                       # Este archivo: guía para agentes
├── CLAUDE.md                       # Remite a AGENTS.md
├── .claude/
│   ├── settings.json               # Permisos compartidos de Claude Code
│   └── skills/
│       └── pull-request/
│           └── SKILL.md            # Skill que llena la plantilla de pull request
├── .github/
│   └── pull_request_template.md    # Plantilla de pull request
├── docs/                           # Documentación del trabajo de grado en texto (generada)
│   ├── decisiones.md               # D01–D36: fuente de verdad
│   ├── historias/                  # Las 38 HU con sus criterios de aceptación
│   ├── dominio/                    # Modelo de dominio enriquecido, un archivo por objeto
│   ├── especificacion/             # Catálogos de mensajes y parámetros, aspectos generales
│   ├── arquitectura/               # Drivers, tácticas, arquetipo y plataforma tecnológica
│   └── diseno/                     # Diagramas de clases, componentes y modelo entidad relación
├── scripts/
│   └── exportar_docs.py            # Regenera docs/ a partir de los Excel
└── architecture/
    └── projects/                   # Proyectos de la aplicación (vacío por ahora)
```

| Ruta | Para qué sirve |
|---|---|
| `README.md` | Visión, alcance, roles, stack y cronograma del proyecto. |
| `AGENTS.md` | Reglas y convenciones para agentes. Se actualiza cada vez que cambia la estructura del repositorio. |
| `CLAUDE.md` | Solo importa `AGENTS.md` con `@AGENTS.md`; no se le agrega contenido propio. |
| `.claude/` | Configuración de Claude Code del repositorio: permisos y skills, y más adelante agentes. |
| `.github/pull_request_template.md` | Plantilla de los pull request: contexto, tipo de cambio e instrucciones de prueba. |
| `docs/` | Copia en Markdown de los artefactos del trabajo de grado. **No se edita a mano**: se regenera con `scripts/exportar_docs.py`. La excepción es `docs/README.md`. |
| `scripts/` | Utilidades del repositorio que no son parte de la aplicación. |
| `architecture/projects/` | Aquí van los proyectos de código (backend, frontend, infraestructura). Todavía no se ha creado ninguno. |

## Reglas para agentes

1. **Idioma**: la documentación, los mensajes al usuario, los pull request y los mensajes de commit van en español de Colombia. Los nombres del dominio en el código también van en español (`Reserva`, `horaInicio`), como en el modelo de dominio enriquecido.
2. **Terminología**: no se usa el término "MVP" ni "Producto Mínimo Viable". Se habla de *el proyecto*, *la aplicación*, *el software* o *la plataforma*.
3. **Fuente de verdad**: las reglas de negocio están en las decisiones D01–D36 ([docs/decisiones.md](docs/decisiones.md)). Si el código o un artefacto las contradice, mandan las decisiones. Si encuentras una contradicción, repórtala en lugar de resolverla por tu cuenta.
4. **Modelo de dominio**: la referencia oficial es el modelo de dominio enriquecido ([docs/dominio/](docs/dominio/README.md)). Toda entidad lleva un `id` de tipo UUID que genera el servidor, y los atributos se escriben en camelCase. Las políticas de cada responsabilidad se implementan en el dominio, con su número en un comentario o en el nombre de la prueba.
5. **Historias de usuario**: el backlog oficial es el del Tallaje del producto (38 HU, [docs/historias/](docs/historias/README.md)). Todo cambio funcional debe poder trazarse a una HU y a sus criterios de aceptación (`CAn`).
6. **Mensajes**: los textos que ve el usuario salen del catálogo de mensajes por su código (`ERR/EXI/INF/CNF-<OBJ>-nn`, [docs/especificacion/mensajes.md](docs/especificacion/mensajes.md)). No se escriben textos sueltos en el código fuente.
7. **Parámetros**: los valores del catálogo de parámetros (`PAR-*`, [docs/especificacion/parametros.md](docs/especificacion/parametros.md)) se leen de la configuración, no se escriben fijos en el código. Si un parámetro está «Por definir», pregunta antes de inventar un valor.
8. **Documentación generada**: no edites los archivos de `docs/` (salvo `docs/README.md`). Si encuentras un error en ellos, el error está en el Excel de origen: repórtalo.
9. **Estructura**: si agregas, mueves o eliminas carpetas de primer o segundo nivel, actualiza la sección «Estructura del repositorio» de este archivo en el mismo cambio.

## Arquitectura del backend

Resumen de [docs/arquitectura/arquetipo.md](docs/arquitectura/arquetipo.md) (hoja «Estructura del backend») y [docs/diseno/componentes.md](docs/diseno/componentes.md). Si algo de aquí no coincide con esos archivos, mandan ellos y hay que corregir este resumen.

- **Un solo backend organizado por módulos** (monolito modular con arquitectura hexagonal y DDD, D30). Sin puerta de enlace (D32): la seguridad está en el propio backend con Spring Security.
- **Módulos** y qué puede usar cada uno. No se permiten otras dependencias ni dependencias en círculo:

  | Módulo | Paquete | Hexagonal | Puede usar a |
  |---|---|---|---|
  | Identidad y acceso | `co.edu.uco.canchaya.identidad` | Simplificada | Ninguno |
  | Ubicación | `co.edu.uco.canchaya.ubicacion` | Simplificada | Ninguno |
  | Escenarios y canchas | `co.edu.uco.canchaya.escenarios` | Completa | Identidad, Ubicación |
  | Reservas | `co.edu.uco.canchaya.reservas` | Completa | Identidad, Escenarios |
  | Reputación | `co.edu.uco.canchaya.reputacion` | Simplificada | Identidad, Reservas, Escenarios |
  | Transversal | `co.edu.uco.canchaya.transversal` | — | Piezas compartidas: `ReglaNegocioException`, envío de correo |

- **Partes de cada módulo**, de adentro hacia afuera: dominio → aplicación (un caso de uso por comando del Event Storming) → puertos → adaptadores de entrada (REST, tarea programada) y de salida (base de datos, correo, fotos). Las clases del dominio van en `<paquete del módulo>.domain`. Los nombres de los demás paquetes se fijan en el diagrama de paquetes; mientras no exista, pregunta antes de crear uno nuevo.
- **El dominio no depende de Spring, de la base de datos ni de ninguna librería externa.**
- **Entre módulos solo se usan los puertos de entrada** que cada uno ofrece (`ConsultaUsuarios`, `ConsultaUbicacion`, `ConsultaCanchas`, `ConsultaEscenarios`, `ConsultaReservas`). Nunca el dominio, los adaptadores ni las tablas de otro módulo. `ConsultaReservasFuturas` la define Escenarios y la implementa Reservas (D08). Estas reglas se revisan con pruebas de ArchUnit.
- **Concurrencia de reservas** (D29, D36): restricción de exclusión en la base de datos, turno por cancha con `ConsultaCanchas.tomarTurno(canchaId)` dentro de la transacción y actualización condicional para confirmar asistencia y cancelar automáticamente.
- **Correos** (D15): se envían después de guardar. Si fallan, la operación no se revierte: se registra el error y no se reintenta.
- **Fechas y horas** (D27): todo se calcula en `America/Bogota`.

## Cómo trabajar una historia de usuario

El desarrollador debe entender y poder sustentar todo el código. Por eso:

1. **Plan antes que código.** Lee la HU, las hojas del dominio que cita su «Requisito de información», los mensajes y los parámetros que nombra. Propón un plan corto (qué clases, en qué módulo y qué pruebas) y espera la aprobación antes de escribir código.
2. **Pruebas a partir de los criterios de aceptación.** Cada `CAn` tiene al menos una prueba, y su nombre o comentario cita el criterio.
3. **Pasos pequeños, por capas:** dominio → caso de uso → persistencia → servicio REST → pantalla. Al terminar cada paso, explícalo antes de seguir.
4. **Explicaciones en lenguaje sencillo:** primero qué hace y qué problema evita; el término técnico va entre paréntesis. Por ejemplo: «la base de datos rechaza dos reservas que se cruzan (restricción de exclusión)».
5. **No agregues lo que la HU no pide.** Si ves algo que falta o sobra, coméntalo, pero no lo hagas.

## Mensajes de commit

Los mensajes de commit siguen [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/):

```
<tipo>(<alcance opcional>): <descripción>

[cuerpo opcional]

[pie opcional]
```

- **Tipos**: `feat` (nueva funcionalidad), `fix` (corrección de error), `refactor`, `test`, `docs`, `style`, `perf`, `build`, `ci`, `chore`, `revert`.
- **Alcance**: el módulo o contexto afectado, en minúsculas, por ejemplo `reservas`, `escenarios`, `identidad`, `ubicacion`, `reputacion`, `frontend`, `infra`.
- **Descripción**: en español, en imperativo, en minúscula y sin punto final, de máximo 72 caracteres. Por ejemplo: `feat(reservas): validar el límite de 2 horas por día`.
- **Cuerpo**: explica el qué y el porqué. Cita la HU o la decisión cuando aplique (`HU21`, `D10`).
- **Cambios que rompen compatibilidad**: agrega `!` después del tipo o del alcance (`feat(api)!: ...`) y un pie `BREAKING CHANGE: <detalle>`.
- **Referencia a la HU**: si el commit corresponde a una historia de usuario, agrega el pie `Refs: HU<nn>`.

## Pull requests

- Todo pull request usa la plantilla `.github/pull_request_template.md`.
- Para llenarla, usa el skill `pull-request` (`.claude/skills/pull-request/SKILL.md`). El skill lee el diff, identifica la HU y escribe instrucciones de prueba que permiten verificar cada criterio de aceptación.
- Nombre de rama: `<tipo>/HU<nn>-<descripcion-corta>`, donde `<tipo>` es un tipo de Conventional Commits. Por ejemplo `feat/HU21-reservar-franja` o `fix/HU13-validar-horario`. Si no hay HU: `<tipo>/<descripcion-corta>`, por ejemplo `ci/pipeline-backend`.
- El título del pull request también sigue Conventional Commits, por ejemplo `feat(reservas): reservar una o dos franjas consecutivas`, porque es el mensaje del commit que queda en `main` al hacer *squash merge*.
