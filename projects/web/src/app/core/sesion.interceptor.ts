import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { RUTA_INICIO_SESION, SesionService } from '../identidad/sesion.service';
import { MensajesService } from './mensajes.service';
import { RespuestaError } from './modelos';

/**
 * Pasa por todas las peticiones HTTP, como un filtro de Spring Security pero en el navegador (HU2).
 * - Agrega el token a las peticiones al backend (CA1), solo a las que van a environment.apiUrl,
 *   para no enviarlo a otros servidores (por ejemplo, el de los mapas).
 * - Si el backend responde 401 (sin sesión o con el token vencido o alterado, RespuestaSinSesion),
 *   borra la sesión del dispositivo y lleva al inicio de sesión con ERR-GEN-05 (CA6). El error
 *   sigue hasta quien hizo la petición: la acción no se ejecuta.
 */
export const sesionInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  if (!vaAlBackend(peticion.url)) {
    return siguiente(peticion);
  }

  const sesion = inject(SesionService);
  const router = inject(Router);
  const mensajes = inject(MensajesService);

  const token = sesion.token();
  const conToken = token ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : peticion;

  return siguiente(conToken).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        sesion.cerrar();
        // El mensaje tal como llega (ERR-GEN-05); sin cuerpo, el mismo texto desde el catálogo
        const aviso = (error.error as RespuestaError | null)?.errores?.[0]?.mensaje ?? mensajes.texto('ERR-GEN-05');
        void router.navigate([RUTA_INICIO_SESION], { state: { aviso } });
      }
      return throwError(() => error);
    }),
  );
};

function vaAlBackend(url: string): boolean {
  return url === environment.apiUrl || url.startsWith(`${environment.apiUrl}/`);
}
