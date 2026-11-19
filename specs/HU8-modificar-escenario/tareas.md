# HU8 · Tareas

[spec.md](spec.md) está **Aprobada**. En cada tarea se escribe primero la prueba del criterio.

- [x] T1 · Dominio · `Escenario.modificar(...)` con la firma de `clases.md` y `Escenario.validarPropietario(Solicitante)`, con `EscenarioModificarTest` (16 pruebas: CA1, CA4, CA6, CA8; políticas 3, 7 y 13)
- [x] T2 · Caso de uso · Sacar `ValidacionFotos` de `RegistrarEscenarioCasoUso` sin cambiar su comportamiento (`./mvnw verify` en verde el 18-nov-2026: 505 pruebas, incluida `RegistroEscenarioPuntaAPuntaTest`)
- [x] T3 · Caso de uso · `ModificarEscenario` y `ModificarEscenarioCasoUso` con `fotosConservadas` (P1), con `ModificarEscenarioCasoUsoTest` (14 pruebas: CA1, CA3 a CA6, CA8 a CA10 y P1; políticas 2, 3, 6, 7 y 13). Agrega `RepositorioEscenarios.actualizar` al puerto y a los 5 falsos de prueba; en `RepositorioEscenariosJpa` queda pendiente hasta T5
- [x] T4 · Caso de uso · `ConsultarEscenarioPropietario` y su caso de uso, con `ConsultarEscenarioPropietarioCasoUsoTest` (5 pruebas: CA1 formulario, también con el escenario INACTIVO, CA3, CA4; P2)
- [x] T5 · Persistencia · `RepositorioEscenarios.actualizar` con el reemplazo de fotos sin chocar `uq_foto_escenario_orden`, con `ModificacionEscenarioPersistenciaTest` (3 pruebas: CA1 datos, reordenar fotos y quitarlas todas)
- [x] T6 · REST · `GET` y `PUT /api/escenarios/{id}`, `SolicitudModificarEscenario`, `RespuestaEscenario`, `RespuestaModificarEscenario`, `ERR-ESC-07 → 403` y `ERR-GEN-07 → 400` (P1), `EXI-ESC-02` y `ERR-ESC-07` en el catálogo, `fotosConservadas` obligatoria (`ERR-GEN-01`, `[]` sí vale) con `campo.fotosConservadas` en el catálogo, con `ModificarEscenarioControllerTest` (16 pruebas: CA1 a CA6, CA8 a CA10 y P1). Los otros 3 `@WebMvcTest` del controlador reciben los 2 nuevos `@MockitoBean`
- [x] T7 · Punta a punta · `ModificarEscenarioPuntaAPuntaTest` (6 pruebas): modificar, ver el cambio en `/perfil`, quitar una foto y que se borre del disco, sin `fotosConservadas`, inexistente, no propietario y foto conservada ajena (CA1 a CA4, P1)
- [x] T8 · Verificación · `./mvnw verify` (18-nov-2026: 549 pruebas, BUILD SUCCESS) y `/pull-request`
