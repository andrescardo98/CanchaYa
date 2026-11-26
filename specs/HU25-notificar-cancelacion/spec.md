# HU25 · Recibir la notificación de una cancelación

**Estado:** Implementada (25-nov-2026) · **HU:** [HU25](../../docs/historias/HU25.md) · **Módulo:** Reservas (con un cambio pequeño en Transversal)

## Fuentes

- Dominio: [Reserva](../../docs/dominio/reserva.md), responsabilidades `cancelar` y `cancelarPorFaltaDeConfirmacion`, **política 21** (RN-06: toda cancelación se notifica al jugador y al administrador; si canceló el administrador, lleva la justificación; si el correo falla, no se revierte ni se reintenta). Atributos que van en los correos: `fecha`, `horaInicio`, `horaFin`, `precio`, `origenCancelacion`, `motivoCancelacion` y `fechaCancelacion`.
- [Usuario](../../docs/dominio/usuario.md): `nombre` y `correoElectronico` del jugador y del propietario.
- Mensajes ([mensajes.md](../../docs/especificacion/mensajes.md), hoja «Plantillas de correo»): `COR-RES-03` (cancela el jugador), `COR-RES-04` (cancela el propietario por fuerza mayor, con `{justificacion}`) y `COR-RES-05` (cancela el sistema por falta de confirmación). Los tres van al jugador y al propietario: «un correo a cada destinatario». `ERR-GEN-08` para el log (ya existe).
- Parámetros ([parametros.md](../../docs/especificacion/parametros.md)): `PAR-GEN-02` = 0 reintentos (ya lo cumple `EnvioCorreoSmtp`, como en HU13 y HU22).
- Aspectos generales: AG-15 (el celular no va en estos correos ni en el log), AG-18 (fecha dd/mm/aaaa y hora hh:mm), AG-19 (pesos `$ 160.000`) y AG-23 (solo los correos del catálogo; si falla, `ERR-GEN-08` y sin reintento).
- Decisiones: **D15** (el correo sale después de guardar; si falla se registra y no se reintenta), D19 (no van a los encargados), D27 (America/Bogota), D29 punto 3 (no cambia: la actualización condicional de HU24 sigue dentro de la transacción y los correos van fuera).
- Código que se reutiliza: `CancelarReservaCasoUso` (HU24 dejó el punto exacto para los correos: P2 de HU24), el envío de HU22 en `ReservarCanchaCasoUso.notificar` (mismo patrón: datos de la cancha, contactos en una sola llamada, un `try/catch` por correo), `ConsultaCanchas.buscarDatosCorreo`, `ConsultaUsuarios.buscarContactos`, `EnvioCorreo`, `EnvioCorreoSmtp`, `FormatoTexto` y `OrigenCancelacion`, que ya tiene los tres valores.

## Qué se construye

Cuando una reserva queda CANCELADA y la cancelación ya está guardada, el backend envía dos correos con la misma plantilla: uno al jugador y otro al administrador propietario del escenario. La plantilla depende de quién canceló: `COR-RES-03` si fue el jugador, `COR-RES-04` (con la justificación) si fue el propietario y `COR-RES-05` si fue el sistema por falta de confirmación. Si un correo falla, la reserva sigue CANCELADA, el error queda en el log con `ERR-GEN-08` y no se reintenta.

Hoy solo existe la cancelación del jugador (HU24). Las otras dos llegan con HU32 y HU28 (Release 2): esta HU deja listo el envío de los tres correos para que esas HU solo lo llamen (P1). No hay pantalla ni cambia la respuesta de `POST /api/reservas/{id}/cancelacion`.

## Contrato del servicio

No hay servicio nuevo. `POST /api/reservas/{id}/cancelacion` responde igual que en HU24 (200 con `EXI-RES-02`, mismos errores). Lo nuevo es lo que pasa **después** de confirmar la transacción:

| Correo | Cuándo (`origenCancelacion`) | Asunto | Parámetros del cuerpo, en orden |
|---|---|---|---|
| `COR-RES-03` | `JUGADOR` | `Reserva cancelada: {escenario}, {fecha} a las {horaInicio}` | nombreDestinatario, fechaCancelacion, escenario, direccion, ciudad, cancha, fecha, horaInicio, horaFin, precio, nombreJugador |
| `COR-RES-04` | `ADMINISTRADOR` | `Reserva cancelada por el escenario: {escenario}, {fecha} a las {horaInicio}` | nombreDestinatario, escenario, fechaCancelacion, escenario, direccion, ciudad, cancha, fecha, horaInicio, horaFin, precio, justificacion |
| `COR-RES-05` | `SISTEMA_SIN_CONFIRMACION` | `Reserva cancelada por falta de confirmación: {escenario}, {fecha} a las {horaInicio}` | nombreDestinatario, escenario, direccion, ciudad, cancha, fecha, horaInicio, horaFin, precio, nombreJugador |

- Cada plantilla sale dos veces: al `correoElectronico` del jugador con `{nombreDestinatario}` = su nombre, y al del propietario del escenario con `{nombreDestinatario}` = el nombre del propietario. Los dos se leen al momento de enviar.
- `{fecha}` en dd/mm/aaaa, horas en hh:mm (AG-18), `{precio}` como `$ 160.000` (AG-19) y `{fechaCancelacion}` en dd/mm/aaaa hh:mm en la hora de Bogotá (D27), como `{fechaHora}` de `COR-ESC-01`.
- `{justificacion}` es `motivoCancelacion` tal como lo registró el propietario.
- `COR-RES-04` repite `{escenario}` en el cuerpo: el texto del Excel lo usa dos veces y se pasa dos veces, en el orden en que aparece.
- Si falta un dato (la cancha o un usuario) o algo lanza un error antes de llegar al servidor de correo, `NotificacionCancelacion` escribe en el log `ERR-GEN-08` con el código del correo, el destinatario por su papel (`jugador` o `propietario`, nunca la dirección), el id de la reserva y el tipo de error (P3). Si el que falla es el SMTP, el registro lo escribe `EnvioCorreoSmtp` con su formato de siempre (HU13), porque no lanza el error. Un correo que falla no impide enviar el otro.

## Diseño por capas

### Dominio (`reservas.domain`)

Sin cambios. `Reserva` ya trae `origenCancelacion`, `motivoCancelacion` y `fechaCancelacion` (HU24), y `Reserva.reconstruir` permite armar en las pruebas reservas canceladas por el propietario o por el sistema.

### Transversal

- `FormatoTexto.fechaHora(OffsetDateTime)` (nuevo): dd/MM/yyyy HH:mm en America/Bogota, sin importar el desfase con el que llegue el valor (D27, AG-18). `COR-ESC-01` (HU13) sigue con su formato propio: no se toca (Fuera de alcance).
- `catalogo-mensajes.properties`: `COR-RES-03.asunto`, `COR-RES-03.cuerpo`, `COR-RES-04.asunto`, `COR-RES-04.cuerpo`, `COR-RES-05.asunto` y `COR-RES-05.cuerpo`, copiados de la hoja «Plantillas de correo».

### Caso de uso (`reservas.application.usecase`)

- `NotificacionCancelacion` (nueva, `@Component`, P2): `void notificar(Reserva reserva)` con la política 21 (RN-06, D15, PAR-GEN-02):
  1. Elige la plantilla por `reserva.getOrigenCancelacion()`: `JUGADOR` → `COR-RES-03`, `ADMINISTRADOR` → `COR-RES-04`, `SISTEMA_SIN_CONFIRMACION` → `COR-RES-05`.
  2. Busca `buscarDatosCorreo(canchaId)` y los contactos del jugador y del propietario con una sola llamada a `buscarContactos`, como HU22.
  3. Arma y envía el correo del jugador y luego el del propietario, cada uno en su propio `try/catch (RuntimeException)`. Si falla, registra `ERR-GEN-08 codigoCorreo=… destinatario=jugador|propietario reservaId=… detalle=<tipo de error>` y sigue.
  4. Si falta un dato, registra `ERR-GEN-08` para el correo afectado y no lo envía. Sin la cancha no sale ninguno. Sin el jugador tampoco sale ninguno, porque `COR-RES-03` y `COR-RES-05` llevan `{nombreJugador}` también en el correo del propietario; en `COR-RES-04`, que no lo lleva, el del propietario sí sale. Sin el propietario solo falta el suyo. Si la consulta de los datos lanza un error, se registran los dos.
  5. Ni una falla del envío ni la falta de datos lanzan un error. Recibe siempre una reserva ya CANCELADA y guardada, con su origen y su fecha de cancelación: así la llaman los casos de uso que cancelan.
- `CancelarReservaCasoUso` recibe además `NotificacionCancelacion`. En `ejecutar`: `transaccion.execute(...)` igual que HU24 y, al salir, `notificar(reserva)` **fuera** de la transacción y en el mismo hilo (como HU22, P1 de HU22). Si la cancelación falla (ERR-RES-14, 15 o 16), no se envía nada.
- HU32 y HU28 solo llaman a `notificar` después de su propia transacción.

### Persistencia y REST

Sin cambios. No hay migración nueva.

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1, CA2 | `fechaHora` da dd/mm/aaaa hh:mm en Bogotá, también si el valor llega en UTC | `FormatoTextoTest.HU25_fechaHora_enBogota` | Transversal |
| CA1 | La plantilla arma el asunto y el cuerpo de `COR-RES-03` | `EnvioCorreoSmtpTest.HU25_CA1_armaCorRes03` | Transversal |
| CA2 | La plantilla arma `COR-RES-04` con la justificación y el escenario repetido | `EnvioCorreoSmtpTest.HU25_CA2_armaCorRes04` | Transversal |
| CA3 | La plantilla arma `COR-RES-05` | `EnvioCorreoSmtpTest.HU25_CA3_armaCorRes05` | Transversal |
| CA1 | Origen JUGADOR: al jugador y al propietario les llega `COR-RES-03`, cada uno con su nombre, con la fecha de cancelación, los datos de la reserva y el nombre del jugador | `NotificacionCancelacionTest.CA1_canceloElJugador_losDosRecibenCorRes03` | Caso de uso |
| CA2 | Origen ADMINISTRADOR: los dos reciben `COR-RES-04` con `motivoCancelacion` como justificación | `NotificacionCancelacionTest.CA2_canceloElPropietario_losDosRecibenCorRes04ConLaJustificacion` | Caso de uso |
| CA3 | Origen SISTEMA_SIN_CONFIRMACION: los dos reciben `COR-RES-05` | `NotificacionCancelacionTest.CA3_canceloElSistema_losDosRecibenCorRes05` | Caso de uso |
| CA4 | Si el envío al jugador lanza un error, se registra `ERR-GEN-08` y el del propietario sí sale; `notificar` no lanza nada | `NotificacionCancelacionTest.CA4_siUnCorreoFalla_elOtroSaleYNoSeLanzaNada` | Caso de uso |
| CA4 | Sin la cancha no sale ninguno y se registran los dos | `NotificacionCancelacionTest.CA4_sinLaCancha_noSaleNingunoYSeRegistranLosDos` | Caso de uso |
| CA4 | Sin el jugador, en `COR-RES-03` no sale ninguno; en `COR-RES-04` el propietario sí recibe el suyo | `NotificacionCancelacionTest.CA4_sinElJugador_enCorRes03NoSaleNinguno` y `CA4_sinElJugador_enCorRes04ElPropietarioSiLoRecibe` | Caso de uso |
| CA4 | Sin el propietario, solo sale el del jugador | `NotificacionCancelacionTest.CA4_sinElPropietario_soloSaleElDelJugador` | Caso de uso |
| CA4 | Si la consulta de los datos lanza un error, se registran los dos y no se lanza nada | `NotificacionCancelacionTest.CA4_siLaConsultaDeDatosFalla_seRegistranLosDosYNoSeLanzaNada` | Caso de uso |
| CA4 | El log no lleva direcciones de correo ni datos del cuerpo (P3) | `NotificacionCancelacionTest.CA4_elLogNoLlevaDatosDelDestinatario` (`OutputCaptureExtension`) | Caso de uso |
| CA1 | El caso de uso notifica después de cerrar la transacción (D15), con la reserva ya CANCELADA | `CancelarReservaCasoUsoTest.HU25_CA1_notificaDespuesDeLaTransaccion` (`InOrder`) | Caso de uso |
| CA1 | Si la cancelación falla (ERR-RES-14, 15 o 16), no se notifica | `CancelarReservaCasoUsoTest.HU25_CA1_siNoSeCancela_noSeNotifica` | Caso de uso |
| CA4 | Si `notificar` lanzara un error inesperado, el caso de uso devuelve la reserva cancelada igual | Lo garantiza `NotificacionCancelacion` (paso 5) y lo prueba `CA4_siUnCorreoFalla_elOtroSaleYNoSeLanzaNada` | Caso de uso |
| CA1 | De punta a punta: un `POST /api/reservas/{id}/cancelacion` real envía `COR-RES-03` a las direcciones del jugador y del propietario | `CancelacionPuntaAPuntaTest.HU25_CA1_cancelarEnviaLosDosCorreos` (`@MockitoBean JavaMailSender`) | Punta a punta |
| CA4 | Con el SMTP fallando: 200, la fila queda CANCELADA, el log tiene `ERR-GEN-08` y `send` se llamó una sola vez por correo (sin reintento) | `CancelacionPuntaAPuntaTest.HU25_CA4_smtpFalla_reservaSigueCancelada` | Punta a punta |
| Reglas | Reservas solo usa Identidad, Escenarios y Transversal; el dominio no usa Spring | `ArquitecturaTest` (sin cambios) | Arquitectura |

CA2 y CA3 se prueban sin servicio, con reservas armadas con `Reserva.reconstruir`, porque todavía no hay quién cancele con esos orígenes (P1). Sus pruebas de punta a punta van en HU32 y HU28.

## Preguntas abiertas

No quedan preguntas abiertas. Lo que se decidió (25-nov-2026):

- **P1 · Los tres correos quedan listos (opción A).** `NotificacionCancelacion` arma `COR-RES-03`, `COR-RES-04` y `COR-RES-05`. CA2 y CA3 se prueban con reservas armadas con `Reserva.reconstruir`, porque la cancelación del propietario (HU32) y la automática (HU28) son del Release 2. Esas HU solo llaman a `notificar` después de su transacción y agregan su prueba de punta a punta.
- **P2 · Clase propia `NotificacionCancelacion` en `reservas.application.usecase` (opción A).** Es la única forma de probar CA2 y CA3 hoy, y evita mover código cuando llegue HU28. No hace falta un paquete fuera de D39. El envío de HU22 (`ReservarCanchaCasoUso.notificar`) no se toca: unir los dos envíos sería una refactorización que esta HU no pide.
- **P3 · El log de `ERR-GEN-08` lleva el papel del destinatario (opción A).** Lleva `destinatario=jugador` o `propietario`, el código del correo, el id de la reserva y el tipo de error, sin la dirección de correo ni el mensaje del error, como HU22 (RT-12). Con el id de la reserva se encuentra la dirección si hace falta. `EnvioCorreoSmtp` sigue escribiendo su propio registro como hoy.

## Pendientes para los Excel

- `COR-RES-05` trae «3 horas» escrito en el texto, aunque el valor es `PAR-RES-08`. Se copia tal como está en el Excel; si el parámetro cambia, la plantilla quedaría desactualizada.

## Fuera de alcance

- Reintentos o cola de correos pendientes (PAR-GEN-02 = 0, D15; Release 1).
- Correos a los encargados del escenario (D19).
- La cancelación del propietario por fuerza mayor (HU32) y la cancelación automática (HU28): esta HU solo deja el envío listo para que lo llamen.
- Unificar el envío de HU22 (`ReservarCanchaCasoUso.notificar`) con `NotificacionCancelacion`, y que `COR-ESC-01` (HU13) use `FormatoTexto.fechaHora`.
- Cualquier cambio en la pantalla o en la respuesta de `POST /api/reservas/{id}/cancelacion`.
