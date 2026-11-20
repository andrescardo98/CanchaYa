# HU13 · Activar o inactivar un escenario (arreglo de concurrencia)

**Estado:** Implementada (19-nov-2026) · **HU:** [HU13](../../docs/historias/HU13.md) · **Módulo:** Escenarios y canchas (`co.edu.uco.canchaya.escenarios`)

La HU13 se implementó antes de que existiera esta carpeta (pull request #13). Esta especificación **solo cubre el arreglo**: tomar el turno de las canchas al inactivar un escenario. El resto de la HU (contrato, políticas 6, 8, 9, 10, 14 y 15, y el correo COR-ESC-01) no cambia y no se repite aquí.

## Fuentes

- Dominio: [Escenario](../../docs/dominio/escenario.md), responsabilidad `cambiarEstado`, política 14 (RN-17): no se inactiva un escenario con reservas confirmadas futuras en alguna de sus canchas.
- Criterios: CA7 (ERR-ESC-14) y CA8 (el correo sale cuando el cambio ya quedó guardado; si falla, no se revierte).
- Decisiones: D08, D15 (correo después de guardar), D29 punto 2 y D36 (turno de la cancha).
- Origen del arreglo: P2 de la [especificación de HU10](../HU10-cambiar-estado-cancha/spec.md), que tomó el turno al inactivar una cancha y dejó anotado este mismo riesgo en la HU13.
- Código que se reutiliza: `RepositorioCanchas.tomarTurno` (bloquea la fila de la cancha y exige una transacción abierta), `RepositorioEscenarios.idsCanchas` y `ConsultaReservasFuturas.existenConfirmadasFuturas`.

## El problema

Hoy `CambiarEstadoEscenarioCasoUso` no tiene transacción, a propósito: así el correo sale cuando el cambio ya está guardado (D15). Pero sin transacción y sin turno puede pasar esto:

1. El administrador del sistema inactiva el escenario. El caso de uso pregunta si alguna cancha tiene reservas futuras, y no hay.
2. En ese mismo instante un jugador reserva una de sus canchas (HU21). La reserva ve el escenario todavía ACTIVO y se guarda.
3. El escenario queda INACTIVO con una reserva confirmada futura. Se rompe la política 14 (CA7, D08).

Hoy no pasa porque HU21 todavía no está en `main`. Pasará en producción desde el día en que se una HU21.

## Qué cambia

Solo `CambiarEstadoEscenarioCasoUso`. Ni el contrato, ni el dominio, ni la base cambian.

| Paso | Antes | Después |
|---|---|---|
| 1 | Política 8 (ERR-ESC-08) | Igual, fuera de la transacción: quien no tiene permiso no bloquea nada |
| 2 | Política 6 (ERR-ESC-06) | **Dentro de una transacción** (`TransactionOperations`, P1) |
| 3 | — | **Solo al inactivar:** toma el turno de **cada** cancha del escenario, **ordenadas por `id`** (P2), con `RepositorioCanchas.tomarTurno` |
| 4 | Política 14: pregunta a Reservas por las canchas | Igual, pero ya con los turnos tomados |
| 5 | `cambiarEstado` y `actualizarEstado` | Igual, dentro de la transacción. Al terminar, se guarda el cambio y se sueltan los turnos |
| 6 | Correo al propietario (política 15) | Igual, **fuera** de la transacción, cuando el cambio ya quedó guardado (D15, CA8) |

Con esto, una reserva de HU21 que llega mientras se inactiva el escenario espera su turno. Cuando sigue, revisa que la cancha sea reservable (paso 2 de HU21: `buscarReservable` exige el escenario ACTIVO), encuentra el escenario INACTIVO y responde ERR-RES-01. Una reserva que llegó antes ya está guardada, y la política 14 la encuentra y responde ERR-ESC-14.

Activar no toma turnos ni pregunta a Reservas: activar no puede romper la política 14.

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA7 | Al inactivar, se toman los turnos de todas las canchas, ordenadas por `id`, antes de preguntar a Reservas | `ca7Inactivar_tomaLosTurnosEnOrdenAntesDeConsultarReservas` (orden con `InOrder`) | Caso de uso |
| CA7 | Con reservas futuras: ERR-ESC-14, no se guarda y no se envía correo | La prueba que ya existe (`ca7…`) sigue pasando | Caso de uso |
| CA2 | Activar no toma turnos | `ca2Activar_noTomaTurnos` | Caso de uso |
| CA5 | Sin permiso: no se abre la transacción ni se toman turnos | `ca5SinPermiso_noTomaTurnos` | Caso de uso |
| CA8 | Guardar el estado pasa dentro de la transacción y el correo sale después de cerrarla | `ca8ElCorreoSaleDespuesDeCerrarLaTransaccion` (el falso de la transacción anota si estaba abierta) | Caso de uso |
| CA3 | Inactivar un escenario **con canchas** funciona de punta a punta: el turno se toma dentro de una transacción real (si no, `tomarTurno` falla) | `ca3InactivaUnEscenarioConCanchasSinReservasFuturas` | Punta a punta |
| CA7 | De punta a punta con una reserva futura (la prueba que ya existe) | `ca7NoSeInactivaConUnaReservaConfirmadaFutura` | Punta a punta |

Todas las demás pruebas de HU13 deben seguir pasando sin cambios en lo que verifican. En `CambioEstadoEscenarioPuntaAPuntaTest` el propietario pasa de `ana@correo.com` a `propietario.hu13@correo.com` (también en la verificación del destinatario del correo). `InicioSesionPuntaAPuntaTest` deja bloqueada esa cuenta (D28), y la prueba de HU13 puede fallar con 422 según el orden en que corran las pruebas.

## Decisiones tomadas

**P1 · Cómo abrir la transacción sin meter el correo en ella (aprobada el 19-nov-2026: opción A)**
- A. Inyectar `TransactionOperations` (la interfaz de `TransactionTemplate`, que Spring Boot ya crea) y envolver los pasos 2 a 5 en `execute(...)`. El correo queda afuera. En las pruebas del caso de uso se usa un falso sencillo. HU21 usa la misma herramienta (`TransactionTemplate`).
- B. Pasar los pasos 2 a 5 a otro bean con `@Transactional` y llamarlo desde el caso de uso. Es una clase más solo para la transacción.

**Decisión: A.** Se inyecta `TransactionOperations` y en `execute(...)` van buscar el escenario, tomar los turnos, consultar las reservas y guardar. El correo queda afuera (D15).

**P2 · Orden de los turnos (aprobada el 19-nov-2026)**
Si dos operaciones toman los mismos turnos en distinto orden, cada una puede quedarse esperando a la otra para siempre (bloqueo mutuo, *deadlock*). Hoy solo la HU13 toma varios turnos a la vez. HU10, HU21 y HU30 toman uno solo. Igual, tomarlos siempre ordenados por `id` evita el problema si aparece otra operación que tome varios.

**Decisión:** ordenar por `id` dentro del caso de uso, sin cambiar `idsCanchas`.

## Preguntas abiertas

Ninguna.

## Fuera de alcance

- La prueba de concurrencia real con dos hilos: es la del spike RT-03 de HU21 (su P5). Se puede extender allí a este caso.
- Cualquier otro cambio de la HU13.
