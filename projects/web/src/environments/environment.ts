/**
 * Configuración del frontend publicado (como application.properties en el backend).
 * Estos valores quedan dentro del JavaScript que descarga el navegador:
 * aquí nunca van claves ni secretos.
 * Para `ng serve` (configuración development) este archivo se reemplaza por
 * environment.development.ts (fileReplacements en angular.json).
 */
export const environment = {
  /**
   * Dirección completa del backend publicado en Render. El backend solo acepta
   * llamadas del origen del frontend publicado (CORS, plataforma.md).
   */
  apiUrl: 'https://canchaya-mqo4.onrender.com/api',

  /** Mapas de OpenStreetMap con Leaflet (D31). Van aquí para cambiar de proveedor sin tocar las pantallas. */
  mapas: {
    urlTeselas: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    /** Crédito que exige la política de uso de OpenStreetMap. */
    credito:
      '&copy; <a href="https://www.openstreetmap.org/copyright">Colaboradores de OpenStreetMap</a>',
  },
};
