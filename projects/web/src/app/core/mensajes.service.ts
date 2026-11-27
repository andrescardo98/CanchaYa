import { Injectable } from '@angular/core';
import catalogo from './catalogo-mensajes.json';

export type Parametro = string | number;

const MARCADOR = /\{[^}]+\}/g;

/**
 * Textos del catálogo de mensajes por su código (regla 6 de AGENTS.md). Es el equivalente de
 * CatalogoMensajes del backend: lee catalogo-mensajes.json, que queda dentro del JavaScript al
 * compilar, y arma ERR-GEN-01 a 04 igual que ManejadorErrores.mensajeDeCampo, para que la
 * pantalla muestre el mismo texto que respondería el backend.
 */
@Injectable({ providedIn: 'root' })
export class MensajesService {
  private readonly mensajes: Record<string, string> = catalogo.mensajes;
  private readonly campos: Record<string, string> = catalogo.campos;
  private readonly formatos: Record<string, string> = catalogo.formatos;

  /** Los parámetros se reemplazan en el orden en que aparecen en el texto, como en el backend. */
  texto(codigo: string, ...parametros: Parametro[]): string {
    let siguiente = 0;
    return buscar(this.mensajes, codigo, 'el mensaje').replace(MARCADOR, (marcador) =>
      siguiente < parametros.length ? String(parametros[siguiente++]) : marcador,
    );
  }

  /** Nombre del campo como se ve en el formulario: el {campo} de ERR-GEN-01 a 04. */
  nombreCampo(atributo: string): string {
    return buscar(this.campos, atributo, 'el nombre del campo');
  }

  /** Descripción del formato del campo: el {formatoEsperado} de ERR-GEN-02. */
  formatoEsperado(atributo: string): string {
    return buscar(this.formatos, atributo, 'el formato del campo');
  }

  /** ERR-GEN-01. */
  obligatorio(atributo: string): string {
    return this.texto('ERR-GEN-01', this.nombreCampo(atributo));
  }

  /** ERR-GEN-02. */
  formatoInvalido(atributo: string): string {
    return this.texto('ERR-GEN-02', this.nombreCampo(atributo), this.formatoEsperado(atributo));
  }

  /** ERR-GEN-03. Si el mínimo es 0 se usa el texto «máximo {maximo} caracteres», como pide el catálogo. */
  longitud(atributo: string, minimo: number, maximo: number): string {
    const campo = this.nombreCampo(atributo);
    return minimo === 0
      ? this.texto('ERR-GEN-03.sin-minimo', campo, maximo)
      : this.texto('ERR-GEN-03', campo, minimo, maximo);
  }

  /** ERR-GEN-04. */
  rango(atributo: string, minimo: number, maximo: number): string {
    return this.texto('ERR-GEN-04', this.nombreCampo(atributo), minimo, maximo);
  }
}

// Como el backend: pedir un código que no está es un error de programación, no un texto vacío
function buscar(tabla: Record<string, string>, clave: string, que: string): string {
  if (!Object.hasOwn(tabla, clave)) {
    throw new Error(`El catálogo no tiene ${que} ${clave}`);
  }
  return tabla[clave];
}
