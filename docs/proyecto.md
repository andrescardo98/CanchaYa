# El proyecto CanchaYa

Visión, alcance, actores, reglas clave y cronograma del trabajo de grado. Este archivo se escribe a mano (no lo genera `scripts/exportar_docs.py`); si algo de aquí no coincide con las decisiones D01–D39 ([decisiones.md](decisiones.md)), mandan las decisiones.

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

## Estado técnico

**Módulos (contextos delimitados):** Identidad y acceso, Ubicación, Escenarios y canchas, Reservas y Reputación.

**Pendientes técnicos:** spike de control de concurrencia (RT-03, antes de HU21), frecuencia de la tarea programada (PAR-RES-11) y prueba del puerto 2525 de Brevo en el Sprint 1.

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

