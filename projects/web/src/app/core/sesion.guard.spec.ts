import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RedirectCommand, RouterStateSnapshot, provideRouter } from '@angular/router';
import { SesionService } from '../identidad/sesion.service';
import { MensajesService } from './mensajes.service';
import { sesionGuard } from './sesion.guard';

describe('sesionGuard', () => {
  let sesion: SesionService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    sesion = TestBed.inject(SesionService);
  });

  function revisar() {
    return TestBed.runInInjectionContext(() =>
      sesionGuard({} as ActivatedRouteSnapshot, { url: '/mis-escenarios' } as RouterStateSnapshot),
    );
  }

  it('CA5: sin sesión no deja entrar y lleva al inicio de sesión con ERR-GEN-05', () => {
    const resultado = revisar();

    expect(resultado).toBeInstanceOf(RedirectCommand);
    const redireccion = resultado as RedirectCommand;
    expect(redireccion.redirectTo.toString()).toBe('/iniciar-sesion');
    expect(redireccion.navigationBehaviorOptions?.state).toEqual({
      aviso: TestBed.inject(MensajesService).texto('ERR-GEN-05'),
    });
  });

  it('con sesión deja entrar (si el token venció, lo dirá el backend con 401: CA6, P2)', () => {
    vi.spyOn(sesion, 'haySesion').mockReturnValue(true);

    expect(revisar()).toBe(true);
  });
});
