# HU18 · Tareas

[spec.md](spec.md) está **Aprobada** (24-nov-2026). En cada tarea se escribe primero la prueba del criterio.

- [ ] T1 · Spike · Prueba con Testcontainers de `ST_DWithin` y `ST_Distance` sobre `escenario.ubicacion`: medidas en metros, borde del radio y columna generada (CA1, restricción de diseño de HU18). Si algo no coincide, se corrige `spec.md` antes de seguir
- [ ] T2 · Dominio · `HorarioRecurrente.validarFechaConsultable`, que `calcularFranjas` pasa a llamar, con sus pruebas de bordes (hoy, hoy + 30, ayer y hoy + 31) y sin romper `HorarioRecurrenteCalcularFranjasTest` (CA6, política 10)
- [ ] T3 · Configuración · PAR-ESC-04 a PAR-ESC-06 en `application.properties` (`canchaya.escenario.radio-busqueda-*`, con su variable `PAR_ESC_nn`) y en la tabla de `projects/api/AGENTS.md` (CA10, CA11; regla 7)
- [ ] T4 · Puertos y caso de uso · `BuscarCanchas` (con `CriteriosBusqueda`, `ResultadoBusqueda` y `CanchaEncontrada`), `RepositorioBusquedaCanchas`, `ConsultaFranjasOcupadas` (P1), `RepositorioBloqueos.rangosDelDia(canchaIds, fecha)`, `RepositorioHorarios.buscarPorEscenarios` y `BuscarCanchasCasoUso`, con dobles en memoria y `BuscarCanchasCasoUsoTest` (CA1 a CA7, CA10, CA11; política 11 de Cancha, políticas 10 a 12 de HorarioRecurrente; P2, P3, P7, P9)
- [ ] T5 · Persistencia de Escenarios · `RepositorioBusquedaCanchasJdbc` (PostGIS, filtros y orden), `RepositorioBloqueosJdbc.rangosDelDia(canchaIds, fecha)` y `RepositorioHorariosJpa.buscarPorEscenarios`, con pruebas de Testcontainers (CA1, CA3, CA4, CA5)
- [ ] T6 · Reservas · `ConsultaFranjasOcupadasCasoUso` y `RepositorioReservas.confirmadasDeCanchasEnFecha` en el adaptador de persistencia, con su prueba: solo CONFIRMADAS de esa fecha, por cancha (CA5; P1, P2)
- [ ] T7 · REST · `GET /api/canchas/busqueda`, `BusquedaCanchasController` y `RespuestaBusquedaCanchas`; rutas públicas en `ConfiguracionSeguridad` (búsqueda, departamentos, ciudades y tipos de cancha, P8); `ERR-GEN-04 → 400` y su texto con `{minimo}` y `{maximo}` en `ManejadorErrores`; `INF-ESC-01`, `INF-ESC-03`, nombres de campo y formato de `hora` en `catalogo-mensajes.properties`, con `BusquedaCanchasControllerTest` (CA2, CA5 a CA7, CA11; P5, P6)
- [ ] T8 · Punta a punta · `BusquedaCanchasPuntaAPuntaTest` sin token, con escenarios en coordenadas conocidas, una reserva CONFIRMADA, una CANCELADA y un bloqueo insertados por SQL (CA1, CA5, CA9, CA10)
- [ ] T9 · Documentación · Registrar en `spec.md` los pendientes de los Excel que aparezcan al implementar
- [ ] T10 · Verificación · `./mvnw verify` en verde (incluye `ArquitecturaTest` y `CatalogoMensajesTest`), estado de `spec.md` en **Implementada** y `/pull-request`
