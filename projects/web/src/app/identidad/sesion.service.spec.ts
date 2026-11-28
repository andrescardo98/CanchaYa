import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { SesionService } from './sesion.service';

// El backend es SesionController (POST /api/sesiones); aquí lo reemplaza HttpTestingController
describe('SesionService', () => {
  let sesion: SesionService;
  let backend: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    sesion = TestBed.inject(SesionService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('CA1: guarda la sesión que responde el backend', async () => {
    const respuesta = firstValueFrom(sesion.iniciar('  santiago@correo.com ', 'Clave123*'));

    const peticion = backend.expectOne(`${environment.apiUrl}/sesiones`);
    expect(peticion.request.method).toBe('POST');
    // AG-06: el correo va sin espacios en los extremos; la contraseña, tal cual
    expect(peticion.request.body).toEqual({
      correoElectronico: 'santiago@correo.com',
      contrasena: 'Clave123*',
    });
    peticion.flush({ token: 'token-de-prueba', rol: 'JUGADOR', venceEn: '2026-11-26T23:00:00Z' });

    expect(await respuesta).toBe('JUGADOR');
    expect(sesion.token()).toBe('token-de-prueba');
    expect(sesion.rol()).toBe('JUGADOR');
    expect(sesion.haySesion()).toBe(true);
  });

  it('CA1: la sesión sigue en el dispositivo para otra instancia del servicio (localStorage, P1)', async () => {
    const respuesta = firstValueFrom(sesion.iniciar('carolina@correo.com', 'Clave123*'));
    backend
      .expectOne(`${environment.apiUrl}/sesiones`)
      .flush({ token: 'otro-token', rol: 'ADMINISTRADOR_CANCHA', venceEn: '2026-11-26T23:00:00Z' });
    await respuesta;

    const otra = TestBed.runInInjectionContext(() => new SesionService());
    expect(otra.token()).toBe('otro-token');
    expect(otra.rol()).toBe('ADMINISTRADOR_CANCHA');
  });

  it('P2: no guarda venceEn, porque la pantalla no lo usa', async () => {
    const respuesta = firstValueFrom(sesion.iniciar('santiago@correo.com', 'Clave123*'));
    backend
      .expectOne(`${environment.apiUrl}/sesiones`)
      .flush({ token: 't', rol: 'JUGADOR', venceEn: '2026-11-26T23:00:00Z' });
    await respuesta;

    const guardado = Object.keys(localStorage).map((clave) => localStorage.getItem(clave));
    expect(guardado).not.toContain('2026-11-26T23:00:00Z');
  });

  it('CA3, CA4: con un error del backend no guarda nada y deja pasar el error', async () => {
    const respuesta = firstValueFrom(sesion.iniciar('nadie@correo.com', 'Clave123*'));
    backend.expectOne(`${environment.apiUrl}/sesiones`).flush(
      { errores: [{ codigo: 'ERR-USU-06', mensaje: 'El correo electrónico o la contraseña son incorrectos.' }] },
      { status: 422, statusText: 'Unprocessable Content' },
    );

    await expect(respuesta).rejects.toMatchObject({ status: 422 });
    expect(sesion.haySesion()).toBe(false);
    expect(localStorage.length).toBe(0);
  });

  it('sin sesión iniciada no hay token ni rol', () => {
    expect(sesion.token()).toBeNull();
    expect(sesion.rol()).toBeNull();
    expect(sesion.haySesion()).toBe(false);
  });

  it('cerrar borra la sesión del dispositivo (D37, la usarán CA6 y HU4)', async () => {
    const respuesta = firstValueFrom(sesion.iniciar('santiago@correo.com', 'Clave123*'));
    backend
      .expectOne(`${environment.apiUrl}/sesiones`)
      .flush({ token: 't', rol: 'JUGADOR', venceEn: '2026-11-26T23:00:00Z' });
    await respuesta;

    sesion.cerrar();

    expect(sesion.haySesion()).toBe(false);
    expect(sesion.rol()).toBeNull();
    expect(localStorage.length).toBe(0);
  });
});
