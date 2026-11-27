# HU2 · Tareas (solo la pantalla)

[spec.md](spec.md) está **Aprobada** (pantalla, 26-nov-2026). El backend ya está implementado. En cada tarea se escribe primero la prueba del criterio.

- [x] T1 · Pantalla · `identidad/sesion.service.ts` (`iniciar`, `token`, `rol`, `haySesion`, `cerrar`) que guarda token y rol en `localStorage`, `provideHttpClient` en `app.config.ts` y se borra `identidad/.gitkeep`, con `sesion.service.spec.ts` (CA1)
- [x] T2 · Pantalla · `core/sesion.interceptor.ts`: agrega `Authorization: Bearer` y, con un 401, borra la sesión y lleva a `/iniciar-sesion` con el `mensaje` ERR-GEN-05, con `sesion.interceptor.spec.ts` (CA1, CA6)
- [x] T3 · Pantalla · `core/sesion.guard.ts`, `identidad/pagina-inicio.ts` (ruta de cada rol), `core/pagina-pendiente.ts` y las rutas `/canchas`, `/mis-escenarios` y `/administracion` (las dos últimas con el guard), con `sesion.guard.spec.ts` y `app.routes.spec.ts` (CA5)
- [ ] T4 · Pantalla · Componente `identidad/inicio-sesion` con el formulario, las validaciones de guía y las respuestas del backend, ruta `/iniciar-sesion`, con `inicio-sesion.spec.ts` (CA1, CA2, CA3, CA4, CA5, CA7, AG-10)
- [ ] T5 · Verificación · `npx ng lint`, `npx ng test --watch=false` y `npx ng build`; prueba manual con el backend local en celular y computador; estado en **Implementada** y `/pull-request`
