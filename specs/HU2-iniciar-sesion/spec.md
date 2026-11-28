# HU2 · Iniciar sesión (solo la pantalla)

**Estado:** Implementada (backend, antes de `specs/`) · Implementada (pantalla, 27-nov-2026; aprobada el 26-nov-2026) · **HU:** [HU2](../../docs/historias/HU2.md) · **Módulo:** Identidad y acceso (`co.edu.uco.canchaya.identidad`) · **Alcance:** solo la pantalla

El backend se implementó antes de que existiera esta carpeta (PR de la rama `feat/HU2-iniciar-sesion`), así que esta especificación cubre **solo la pantalla** y el contrato se copia del código real. Es la primera pantalla del frontend: además del formulario, trae la sesión que necesitan las demás pantallas (guardar el token, enviarlo en cada petición y llevar al inicio de sesión cuando falta o venció).

## Fuentes

- HU: [HU2](../../docs/historias/HU2.md), CA1 a CA7 y la conversación (página de inicio de cada rol, contraseña en campo oculto, mismo mensaje para correo o contraseña incorrectos).
- Dominio: [Usuario](../../docs/dominio/usuario.md), responsabilidad `autenticar` (políticas 6 y 7). Ya implementadas en `IniciarSesionCasoUso`.
- Mensajes ([mensajes.md](../../docs/especificacion/mensajes.md)): ERR-GEN-01, ERR-GEN-02, ERR-GEN-05, ERR-USU-06, ERR-USU-07, ERR-USU-16, y ERR-GEN-07 para fallas inesperadas (AG-10). Todos están en `core/catalogo-mensajes.json`; `campos` y `formatos` ya traen `correoElectronico` y `contrasena`.
- Parámetros ([parametros.md](../../docs/especificacion/parametros.md)): PAR-USU-09 (60 min, sin renovación) y PAR-USU-10 a 12 (intentos). Todos **Definidos** y leídos por el backend; la pantalla no usa ninguno: el `{minutos}` de ERR-USU-16 ya llega dentro del `mensaje`.
- Aspectos generales: AG-01 y AG-02 (validar en el cliente para guiar, al salir del campo), AG-03, AG-04 (con error se conserva lo escrito menos la contraseña), AG-06 (espacios sobrantes), AG-10, AG-11 (rutas públicas y ERR-GEN-05), AG-13 (campo oculto), AG-21 (celular y computador).
- Decisiones: D28 (límite de intentos), D37 (60 minutos, sin renovación; al cerrar sesión se borra el token del dispositivo).
- Prototipo: [02 Inicio de sesión](../../docs/prototipos/README.md) ([celular](../../docs/prototipos/celular/02-inicio-de-sesion.png), [computador](../../docs/prototipos/computador/02-inicio-de-sesion.png)).
- Frontend: [projects/web/AGENTS.md](../../projects/web/AGENTS.md), `core/mensajes.service.ts` y `core/modelos.ts`.

## Qué se construye

La pantalla «Iniciar sesión» con correo y contraseña. Antes de enviar, guía al usuario con ERR-GEN-01 y ERR-GEN-02; si el backend responde con error, muestra su `mensaje` tal como llega. Con éxito guarda la sesión en el dispositivo y lleva al usuario a la página de inicio de su rol.

Además, tres piezas de sesión para toda la aplicación: un servicio que guarda y borra el token, un interceptor que lo agrega a cada petición al backend y, si el backend responde 401, borra el token y lleva al inicio de sesión con ERR-GEN-05 (CA6), y un guard que no deja entrar sin sesión a una ruta protegida (CA5).

## Contrato del servicio

Tomado del código; no cambia.

`POST /api/sesiones` · **público** (`identidad/infrastructure/security/ConfiguracionSeguridad.java`: `.requestMatchers(HttpMethod.POST, "/api/sesiones").permitAll()`). Controlador: `identidad/infrastructure/rest/SesionController.java`.

Entrada (`identidad/infrastructure/rest/SolicitudInicioSesion.java`):

```json
{ "correoElectronico": "santiago.ramirez@correo.com", "contrasena": "********" }
```

| Campo | Validación del backend | Código |
|---|---|---|
| `correoElectronico` | `@NotBlank`; `@Pattern("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")`; se le quitan los espacios de los extremos (AG-06) | ERR-GEN-01, ERR-GEN-02 |
| `contrasena` | `@NotBlank` (solo espacios cuenta como vacía); no se recorta | ERR-GEN-01 |

Respuesta `200` (`identidad/infrastructure/rest/RespuestaInicioSesion.java`, `identidad/domain/Rol.java`):

```json
{ "token": "eyJ...", "rol": "JUGADOR", "venceEn": "2026-11-26T23:00:00Z" }
```

`rol` es `JUGADOR`, `ADMINISTRADOR_CANCHA` (también el encargado de sede, D07) o `ADMINISTRADOR_SISTEMA`. La pantalla no usa `venceEn` (P2): el vencimiento lo detecta el backend con el 401.

Errores, con el formato `RespuestaError` (`transversal/infrastructure/rest/RespuestaError.java`, ya en `core/modelos.ts`):

| HTTP | Código | Cuándo | Origen |
|---|---|---|---|
| 400 | ERR-GEN-01 / ERR-GEN-02, con `campo` | Dato vacío o correo sin formato (CA2); un mensaje por campo | `ManejadorErrores.camposInvalidos` |
| 422 | ERR-USU-06 | El correo no está registrado (CA3) | `IniciarSesionCasoUso`, política 6; 422 por defecto en `ManejadorErrores` |
| 422 | ERR-USU-07 | La contraseña no coincide (CA4); mismo texto que ERR-USU-06 | Política 7 |
| 422 | ERR-USU-16 | Cuenta u origen bloqueado (CA7); el `mensaje` ya trae los minutos | D28 |
| 400 / 500 | ERR-GEN-07 | JSON ilegible o falla inesperada (AG-10) | `ManejadorErrores` |

Cualquier otra ruta que exige sesión responde **401 con ERR-GEN-05** si falta el token, venció o fue alterado (`identidad/infrastructure/security/RespuestaSinSesion.java`). Las rutas protegidas esperan la cabecera `Authorization: Bearer <token>` (`oauth2ResourceServer().jwt()`).

## Diseño por capas

No cambia: el backend ya está ([`SesionController`](../../projects/api/src/main/java/co/edu/uco/canchaya/identidad/infrastructure/rest/SesionController.java), [`IniciarSesionCasoUso`](../../projects/api/src/main/java/co/edu/uco/canchaya/identidad/application/usecase/IniciarSesionCasoUso.java)), con sus pruebas en `SesionControllerTest`, `IniciarSesionCasoUsoTest` e `InicioSesionPuntaAPuntaTest`.

## Pantalla

- **Prototipo:** se sigue el de celular y el de computador: título «Iniciar sesión», una tarjeta con «Correo electrónico *» y «Contraseña *», el botón «Iniciar sesión» a todo el ancho y el enlace «¿No tiene cuenta? Regístrese». En computador la tarjeta queda centrada con ancho máximo; en celular ocupa el ancho. Diferencias que **no** se construyen: la lista «Usuarios de ejemplo» (es del prototipo, no de la HU), la etiqueta «HU2 · INICIO DE SESIÓN», el pie de página, la barra superior con modo oscuro y menú (la barra es de HU4; ver «Fuera de alcance») y el enlace «¿No tiene cuenta? Regístrese», que se agrega con la pantalla de HU1 (P3).
- **Ruta:** `/iniciar-sesion` en `app.routes.ts`, **pública** (AG-11). Carga el componente de forma diferida (`loadComponent`).
- **Página de inicio de cada rol (P5):** las rutas definitivas se fijan en un solo lugar (`identidad/pagina-inicio.ts`, un `Record<Rol, string>`) y se registran en `app.routes.ts` con el componente vacío provisional `core/pagina-pendiente.ts` (sin texto, por la regla 6). La HU de cada pantalla reemplaza su componente provisional.

  | Rol | Ruta | ¿Guard? | Por qué | Reemplaza el provisional |
  |---|---|---|---|---|
  | `JUGADOR` | `/canchas` | No | AG-11: buscar canchas no exige sesión; el backend la deja pública (`/api/canchas/busqueda`, HU18) | HU18 |
  | `ADMINISTRADOR_CANCHA` | `/mis-escenarios` | Sí | D07: el encargado de sede también tiene este rol; «mis escenarios» cubre los propios y los asignados | Ninguna HU define esta pantalla; lo más parecido en el backend es `GET /api/agenda/escenarios` (HU29, `AgendaEscenariosController`), que lista los escenarios del propietario y del encargado |
  | `ADMINISTRADOR_SISTEMA` | `/administracion` | Sí | Ahí estarán la administración de escenarios (HU13) y los tipos de cancha (HU14 a HU17) | HU13 |

  El guard solo revisa que haya sesión, no el rol (AG-12): un jugador que abra `/administracion` ve la página vacía, y el backend es el que niega los datos.
- **Componentes y servicios:**
  - `identidad/inicio-sesion/inicio-sesion.ts` (+ `.html`, `.scss`, `.spec.ts`): componente standalone con formulario reactivo; Angular Material `MatFormField`, `MatInput`, `MatButton` y `MatError`.
  - `identidad/sesion.service.ts`: `iniciar(correo, contrasena)` llama a `POST {apiUrl}/sesiones`, y con el `200` guarda `token` y `rol` en `localStorage` (P1); `venceEn` no se guarda porque nadie lo lee (P2). También `token()`, `rol()`, `haySesion()` (solo mira si hay token guardado, P2) y `cerrar()` (borra lo guardado; lo usará HU4). Como un `@Service` de Spring que además guarda el estado de la sesión. Con este archivo se borra `identidad/.gitkeep`.
  - `core/sesion.interceptor.ts` (interceptor funcional, `HttpInterceptorFn`): agrega `Authorization: Bearer <token>` a las peticiones que van a `environment.apiUrl` cuando hay token. Si la respuesta es **401**, borra la sesión y navega a `/iniciar-sesion` con el `mensaje` de la `RespuestaError` (ERR-GEN-05) (CA6). Equivale a un filtro de Spring Security, pero del lado del cliente.
  - `core/sesion.guard.ts` (`CanActivateFn`): si no hay sesión, no deja entrar y navega a `/iniciar-sesion` con `MensajesService.texto('ERR-GEN-05')` (CA5). Equivale a `.authenticated()` en `ConfiguracionSeguridad`, solo que aquí guía y el backend decide.
  - `app.config.ts`: `provideHttpClient(withInterceptors([sesionInterceptor]))`.
  - Ubicación (P4): lo del módulo de identidad en `identidad/` y lo que usa toda la aplicación en `core/` (equivale a `transversal`). No se crean carpetas.
- **Aviso de ERR-GEN-05:** el guard y el interceptor pasan el texto en el `state` de la navegación (`router.navigate(['/iniciar-sesion'], { state: { aviso } })`) y la pantalla lo muestra arriba del formulario. No va en la URL.
- **Formulario:** validaciones que solo guían (AC-03.1, AG-01) y que repiten las del backend:
  - `correoElectronico`: obligatorio (vacío o solo espacios → `mensajes.obligatorio('correoElectronico')`, ERR-GEN-01) y formato con la misma expresión del backend sobre el valor sin espacios en los extremos (→ `mensajes.formatoInvalido('correoElectronico')`, ERR-GEN-02). Se envía recortado (AG-06).
  - `contrasena`: `type="password"` (AG-13), obligatoria (vacía o solo espacios → ERR-GEN-01). Se envía tal cual.
  - El mensaje se muestra junto al campo al salir de él (AG-02) y en todos los campos al pulsar «Iniciar sesión» (AG-03). Si hay errores no se llama al backend (CA2).
  - Mientras espera la respuesta, el botón queda deshabilitado para no enviar dos veces.
- **Respuestas del backend:**
  - `200`: guarda la sesión y navega a la página de inicio del rol (CA1, P5).
  - `400` con `campo`: el `mensaje` junto a ese campo.
  - `422` (ERR-USU-06, 07, 16) o cualquier error sin `campo`: el `mensaje` tal como llega, en un aviso arriba del botón.
  - Sin cuerpo `RespuestaError` (sin conexión o falla del servidor): `mensajes.texto('ERR-GEN-07')` (AG-10).
  - Con cualquier error se conserva el correo y se borra la contraseña (AG-04).
- **CA en la pantalla:** ver la tabla siguiente (filas «Pantalla»). Las pruebas usan `HttpTestingController` en lugar del backend y un `Router` de prueba para verificar la navegación.

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1 | Con credenciales correctas: token, rol y vencimiento | `SesionControllerTest.ca1ConCredencialesCorrectasRespondeElTokenElRolYElVencimiento` (ya existe) | REST |
| CA1 | `iniciar` hace `POST /sesiones` con el correo recortado y guarda token y rol en `localStorage` | `sesion.service.spec.ts` · `it('CA1: guarda la sesión que responde el backend')` | Pantalla |
| CA1 | Con `200`, lleva a la página de inicio de cada uno de los tres roles (`/canchas`, `/mis-escenarios`, `/administracion`) | `inicio-sesion.spec.ts` · `it('CA1: lleva a la página de inicio del rol …')` (una por rol) | Pantalla |
| CA5 | `/mis-escenarios` y `/administracion` tienen el guard; `/canchas` e `/iniciar-sesion` no (AG-11) | `app.routes.spec.ts` · `it('CA5: …')` | Pantalla |
| CA1 | El interceptor agrega `Authorization: Bearer` a las peticiones al backend cuando hay sesión, y no la agrega sin sesión | `sesion.interceptor.spec.ts` · `it('CA1: envía el token guardado …')` | Pantalla |
| CA2 | Datos vacíos o correo sin formato: 400 sin autenticar | `SesionControllerTest.ca2…` (ya existen) | REST |
| CA2 | Vacíos (y solo espacios) → ERR-GEN-01; correo sin formato → ERR-GEN-02; **no** sale ninguna petición | `inicio-sesion.spec.ts` · `it('CA2: …')` con `httpTesting.expectNone` | Pantalla |
| CA2 | Al salir de un campo con error se muestra su mensaje (AG-02) | `inicio-sesion.spec.ts` · `it('CA2: valida el campo al salir de él')` | Pantalla |
| CA3, CA4 | 422 con ERR-USU-06 / ERR-USU-07 y el mismo texto | `SesionControllerTest.ca3…`, `ca4…` (ya existen) | REST |
| CA3, CA4 | Muestra el `mensaje` de la respuesta tal como llega, no guarda sesión, conserva el correo y borra la contraseña | `inicio-sesion.spec.ts` · `it('CA3: …')`, `it('CA4: …')` | Pantalla |
| CA5 | Sin token, una ruta protegida responde 401 con ERR-GEN-05 | `InicioSesionPuntaAPuntaTest` (ya existe) | Punta a punta |
| CA5 | El guard no deja entrar sin sesión, lleva a `/iniciar-sesion` y la pantalla muestra ERR-GEN-05 | `sesion.guard.spec.ts` · `it('CA5: …')` e `inicio-sesion.spec.ts` · `it('CA5: muestra el aviso que llega en la navegación')` | Pantalla |
| CA6 | Con el token vencido, una ruta protegida responde 401 con ERR-GEN-05 | `InicioSesionPuntaAPuntaTest` (ya existe) | Punta a punta |
| CA6 | Con un 401, el interceptor borra el token y navega a `/iniciar-sesion` con el `mensaje` ERR-GEN-05 | `sesion.interceptor.spec.ts` · `it('CA6: …')` | Pantalla |
| CA7 | Cuenta u origen bloqueado: 422 con ERR-USU-16 y los minutos | `SesionControllerTest.ca7…` (ya existe) | REST |
| CA7 | Muestra el `mensaje` de ERR-USU-16 tal como llega (con los minutos) | `inicio-sesion.spec.ts` · `it('CA7: …')` | Pantalla |
| AG-10 | Sin `RespuestaError` (status 0 o 500) muestra ERR-GEN-07 | `inicio-sesion.spec.ts` · `it('AG-10: …')` | Pantalla |

## Preguntas abiertas

Ninguna. Las cinco se resolvieron el 26-nov-2026:

- **P1 · Dónde se guarda la sesión → `localStorage`.** CA1 dice que el token «se guarda en el dispositivo» y D37 (2), que al cerrar sesión «se elimina el token del dispositivo» (HU4); con `sessionStorage` esa regla no tendría sentido. El riesgo de que un script malicioso lo lea (XSS) es bajo: Angular escapa lo que muestra y el token dura 60 minutos (PAR-USU-09).
- **P2 · La pantalla no revisa `venceEn`.** AG-17 y D27 descartan la hora del dispositivo y RT-01 dice que la pantalla no decide. CA6 detecta el vencimiento «cuando ejecute cualquier acción que exija autenticación»: es el 401 que recibe el interceptor. `POST /api/sesiones` responde 422 en sus errores, nunca 401, así que un inicio de sesión fallido no activa el interceptor. Como nadie lo lee, `venceEn` no se guarda.
- **P3 · El enlace «¿No tiene cuenta? Regístrese» se agrega con HU1**, cuando exista su ruta. Quedó anotado en la fila de HU1 de «Pendientes» en [specs/README.md](../README.md).
- **P4 · Ubicación:** `identidad/sesion.service.ts`, `core/sesion.interceptor.ts` y `core/sesion.guard.ts`, como dice `projects/web/AGENTS.md` (`core/` equivale a `transversal`; cada área, a su módulo). No se crean carpetas; se borra `identidad/.gitkeep`.
- **P5 · Rutas definitivas con un componente vacío provisional**, con los nombres de la tabla de «Página de inicio de cada rol». Se mantiene `/mis-escenarios` (se descartó `/agenda`): coincide con la conversación de HU2 («a sus escenarios») y con los prototipos 06, 07 y 17, que muestran «Mis escenarios» en la barra superior y como enlace para volver.

**Para reportar al Excel (no bloquea HU2):** la pantalla «Mis escenarios» no tiene HU ni prototipo propio (solo aparece nombrada en la barra y en el enlace para volver de los prototipos 06 y 07), y el backend no tiene un servicio dedicado; lo más cercano es `GET /api/agenda/escenarios` (HU29). Hay que definirla con una HU nueva o con su prototipo dentro de una HU existente (por ejemplo, HU5).

## Fuera de alcance

- La barra superior del prototipo (logo, «Buscar canchas», «Iniciar sesión», «Crear cuenta», modo oscuro y menú de celular): el menú de usuario es HU4. Se deja la `mat-toolbar` actual.
- Cerrar sesión (HU4): `SesionService.cerrar()` queda listo, pero el botón es de HU4.
- Volver a la página que el usuario intentaba abrir después de iniciar sesión: la HU pide ir a la página de inicio del rol (CA1).
- Ocultar rutas según el rol: los permisos los valida el servidor (AG-12); el guard solo revisa que haya sesión.
- La lista «Usuarios de ejemplo» del prototipo.
- El enlace a registrarse (P3): va con HU1.
- Las pantallas de `/canchas` (HU18), `/mis-escenarios` (sin HU que la defina) y `/administracion` (HU13): aquí solo quedan sus rutas con el componente provisional.
- `LOCALE_ID`: esta pantalla no muestra fechas.
