# HorarioRecurrente

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.4. Modelo de dominio/1.1.3.2. Modelo de dominio enriquecido/Modelo_de_dominio_enriquecido_CanchaYa.xlsx, hoja HorarioRecurrente`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

Objeto de dominio que define la disponibilidad semanal recurrente de un escenario (aplica a todas sus canchas). A partir de él se calculan, en cada consulta, las franjas de una hora reservables, descontando reservas y bloqueos; las franjas no se almacenan como entidad.

## Atributos

| Atributo | Tipo de Dato | Longitud mínima | Longitud máxima | Formato | Regla especial | ¿Auto generado? | ¿Calculado? | ¿Obligatorio? | ¿Sensible? | ¿Identifica al registro? | Descripción |
|---|---|---|---|---|---|---|---|---|---|---|---|
| id | Alfanumérico | 36 | 36 | Formato de un Identificador Único Universal (UUID) | Corresponde a una cadena de 36 caracteres: 32 dígitos del 0 al 9 y letras de la "a" a la "f", en grupos de 8-4-4-4-12 separados por guiones (D37) | SI | NO | SI | NO | SI | Identificador único de la regla de horario recurrente. |
| escenario | Escenario |  |  |  |  | NO | NO | SI | NO | NO | Escenario al que aplica este horario recurrente (rige para todas sus canchas). |
| diaSemana | Alfanumérico |  |  | Enumeración | Valores permitidos: LUNES..DOMINGO | NO | NO | SI | NO | NO | Día de la semana en que aplica el horario. |
| horaInicio | Hora |  |  | hh:mm | Debe ser una hora en punto (hh:00) | NO | NO | SI | NO | NO | Hora de inicio de disponibilidad. |
| horaFin | Hora |  |  | hh:mm | horaFin debe ser mayor que horaInicio y ser una hora en punto (hh:00) | NO | NO | SI | NO | NO | Hora de fin de disponibilidad. |


### Uso de cada atributo por responsabilidad

| Atributo | definir | modificar | eliminar | calcularFranjas |
|---|---|---|---|---|
| id | Se asigna automáticamente | Requerido/No modificable | Requerido | No requerido |
| escenario | Requerido | No modificable | No requerido | Filtro |
| diaSemana | Requerido | Requerido | No requerido | Filtro |
| horaInicio | Requerido | Requerido | No requerido | Se consulta |
| horaFin | Requerido | Requerido | No requerido | Se consulta |


## Responsabilidades


### definir

Permite indicar en qué día de la semana y entre qué horas atiende el escenario; rige para todas sus canchas.

**Entradas:**

- `horarioRecurrente` (HorarioRecurrente): Contiene los datos del nuevo horario recurrente que se va a definir.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** HorarioRecurrente: Horario recurrente registrado, con su identificador asignado.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | Debe existir el escenario al que aplica el horario. | No existe un escenario con el identificador enviado. | Abortar la definición del horario recurrente y reportar un mensaje de error al usuario, indicándole que no existe el escenario al que se quiere definir el horario. |
| 2 | RN-05 / RN-15: únicamente el administrador propietario del escenario puede definir, modificar o eliminar sus horarios; el encargado de sede no puede. | El solicitante no es el administrador propietario del escenario. | Abortar la definición del horario recurrente y reportar un mensaje de error al usuario, indicándole que no tiene permiso para gestionar los horarios de este escenario. |
| 3 | La hora de fin debe ser mayor que la hora de inicio. | La hora de fin es igual o anterior a la hora de inicio. | Abortar la definición del horario recurrente y reportar un mensaje de error al usuario, indicándole que la hora de fin debe ser mayor que la hora de inicio. |
| 4 | El horario no debe solaparse con otro horario del mismo escenario en el mismo día de la semana. | Existe otro horario del escenario que se cruza con el horario enviado en el mismo día. | Abortar la definición del horario recurrente y reportar un mensaje de error al usuario, indicándole que el horario se cruza con otro horario ya definido para ese día. |
| 5 | No debe existir otro horario recurrente con el mismo identificador (UUID). | Existe un horario recurrente con el mismo identificador. | Se debe reintentar con un nuevo identificador generado de forma automática, hasta que no exista, para continuar con el proceso de registro. |
| 13 | RN-18: la hora de inicio y la hora de fin deben ser en punto. | La hora de inicio o la hora de fin tiene minutos distintos de 00. | Abortar la definición del horario recurrente y reportar un mensaje de error al usuario, indicándole que los horarios se definen en horas en punto. |


### modificar

Permite cambiar el día o las horas de atención de un horario ya definido.

**Entradas:**

- `horarioRecurrente` (HorarioRecurrente): Contiene los datos que serán usados para modificar el horario recurrente.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** HorarioRecurrente: Horario recurrente con sus datos actualizados.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 6 | Debe existir un horario recurrente con el identificador enviado. | No existe un horario recurrente con el identificador enviado. | Abortar la modificación del horario recurrente y reportar un mensaje de error al usuario, indicándole que no existe un horario con ese identificador. |
| 2 | RN-05 / RN-15: únicamente el administrador propietario del escenario puede definir, modificar o eliminar sus horarios; el encargado de sede no puede. | El solicitante no es el administrador propietario del escenario. | Abortar la modificación del horario recurrente y reportar un mensaje de error al usuario, indicándole que no tiene permiso para gestionar los horarios de este escenario. |
| 3 | La hora de fin debe ser mayor que la hora de inicio. | La hora de fin es igual o anterior a la hora de inicio. | Abortar la modificación del horario recurrente y reportar un mensaje de error al usuario, indicándole que la hora de fin debe ser mayor que la hora de inicio. |
| 7 | El horario no debe solaparse con otro horario del mismo escenario en el mismo día, a excepción de que sea el mismo horario que se está modificando. | Existe otro horario con diferente identificador que se cruza con el horario enviado en el mismo día. | Abortar la modificación del horario recurrente y reportar un mensaje de error al usuario, indicándole que el horario se cruza con otro horario ya definido para ese día. |
| 13 | RN-18: la hora de inicio y la hora de fin deben ser en punto. | La hora de inicio o la hora de fin tiene minutos distintos de 00. | Abortar la modificación del horario recurrente y reportar un mensaje de error al usuario, indicándole que los horarios se definen en horas en punto. |
| 14 | RN-17: la modificación aplica hacia adelante; las reservas confirmadas ya registradas se conservan aunque queden por fuera del nuevo horario. | No aplica | No aplica |


### eliminar

Permite retirar un horario de atención del escenario.

**Entradas:**

- `id` (Alfanumérico): Identificador del horario recurrente que se va a eliminar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** No aplica: No retorna información.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 6 | Debe existir un horario recurrente con el identificador enviado. | No existe un horario recurrente con el identificador enviado. | Abortar la eliminación del horario recurrente y reportar un mensaje de error al usuario, indicándole que no existe un horario con ese identificador. |
| 2 | RN-05 / RN-15: únicamente el administrador propietario del escenario puede definir, modificar o eliminar sus horarios; el encargado de sede no puede. | El solicitante no es el administrador propietario del escenario. | Abortar la eliminación del horario recurrente y reportar un mensaje de error al usuario, indicándole que no tiene permiso para gestionar los horarios de este escenario. |
| 8 | RN-04: si el escenario queda sin horarios recurrentes, sus canchas dejan de aparecer en la búsqueda. | No aplica | No aplica |
| 15 | RN-17: no se puede eliminar un horario si hay reservas confirmadas futuras en ese día de la semana y rango de horas. | Existen reservas confirmadas futuras dentro del horario que se quiere eliminar. | Abortar la eliminación del horario recurrente y reportar un mensaje de error al usuario, indicándole que hay reservas confirmadas futuras dentro de ese horario. |


### calcularFranjas

Permite calcular, para una fecha, las franjas de una hora reservables de una cancha a partir de los horarios recurrentes de su escenario.

**Entradas:**

- `cancha` (Cancha): Cancha de la que se calculan las franjas; de ella se obtiene el escenario.
- `fecha` (Fecha): Fecha para la que se calculan las franjas.

**Salida:** Franja[]: Contiene el listado de franjas de una hora de la fecha consultada, cada una con su hora de inicio, su hora de fin y si está libre u ocupada.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 9 | Debe existir la cancha para la que se calculan las franjas. | No existe una cancha con el identificador enviado. | Abortar el cálculo de las franjas y reportar un mensaje de error al usuario, indicándole que no existe una cancha con ese identificador. |
| 10 | RN-11: solo se ofrecen fechas dentro de los 30 días calendario siguientes. | La fecha consultada es anterior a hoy o supera los 30 días calendario siguientes. | Abortar el cálculo de las franjas y reportar un mensaje de error al usuario, indicándole que solo se pueden consultar fechas dentro de los próximos 30 días. |
| 11 | Las franjas son de una hora, comienzan en hora en punto y se calculan en cada consulta a partir de los horarios recurrentes del escenario, descontando las reservas confirmadas y los bloqueos de la cancha. No se almacenan como entidad. | No aplica | No aplica |
| 12 | No se ofrecen franjas cuya hora de inicio ya pasó. | No aplica | No aplica |
