import { inject } from '@angular/core';
import { CanActivateFn, RedirectCommand, Router } from '@angular/router';
import { RUTA_INICIO_SESION, SesionService } from '../identidad/sesion.service';
import { MensajesService } from './mensajes.service';

/**
 * Rutas que exigen sesión (AG-11), como .authenticated() en ConfiguracionSeguridad del backend.
 * Sin sesión no deja entrar y lleva al inicio de sesión con ERR-GEN-05 (CA5). Solo revisa que haya
 * token, no el rol ni si venció: los permisos y el vencimiento los decide el backend (AG-12, P2).
 */
export const sesionGuard: CanActivateFn = () => {
  if (inject(SesionService).haySesion()) {
    return true;
  }
  const aviso = inject(MensajesService).texto('ERR-GEN-05');
  return new RedirectCommand(inject(Router).parseUrl(RUTA_INICIO_SESION), { state: { aviso } });
};
