import { Routes } from '@angular/router';
import { PaginaPendiente } from './core/pagina-pendiente';
import { sesionGuard } from './core/sesion.guard';

// Las rutas sin canActivate son públicas (AG-11); las que llevan sesionGuard exigen sesión (CA5 de HU2).
// Las páginas de inicio de cada rol (identidad/pagina-inicio.ts) usan PaginaPendiente hasta que exista su pantalla.
export const routes: Routes = [
  {
    path: 'iniciar-sesion', // HU2
    loadComponent: () => import('./identidad/inicio-sesion/inicio-sesion').then((m) => m.InicioSesion),
  },
  { path: 'canchas', component: PaginaPendiente }, // HU18: buscar canchas no exige sesión
  { path: 'mis-escenarios', component: PaginaPendiente, canActivate: [sesionGuard] },
  { path: 'administracion', component: PaginaPendiente, canActivate: [sesionGuard] }, // HU13
];
