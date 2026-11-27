# Especificaciones técnicas

Esta carpeta guarda **cómo** se construye cada historia de usuario, en el backend y en la pantalla. El **qué** ya está en `docs/` (HU, criterios de aceptación, dominio, mensajes, parámetros y decisiones) y no se repite aquí: se enlaza.

A diferencia de `docs/`, esta carpeta **se escribe a mano** (con ayuda del skill `especificar`) y se revisa como cualquier código. Si una especificación contradice las decisiones D01–D39, mandan las decisiones.

## Flujo de trabajo de una HU

| Paso | Qué se hace | Resultado |
|---|---|---|
| 1. Especificar | `/especificar HUnn`: el agente decide el alcance (backend y pantalla, solo backend o solo pantalla), lee la HU, sus fuentes y su prototipo, y escribe `spec.md` y `tareas.md` | Carpeta `specs/HUnn-<nombre>/` con estado **Borrador** |
| 2. Resolver preguntas | El desarrollador responde las «Preguntas abiertas». Las contradicciones con `docs/` se reportan y se corrigen en el Excel de origen | Sin preguntas bloqueantes |
| 3. Aprobar | El desarrollador revisa el contrato y la tabla de criterios → pruebas, y cambia el estado a **Aprobada**. Esta aprobación reemplaza a la del plan en el chat (paso 1 de «Cómo trabajar una historia de usuario» en `AGENTS.md`) | `spec.md` aprobado |
| 4. Implementar | Una tarea de `tareas.md` a la vez, por capas (dominio → caso de uso → persistencia → REST → pantalla), con la prueba del criterio primero. Al terminar cada tarea se marca `[x]` y se explica | Código y pruebas |
| 5. Verificar y abrir el PR | Cada CA tiene su prueba; pasa `./mvnw verify` si se tocó el backend, y `npx ng lint`, `npx ng test --watch=false` y `npx ng build` si se tocó el frontend; se usa `/pull-request` | Estado **Implementada** |

**Alcance.** Una especificación puede cubrir el backend y la pantalla, solo el backend (HU que solo envían correos o corren solas) o solo la pantalla. Cuando el backend ya está implementado, la pantalla se agrega al `spec.md` que ya existe (con un estado para cada parte, por ejemplo `Implementada (backend) · Borrador (pantalla)`). Si la HU se hizo antes de que existiera esta carpeta, se escribe un `spec.md` nuevo de solo la pantalla, con el contrato **tomado del código real** y no inventado.

Si al implementar aparece algo que la especificación no previó, primero se corrige `spec.md` y después el código. La especificación siempre describe lo que el código hace.

## Plantilla de `spec.md`

```markdown
# HUnn · <Nombre de la HU>

**Estado:** Borrador | Aprobada | Implementada (uno por parte si hay dos: `Implementada (backend) · Borrador (pantalla)`) · **HU:** [HUnn](../../docs/historias/HUnn.md) · **Módulo:** <módulo> · **Alcance:** backend y pantalla | solo backend | solo pantalla

## Fuentes
Hojas del dominio, mensajes, parámetros, aspectos generales, decisiones y prototipo que aplican, con enlace.

## Qué se construye
Dos o tres frases: qué cambia para el usuario, en el backend y en la pantalla.

## Contrato del servicio
Método y ruta, quién lo puede usar, entrada, respuesta exitosa y tabla código HTTP ↔ código de mensaje. Si el backend ya está implementado, se copia del código (controlador, records `Solicitud*` y `Respuesta*`, `ManejadorErrores`, `ConfiguracionSeguridad`) citando cada archivo.

## Diseño por capas
Clases nuevas o que cambian, por capa (dominio, caso de uso, puertos, persistencia, REST), con la política que implementa cada una. En «solo pantalla»: «No cambia: el backend ya está», con el enlace al código.

## Pantalla
Se omite si la HU no tiene pantalla.
- **Prototipo:** enlaces a celular y computador en [docs/prototipos/](../../docs/prototipos/README.md) y diferencias con la HU, si las hay.
- **Ruta:** dirección en `app.routes.ts` y si exige sesión (AG-11).
- **Componentes y servicio:** carpeta del área (`identidad/registro/`), componente, servicio HTTP que llama al contrato y piezas de Angular Material.
- **Formulario:** validaciones que solo guían al usuario antes de enviar (AC-03.1), con el mensaje que muestra cada una (`MensajesService`, ERR-GEN-01 a 04).
- **Respuestas del backend:** qué se muestra en cada caso del contrato (el `mensaje` de la `RespuestaError` tal como llega, en el campo si trae `campo`) y a dónde se va después del éxito.
- **CA en la pantalla:** qué criterios se ven en la pantalla y con qué prueba de Vitest (`it('CAn: …')`, con `HttpTestingController`).

## Criterios de aceptación → pruebas
| CA | Qué se verifica | Prueba | Capa (Dominio, Caso de uso, Persistencia, REST, Punta a punta o Pantalla) |

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
- [ ] T5 · Pantalla · <componente o servicio> (CAn)
- [ ] T6 · Verificación · ./mvnw verify (si se tocó el backend); npx ng lint, npx ng test --watch=false y npx ng build (si se tocó el frontend); /pull-request
```

## Pendientes para especificaciones futuras

Propuestas y preguntas que salieron antes de especificar la HU que las necesita. Cuando se escriba su `spec.md` (en la rama de la HU), las propuestas se usan como punto de partida, las preguntas pasan a sus «Preguntas abiertas» y la fila se borra de aquí.

### HU1: solo la pantalla

Su backend se implementó antes de que existiera esta carpeta, así que no tienen `spec.md`. Se propone una especificación de **solo la pantalla** para cada una, con `/especificar HUnn pantalla`, y el contrato copiado del código. HU2 ya tiene la suya ([HU2-iniciar-sesion](HU2-iniciar-sesion/spec.md)); queda HU1, que al terminar el registro (CA11) lleva al usuario a la pantalla de inicio de sesión.

| HU | Carpeta | Contrato (del código) | Prototipo | Pruebas de pantalla | Preguntas para su `spec.md` |
|---|---|---|---|---|---|
| HU1 | `specs/HU1-registrar-usuario/` | `POST /api/usuarios` y `GET /api/usuarios/correo-disponible` (públicos): `UsuarioController`, `SolicitudRegistro` (validaciones con su código), `CorreoDisponible`, `RespuestaRegistro` con EXI-USU-01; ERR-USU-01 a 03 y 12 a 15 | [01 Registro de usuario](../docs/prototipos/README.md) | CA6 y CA7 (validar al salir del campo, con `MensajesService`), CA8 (correo repetido al salir del campo, con `correo-disponible`), CA2, CA9, CA10, CA12 y CA13 (mensajes junto al campo), CA1 y CA11 (EXI-USU-01 y paso a iniciar sesión) | De dónde toma la pantalla el `{formatoEsperado}` de ERR-USU-13 y la ayuda «Formato: de 6 a 10 dígitos» de cada tipo de documento: hoy está fijo en el enum `TipoDocumento` del dominio de identidad y no llega a `catalogo-mensajes.json`. El prototipo no muestra la declaración de autorización para TI (CA13): ¿aparece solo cuando se elige TI? La ayuda del celular del prototipo («+57 300 123 4567», con espacios) no coincide con `formato.numeroCelular` ni con la validación del backend, que no admite espacios. De dónde sale la lista de tipos de documento (fija en la pantalla o desde el backend). Además, al crear la ruta de registro se agrega el enlace «¿No tiene cuenta? Regístrese» de la pantalla de inicio de sesión (HU2, P3). |

## Estado de las especificaciones

| HU | Especificación | Estado |
|---|---|---|
| HU2 | [HU2-iniciar-sesion](HU2-iniciar-sesion/spec.md) (solo la pantalla) | Implementada (backend) · Aprobada (pantalla) |
| HU8 | [HU8-modificar-escenario](HU8-modificar-escenario/spec.md) | Implementada |
| HU9 | [HU9-modificar-cancha](HU9-modificar-cancha/spec.md) | Implementada |
| HU10 | [HU10-cambiar-estado-cancha](HU10-cambiar-estado-cancha/spec.md) | Implementada |
| HU11 | [HU11-modificar-horario-atencion](HU11-modificar-horario-atencion/spec.md) | Implementada |
| HU12 | [HU12-eliminar-horario-atencion](HU12-eliminar-horario-atencion/spec.md) | Implementada |
| HU13 | [HU13-activar-inactivar-escenario](HU13-activar-inactivar-escenario/spec.md) (solo el arreglo de concurrencia) | Implementada |
| HU18 | [HU18-buscar-canchas](HU18-buscar-canchas/spec.md) | Implementada |
| HU19 | [HU19-perfil-escenario](HU19-perfil-escenario/spec.md) | Implementada |
| HU20 | [HU20-consultar-disponibilidad](HU20-consultar-disponibilidad/spec.md) | Implementada |
| HU21 | [HU21-reservar-cancha](HU21-reservar-cancha/spec.md) | Implementada |
| HU22 | [HU22-notificar-reserva](HU22-notificar-reserva/spec.md) | Implementada |
| HU23 | [HU23-consultar-reservas](HU23-consultar-reservas/spec.md) | Implementada |
| HU24 | [HU24-cancelar-reserva](HU24-cancelar-reserva/spec.md) | Implementada |
| HU25 | [HU25-notificar-cancelacion](HU25-notificar-cancelacion/spec.md) | Implementada |
| HU29 | [HU29-consultar-agenda-escenario](HU29-consultar-agenda-escenario/spec.md) | Implementada |
