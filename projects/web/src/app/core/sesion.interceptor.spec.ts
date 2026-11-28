import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { SesionService } from '../identidad/sesion.service';
import { MensajesService } from './mensajes.service';
import { sesionInterceptor } from './sesion.interceptor';

// Una ruta protegida cualquiera del backend (HU23)
const RUTA_PROTEGIDA = `${environment.apiUrl}/reservas/mias`;

describe('sesionInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let sesion: SesionService;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([sesionInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    sesion = TestBed.inject(SesionService);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  describe('token en las peticiones', () => {
    it('CA1: envía el token guardado en las peticiones al backend', () => {
      vi.spyOn(sesion, 'token').mockReturnValue('token-de-prueba');

      http.get(RUTA_PROTEGIDA).subscribe();

      const peticion = backend.expectOne(RUTA_PROTEGIDA);
      expect(peticion.request.headers.get('Authorization')).toBe('Bearer token-de-prueba');
      peticion.flush({});
    });

    it('CA1: sin sesión no agrega la cabecera', () => {
      http.get(RUTA_PROTEGIDA).subscribe();

      const peticion = backend.expectOne(RUTA_PROTEGIDA);
      expect(peticion.request.headers.has('Authorization')).toBe(false);
      peticion.flush({});
    });

    it('no envía el token a otras direcciones, como las teselas del mapa', () => {
      vi.spyOn(sesion, 'token').mockReturnValue('token-de-prueba');
      const otra = 'https://tile.openstreetmap.org/1/0/0.png';

      http.get(otra).subscribe();

      const peticion = backend.expectOne(otra);
      expect(peticion.request.headers.has('Authorization')).toBe(false);
      peticion.flush({});
    });
  });

  describe('sesión vencida o sin sesión (401)', () => {
    it('CA6: borra la sesión y lleva al inicio de sesión con el mensaje ERR-GEN-05 que responde el backend', async () => {
      vi.spyOn(sesion, 'token').mockReturnValue('token-vencido');
      const cerrar = vi.spyOn(sesion, 'cerrar');
      const respuesta = firstValueFrom(http.get(RUTA_PROTEGIDA));

      // Como lo responde RespuestaSinSesion en el backend
      backend.expectOne(RUTA_PROTEGIDA).flush(
        { errores: [{ codigo: 'ERR-GEN-05', mensaje: 'Mensaje ERR-GEN-05 del backend' }] },
        { status: 401, statusText: 'Unauthorized' },
      );

      // La acción no se ejecuta: el error sigue hasta quien hizo la petición
      await expect(respuesta).rejects.toMatchObject({ status: 401 });
      expect(cerrar).toHaveBeenCalled();
      expect(navegar).toHaveBeenCalledWith(['/iniciar-sesion'], {
        state: { aviso: 'Mensaje ERR-GEN-05 del backend' },
      });
    });

    it('CA6: si el 401 llega sin cuerpo, el aviso es ERR-GEN-05 del catálogo', async () => {
      const respuesta = firstValueFrom(http.get(RUTA_PROTEGIDA));

      backend.expectOne(RUTA_PROTEGIDA).flush(null, { status: 401, statusText: 'Unauthorized' });

      await expect(respuesta).rejects.toMatchObject({ status: 401 });
      expect(navegar).toHaveBeenCalledWith(['/iniciar-sesion'], {
        state: { aviso: TestBed.inject(MensajesService).texto('ERR-GEN-05') },
      });
    });

    it('otros errores no cierran la sesión ni cambian de pantalla', async () => {
      vi.spyOn(sesion, 'token').mockReturnValue('token-de-prueba');
      const cerrar = vi.spyOn(sesion, 'cerrar');
      const respuesta = firstValueFrom(http.get(RUTA_PROTEGIDA));

      backend.expectOne(RUTA_PROTEGIDA).flush(
        { errores: [{ codigo: 'ERR-GEN-06', mensaje: 'Sin permiso' }] },
        { status: 403, statusText: 'Forbidden' },
      );

      await expect(respuesta).rejects.toMatchObject({ status: 403 });
      expect(cerrar).not.toHaveBeenCalled();
      expect(navegar).not.toHaveBeenCalled();
    });
  });
});
