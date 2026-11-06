# Bloqueo

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.4. Modelo de dominio/1.1.3.2. Modelo de dominio enriquecido/Modelo_de_dominio_enriquecido_CanchaYa.xlsx, hoja Bloqueo`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

Objeto de dominio que representa un bloque de tiempo de una cancha inhabilitado manualmente por el administrador o un encargado (mantenimiento, torneo, uso propio o reserva presencial), no disponible para reservas de jugadores.

## Atributos

| Atributo | Tipo de Dato | Longitud mínima | Longitud máxima | Formato | Regla especial | ¿Auto generado? | ¿Calculado? | ¿Obligatorio? | ¿Sensible? | ¿Identifica al registro? | Descripción |
|---|---|---|---|---|---|---|---|---|---|---|---|
| id | Alfanumérico | 36 | 36 | Formato de un Identificador Único Universal (UUID) | Corresponde a una cadena de 36 caracteres: 32 dígitos del 0 al 9 y letras de la "a" a la "f", en grupos de 8-4-4-4-12 separados por guiones (D37) | SI | NO | SI | NO | SI | Identificador único del bloqueo. |
| cancha | Cancha |  |  |  |  | NO | NO | SI | NO | NO | Cancha sobre la que se aplica el bloqueo. |
| fecha | Fecha |  |  | dd/mm/aaaa |  | NO | NO | SI | NO | NO | Fecha del bloqueo. |
| horaInicio | Hora |  |  | hh:mm | Debe ser una hora en punto (hh:00) | NO | NO | SI | NO | NO | Hora de inicio del bloqueo. |
| horaFin | Hora |  |  | hh:mm | horaFin debe ser mayor que horaInicio y ser una hora en punto (hh:00) | NO | NO | SI | NO | NO | Hora de fin del bloqueo. |
| tipoBloqueo | Alfanumérico |  |  | Enumeración | Valores permitidos: MANTENIMIENTO \| TORNEO \| USO_PROPIO \| RESERVA_PRESENCIAL | NO | NO | SI | NO | NO | Razón por la que se bloquea el horario (RN-12). |
| motivo | Alfanumérico | 0 | 200 |  |  | NO | NO | NO | NO | NO | Detalle opcional del bloqueo (por ejemplo, el nombre del torneo o del cliente de una reserva presencial). |
| creadoPor | Usuario |  |  |  | Debe ser el administrador propietario o un encargado asignado al escenario de la cancha (RN-05/RN-15) | NO | NO | SI | NO | NO | Usuario que creó el bloqueo; deja trazabilidad entre el personal del escenario. |


### Uso de cada atributo por responsabilidad

| Atributo | crear | validarSolapamiento | eliminar |
|---|---|---|---|
| id | Se asigna automáticamente | No requerido | Requerido |
| cancha | Requerido | Requerido | No requerido |
| fecha | Requerido | Requerido | No requerido |
| horaInicio | Requerido | Requerido | No requerido |
| horaFin | Requerido | Requerido | No requerido |
| tipoBloqueo | Requerido | No requerido | No requerido |
| motivo | Opcional | No requerido | No requerido |
| creadoPor | Se asigna automáticamente | No requerido | No requerido |


## Responsabilidades


### crear

Permite registrar un bloqueo manual de disponibilidad sobre una cancha (mantenimiento, torneo, uso propio o reserva presencial).

**Entradas:**

- `bloqueo` (Bloqueo): Contiene los datos del nuevo bloqueo que se va a registrar; el atributo creadoPor corresponde al usuario autenticado.

**Salida:** Bloqueo: Bloqueo registrado, con su identificador asignado.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | Debe existir la cancha sobre la que se aplica el bloqueo. | No existe una cancha con el identificador enviado. | Abortar la creación del bloqueo y reportar un mensaje de error al usuario, indicándole que no existe la cancha que se quiere bloquear. |
| 2 | RN-05 / RN-12 / RN-15: el bloqueo lo puede crear el administrador propietario o un encargado asignado al escenario de la cancha. | El usuario que crea el bloqueo no es el propietario ni un encargado asignado al escenario. | Abortar la creación del bloqueo y reportar un mensaje de error al usuario, indicándole que no tiene permiso para bloquear horarios en este escenario. |
| 3 | RN-12: se debe indicar el tipo de bloqueo: MANTENIMIENTO, TORNEO, USO_PROPIO o RESERVA_PRESENCIAL. | No se envía el tipo de bloqueo o no corresponde a un valor permitido. | Abortar la creación del bloqueo y reportar un mensaje de error al usuario, indicándole que debe seleccionar un tipo de bloqueo válido. |
| 4 | La hora de fin debe ser mayor que la hora de inicio. | La hora de fin es igual o anterior a la hora de inicio. | Abortar la creación del bloqueo y reportar un mensaje de error al usuario, indicándole que la hora de fin debe ser mayor que la hora de inicio. |
| 5 | El horario a bloquear no debe cruzarse con otro bloqueo de la misma cancha. | Existe otro bloqueo de la cancha que se cruza con el horario enviado. | Abortar la creación del bloqueo y reportar un mensaje de error al usuario, indicándole que ya existe un bloqueo en ese horario. |
| 6 | RN-12: el bloqueo solo puede crearse sobre un horario libre; si hay una reserva confirmada, para continuar el administrador propietario debe cancelarla antes conforme a RN-07 (Reserva.cancelar con origen ADMINISTRADOR y justificación); el encargado no puede cancelarla. | Existe una reserva confirmada de la cancha que se cruza con el horario enviado. | Abortar la creación del bloqueo y reportar un mensaje de error al usuario, indicándole que hay una reserva confirmada en ese horario, informándole cuál es la reserva en conflicto. |
| 7 | No debe existir otro bloqueo con el mismo identificador (UUID). | Existe un bloqueo con el mismo identificador. | Se debe reintentar con un nuevo identificador generado de forma automática, hasta que no exista, para continuar con el proceso de registro. |
| 8 | RN-12: un bloqueo no está sujeto a los límites de RN-08, RN-09 ni RN-10. | No aplica | No aplica |
| 12 | RN-18: la hora de inicio y la hora de fin deben ser en punto. (D18: los bloqueos también se definen en horas en punto). | La hora de inicio o la hora de fin tiene minutos distintos de 00. | Abortar la creación del bloqueo y reportar un mensaje de error al usuario, indicándole que los bloqueos se definen en horas en punto. |
| 13 | RN-12: un bloqueo cubre un rango de horas de un solo día (una sola fecha); para bloquear varios días se crea un bloqueo por cada día. | No aplica | No aplica |


### validarSolapamiento

Permite verificar que el horario a bloquear no se cruce con otro bloqueo ni con una reserva confirmada.

**Entradas:**

- `bloqueo` (Bloqueo): Contiene la cancha, la fecha y las horas de inicio y de fin que se quieren bloquear.

**Salida:** Lógico: Verdadero si el horario está libre de otros bloqueos y de reservas confirmadas.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 5 | El horario a bloquear no debe cruzarse con otro bloqueo de la misma cancha. | Existe otro bloqueo de la cancha que se cruza con el horario enviado. | Abortar la creación del bloqueo y reportar un mensaje de error al usuario, indicándole que ya existe un bloqueo en ese horario. |
| 6 | RN-12: el bloqueo solo puede crearse sobre un horario libre; si hay una reserva confirmada, para continuar el administrador propietario debe cancelarla antes conforme a RN-07 (Reserva.cancelar con origen ADMINISTRADOR y justificación); el encargado no puede cancelarla. | Existe una reserva confirmada de la cancha que se cruza con el horario enviado. | Abortar la creación del bloqueo y reportar un mensaje de error al usuario, indicándole que hay una reserva confirmada en ese horario, informándole cuál es la reserva en conflicto. |


### eliminar

Permite retirar un bloqueo para que el horario vuelva a quedar disponible para los jugadores.

**Entradas:**

- `id` (Alfanumérico): Identificador del bloqueo que se va a eliminar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** No aplica: No retorna información.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 9 | Debe existir un bloqueo con el identificador enviado. | No existe un bloqueo con el identificador enviado. | Abortar la eliminación del bloqueo y reportar un mensaje de error al usuario, indicándole que no existe un bloqueo con ese identificador. |
| 10 | RN-12 / RN-15: el bloqueo lo puede eliminar el administrador propietario o un encargado asignado al escenario de la cancha. | El solicitante no es el propietario ni un encargado asignado al escenario. | Abortar la eliminación del bloqueo y reportar un mensaje de error al usuario, indicándole que no tiene permiso para eliminar este bloqueo. |
| 11 | Al eliminar el bloqueo, el horario vuelve a ofrecerse a los jugadores. | No aplica | No aplica |
