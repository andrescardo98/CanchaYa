import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { MensajesService } from '../../core/mensajes.service';
import { RespuestaError } from '../../core/modelos';
import { PAGINA_INICIO } from '../pagina-inicio';
import { SesionService } from '../sesion.service';

type Campo = 'correoElectronico' | 'contrasena';

// La misma expresión de SolicitudInicioSesion en el backend: aquí solo guía al usuario (AC-03.1)
const FORMATO_CORREO = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

/** AG-06: un campo con solo espacios cuenta como vacío (ERR-GEN-01). */
function sinSoloEspacios(control: AbstractControl<string>): ValidationErrors | null {
  return control.value && control.value.trim() === '' ? { obligatorio: true } : null;
}

/** ERR-GEN-02, sobre el correo sin los espacios de los extremos (AG-06). */
function formatoCorreo(control: AbstractControl<string>): ValidationErrors | null {
  const correo = control.value?.trim() ?? '';
  return correo === '' || FORMATO_CORREO.test(correo) ? null : { formato: true };
}

/**
 * Muestra el error de un campo solo cuando el usuario ya salió de él o intentó enviar (AG-02, AG-03).
 * Así, la contraseña que se borra después de un error no aparece marcada como obligatoria (AG-04).
 */
const soloTocados: ErrorStateMatcher = {
  isErrorState: (control) => !!control && control.invalid && control.touched,
};

/**
 * Pantalla «Iniciar sesión» (HU2). Las validaciones solo guían al usuario antes de enviar; quien
 * decide es el backend (RT-01), y sus errores se muestran con el mensaje tal como llega.
 */
@Component({
  selector: 'app-inicio-sesion',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './inicio-sesion.html',
  styleUrl: './inicio-sesion.scss',
})
export class InicioSesion {
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);
  private readonly mensajes = inject(MensajesService);

  protected readonly soloTocados = soloTocados;

  protected readonly formulario = new FormGroup({
    correoElectronico: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, sinSoloEspacios, formatoCorreo],
    }),
    contrasena: new FormControl('', { nonNullable: true, validators: [Validators.required, sinSoloEspacios] }),
  });

  /** CA5, CA6: ERR-GEN-05 que mandan el guard o el interceptor en el state de la navegación. */
  protected readonly avisoSesion = signal<string | null>(
    (this.router.currentNavigation()?.extras.state?.['aviso'] as string | undefined) ?? null,
  );

  /** Error del backend que no es de un campo (CA3, CA4, CA7) o ERR-GEN-07 (AG-10). */
  protected readonly aviso = signal<string | null>(null);

  protected readonly enviando = signal(false);

  protected iniciarSesion(): void {
    this.aviso.set(null);
    // CA2: con datos inválidos no se intenta la autenticación y se muestran todos los mensajes (AG-03)
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.enviando.set(true);
    const { correoElectronico, contrasena } = this.formulario.getRawValue();
    this.sesion.iniciar(correoElectronico, contrasena).subscribe({
      // CA1: la página de inicio del rol
      next: (rol) => void this.router.navigateByUrl(PAGINA_INICIO[rol]),
      error: (error: unknown) => {
        // AG-04: se conserva lo escrito, menos la contraseña
        this.formulario.controls.contrasena.reset();
        this.mostrarError(error);
        this.enviando.set(false);
      },
    });
  }

  protected mensajeError(campo: Campo): string {
    const errores = this.formulario.controls[campo].errors;
    if (!errores) {
      return '';
    }
    if (errores['servidor']) {
      return errores['servidor'] as string;
    }
    if (errores['required'] || errores['obligatorio']) {
      return this.mensajes.obligatorio(campo);
    }
    return errores['formato'] ? this.mensajes.formatoInvalido(campo) : '';
  }

  private mostrarError(error: unknown): void {
    const errores = error instanceof HttpErrorResponse ? (error.error as RespuestaError | null)?.errores : undefined;
    if (!errores?.length) {
      this.aviso.set(this.mensajes.texto('ERR-GEN-07'));
      return;
    }

    const generales: string[] = [];
    for (const { campo, mensaje } of errores) {
      const control = campo ? this.formulario.get(campo) : null;
      if (control) {
        // El mensaje del backend junto a su campo, tal como llega
        control.setErrors({ servidor: mensaje });
        control.markAsTouched();
      } else {
        generales.push(mensaje);
      }
    }
    this.aviso.set(generales.length ? generales.join(' ') : null);
  }
}
