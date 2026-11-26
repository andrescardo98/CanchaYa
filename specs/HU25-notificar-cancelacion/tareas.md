# HU25 · Tareas

[spec.md](spec.md) está **Aprobada** (25-nov-2026). En cada tarea se escribe primero la prueba del criterio.

- [x] T1 · Transversal · `FormatoTexto.fechaHora(OffsetDateTime)` en Bogotá (D27, AG-18) y las plantillas `COR-RES-03`, `COR-RES-04` y `COR-RES-05` en `catalogo-mensajes.properties`. Pruebas: `FormatoTextoTest.HU25_fechaHora_enBogota` y `EnvioCorreoSmtpTest.HU25_CA1_armaCorRes03`, `HU25_CA2_armaCorRes04` y `HU25_CA3_armaCorRes05` (CA1, CA2, CA3)
- [ ] T2 · Caso de uso · `NotificacionCancelacion.notificar` (política 21, D15, P1, P2, P3). Pruebas en `NotificacionCancelacionTest`: CA1, CA2, CA3, falla del envío, datos no encontrados y log sin datos del destinatario (CA4)
- [ ] T3 · Caso de uso · `CancelarReservaCasoUso` llama a `notificar` después de `transaccion.execute`. Pruebas en `CancelarReservaCasoUsoTest`: orden con la transacción (`InOrder`) y sin notificar si la cancelación falla (CA1)
- [ ] T4 · Punta a punta · `CancelacionPuntaAPuntaTest` con `@MockitoBean JavaMailSender`: `HU25_CA1_cancelarEnviaLosDosCorreos` y `HU25_CA4_smtpFalla_reservaSigueCancelada` (CA1, CA4)
- [ ] T5 · Verificación · `./mvnw verify` en verde (incluye `ArquitecturaTest`), estado de `spec.md` en **Implementada** y `/pull-request`
