# HU22 · Recibir la notificación de una reserva

**Estado:** Aprobada (24-nov-2026) · **HU:** [HU22](../../docs/historias/HU22.md) · **Módulo:** Reservas (con cambios pequeños en Escenarios, Identidad y Transversal)

## Fuentes

- Dominio: [Reserva](../../docs/dominio/reserva.md), responsabilidad «confirmar», políticas 12 (confirmación inicial) y **13** (RN-06: notificar por correo al jugador y al administrador; si falla, no se revierte ni se reintenta). Atributos que van en los correos: `fecha`, `horaInicio`, `horaFin`, `precio`, `confirmacionAsistencia`.
- [Usuario](../../docs/dominio/usuario.md): `nombre`, `correoElectronico` y `numeroCelular` del jugador; `nombre` y `correoElectronico` del propietario.
- Mensajes ([mensajes.md](../../docs/especificacion/mensajes.md), hoja «Plantillas de correo»): `COR-RES-01` (jugador), `COR-RES-02` (propietario) y `ERR-GEN-08` (registro de la falla, ya existe).
- Parámetros ([parametros.md](../../docs/especificacion/parametros.md)): `PAR-GEN-02` = 0 reintentos (ya lo cumple `EnvioCorreoSmtp`, como en HU13) y `PAR-RES-12` = No (ya está en `ParametrosReserva.confirmacionAsistenciaActiva`, HU20/HU21).
- Aspectos generales: AG-18 (fecha dd/mm/aaaa y hora hh:mm) y AG-19 (pesos `$ 160.000`).
- Decisiones: **D15** (el correo sale después de guardar; si falla se registra y no se reintenta), D19 (los encargados no reciben COR-RES-02; no contradice RN-06, que es la que pide este correo), D27 (America/Bogota), D29 y D36 (no cambian: el turno sigue dentro de la transacción y los correos van fuera), **D33** (con PAR-RES-12 en «No», sin aviso de confirmación).
- Código existente que se reutiliza: `EnvioCorreo` y `EnvioCorreoSmtp` (HU13), `TextoCatalogo`, `CatalogoMensajes`, `ConsultaUsuarios`, `ConsultaCanchas`, `ConsultaUbicacion.buscarCiudades`, `ReservarCanchaCasoUso` (HU21 dejó el punto exacto para los correos: P6 de HU21) y el patrón de `CambiarEstadoEscenarioCasoUso.notificarPropietario`.

## Qué se construye

Cuando un jugador confirma una reserva (`POST /api/reservas`, HU21) y la reserva ya quedó guardada, el backend envía dos correos: uno al jugador con los datos de la reserva (`COR-RES-01`) y otro al administrador propietario del escenario con los datos del jugador (`COR-RES-02`). Si alguno falla, la reserva sigue confirmada, el error queda en el log con `ERR-GEN-08` y no se reintenta. No hay pantalla: la respuesta del servicio no cambia (ya dice «Le enviamos los datos a su correo», `EXI-RES-01`).

## Contrato del servicio

No hay servicio nuevo. `POST /api/reservas` responde igual que en HU21 (201 con `EXI-RES-01`, mismos errores). Lo nuevo es lo que pasa **después** de confirmar la transacción:

| Correo | Destinatario | Asunto | Parámetros del cuerpo, en orden |
|---|---|---|---|
| `COR-RES-01` | `correoElectronico` del jugador, leído al reservar | `Reserva confirmada: {escenario}, {fecha} a las {horaInicio}` | nombreJugador, escenario, direccion, ciudad, cancha, fecha, horaInicio, horaFin, precio, avisoConfirmacion |
| `COR-RES-02` | `correoElectronico` del propietario del escenario | `Nueva reserva en {escenario}: {cancha}, {fecha} a las {horaInicio}` | nombrePropietario, cancha, fecha, horaInicio, horaFin, nombreJugador, celularJugador |

- `{fecha}` en dd/mm/aaaa, horas en hh:mm (AG-18) y `{precio}` como `$ 160.000` (AG-19), igual que `EXI-RES-01`.
- `{avisoConfirmacion}` es el texto `COR-RES-01.avisoConfirmacion` del catálogo solo si la reserva quedó con `confirmacionAsistencia = PENDIENTE`; si quedó `NO_REQUERIDA` (siempre, mientras PAR-RES-12 esté en «No», D33) va vacío.
- Ante una falla (no se encontró al usuario o al escenario, el SMTP falla, etc.) se escribe en el log `ERR-GEN-08` con el código del correo y el id de la reserva, sin datos sensibles: nunca el celular (RT-12, P4). Un correo que falla no impide enviar el otro.

## Diseño por capas

### Dominio (`reservas.domain`)

- `Reserva.requiereConfirmacionAsistencia()`: verdadero si `confirmacionAsistencia == PENDIENTE`. Política 13 + D33: decide si `COR-RES-01` lleva el aviso. La regla de cuándo nace PENDIENTE no cambia (política 12, `calcularConfirmacionInicial`).

### Transversal

- `transversal.domain.FormatoTexto` (nueva, Java puro): `fecha(LocalDate)` → dd/MM/yyyy, `hora(LocalTime)` → HH:mm y `precio(BigDecimal)` → `$ 160.000` (AG-18, AG-19). Se mueve aquí `RespuestaReserva.precioConFormato` para que el caso de uso (que no puede usar `infrastructure`) y la respuesta REST formateen igual (P3). `RespuestaReserva` pasa a usarla.
- `catalogo-mensajes.properties`: `COR-RES-01.asunto`, `COR-RES-01.cuerpo`, `COR-RES-01.avisoConfirmacion`, `COR-RES-02.asunto` y `COR-RES-02.cuerpo`, copiados de la hoja «Plantillas de correo».

### Identidad (puerto de entrada)

- `ConsultaUsuarios.ContactoUsuario` agrega `numeroCelular` (P4); `ConsultaUsuariosCasoUso` lo llena. HU13 no cambia de comportamiento (solo se ajustan sus dobles de prueba).

### Escenarios (puerto de entrada)

- `ConsultaCanchas.buscarDatosCorreo(UUID canchaId)` → `Optional<DatosCorreoCancha>` con `cancha`, `escenario`, `direccion`, `ciudad` (nombre, por `ConsultaUbicacion.buscarCiudades`) y `administradorId` (P2). No filtra por estado: la reserva ya quedó confirmada y el correo se envía aunque la cancha cambie justo después. Vacío si la cancha, su escenario o su ciudad no existen (el caso de uso lo registra con `ERR-GEN-08`, CA3).

### Caso de uso (`reservas.application.usecase`)

- `ReservarCanchaCasoUso` recibe además `ConsultaUsuarios` y `EnvioCorreo`. En `ejecutar`:
  1. `transaccion.execute(...)` igual que HU21 (turno, validaciones, guardar). Al salir, la reserva quedó guardada y el turno se soltó (D29, D36).
  2. `notificar(confirmada)` (política 13, D15), **fuera** de la transacción y de forma síncrona, como HU13 (P1):
     - Busca `buscarDatosCorreo(canchaId)` y los contactos del jugador y del propietario con una sola llamada a `buscarContactos`.
     - Arma y envía `COR-RES-01` y luego `COR-RES-02`, cada uno en su propio `try/catch (RuntimeException)`: si algo falla, registra `ERR-GEN-08 codigoCorreo=… reservaId=… detalle=<tipo de error>` y sigue. El detalle es el tipo de la excepción, no su mensaje ni los parámetros del correo: así el celular nunca llega al log (P4, RT-12).
     - Si falta un dato (cancha o usuario no encontrado), registra `ERR-GEN-08` para el correo afectado y no lo envía.
  3. Devuelve la misma `ReservaConfirmada`: un error del correo nunca llega al controlador (CA3).
- `EnvioCorreoSmtp` no cambia: ya registra sus fallas con `ERR-GEN-08`, no lanza el error y no reintenta (PAR-GEN-02 = 0).

### Persistencia y REST

Sin cambios. No hay migración nueva.

## Criterios de aceptación → pruebas

| CA | Qué se verifica | Prueba | Capa |
|---|---|---|---|
| CA1 | `requiereConfirmacionAsistencia` es verdadero solo con PENDIENTE | `ReservaTest.CA1_requiereConfirmacionAsistencia_soloSiPendiente` | Dominio |
| CA1 | Al jugador le llega `COR-RES-01` con escenario, dirección, ciudad, cancha, fecha, horas y precio con formato, y sin aviso (PAR-RES-12 = No) | `ReservarCanchaCasoUsoTest.CA1_alJugadorLeLlegaCorRes01SinAviso` | Caso de uso |
| CA1 | Con PAR-RES-12 en «Sí» y una reserva PENDIENTE, `COR-RES-01` lleva `COR-RES-01.avisoConfirmacion` | `ReservarCanchaCasoUsoTest.CA1_conConfirmacionPendiente_llevaElAviso` | Caso de uso |
| CA1 | La plantilla arma el asunto y el cuerpo con el aviso y con el aviso vacío | `EnvioCorreoSmtpTest.CA1_armaCorRes01` | Transversal |
| CA2 | Al propietario le llega `COR-RES-02` con cancha, fecha, horas, nombre y celular del jugador | `ReservarCanchaCasoUsoTest.CA2_alPropietarioLeLlegaCorRes02` | Caso de uso |
| CA2 | `buscarDatosCorreo` trae dirección, nombre de la ciudad y propietario del escenario | `ConsultaCanchasCasoUsoTest.HU22_CA2_buscarDatosCorreo` | Escenarios |
| CA2 | `buscarContactos` trae el celular | `ConsultaUsuariosCasoUsoTest.HU22_CA2_traeElCelular` | Identidad |
| CA1, CA2 | Los correos salen después de cerrar la transacción (D15) | `ReservarCanchaCasoUsoTest.CA1_CA2_losCorreosSalenDespuesDeLaTransaccion` (`InOrder`) | Caso de uso |
| CA1, CA2 | De punta a punta: un `POST /api/reservas` real envía los dos correos a las direcciones registradas | `ReservaPuntaAPuntaTest.HU22_CA1_CA2_reservarEnviaLosDosCorreos` (`@MockitoBean JavaMailSender`) | Punta a punta |
| CA3 | Si el envío lanza un error, la reserva se devuelve igual y el otro correo sí sale | `ReservarCanchaCasoUsoTest.CA3_siUnCorreoFalla_laReservaSeMantieneYElOtroSale` | Caso de uso |
| CA3 | Si no se encuentran el usuario o la cancha, se registra `ERR-GEN-08` y la reserva se devuelve igual | `ReservarCanchaCasoUsoTest.CA3_sinDatosParaElCorreo_registraElErrorYNoRevierte` | Caso de uso |
| CA3 | El registro de `ERR-GEN-08` del caso de uso no lleva el celular del jugador (P4, RT-12) | `ReservarCanchaCasoUsoTest.CA3_elLogNoLlevaElCelular` (`OutputCaptureExtension`) | Caso de uso |
| CA3 | Con el SMTP fallando: 201, la reserva queda CONFIRMADA en la base, el log tiene `ERR-GEN-08` y `send` se llamó una sola vez por correo (sin reintento) | `ReservaPuntaAPuntaTest.HU22_CA3_smtpFalla_reservaSigueConfirmada` | Punta a punta |
| Reglas | Reservas solo usa Identidad, Escenarios y Transversal; el dominio no usa Spring | `ArquitecturaTest` (sin cambios) | Arquitectura |

## Preguntas abiertas

No quedan preguntas abiertas. Lo que se decidió (24-nov-2026):

- **P1 · Correo en la misma petición.** Se envía en el mismo hilo, después de confirmar la transacción, como HU13. La reserva ya está guardada cuando se envía, así que el correo no la puede impedir ni revertir; la respuesta solo espera al SMTP. Sin `@Async`.
- **P2 · Método nuevo `ConsultaCanchas.buscarDatosCorreo(canchaId)`.** `CanchaReservable` no cambia, así la consulta de disponibilidad (HU20) y la reserva (HU21) no cargan con datos que solo sirven para el correo.
- **P3 · `transversal.domain.FormatoTexto`.** Se mueve ahí el formato del precio de `RespuestaReserva` y se agregan los de fecha y hora, para que `EXI-RES-01` y los correos muestren lo mismo sin duplicar código.
- **P4 · `ContactoUsuario.numeroCelular`.** Se agrega al record existente. El celular solo va en el cuerpo de `COR-RES-02`: **nunca en el log** (RT-12). Los registros de `ERR-GEN-08` del caso de uso llevan solo el código del correo, el id de la reserva y el tipo de error; una prueba lo revisa (CA3).
- **P5 · Aviso vacío.** Se deja la plantilla tal como está en el Excel, aunque quede una línea en blanco de más.

## Fuera de alcance

- Reintentos o cola de correos pendientes (PAR-GEN-02 = 0, D15; Release 1).
- Correos a los encargados del escenario (D19).
- Los correos de cancelación (`COR-RES-03` a `COR-RES-05`, HU25) y la solicitud de confirmación de asistencia (`COR-RES-06`, HU26).
- Cualquier cambio en la pantalla o en la respuesta de `POST /api/reservas`.
