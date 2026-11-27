import { Rol } from './sesion.service';

/**
 * Página de inicio de cada rol después de iniciar sesión (HU2, CA1 y P5 de su especificación):
 * el jugador a la búsqueda de canchas, el administrador de cancha (también el encargado de sede,
 * D07) a sus escenarios y el administrador del sistema a la administración de la plataforma.
 */
export const PAGINA_INICIO: Record<Rol, string> = {
  JUGADOR: '/canchas',
  ADMINISTRADOR_CANCHA: '/mis-escenarios',
  ADMINISTRADOR_SISTEMA: '/administracion',
};
