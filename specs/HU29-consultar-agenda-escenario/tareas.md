# HU29 · Tareas

- [x] T1 · Dominio de Escenarios · `HorarioRecurrente.franjasDeLaFecha` (y `calcularFranjas` lo reutiliza) y `Escenario.validarAccesoAgenda` (CA1, CA6, RN-15)
- [x] T2 · Dominio de Reservas · `Reserva.ocupa`, `sePuedeCancelarPorFuerzaMayor`, `sePuedeMarcarInasistencia`, `EstadoFranja` y `VistaAgenda` (CA1, CA2, CA3; políticas 15, 17, 22 y 32; D02)
- [x] T3 · Casos de uso de Escenarios · `ListarEscenariosAgenda` y `ConsultaEscenarios.datosAgenda`, con los puertos de salida nuevos (`RepositorioEncargados` y los métodos de `RepositorioEscenarios` y `RepositorioBloqueos`) y sus adaptadores, para que la aplicación compile y arranque (CA4, CA5, CA6)
- [x] T4 · Caso de uso de Reservas · `ConsultarAgenda` con `RepositorioReservas.deCanchasEntre` (y su adaptador) y `ConsultaUsuarios` (CA1, CA2, CA3)
- [x] T5 · Persistencia · pruebas contra la base de `RepositorioEncargadosJdbc`, `listarPorAdministrador`/`listarPorIds`, `RepositorioBloqueosJdbc.deCanchasEntre` y `RepositorioReservasJpa.deCanchasEntre`
- [ ] T6 · REST · `GET /api/agenda/escenarios` y `GET /api/escenarios/{escenarioId}/agenda`, con `AgendaPuntaAPuntaTest` (CA1 a CA6)
- [ ] T7 · Verificación · `./mvnw verify`, revisar la tabla de criterios → pruebas y `/pull-request`
