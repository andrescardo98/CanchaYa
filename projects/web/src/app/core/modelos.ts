/**
 * Formato de todos los errores que devuelve el backend.
 * Es el mismo record RespuestaError de transversal (ManejadorErrores).
 * `campo` solo llega cuando el error es de un dato del formulario.
 */
export interface RespuestaError {
  errores: { codigo: string; campo?: string; mensaje: string }[];
}
