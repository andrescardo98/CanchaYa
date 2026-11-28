# AGENTS.md · Frontend

Reglas propias del frontend. Las generales (idioma, decisiones, mensajes, commits, arquitectura de módulos) están en el [AGENTS.md de la raíz](../../AGENTS.md). Para entender cada pieza de Angular comparada con Spring, sirve la guía de estudio «Angular para quien ya sabe Spring».

## Tecnología

- Angular 22 con TypeScript, sin `zone.js` (la pantalla se actualiza con signals) y con componentes *standalone* (sin `NgModule`).
- Angular Material y Angular CDK 22 para las piezas de pantalla. El tema está en `src/styles.scss` (`mat.theme`) y sigue la identidad visual de los prototipos (`docs/prototipos/`): verde `#03A14A`, letra Plus Jakarta Sans (paquete `@fontsource-variable/plus-jakarta-sans`, servida con la aplicación) y campos con el rótulo encima de la caja. Las paletas salen de `src/_theme-colors.scss`, generado con `npx ng generate @angular/material:theme-color`; los colores exactos de las capturas van en `mat.theme-overrides`. Las pantallas usan las variables del tema (`var(--mat-sys-primary)`…), no colores fijos.
- Pruebas con Vitest y jsdom (simula el navegador sin abrir ninguno). Revisión de estilo con angular-eslint, que incluye las reglas de accesibilidad de las plantillas.
- Node.js 24 (LTS) y npm. Angular CLI no se instala global: se usa con `npx ng`, que toma la versión del `package.json`.

## Comandos

Desde `projects/web/`:

| Para… | Comando |
|---|---|
| Instalar las dependencias (la primera vez y cada vez que cambie `package-lock.json`) | `npm install` (la CI usa `npm ci`) |
| Levantar el frontend en local | `npx ng serve` → <http://localhost:4200> |
| Correr las pruebas una vez | `npx ng test --watch=false` |
| Correr las pruebas cada vez que se guarda un archivo | `npx ng test` |
| Revisar el estilo | `npx ng lint` |
| Compilar como en Render | `npx ng build` → `dist/web/browser/` |
| Crear un componente con su prueba | `npx ng generate component <area>/<nombre>` |

`npx ng serve` usa el proxy de `proxy.conf.json`: las llamadas a `/api/...` llegan al backend local en `http://localhost:8080`, así que el backend debe estar corriendo (ver el [README](../../README.md#cómo-ejecutar-en-local)). La CI (`.github/workflows/frontend.yml`) corre `npm ci`, lint, build y pruebas en cada pull request que toque `projects/web/`.

## Configuración

Está en `src/environments/` y es el equivalente de `application.properties`, pero se fija **al compilar** y queda dentro del JavaScript que descarga el navegador: ahí nunca van claves ni secretos.

| Archivo | Cuándo se usa | `apiUrl` |
|---|---|---|
| `environment.ts` | `npx ng build` (configuración `production`, la que se publica) | Dirección completa del backend en Render |
| `environment.development.ts` | `npx ng serve` (configuración `development`) | `/api`, que atiende el proxy |

El código siempre importa `environment.ts`; en `development`, `angular.json` lo reemplaza por `environment.development.ts` (`fileReplacements`). Se reemplaza el archivo completo, así que los dos deben tener los mismos campos. Los dos guardan también la dirección de los mapas de OpenStreetMap y su texto de crédito (D31), para cambiar de proveedor sin tocar las pantallas.

## Estructura

```
src/
├── app/
│   ├── core/              # Piezas compartidas, como transversal: modelos.ts (RespuestaError),
│   │                      # mensajes.service.ts y catalogo-mensajes.json (generado, no se edita)
│   ├── identidad/         # Una carpeta por módulo del backend
│   ├── ubicacion/
│   ├── escenarios/
│   ├── reservas/
│   ├── reputacion/
│   ├── app.ts             # Componente raíz: barra superior y <router-outlet />
│   ├── app.config.ts      # Servicios de toda la aplicación (como una clase @Configuration)
│   └── app.routes.ts      # Rutas de las pantallas
├── environments/          # Configuración (ver arriba)
├── index.html             # La única página: lang="es-CO"
└── styles.scss            # Tema de Material y estilos globales
```

Cada pantalla va en la carpeta del área cuyo módulo del backend la atiende (`identidad/inicio-sesion/`, `reservas/disponibilidad/`…). Las carpetas de área tienen un `.gitkeep` mientras están vacías; se borra cuando llega el primer archivo. Si hace falta una carpeta que no está aquí, pregunta antes de crearla.

## Reglas

1. **Ninguna regla de negocio va en el frontend (RT-01).** La disponibilidad, los límites, los permisos y los estados los decide el backend; la pantalla solo muestra lo que responde. Las validaciones de los formularios solo guían al usuario antes de enviar (AC-03.1): repiten las del backend, no las reemplazan.
2. **No se escriben textos de mensajes a mano (regla 6 de la raíz).** Si el error viene del backend, se muestra el `mensaje` de la `RespuestaError` tal como llega. Los mensajes que detecta la pantalla antes de llamar al backend salen del catálogo por su código, con `MensajesService` (`texto(codigo, ...parametros)`, y `obligatorio`, `formatoInvalido`, `longitud` y `rango` para ERR-GEN-01 a 04, que arman el mismo texto que el backend). Sus textos están en `core/catalogo-mensajes.json`, que genera `scripts/exportar_docs.py`: no se edita a mano.
3. **Las fechas y horas se muestran en `America/Bogota` (D27)**, sin importar la zona horaria del dispositivo.
4. **Las pantallas se diseñan primero para el celular** y después se adaptan al computador: todas deben funcionar en los dos (AG-21, táctica AC-03.2).
5. **Cada CA de la pantalla tiene su prueba**, con el código del criterio en el nombre (`it('CA2: …')`). El backend se reemplaza con `HttpTestingController`.
6. **Configuración**: las direcciones (API, mapas) se leen de `environment`, nunca se escriben en los componentes ni en los servicios.

## Pendientes

- **`LOCALE_ID`** (formatos de fecha y número en español de Colombia): se configura cuando la primera pantalla muestre fechas. `index.html` ya declara `lang="es-CO"`.

## Despliegue en Render Static Site (todavía no se hace)

El frontend publicado llama al backend con su dirección completa (`apiUrl` de `environment.ts`) y el backend solo acepta llamadas del navegador desde el origen del frontend publicado (plataforma.md, «Orígenes permitidos (CORS)», ESC-SEG-02). No se usa una reescritura `/api/*` en el Static Site: sería otro intermediario antes del backend y podría alterar la IP real que llega en `X-Forwarded-For`, que usa el límite de intentos de inicio de sesión (D28).

**CORS en el backend es requisito para publicar el frontend.** Va en un pull request aparte, `feat(transversal): ...`. El origen permitido se lee de una variable de entorno con un valor por defecto para local en `application.properties`, como `DB_URL`. El orden es:

1. **Crear el Static Site** en Render: Root Directory `projects/web`, comando de compilación `npm ci && npx ng build`, carpeta de publicación `dist/web/browser` y variable de entorno `NODE_VERSION=24`. Sin esa variable, Render compila con su versión de Node por defecto, que puede no cumplir lo que exige Angular 22; con ella queda la misma versión que en la CI. Así se conoce su dirección.
2. **Poner la variable del origen permitido** en el Web Service del backend, con la dirección del Static Site.
3. **Unir el PR de CORS.** Render publica el backend cuando la CI termina en verde.

**Toda ruta responde con `index.html`.** En el Static Site se agrega la regla de reescritura `/*` → `/index.html`. Angular maneja las rutas en el navegador; sin esta regla, recargar una pantalla como `/escenarios/123` da 404.
