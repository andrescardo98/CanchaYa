import { routes } from './app.routes';
import { sesionGuard } from './core/sesion.guard';
import { PAGINA_INICIO } from './identidad/pagina-inicio';

function ruta(url: string) {
  return routes.find((r) => `/${r.path}` === url);
}

describe('rutas', () => {
  it('CA1: la página de inicio de cada rol es una ruta que existe', () => {
    for (const url of Object.values(PAGINA_INICIO)) {
      expect(ruta(url), url).toBeDefined();
    }
  });

  it('CA5: /mis-escenarios y /administracion exigen sesión', () => {
    expect(ruta('/mis-escenarios')?.canActivate).toContain(sesionGuard);
    expect(ruta('/administracion')?.canActivate).toContain(sesionGuard);
  });

  it('AG-11: /canchas e /iniciar-sesion no exigen sesión', () => {
    expect(ruta('/canchas')?.canActivate).toBeUndefined();
    expect(ruta('/iniciar-sesion')).toBeDefined();
    expect(ruta('/iniciar-sesion')?.canActivate).toBeUndefined();
  });
});
