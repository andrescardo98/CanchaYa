import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { sesionInterceptor } from './core/sesion.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // HttpClient para llamar al backend (como un RestClient de Spring), con el token de la sesión (HU2)
    provideHttpClient(withInterceptors([sesionInterceptor])),
  ]
};
