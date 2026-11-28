import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { environment } from '../../../environments/environment';
import { routes } from '../../app.routes';
import { MensajesService } from '../../core/mensajes.service';
import { sesionInterceptor } from '../../core/sesion.interceptor';
import { PAGINA_INICIO } from '../pagina-inicio';
import { Rol, SesionService } from '../sesion.service';

const URL_SESIONES = `${environment.apiUrl}/sesiones`;
// Mismo texto de ERR-USU-06 y ERR-USU-07: la pantalla lo muestra tal como llega
const INCORRECTOS = 'El correo electrónico o la contraseña son incorrectos.';

// El backend (SesionController) lo reemplaza HttpTestingController; las rutas son las reales de app.routes.ts
describe('InicioSesion', () => {
  let harness: RouterTestingHarness;
  let backend: HttpTestingController;
  let mensajes: MensajesService;
  let pagina: HTMLElement;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(withInterceptors([sesionInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    mensajes = TestBed.inject(MensajesService);
  });

  afterEach(() => backend.verify());

  async function abrir(aviso?: string) {
    harness = await RouterTestingHarness.create();
    if (aviso) {
      await TestBed.inject(Router).navigateByUrl('/iniciar-sesion', { state: { aviso } });
    } else {
      await harness.navigateByUrl('/iniciar-sesion');
    }
    harness.detectChanges();
    pagina = harness.routeNativeElement!;
  }

  function campo(nombre: string): HTMLInputElement {
    return pagina.querySelector(`input[formcontrolname="${nombre}"]`)!;
  }

  function escribir(nombre: string, valor: string) {
    const input = campo(nombre);
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    harness.detectChanges();
  }

  function enviar() {
    pagina.querySelector('form')!.dispatchEvent(new Event('submit'));
    harness.detectChanges();
  }

  function errores(): string[] {
    return [...pagina.querySelectorAll('mat-error')].map((e) => e.textContent!.trim());
  }

  function aviso(): string | undefined {
    return pagina.querySelector('[role="alert"]')?.textContent?.trim();
  }

  async function responderError(status: number, cuerpo: object) {
    backend.expectOne(URL_SESIONES).flush(cuerpo, { status, statusText: 'Error' });
    await harness.fixture.whenStable();
    harness.detectChanges();
  }

  it('AG-13: la contraseña va en un campo oculto', async () => {
    await abrir();
    expect(campo('contrasena').type).toBe('password');
  });

  describe('CA1: credenciales correctas', () => {
    const casos: Rol[] = ['JUGADOR', 'ADMINISTRADOR_CANCHA', 'ADMINISTRADOR_SISTEMA'];

    for (const rol of casos) {
      it(`CA1: lleva a la página de inicio del rol ${rol}`, async () => {
        await abrir();
        escribir('correoElectronico', ' santiago@correo.com ');
        escribir('contrasena', 'Clave123*');
        enviar();

        const peticion = backend.expectOne(URL_SESIONES);
        expect(peticion.request.body).toEqual({ correoElectronico: 'santiago@correo.com', contrasena: 'Clave123*' });
        peticion.flush({ token: 'token-de-prueba', rol, venceEn: '2026-11-26T23:00:00Z' });
        await harness.fixture.whenStable();

        expect(TestBed.inject(Router).url).toBe(PAGINA_INICIO[rol]);
        expect(TestBed.inject(SesionService).token()).toBe('token-de-prueba');
      });
    }

    it('mientras espera la respuesta, el botón queda deshabilitado', async () => {
      await abrir();
      escribir('correoElectronico', 'santiago@correo.com');
      escribir('contrasena', 'Clave123*');
      enviar();

      expect(pagina.querySelector('button')!.disabled).toBe(true);
      await responderError(422, { errores: [{ codigo: 'ERR-USU-06', mensaje: INCORRECTOS }] });
      expect(pagina.querySelector('button')!.disabled).toBe(false);
    });
  });

  describe('CA2: validaciones antes de enviar', () => {
    it('CA2: con los dos campos vacíos muestra ERR-GEN-01 en cada uno y no intenta la autenticación', async () => {
      await abrir();
      enviar();

      expect(errores()).toEqual([mensajes.obligatorio('correoElectronico'), mensajes.obligatorio('contrasena')]);
      backend.expectNone(URL_SESIONES);
    });

    it('CA2: un campo con solo espacios cuenta como vacío (AG-06)', async () => {
      await abrir();
      escribir('correoElectronico', '   ');
      escribir('contrasena', '   ');
      enviar();

      expect(errores()).toEqual([mensajes.obligatorio('correoElectronico'), mensajes.obligatorio('contrasena')]);
      backend.expectNone(URL_SESIONES);
    });

    it('CA2: con un correo sin formato válido muestra ERR-GEN-02 y no intenta la autenticación', async () => {
      await abrir();
      escribir('correoElectronico', 'santiago@correo');
      escribir('contrasena', 'Clave123*');
      enviar();

      expect(errores()).toEqual([mensajes.formatoInvalido('correoElectronico')]);
      backend.expectNone(URL_SESIONES);
    });

    it('CA2: valida el campo al salir de él (AG-02)', async () => {
      await abrir();
      expect(errores()).toEqual([]);

      escribir('correoElectronico', 'sin-arroba');

      expect(errores()).toEqual([mensajes.formatoInvalido('correoElectronico')]);
    });
  });

  describe('errores del backend', () => {
    async function intentar() {
      await abrir();
      escribir('correoElectronico', 'santiago@correo.com');
      escribir('contrasena', 'Clave123*');
      enviar();
    }

    it('CA3: correo no registrado: muestra el mensaje tal como llega, conserva el correo y borra la contraseña', async () => {
      await intentar();
      await responderError(422, { errores: [{ codigo: 'ERR-USU-06', mensaje: INCORRECTOS }] });

      expect(aviso()).toBe(INCORRECTOS);
      expect(campo('correoElectronico').value).toBe('santiago@correo.com');
      expect(campo('contrasena').value).toBe('');
      // La contraseña borrada no se marca como error (AG-04)
      expect(errores()).toEqual([]);
      expect(TestBed.inject(SesionService).haySesion()).toBe(false);
    });

    it('CA4: contraseña que no coincide: muestra el mismo mensaje tal como llega', async () => {
      await intentar();
      await responderError(422, { errores: [{ codigo: 'ERR-USU-07', mensaje: INCORRECTOS }] });

      expect(aviso()).toBe(INCORRECTOS);
      expect(campo('contrasena').value).toBe('');
      expect(TestBed.inject(SesionService).haySesion()).toBe(false);
    });

    it('CA7: cuenta u origen bloqueado: muestra ERR-USU-16 con los minutos, tal como llega', async () => {
      const bloqueo = mensajes.texto('ERR-USU-16', 15);
      await intentar();
      await responderError(422, { errores: [{ codigo: 'ERR-USU-16', mensaje: bloqueo }] });

      expect(aviso()).toBe(bloqueo);
      expect(TestBed.inject(SesionService).haySesion()).toBe(false);
    });

    it('CA2: un error de un campo que responde el backend se muestra junto a ese campo', async () => {
      await intentar();
      await responderError(400, {
        errores: [{ codigo: 'ERR-GEN-02', campo: 'correoElectronico', mensaje: 'Mensaje del backend para el correo' }],
      });

      expect(errores()).toEqual(['Mensaje del backend para el correo']);
      expect(aviso()).toBeUndefined();
    });

    it('AG-10: sin respuesta del backend muestra ERR-GEN-07', async () => {
      await intentar();
      backend.expectOne(URL_SESIONES).error(new ProgressEvent('error'));
      await harness.fixture.whenStable();
      harness.detectChanges();

      expect(aviso()).toBe(mensajes.texto('ERR-GEN-07'));
      expect(campo('contrasena').value).toBe('');
    });
  });

  describe('CA5 y CA6: aviso de sesión', () => {
    it('CA5: muestra el aviso ERR-GEN-05 que llega en la navegación', async () => {
      await abrir(mensajes.texto('ERR-GEN-05'));

      expect(pagina.querySelector('[role="status"]')?.textContent?.trim()).toBe(mensajes.texto('ERR-GEN-05'));
    });

    it('CA5: al entrar a una ruta protegida sin sesión, termina en esta pantalla con ERR-GEN-05', async () => {
      harness = await RouterTestingHarness.create();
      await harness.navigateByUrl('/mis-escenarios');
      harness.detectChanges();
      pagina = harness.routeNativeElement!;

      expect(TestBed.inject(Router).url).toBe('/iniciar-sesion');
      expect(pagina.querySelector('[role="status"]')?.textContent?.trim()).toBe(mensajes.texto('ERR-GEN-05'));
    });

    it('sin aviso en la navegación no muestra nada arriba del formulario', async () => {
      await abrir();

      expect(pagina.querySelector('[role="status"]')).toBeNull();
    });
  });
});
