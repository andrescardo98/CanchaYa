/**
 * Configuración para desarrollo local (ng serve). Reemplaza a environment.ts
 * con fileReplacements (angular.json); debe tener los mismos campos.
 */
export const environment = {
  /** Relativa: el proxy de ng serve (proxy.conf.json) la lleva a http://localhost:8080. */
  apiUrl: '/api',

  mapas: {
    urlTeselas: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    credito:
      '&copy; <a href="https://www.openstreetmap.org/copyright">Colaboradores de OpenStreetMap</a>',
  },
};
