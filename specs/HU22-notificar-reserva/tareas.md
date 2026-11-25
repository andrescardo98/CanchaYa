# HU22 · Tareas

[spec.md](spec.md) está **Aprobada** (24-nov-2026). En cada tarea se escribe primero la prueba del criterio.

- [x] T1 · Dominio · `Reserva.requiereConfirmacionAsistencia()` con `ReservaTest.CA1_requiereConfirmacionAsistencia_soloSiPendiente` (CA1, política 13, D33)
- [x] T2 · Transversal · `FormatoTexto` (fecha, hora y precio, AG-18 y AG-19), `RespuestaReserva` la usa en lugar de `precioConFormato` (P3), y las plantillas `COR-RES-01` (con `avisoConfirmacion`) y `COR-RES-02` en `catalogo-mensajes.properties`. Pruebas: `FormatoTextoTest` y `EnvioCorreoSmtpTest.CA1_armaCorRes01` (CA1, CA2)
- [ ] T3 · Puertos de otros módulos · `ContactoUsuario.numeroCelular` en Identidad (P4) y `ConsultaCanchas.buscarDatosCorreo` con `DatosCorreoCancha` en Escenarios (P2). Pruebas: `ConsultaUsuariosCasoUsoTest.HU22_CA2_traeElCelular` y `ConsultaCanchasCasoUsoTest.HU22_CA2_buscarDatosCorreo`; se ajustan los dobles de HU13 (CA2)
- [ ] T4 · Caso de uso · `ReservarCanchaCasoUso.notificar` después de `transaccion.execute`, cada correo en su `try/catch` con `ERR-GEN-08` (política 13, D15, P1). Pruebas en `ReservarCanchaCasoUsoTest`: CA1 sin aviso y con aviso, CA2, orden con la transacción (`InOrder`) y CA3 (falla del envío, datos no encontrados y log sin el celular, P4)
- [ ] T5 · Punta a punta · `ReservaPuntaAPuntaTest` con `@MockitoBean JavaMailSender`: `HU22_CA1_CA2_reservarEnviaLosDosCorreos` y `HU22_CA3_smtpFalla_reservaSigueConfirmada` (CA1, CA2, CA3)
- [ ] T6 · Verificación · `./mvnw verify` en verde (incluye `ArquitecturaTest`), estado de `spec.md` en **Implementada** y `/pull-request`
