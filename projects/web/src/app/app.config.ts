import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS, MatFormFieldDefaultOptions } from '@angular/material/form-field';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { sesionInterceptor } from './core/sesion.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // HttpClient para llamar al backend (como un RestClient de Spring), con el token de la sesión (HU2)
    provideHttpClient(withInterceptors([sesionInterceptor])),
    // Todos los campos como en el prototipo: con borde y el rótulo siempre encima de la caja (styles.scss).
    // El espacio del mensaje de error crece con el texto para que no tape el campo siguiente.
    {
      provide: MAT_FORM_FIELD_DEFAULT_OPTIONS,
      useValue: {
        appearance: 'outline',
        floatLabel: 'always',
        subscriptSizing: 'dynamic',
      } satisfies MatFormFieldDefaultOptions,
    },
  ]
};
