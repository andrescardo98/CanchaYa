# EncargadoEscenario

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.4. Modelo de dominio/1.1.3.2. Modelo de dominio enriquecido/Modelo_de_dominio_enriquecido_CanchaYa.xlsx, hoja EncargadoEscenario`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

Objeto de dominio que representa la asignación de un usuario como encargado de un escenario, con permisos limitados a la operación diaria de esa sede. El propietario del escenario no se registra aquí: está en el atributo administrador de Escenario.

## Atributos

| Atributo | Tipo de Dato | Longitud mínima | Longitud máxima | Formato | Regla especial | ¿Auto generado? | ¿Calculado? | ¿Obligatorio? | ¿Sensible? | ¿Identifica al registro? | Descripción |
|---|---|---|---|---|---|---|---|---|---|---|---|
| id | Alfanumérico | 36 | 36 | Formato de un Identificador Único Universal (UUID) | Corresponde a una cadena de 36 caracteres: 32 dígitos del 0 al 9 y letras de la "a" a la "f", en grupos de 8-4-4-4-12 separados por guiones (D37) | SI | NO | SI | NO | SI | Identificador único de la asignación. |
| escenario | Escenario |  |  |  |  | NO | NO | SI | NO | NO | Escenario al que se asigna el usuario. |
| usuario | Usuario |  |  |  | El usuario referenciado debe tener rol ADMINISTRADOR_CANCHA | NO | NO | SI | NO | NO | Usuario asignado como encargado del escenario. |
| fechaAsignacion | Fecha |  |  | dd/mm/aaaa hh:mm:ss |  | SI | NO | SI | NO | NO | Fecha y hora en que se realizó la asignación. |


### Uso de cada atributo por responsabilidad

| Atributo | asignar | retirar |
|---|---|---|
| id | Se asigna automáticamente | Requerido |
| escenario | Requerido | No requerido |
| usuario | Requerido | No requerido |
| fechaAsignacion | Se asigna automáticamente | No requerido |


## Combinaciones únicas

| Nombre combinación | Descripción | Atributos |
|---|---|---|
| AsignacionUnica | No es posible asignar al mismo usuario más de una vez al mismo escenario. | escenario, usuario |


## Responsabilidades


### asignar

Permite al propietario asignar a un usuario como encargado de un escenario, con permisos limitados a la operación diaria de esa sede.

**Entradas:**

- `encargadoEscenario` (EncargadoEscenario): Contiene el escenario y el usuario que se va a asignar como encargado.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** EncargadoEscenario: Asignación registrada, con su identificador y su fecha de asignación.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | Debe existir el escenario al que se asigna el encargado. | No existe un escenario con el identificador enviado. | Abortar la asignación del encargado y reportar un mensaje de error al usuario, indicándole que no existe el escenario al que se quiere asignar el encargado. |
| 2 | RN-05 / RN-15: solo el administrador propietario del escenario puede asignar o retirar encargados. | El solicitante no es el administrador propietario del escenario. | Abortar la asignación del encargado y reportar un mensaje de error al usuario, indicándole que no tiene permiso para gestionar los encargados de este escenario. |
| 3 | El usuario a asignar debe existir y tener rol ADMINISTRADOR_CANCHA. | No existe el usuario enviado o no tiene rol ADMINISTRADOR_CANCHA. | Abortar la asignación del encargado y reportar un mensaje de error al usuario, indicándole que el usuario indicado no existe o no puede ser encargado. |
| 4 | No se puede asignar al mismo usuario más de una vez al mismo escenario (AsignacionUnica). | El usuario ya está asignado como encargado del escenario. | Abortar la asignación del encargado y reportar un mensaje de error al usuario, indicándole que el usuario ya es encargado de este escenario. |
| 5 | No debe existir otra asignación con el mismo identificador (UUID). | Existe una asignación con el mismo identificador. | Se debe reintentar con un nuevo identificador generado de forma automática, hasta que no exista, para continuar con el proceso de registro. |
| 9 | RN-15: el administrador propietario no puede asignarse a sí mismo como encargado de su propio escenario. | El usuario a asignar es el administrador propietario del escenario. | Abortar la asignación del encargado y reportar un mensaje de error al usuario, indicándole que el propietario no puede ser encargado de su propio escenario. |


### retirar

Permite al propietario retirar la asignación de un encargado.

**Entradas:**

- `id` (Alfanumérico): Identificador de la asignación que se va a retirar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** No aplica: No retorna información.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 6 | Debe existir la asignación con el identificador enviado. | No existe una asignación con el identificador enviado. | Abortar el retiro del encargado y reportar un mensaje de error al usuario, indicándole que no existe la asignación del encargado. |
| 2 | RN-05 / RN-15: solo el administrador propietario del escenario puede asignar o retirar encargados. | El solicitante no es el administrador propietario del escenario. | Abortar el retiro del encargado y reportar un mensaje de error al usuario, indicándole que no tiene permiso para gestionar los encargados de este escenario. |
| 7 | RN-15: al retirarlo, el encargado pierde de inmediato sus permisos sobre el escenario. | No aplica | No aplica |
| 8 | Los bloqueos que creó el encargado se conservan. | No aplica | No aplica |
