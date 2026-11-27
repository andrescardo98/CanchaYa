import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../environments/environment';

/** El enum Rol de identidad.domain en el backend. */
export type Rol = 'JUGADOR' | 'ADMINISTRADOR_CANCHA' | 'ADMINISTRADOR_SISTEMA';

/** RespuestaInicioSesion de SesionController. venceEn no se usa en la pantalla (P2 de la especificación de HU2). */
interface RespuestaInicioSesion {
  token: string;
  rol: Rol;
  venceEn: string;
}

const CLAVE_TOKEN = 'canchaya.token';
const CLAVE_ROL = 'canchaya.rol';

/**
 * Sesión del usuario (HU2). Inicia sesión contra el backend y guarda el token y el rol en
 * localStorage: «se guarda en el dispositivo» (CA1) hasta que vence o se cierra sesión (D37).
 * No decide si el token venció: eso lo responde el backend con 401 (CA6, P2).
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly http = inject(HttpClient);

  /** CA1. Devuelve el rol para llevar al usuario a su página de inicio. Si el backend responde con error, no guarda nada. */
  iniciar(correoElectronico: string, contrasena: string): Observable<Rol> {
    // AG-06: el correo sin espacios en los extremos, como lo recorta el backend; la contraseña, tal cual
    const solicitud = { correoElectronico: correoElectronico.trim(), contrasena };
    return this.http.post<RespuestaInicioSesion>(`${environment.apiUrl}/sesiones`, solicitud).pipe(
      tap((respuesta) => {
        localStorage.setItem(CLAVE_TOKEN, respuesta.token);
        localStorage.setItem(CLAVE_ROL, respuesta.rol);
      }),
      map((respuesta) => respuesta.rol),
    );
  }

  token(): string | null {
    return localStorage.getItem(CLAVE_TOKEN);
  }

  rol(): Rol | null {
    return localStorage.getItem(CLAVE_ROL) as Rol | null;
  }

  /** Solo dice si hay un token guardado; si ya venció, lo sabrá el backend (P2). */
  haySesion(): boolean {
    return this.token() !== null;
  }

  /** Borra la sesión del dispositivo (D37). La usan el interceptor con un 401 (CA6) y, más adelante, HU4. */
  cerrar(): void {
    localStorage.removeItem(CLAVE_TOKEN);
    localStorage.removeItem(CLAVE_ROL);
  }
}
