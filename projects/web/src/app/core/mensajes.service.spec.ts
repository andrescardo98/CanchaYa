import { TestBed } from '@angular/core/testing';
import { MensajesService } from './mensajes.service';

// Los textos esperados son los mismos que arma el backend (CatalogoMensajes y ManejadorErrores)
describe('MensajesService', () => {
  let mensajes: MensajesService;

  beforeEach(() => {
    mensajes = TestBed.inject(MensajesService);
  });

  describe('texto', () => {
    it('devuelve el texto de un mensaje sin parámetros', () => {
      expect(mensajes.texto('ERR-GEN-05')).toBe(
        'Su sesión no está iniciada o venció. Inicie sesión para continuar.',
      );
    });

    it('reemplaza los parámetros en el orden en que aparecen', () => {
      expect(mensajes.texto('ERR-USU-16', 15)).toBe(
        'Superó el número de intentos permitidos para iniciar sesión. Intente de nuevo en 15 minutos.',
      );
      expect(mensajes.texto('ERR-GEN-04', 'radio de búsqueda', 1, 30)).toBe(
        'El valor de radio de búsqueda debe estar entre 1 y 30.',
      );
    });

    it('deja el marcador cuando falta el parámetro, como el backend', () => {
      expect(mensajes.texto('ERR-GEN-04', 'radio de búsqueda')).toBe(
        'El valor de radio de búsqueda debe estar entre {minimo} y {maximo}.',
      );
    });

    it('falla si el código no está en el catálogo', () => {
      expect(() => mensajes.texto('ERR-XXX-99')).toThrow('El catálogo no tiene el mensaje ERR-XXX-99');
    });

    it('no trae los mensajes de tipo Técnico, que solo van al log', () => {
      expect(() => mensajes.texto('ERR-GEN-08')).toThrow();
    });
  });

  describe('mensajes de campo (ManejadorErrores.mensajeDeCampo)', () => {
    it('ERR-GEN-01: obligatorio', () => {
      expect(mensajes.obligatorio('correoElectronico')).toBe(
        'El campo correo electrónico es obligatorio.',
      );
    });

    it('ERR-GEN-02: formato inválido con el formato esperado del campo', () => {
      expect(mensajes.formatoInvalido('correoElectronico')).toBe(
        'El campo correo electrónico no tiene un formato válido. Ejemplo: nombre@correo.com',
      );
    });

    it('ERR-GEN-03: longitud con mínimo y máximo', () => {
      expect(mensajes.longitud('nombre', 2, 80)).toBe(
        'El campo nombre debe tener entre 2 y 80 caracteres.',
      );
    });

    it('ERR-GEN-03: con mínimo 0 usa el texto de solo máximo', () => {
      expect(mensajes.longitud('descripcion', 0, 500)).toBe(
        'El campo descripción debe tener máximo 500 caracteres.',
      );
    });

    it('ERR-GEN-04: rango', () => {
      expect(mensajes.rango('radioKm', 1, 30)).toBe(
        'El valor de radio de búsqueda debe estar entre 1 y 30.',
      );
    });

    it('falla si el campo no tiene nombre o formato en el catálogo', () => {
      expect(() => mensajes.obligatorio('inexistente')).toThrow(
        'El catálogo no tiene el nombre del campo inexistente',
      );
      expect(() => mensajes.formatoInvalido('rol')).toThrow(
        'El catálogo no tiene el formato del campo rol',
      );
    });
  });
});
