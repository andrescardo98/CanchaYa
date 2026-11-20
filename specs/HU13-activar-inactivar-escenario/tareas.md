# HU13 · Tareas del arreglo de concurrencia

[spec.md](spec.md) está **Implementada**. En cada tarea se escribe primero la prueba del criterio.

- [x] T1 · Caso de uso · `CambiarEstadoEscenarioCasoUso` con `TransactionOperations` (P1) y los turnos de las canchas ordenados por `id` al inactivar (P2), con las pruebas nuevas en `CambiarEstadoEscenarioCasoUsoTest` (CA2, CA5, CA7 y CA8)
- [x] T2 · Punta a punta · `ca3InactivaUnEscenarioConCanchasSinReservasFuturas` en `CambioEstadoEscenarioPuntaAPuntaTest` (CA3 y CA7)
- [x] T2b · Punta a punta · en `CambioEstadoEscenarioPuntaAPuntaTest`, `ana@correo.com` pasa a `propietario.hu13@correo.com`, incluido el destinatario del correo: `InicioSesionPuntaAPuntaTest` deja bloqueada esa cuenta (D28) y, según el orden en que corran las pruebas, la de HU13 puede fallar con 422. #23 hace el mismo cambio
- [x] T3 · Verificación · corregir la P2 de la especificación de HU10 («tiene el mismo riesgo»), `./mvnw verify`, estado Implementada, fila en `specs/README.md` y `/pull-request` (unir antes de #23)
