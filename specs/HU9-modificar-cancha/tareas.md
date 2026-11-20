# HU9 · Tareas

[spec.md](spec.md) está **Implementada**. En cada tarea se escribe primero la prueba del criterio.

- [x] T1 · Dominio · `Cancha.modificar(escenario, tipoCanchaId, nombre, precioHora, solicitante)`, con `CanchaModificarTest` (CA1, CA4, CA7; políticas 2 y 5)
- [x] T2 · Caso de uso · `ModificarCancha` y `ModificarCanchaCasoUso`, con `ModificarCanchaCasoUsoTest` y `CanchasEnMemoria` (CA1, CA3 a CA7; políticas 7, 2, 8, 4 y 5). Agrega `existeNombreEnOtraCancha` y `actualizar` a `RepositorioCanchas` (`buscarPorId` ya existe desde HU20), a `CanchasEnMemoria` y a `CanchaConIdOcupado`; en `RepositorioCanchasJpa` quedan pendientes hasta T3
- [x] T3 · Persistencia · `existeNombreEnOtraCancha` y `actualizar` (ERR-CAN-08 si la base rechaza el nombre), con `ModificacionCanchaPersistenciaTest` (CA1, CA5 y CA8 con la reserva insertada por SQL, P2)
- [x] T4 · REST · `PUT /api/canchas/{id}` (P1), `SolicitudModificarCancha`, `RespuestaModificarCancha`, `ERR-CAN-07 → 404`, y `EXI-CAN-02`, `ERR-CAN-07` y `ERR-CAN-08` en el catálogo, con `ModificarCanchaControllerTest` (CA1 a CA7). Los otros `@WebMvcTest` de `CanchaController` reciben el nuevo `@MockitoBean`
- [x] T5 · Punta a punta · `ModificarCanchaPuntaAPuntaTest`: modificar y ver el cambio en la lista, inexistente y no propietario (CA1, CA3, CA4)
- [ ] T6 · Verificación · `./mvnw verify` y `/pull-request`
