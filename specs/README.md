# Especificaciones técnicas

Esta carpeta guarda **cómo** se construye cada historia de usuario. El **qué** ya está en `docs/` (HU, criterios de aceptación, dominio, mensajes, parámetros y decisiones) y no se repite aquí: se enlaza.

A diferencia de `docs/`, esta carpeta **se escribe a mano** (con ayuda del skill `especificar`) y se revisa como cualquier código. Si una especificación contradice las decisiones D01–D39, mandan las decisiones.

## Flujo de trabajo de una HU

| Paso | Qué se hace | Resultado |
|---|---|---|
| 1. Especificar | `/especificar HUnn`: el agente lee la HU y sus fuentes y escribe `spec.md` y `tareas.md` | Carpeta `specs/HUnn-<nombre>/` con estado **Borrador** |
| 2. Resolver preguntas | El desarrollador responde las «Preguntas abiertas». Las contradicciones con `docs/` se reportan y se corrigen en el Excel de origen | Sin preguntas bloqueantes |
| 3. Aprobar | El desarrollador revisa el contrato y la tabla de criterios → pruebas, y cambia el estado a **Aprobada**. Esta aprobación reemplaza a la del plan en el chat (paso 1 de «Cómo trabajar una historia de usuario» en `AGENTS.md`) | `spec.md` aprobado |
| 4. Implementar | Una tarea de `tareas.md` a la vez, por capas, con la prueba del criterio primero. Al terminar cada tarea se marca `[x]` y se explica | Código y pruebas |
| 5. Verificar y abrir el PR | Cada CA tiene su prueba, `./mvnw verify` pasa y se usa `/pull-request` | Estado **Implementada** |

Si al implementar aparece algo que la especificación no previó, primero se corrige `spec.md` y después el código. La especificación siempre describe lo que el código hace.

## Plantilla de `spec.md`

```markdown
# HUnn · <Nombre de la HU>

**Estado:** Borrador | Aprobada | Implementada · **HU:** [HUnn](../../docs/historias/HUnn.md) · **Módulo:** <módulo>

## Fuentes
Hojas del dominio, mensajes, parámetros, aspectos generales y decisiones que aplican, con enlace.

## Qué se construye
Dos o tres frases: qué cambia para el usuario y qué parte queda para el frontend.

## Contrato del servicio
Método y ruta, quién lo puede usar, entrada, respuesta exitosa y tabla código HTTP ↔ código de mensaje.

## Diseño por capas
Clases nuevas o que cambian, por capa (dominio, caso de uso, puertos, persistencia, REST), con la política que implementa cada una.

## Criterios de aceptación → pruebas
| CA | Qué se verifica | Prueba | Capa |

## Preguntas abiertas
Lo que falta decidir, contradicciones entre documentos y parámetros «Por definir». Cada una trae una recomendación y dice si bloquea.

## Fuera de alcance
Lo que se ve relacionado pero esta HU no pide (regla 5 de «Cómo trabajar una historia de usuario»).
```

## Plantilla de `tareas.md`

```markdown
# HUnn · Tareas

- [ ] T1 · Dominio · <tarea> (CAn, política n)
- [ ] T2 · Caso de uso · ...
- [ ] T3 · Persistencia · ...
- [ ] T4 · REST · ...
- [ ] T5 · Verificación · ./mvnw verify y /pull-request
```

## Estado de las especificaciones

| HU | Especificación | Estado |
|---|---|---|
| HU8 | [HU8-modificar-escenario](HU8-modificar-escenario/spec.md) | Implementada |
| HU9 | [HU9-modificar-cancha](HU9-modificar-cancha/spec.md) | Implementada |
| HU10 | [HU10-cambiar-estado-cancha](HU10-cambiar-estado-cancha/spec.md) | Implementada |
| HU11 | [HU11-modificar-horario-atencion](HU11-modificar-horario-atencion/spec.md) | Implementada |
| HU12 | [HU12-eliminar-horario-atencion](HU12-eliminar-horario-atencion/spec.md) | Aprobada |
| HU13 | [HU13-activar-inactivar-escenario](HU13-activar-inactivar-escenario/spec.md) (solo el arreglo de concurrencia) | Implementada |
| HU19 | [HU19-perfil-escenario](HU19-perfil-escenario/spec.md) | Implementada |
| HU20 | [HU20-consultar-disponibilidad](HU20-consultar-disponibilidad/spec.md) | Implementada |
| HU21 | [HU21-reservar-cancha](HU21-reservar-cancha/spec.md) | Implementada |
