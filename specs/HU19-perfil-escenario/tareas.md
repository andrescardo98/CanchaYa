# HU19 · Tareas

[spec.md](spec.md) está **Aprobada**. En cada tarea se escribe primero la prueba del criterio.

- [x] T1 · Dominio · `Escenario.calcularCalificacionPromedio(List<Short>)` y `Cancha.publicarEnBusqueda(Escenario, boolean escenarioTieneHorarios)` con las firmas de `clases.md`, y sus pruebas `EscenarioCalificacionPromedioTest` y `CanchaPublicarEnBusquedaTest` (CA1 promedio y canchas publicables, CA2; políticas 11 y 12 de Escenario, política 11 de Cancha). Se corrigió la celda K62 de la hoja Cancha del Excel de clases (P5)
- [x] T2 · Puertos · `ConsultarPerfilEscenario` (entrada, con los records `PerfilEscenario` y `CanchaPerfil`), `ConsultaCalificaciones` (salida) y `escenarios.infrastructure.calificaciones.ConsultaCalificacionesSinResenas` (P6), con `ConsultaCalificacionesSinResenasTest` y `ConexionConsultaCalificacionesTest` (esta necesita Docker; se confirma en T7)
- [x] T3 · Caso de uso · `ConsultarPerfilEscenarioCasoUso`: ERR-ESC-06 con `estaActivo()`, canchas publicables con `RepositorioHorarios.buscarPorEscenario`, ciudad y departamento con `ConsultaUbicacion`, tipo con `RepositorioTiposCancha` y promedio con `idsCanchas` y `ConsultaCalificaciones` (CA1, CA2, CA4), con `ConsultarPerfilEscenarioCasoUsoTest` (8 pruebas). Si faltara la ciudad o el departamento (la llave foránea lo impide), es un error inesperado (ERR-GEN-07)
- [x] T4 · REST · `GET /api/escenarios/{id}/perfil`, `RespuestaPerfilEscenario`, ruta pública en `ConfiguracionSeguridad` (solo GET) e `INF-RSE-01` en `catalogo-mensajes.properties` (CA1, CA2, CA3, CA4, CA6), con `PerfilEscenarioControllerTest` (6 pruebas). `EscenarioControllerTest` y `EstadoEscenarioControllerTest` reciben el simulacro del nuevo caso de uso
- [x] T5 · Punta a punta · `PerfilEscenarioPuntaAPuntaTest` (7 pruebas) con un escenario ACTIVO, uno INACTIVO, uno sin horarios y uno inexistente, sin token (CA1, CA2, CA3, CA4, CA6). También revisa que el celular del administrador no salga y que `GET /api/escenarios` siga pidiendo sesión
- [x] T6 · Documentación · paquete `calificaciones` (temporal) en `AGENTS.md` (hecho en T2) y sección «Pendientes en los Excel de origen» en [spec.md](spec.md): la D40 y la corrección de P3 quedan para el desarrollador
- [x] T7 · Verificación · `./mvnw verify` en verde el 17-nov-2026 (489 pruebas, incluye `ArquitecturaTest`) y estado de `spec.md` en **Implementada**. El commit y el pull request se hacen con la confirmación del desarrollador

No hay tarea de persistencia: se reutilizan los métodos de los repositorios que ya existen y no se cambia el modelo de datos.
