# Reserva

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.4. Modelo de dominio/1.1.3.2. Modelo de dominio enriquecido/Modelo_de_dominio_enriquecido_CanchaYa.xlsx, hoja Reserva`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

Objeto de dominio que contiene la información de una reserva realizada por un jugador sobre una cancha, para una fecha y horario determinados.

## Atributos

| Atributo | Tipo de Dato | Longitud mínima | Longitud máxima | Precisión | Rango Inicial | Formato | Valor por defecto | Regla especial | ¿Auto generado? | ¿Calculado? | ¿Obligatorio? | ¿Sensible? | ¿Identifica al registro? | Descripción |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| id | Alfanumérico | 36 | 36 |  |  | Formato de un Identificador Único Universal (UUID) |  | Corresponde a una cadena de 36 caracteres: 32 dígitos del 0 al 9 y letras de la "a" a la "f", en grupos de 8-4-4-4-12 separados por guiones (D37) | SI | NO | SI | NO | SI | Identificador único de la reserva. |
| cancha | Cancha |  |  |  |  |  |  |  | NO | NO | SI | NO | NO | Cancha sobre la que se realiza la reserva. |
| jugador | Usuario |  |  |  |  |  |  | El usuario referenciado debe tener rol JUGADOR | NO | NO | SI | NO | NO | Jugador que realiza la reserva. |
| serieReserva | SerieReserva |  |  |  |  |  |  |  | NO | NO | NO | NO | NO | Serie de reservas recurrentes a la que pertenece. Solo se diligencia si la reserva se creó como parte de una serie. Trabajo futuro. |
| fecha | Fecha |  |  |  |  | dd/mm/aaaa |  | Debe estar dentro de los 30 días calendario siguientes a la creación (RN-11) | NO | NO | SI | NO | NO | Fecha de la reserva. |
| horaInicio | Hora |  |  |  |  | hh:mm |  | Debe coincidir con la hora de inicio de una franja del horario recurrente del escenario | NO | NO | SI | NO | NO | Hora de inicio de la reserva. |
| horaFin | Hora |  |  |  |  | hh:mm |  | Corresponde al fin de 1 o 2 franjas seguidas desde horaInicio (máximo 2 horas), dentro de un mismo horario recurrente del escenario | NO | NO | SI | NO | NO | Hora de fin de la reserva. Una reserva dura 1 o 2 horas. |
| precio | Numérico |  |  | 2 | 0.01 |  |  | Es la suma de sus franjas: Cancha.precioHora × número de horas reservadas (1 o 2) al crear la reserva; no cambia si el administrador modifica después el precio de la cancha | SI | NO | SI | NO | NO | Valor de la reserva en el momento en que se creó. |
| estado | Alfanumérico |  |  |  |  | Enumeración | CONFIRMADA | Valores permitidos: CONFIRMADA \| CANCELADA \| NO_ASISTIO | NO | NO | SI | NO | NO | Estado actual de la reserva. |
| confirmacionAsistencia | Alfanumérico |  |  |  |  | Enumeración | PENDIENTE | Valores permitidos: PENDIENTE \| CONFIRMADA \| NO_REQUERIDA. NO_REQUERIDA aplica a reservas con menos de 5 horas de anticipación o que inician antes de las 12:00 m. (RN-13), y a las que no alcanzaron a recibir la solicitud de confirmación antes de que falten 3 horas (D34) | NO | NO | SI | NO | NO | Confirmación de asistencia que el jugador debe dar antes del partido. Es independiente de estado. |
| origenCancelacion | Alfanumérico |  |  |  |  | Enumeración |  | Valores permitidos: JUGADOR \| ADMINISTRADOR \| SISTEMA_SIN_CONFIRMACION. Solo se diligencia si estado = CANCELADA | NO | NO | NO | NO | NO | Quién originó la cancelación: el jugador, el administrador propietario (por causa de fuerza mayor, RN-07) o el sistema por falta de confirmación (RN-13). |
| motivoCancelacion | Alfanumérico |  | 250 |  |  |  |  | Obligatorio si origenCancelacion = ADMINISTRADOR (RN-07); no aplica en los demás orígenes | NO | NO | NO | NO | NO | Justificación registrada por el administrador al cancelar (por ejemplo, evento climático o falta de energía eléctrica). Se incluye en la notificación al jugador. |
| fechaCreacion | Fecha |  |  |  |  | dd/mm/aaaa hh:mm:ss |  |  | SI | NO | SI | NO | NO | Fecha y hora en que se creó la reserva. |
| fechaCancelacion | Fecha |  |  |  |  | dd/mm/aaaa hh:mm:ss |  |  | NO | NO | NO | NO | NO | Solo se diligencia si la reserva fue cancelada. |
| fechaSolicitudConfirmacion | Fecha |  |  |  |  | dd/mm/aaaa hh:mm:ss |  | Solo se diligencia cuando se envía la solicitud de confirmación de asistencia. | NO | NO | NO | NO | NO | Fecha y hora en que se envió al jugador la solicitud de confirmación de asistencia. Solo la diligencia la tarea programada (HU26). Sin ella, la reserva no se cancela por falta de confirmación (D34). |


### Uso de cada atributo por responsabilidad

| Atributo | verificarDisponibilidad | confirmar | cancelar | marcarNoAsistio | confirmarAsistencia | cancelarPorFaltaDeConfirmacion |
|---|---|---|---|---|---|---|
| id | No requerido | Se asigna automáticamente | Requerido/No modificable | Requerido | Requerido | Requerido |
| cancha | Requerido | Requerido | No requerido | No requerido | No requerido | No requerido |
| jugador | No requerido | Requerido | No requerido | No requerido | No requerido | No requerido |
| serieReserva | No requerido | No requerido | No requerido | No requerido | No requerido | No requerido |
| fecha | Requerido | Requerido | No requerido | No requerido | No requerido | No requerido |
| horaInicio | Requerido | Requerido | No requerido | No requerido | No requerido | No requerido |
| horaFin | Requerido | Requerido | No requerido | No requerido | No requerido | No requerido |
| precio | No requerido | Se asigna automáticamente | No requerido | No requerido | No requerido | No requerido |
| estado | No requerido | Se asigna automáticamente | Se asigna automáticamente | Se asigna automáticamente | No requerido | Se asigna automáticamente |
| confirmacionAsistencia | No requerido | Se asigna automáticamente | No requerido | No requerido | Se asigna automáticamente | No requerido |
| origenCancelacion | No requerido | No requerido | Se asigna automáticamente | No requerido | No requerido | Se asigna automáticamente |
| motivoCancelacion | No requerido | No requerido | Opcional | No requerido | No requerido | No requerido |
| fechaCreacion | No requerido | Se asigna automáticamente | No requerido | No requerido | No requerido | No requerido |
| fechaCancelacion | No requerido | No requerido | Se asigna automáticamente | No requerido | No requerido | Se asigna automáticamente |
| fechaSolicitudConfirmacion | No requerido | No requerido | No requerido | No requerido | No requerido | No requerido |


## Combinaciones únicas

| Nombre combinación | Descripción | Atributos |
|---|---|---|
| ReservaUnicaPorFranja | No es posible que dos reservas confirmadas de la misma cancha y fecha se crucen en el tiempo: cada franja de una hora solo puede pertenecer a una reserva vigente. | cancha, fecha, horaInicio, horaFin |


## Responsabilidades


### verificarDisponibilidad

Permite consultar si una cancha está libre para una fecha y un rango de una o dos franjas seguidas, comparando el horario recurrente del escenario con las reservas y los bloqueos existentes.

**Entradas:**

- `reserva` (Reserva): Contiene la cancha, la fecha, la hora de inicio y la hora de fin que se quieren consultar (una o dos franjas seguidas).

**Salida:** Lógico: Verdadero si todas las franjas del rango están libres y se pueden reservar.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | Debe existir la cancha y estar en estado DISPONIBLE. | No existe la cancha enviada o no está en estado DISPONIBLE. | Abortar la consulta de disponibilidad y reportar un mensaje de error al usuario, indicándole que la cancha no existe o no está disponible para reservas. |
| 2 | RN-11: solo se ofrecen y se aceptan fechas dentro de los 30 días calendario siguientes. | La fecha es anterior a hoy o supera los 30 días calendario siguientes. | Abortar la consulta de disponibilidad y reportar un mensaje de error al usuario, indicándole que solo se puede reservar dentro de los próximos 30 días. |
| 3 | RN-02: la disponibilidad presentada debe corresponder al estado real de cada franja del rango en el momento de la consulta: dentro del horario recurrente del escenario y sin reserva confirmada ni bloqueo. | No aplica | No aplica |
| 4 | No se ofrecen franjas cuya hora de inicio ya pasó. | No aplica | No aplica |


### confirmar

Permite registrar la reserva de un jugador: revalida la disponibilidad de la cancha en el momento exacto de la solicitud y la confirma de inmediato, sin intervención del administrador.

**Entradas:**

- `reserva` (Reserva): Contiene la cancha, el jugador (usuario autenticado), la fecha, la hora de inicio y la hora de fin de la reserva que se va a crear (una o dos franjas seguidas).

**Salida:** Reserva: Reserva registrada en estado CONFIRMADA, con su identificador, su precio y su confirmación de asistencia inicial.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 5 | El usuario que reserva debe tener rol JUGADOR. | El usuario que reserva no tiene rol JUGADOR. | Abortar el registro de la reserva y reportar un mensaje de error al usuario, indicándole que solo un jugador puede crear reservas. |
| 1 | Debe existir la cancha y estar en estado DISPONIBLE. | No existe la cancha enviada o no está en estado DISPONIBLE. | Abortar el registro de la reserva y reportar un mensaje de error al usuario, indicándole que la cancha no existe o no está disponible para reservas. |
| 2 | RN-11: solo se ofrecen y se aceptan fechas dentro de los 30 días calendario siguientes. | La fecha es anterior a hoy o supera los 30 días calendario siguientes. | Abortar el registro de la reserva y reportar un mensaje de error al usuario, indicándole que solo se puede reservar dentro de los próximos 30 días. |
| 6 | RN-01 / RN-03: la disponibilidad de todas las franjas de la reserva se valida de nuevo en el momento de confirmar; una franja de una cancha solo puede estar asignada a una reserva vigente (ReservaUnicaPorFranja). Una reserva de 2 horas asegura sus dos franjas en la misma operación: se reservan ambas o ninguna. El control de concurrencia está decidido en D29: restricción de exclusión en la base de datos, turno por cancha (bloqueo pesimista) y actualización condicional; el spike de concurrencia lo confirma. | Alguna franja de la reserva ya fue tomada por otra reserva confirmada o está por fuera del horario del escenario. | Abortar el registro de la reserva y reportar un mensaje de error al usuario, indicándole que el horario elegido ya no está disponible, para que elija otro. |
| 7 | RN-12: ninguna franja de la reserva debe estar cubierta por un bloqueo. | Existe un bloqueo de la cancha que cubre alguna franja de la reserva. | Abortar el registro de la reserva y reportar un mensaje de error al usuario, indicándole que el horario elegido no está disponible. |
| 8 | RN-08: el jugador no puede tener más de 5 reservas confirmadas activas al mismo tiempo. Una reserva de 2 horas cuenta como una sola para este límite. | El jugador ya tiene 5 reservas confirmadas activas. | Abortar el registro de la reserva y reportar un mensaje de error al usuario, indicándole que alcanzó el máximo de 5 reservas activas. |
| 9 | RN-08: el jugador no puede reservar más de 2 horas para un mismo día calendario, sumando todas sus reservas confirmadas de ese día. | Las horas de la nueva reserva, sumadas a las que el jugador ya tiene reservadas para esa fecha, superan 2 horas. | Abortar el registro de la reserva y reportar un mensaje de error al usuario, indicándole que alcanzó el máximo de 2 horas de reserva para ese día. |
| 31 | RN-08: una reserva abarca una o dos franjas consecutivas de la misma cancha (máximo 2 horas), dentro de un mismo horario recurrente del escenario. | La reserva abarca más de 2 franjas, o la hora de fin no corresponde a una o dos franjas seguidas desde la hora de inicio. | Abortar el registro de la reserva y reportar un mensaje de error al usuario, indicándole que una reserva puede durar 1 o 2 horas seguidas. |
| 10 | No debe existir otra reserva con el mismo identificador (UUID). | Existe una reserva con el mismo identificador. | Se debe reintentar con un nuevo identificador generado de forma automática, hasta que no exista, para continuar con el proceso de registro. |
| 11 | El precio de la reserva es la suma del precio de sus franjas (el precio por hora de la cancha en ese momento por cada hora reservada) y no cambia después. | No aplica | No aplica |
| 12 | RN-13: la confirmación de asistencia inicia en PENDIENTE, o en NO_REQUERIDA si la reserva se hace con menos de 5 horas de anticipación o inicia antes de las 12:00 m. | No aplica | No aplica |
| 13 | RN-06: la reserva registrada se notifica por correo al jugador y al administrador del escenario. Si el envío del correo falla, la operación no se revierte: se registra el error y no se reintenta el envío (Release 1). | No aplica | No aplica |


### cancelar

Permite cancelar una reserva confirmada y liberar el horario de la cancha, ya sea por decisión del jugador o del administrador propietario por fuerza mayor.

**Entradas:**

- `reserva` (Reserva): Contiene el identificador de la reserva y, si cancela el administrador, el motivo de la cancelación.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Reserva: Reserva en estado CANCELADA, con su origen y su fecha de cancelación.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 14 | Debe existir una reserva con el identificador enviado, en estado CONFIRMADA. | No existe una reserva con el identificador enviado o no está en estado CONFIRMADA. | Abortar la cancelación de la reserva y reportar un mensaje de error al usuario, indicándole que la reserva no existe o ya no está confirmada. |
| 15 | Solo puede cancelar la reserva el jugador que la hizo o el administrador propietario del escenario de la cancha. | El solicitante no es el jugador de la reserva ni el propietario del escenario. | Abortar la cancelación de la reserva y reportar un mensaje de error al usuario, indicándole que no tiene permiso para cancelar esta reserva. |
| 16 | RN-10: el jugador solo puede cancelar si faltan más de 3 horas para la hora de inicio. | Cancela el jugador y faltan 3 horas o menos para el inicio de la reserva. | Abortar la cancelación de la reserva y reportar un mensaje de error al usuario, indicándole que ya no es posible cancelar porque faltan 3 horas o menos para el partido. |
| 17 | RN-07: el administrador propietario solo puede cancelar una reserva que aún no ha iniciado, por una causa de fuerza mayor (clima, falta de energía, etc.). | Cancela el administrador y la reserva ya inició. | Abortar la cancelación de la reserva y reportar un mensaje de error al usuario, indicándole que no se puede cancelar una reserva que ya inició. |
| 18 | RN-07: cuando cancela el administrador, la justificación (motivoCancelacion) es obligatoria. | Cancela el administrador y no se envía la justificación. | Abortar la cancelación de la reserva y reportar un mensaje de error al usuario, indicándole que debe registrar la justificación de la cancelación. |
| 19 | Se registran la fecha de cancelación y el origen: JUGADOR o ADMINISTRADOR, según quien cancele. La cancelación del administrador no está sujeta a RN-10. | No aplica | No aplica |
| 20 | Al cancelar, el horario queda libre para nuevas reservas. | No aplica | No aplica |
| 21 | RN-06: toda cancelación se notifica por correo al jugador y al administrador; si canceló el administrador, incluye la justificación. Si el envío del correo falla, la operación no se revierte: se registra el error y no se reintenta el envío (Release 1). | No aplica | No aplica |


### marcarNoAsistio

Permite al administrador propietario o a un encargado registrar que el jugador no se presentó a una reserva confirmada.

**Entradas:**

- `id` (Alfanumérico): Identificador de la reserva a la que el jugador no asistió.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Reserva: Reserva en estado NO_ASISTIO.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 14 | Debe existir una reserva con el identificador enviado, en estado CONFIRMADA. | No existe una reserva con el identificador enviado o no está en estado CONFIRMADA. | Abortar el registro de la inasistencia y reportar un mensaje de error al usuario, indicándole que la reserva no existe o ya no está confirmada. |
| 22 | RN-09: solo se puede marcar la inasistencia cuando la hora de fin de la reserva ya pasó. | La hora de fin de la reserva aún no ha pasado. | Abortar el registro de la inasistencia y reportar un mensaje de error al usuario, indicándole que solo se puede marcar la inasistencia después de la hora de fin de la reserva. |
| 23 | RN-09 / RN-15: la inasistencia la puede marcar el administrador propietario o un encargado asignado al escenario de la reserva. | El solicitante no es el propietario ni un encargado asignado al escenario. | Abortar el registro de la inasistencia y reportar un mensaje de error al usuario, indicándole que no tiene permiso para marcar la inasistencia en este escenario. |
| 24 | RN-09: la reserva queda en estado NO_ASISTIO y sirve de base para futuras políticas de sanción. | No aplica | No aplica |
| 32 | RN-09: la inasistencia solo se puede marcar hasta 24 horas después de la hora de fin de la reserva. | Pasaron más de 24 horas desde la hora de fin de la reserva. | Abortar el registro de la inasistencia y reportar un mensaje de error al usuario, indicándole que la inasistencia solo se puede marcar hasta 24 horas después de terminada la reserva. |
| 33 | RN-09: la marca de inasistencia no se revierte: una reserva NO_ASISTIO no vuelve a CONFIRMADA. | No aplica | No aplica |


### confirmarAsistencia

Permite al jugador confirmar, desde el enlace del correo, que asistirá a la reserva. Si en cambio decide no asistir, se aplica cancelar con origen JUGADOR.

**Entradas:**

- `id` (Alfanumérico): Identificador de la reserva cuya asistencia se confirma.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Reserva: Reserva con su confirmación de asistencia en CONFIRMADA.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 14 | Debe existir una reserva con el identificador enviado, en estado CONFIRMADA. | No existe una reserva con el identificador enviado o no está en estado CONFIRMADA. | Abortar la confirmación de asistencia y reportar un mensaje de error al usuario, indicándole que la reserva no existe o ya no está confirmada. |
| 25 | La reserva debe pertenecer al jugador que confirma la asistencia. | El solicitante no es el jugador de la reserva. | Abortar la confirmación de asistencia y reportar un mensaje de error al usuario, indicándole que no tiene permiso para confirmar la asistencia de esta reserva. |
| 26 | La confirmación de asistencia de la reserva debe estar en PENDIENTE. | La confirmación de asistencia ya fue respondida o no se requiere. | Abortar la confirmación de asistencia y reportar un mensaje de error al usuario, indicándole que la asistencia ya fue confirmada o no requiere confirmación. |
| 27 | RN-13: el jugador solo puede responder mientras falten más de 3 horas para el inicio. | Faltan 3 horas o menos para el inicio de la reserva. | Abortar la confirmación de asistencia y reportar un mensaje de error al usuario, indicándole que el plazo para confirmar la asistencia ya venció. |


### cancelarPorFaltaDeConfirmacion

Tarea programada del sistema: cancela la reserva si el jugador no respondió al llegar el plazo y libera la cancha.

**Entradas:**

- `id` (Alfanumérico): Identificador de la reserva que se evalúa.

**Salida:** Reserva: Reserva en estado CANCELADA, con origen SISTEMA_SIN_CONFIRMACION.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 28 | RN-13: se ejecuta al llegar exactamente a 3 horas del inicio, la confirmación de asistencia sigue PENDIENTE y la solicitud de confirmación se envió (fechaSolicitudConfirmacion diligenciada, D34). | La reserva ya fue confirmada, cancelada o no requiere confirmación, o nunca se le envió la solicitud de confirmación. | No se cancela la reserva; la tarea programada continúa con la siguiente reserva. |
| 29 | Se registran la fecha de cancelación y el origen SISTEMA_SIN_CONFIRMACION; no aplica el límite de RN-10. | No aplica | No aplica |
| 30 | La cancelación por falta de confirmación no se registra como NO_ASISTIO. | No aplica | No aplica |
| 20 | Al cancelar, el horario queda libre para nuevas reservas. | No aplica | No aplica |
| 21 | RN-06: toda cancelación se notifica por correo al jugador y al administrador; si canceló el administrador, incluye la justificación. | No aplica | No aplica |
