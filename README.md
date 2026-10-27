# CanchaYa

**CanchaYa** es una aplicación web responsiva para buscar, reservar y gestionar canchas sintéticas de fútbol en Antioquia. En una sola plataforma reúne lo que necesita el jugador (búsqueda y reserva) y lo que necesita el administrador del escenario (agenda, disponibilidad y tarifas). Está pensada para establecimientos pequeños y se valida con administradores de Rionegro y Marinilla.

Es el trabajo de grado de Ingeniería de Sistemas de la Universidad Católica de Oriente (UCO).

## Problema

Según las encuestas del proyecto:

- El **66,3 %** de los 86 jugadores encuestados dice que su mayor problema es conocer la disponibilidad real de las canchas.
- El **100 %** de los 5 administradores encuestados gestiona las reservas por WhatsApp.
- El **80 %** de esos administradores encuentra medianamente complicado ver qué horarios tiene libres.

## Objetivo

Que un jugador complete una reserva de principio a fin (búsqueda → disponibilidad → confirmación) sin llamar ni escribir por WhatsApp, y que el administrador gestione su escenario desde un único panel. La meta técnica es **cero dobles reservas** en las pruebas de concurrencia.

## Alcance

El backlog tiene 38 historias de usuario (113 puntos) en tres releases.

| Release | Contenido | HU | Puntos |
|---|---|---|---|
| **Release 1 · Núcleo** | Registro, inicio de sesión y perfil; escenarios, canchas y horarios; activación de escenarios por el administrador del sistema; búsqueda geográfica sin sesión; ficha del escenario y disponibilidad; reservas de 1 o 2 franjas con control de concurrencia; correos de reserva y cancelación; agenda del escenario; despliegue en contenedores | 22 | 72 |
| **Release 2 · Segunda prioridad** | Catálogo de tipos de cancha; confirmación de asistencia con cancelación automática; bloqueos de un día; cancelación por fuerza mayor; registro de inasistencias | 11 | 29 |
| **Release 3 · Si hay tiempo** | Encargados de sede; reseñas y calificaciones | 5 | 12 |

**Fuera del alcance (trabajo futuro):** matchmaking, sanciones por inasistencia, reservas recurrentes, gestión de usuarios y roles, notificaciones adicionales, moderación de reseñas, bloqueos de varios días, verificación de correo, recuperación de contraseña, pasarela de pagos y aplicación móvil nativa.

## Actores

| Actor | Qué hace | Rol técnico |
|---|---|---|
| Jugador aficionado | Busca, reserva, cancela, confirma asistencia y deja reseñas | `JUGADOR` |
| Administrador de cancha (propietario) | Publica escenarios, canchas, horarios y tarifas; consulta la agenda; crea bloqueos; cancela por fuerza mayor; marca inasistencias; asigna encargados | `ADMINISTRADOR_CANCHA` |
| Encargado de sede | Consulta la agenda, crea y elimina bloqueos y marca inasistencias, solo en sus sedes | `ADMINISTRADOR_CANCHA` + asignación |
| Administrador del sistema | Activa o inactiva escenarios y gestiona el catálogo de tipos de cancha | `ADMINISTRADOR_SISTEMA` |
| CanchaYa (sistema) | Envía correos y ejecuta la tarea programada de confirmación de asistencia | — |

## Reglas de negocio clave

- Las franjas son de 1 hora, en horas en punto. Una reserva cubre 1 o 2 franjas consecutivas de la misma cancha; el precio es la suma de las franjas.
- Cada jugador puede reservar máximo 2 horas por día, con una ventana de reserva de 30 días.
- Buscar, ver la ficha del escenario y consultar la disponibilidad no requieren sesión; reservar sí.
- Radio de búsqueda: mínimo 1 km, por defecto 5 km y máximo 30 km. Sin permiso de ubicación se busca por ciudad.
- El jugador cancela o confirma asistencia solo si faltan más de 3 horas; la cancelación automática ocurre exactamente a las 3 horas.
- Si un correo falla, la operación no se revierte: se registra el error y no se reintenta.
- Todas las fechas y horas se calculan con la hora del servidor en `America/Bogota` (UTC-5).

Las reglas completas están en las decisiones D01–D27 (`decisiones.md`), que son la fuente de verdad del proyecto.

## Stack tecnológico (propuesto)

La justificación final se documenta en la fase de diseño arquitectónico.

| Capa | Tecnología |
|---|---|
| Backend | Java 21 + Spring Boot |
| Frontend | Angular, con diseño responsivo |
| Base de datos | PostgreSQL + PostGIS |
| Arquitectura | Hexagonal + DDD |
| Seguridad | JWT con el rol en el token; contraseñas con bcrypt; permisos validados en el servidor |
| Identificadores | UUID generados en el servidor |
| Correo | Proveedor SMTP |
| Despliegue | Docker en un hosting de bajo costo (Render o Railway) |

**Contextos delimitados:** Identidad y acceso, Escenarios y canchas, Reservas, Reputación y Ubicación.

**Decisiones técnicas abiertas:** control de concurrencia optimista o pesimista (se decide con un spike), actualización de la disponibilidad en tiempo real, vigencia del token JWT, almacenamiento de fotos y frecuencia de la tarea programada.

## Cronograma

El proyecto va del 21 de julio al 30 de noviembre de 2026; la sustentación es en febrero de 2027.

| Fase | Fechas | Contenido |
|---|---|---|
| Fase 1 · Análisis | 21 jul – 16 oct | Requisitos, procesos, dominio, Event Storming e historias de usuario |
| Fase 2 · Diseño | 12 – 30 oct | Arquitectura, modelo E-R, contratos de API, spike de concurrencia |
| Sprint 1 | 3 – 6 nov | HU1–HU7 y HU13 (23 pts): usuarios, escenarios, canchas y horarios |
| Sprint 2 | 9 – 13 nov | HU18–HU21 (22 pts): búsqueda, ficha, disponibilidad y reservas |
| Sprint 3 | 17 – 20 nov | HU8–HU12, HU22–HU25 y HU29 (27 pts): agenda, cancelaciones y correos |
| Fase 4 · Validación | 23 – 27 nov | Pruebas funcionales y con usuarios |
| Entrega final | 30 nov | Documento técnico y artefactos |

## Estructura del repositorio

Es un monorepo:

```
CanchaYa/
├── README.md
├── AGENTS.md                 # Guía para agentes de IA
├── CLAUDE.md                 # Remite a AGENTS.md
├── .github/
│   └── pull_request_template.md
└── architecture/
    ├── .claude/              # Configuración de Claude Code (skills)
    └── projects/             # Proyectos de la aplicación (backend, frontend, …)
```

El detalle de cada carpeta está en [AGENTS.md](AGENTS.md).

## Documentación del proyecto

Los artefactos de análisis y diseño se mantienen fuera de este repositorio, en la carpeta de documentos del trabajo de grado:

- **Artefactos estratégicos:** visión, mapa de impacto y Project Canvas, modelo de procesos (BPMN), modelo de dominio enriquecido, Event Storming, mapa de historias, tallaje del producto, plan de liberaciones y especificación funcional (catálogos de mensajes y parámetros, aspectos generales y prototipos).
- **Artefactos técnicos:** drivers arquitectónicos, tácticas, alternativas de solución, arquetipo y plataforma tecnológica.

## Contribuir

1. Crea una rama por historia de usuario: `<tipo>/HU<nn>-<descripcion-corta>`.
2. Abre el pull request con la plantilla de `.github/`. El skill `pull-request` la llena a partir de los cambios.
3. Las instrucciones de prueba deben permitir verificar cada criterio de aceptación de la HU.

## Equipo

- **Andrés Cardona Jaramillo**: desarrollador (análisis, arquitectura, desarrollo, pruebas y documentación).
- **Juan Camilo Acevedo**: asesor del trabajo de grado.
