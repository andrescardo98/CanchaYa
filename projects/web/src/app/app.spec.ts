import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
    }).compileComponents();
  });

  it('crea la aplicación', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('muestra el nombre de la plataforma en la barra superior', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const pagina = fixture.nativeElement as HTMLElement;
    expect(pagina.querySelector('mat-toolbar')?.textContent).toContain('CanchaYa');
  });
});
