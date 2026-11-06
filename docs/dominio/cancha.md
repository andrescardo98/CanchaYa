# Cancha

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.4. Modelo de dominio/1.1.3.2. Modelo de dominio enriquecido/Modelo_de_dominio_enriquecido_CanchaYa.xlsx, hoja Cancha`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

Objeto de dominio que representa cada cancha sintética individual, perteneciente a un escenario, que puede ser reservada por los jugadores.

## Atributos

| Atributo | Tipo de Dato | Longitud mínima | Longitud máxima | Precisión | Rango Inicial | Formato | Valor por defecto | Regla especial | ¿Auto generado? | ¿Calculado? | ¿Obligatorio? | ¿Sensible? | ¿Identifica al registro? | Descripción |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| id | Alfanumérico | 36 | 36 |  |  | Formato de un Identificador Único Universal (UUID) |  | Corresponde a una cadena de 36 caracteres: 32 dígitos del 0 al 9 y letras de la "a" a la "f", en grupos de 8-4-4-4-12 separados por guiones (D37) | SI | NO | SI | NO | SI | Identificador único de la cancha. |
| nombre | Alfanumérico | 1 | 50 |  |  |  |  | Debe ser único dentro del escenario | NO | NO | SI | NO | NO | Nombre o número con que el escenario identifica la cancha (por ejemplo, «Cancha 1»). Permite distinguir canchas del mismo tipo en la agenda y en la reserva. |
| escenario | Escenario |  |  |  |  |  |  |  | NO | NO | SI | NO | NO | Escenario deportivo al que pertenece la cancha (agrupación 1—N). |
| tipoCancha | TipoCancha |  |  |  |  |  |  |  | NO | NO | SI | NO | NO | Modalidad de fútbol para la que está configurada la cancha (N canchas — 1 TipoCancha). |
| precioHora | Numérico |  |  | 2 | 0.01 |  |  |  | NO | NO | SI | NO | NO | Precio de la cancha por hora reservada. |
| estado | Alfanumérico |  |  |  |  | Enumeración | DISPONIBLE | Valores permitidos: DISPONIBLE \| INACTIVA | NO | NO | SI | NO | NO | Estado de publicación de la cancha. |


### Uso de cada atributo por responsabilidad

| Atributo | registrar | modificar | cambiarEstado | publicarEnBusqueda | validarPropietario |
|---|---|---|---|---|---|
| id | Se asigna automáticamente | Requerido/No modificable | Requerido/No modificable | Requerido | Requerido |
| nombre | Requerido | Requerido | No requerido | No requerido | No requerido |
| escenario | Requerido | No modificable | No requerido | Se consulta | Se consulta |
| tipoCancha | Requerido | Requerido | No requerido | No requerido | No requerido |
| precioHora | Requerido | Requerido | No requerido | Se consulta | No requerido |
| estado | Se asigna automáticamente | No modificable | Requerido/Modificable | Se consulta | No requerido |


## Combinaciones únicas

| Nombre combinación | Descripción | Atributos |
|---|---|---|
| NombreUnicoPorEscenario | No es posible tener más de una cancha con el mismo nombre en un mismo escenario. | escenario, nombre |


## Responsabilidades


### registrar

Permite agregar una cancha a un escenario, con su nombre, tipo y precio por hora.

**Entradas:**

- `cancha` (Cancha): Contiene los datos de la nueva cancha que se va a registrar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Cancha: Cancha registrada, con su identificador y su estado inicial asignados.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | Debe existir el escenario al que pertenece la cancha. | No existe un escenario con el identificador enviado. | Abortar el registro de la nueva cancha y reportar un mensaje de error al usuario, indicándole que no existe el escenario al que se quiere agregar la cancha. |
| 2 | RN-05 / RN-15: únicamente el administrador propietario del escenario puede registrar o modificar sus canchas y tarifas; el encargado de sede no puede. | El solicitante no es el administrador propietario del escenario de la cancha. | Abortar el registro de la nueva cancha y reportar un mensaje de error al usuario, indicándole que no tiene permiso para gestionar las canchas de este escenario. |
| 3 | No debe existir otra cancha con el mismo nombre en el mismo escenario (NombreUnicoPorEscenario). | Existe una cancha con el mismo nombre en el escenario. | Abortar el registro de la nueva cancha y reportar un mensaje de error al usuario, indicándole que ya existe una cancha con ese nombre en el escenario. |
| 4 | Debe existir el tipo de cancha indicado. | No existe un tipo de cancha con el identificador enviado. | Abortar el registro de la nueva cancha y reportar un mensaje de error al usuario, indicándole que el tipo de cancha seleccionado no existe. |
| 5 | El precio por hora debe ser mayor que cero. | El precio por hora es menor o igual a cero. | Abortar el registro de la nueva cancha y reportar un mensaje de error al usuario, indicándole que el precio por hora debe ser mayor que cero. |
| 6 | No debe existir otra cancha con el mismo identificador (UUID). | Existe una cancha con el mismo identificador. | Se debe reintentar con un nuevo identificador generado de forma automática, hasta que no exista, para continuar con el proceso de registro. |


### modificar

Permite cambiar el nombre, el tipo o el precio por hora de una cancha existente.

**Entradas:**

- `cancha` (Cancha): Contiene los datos que serán usados para modificar la cancha.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Cancha: Cancha con sus datos actualizados.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 7 | Debe existir una cancha con el identificador enviado. | No existe una cancha con el identificador enviado. | Abortar la modificación de la cancha y reportar un mensaje de error al usuario, indicándole que no existe una cancha con ese identificador. |
| 2 | RN-05 / RN-15: únicamente el administrador propietario del escenario puede registrar o modificar sus canchas y tarifas; el encargado de sede no puede. | El solicitante no es el administrador propietario del escenario de la cancha. | Abortar la modificación de la cancha y reportar un mensaje de error al usuario, indicándole que no tiene permiso para gestionar las canchas de este escenario. |
| 8 | No debe existir otra cancha con el mismo nombre en el mismo escenario, a excepción de que sea la misma cancha que se está modificando. | Existe otra cancha con diferente identificador y con el mismo nombre en el escenario. | Abortar la modificación de la cancha y reportar un mensaje de error al usuario, indicándole que ya existe otra cancha con el nombre que se desea asignar. |
| 4 | Debe existir el tipo de cancha indicado. | No existe un tipo de cancha con el identificador enviado. | Abortar la modificación de la cancha y reportar un mensaje de error al usuario, indicándole que el tipo de cancha seleccionado no existe. |
| 5 | El precio por hora debe ser mayor que cero. | El precio por hora es menor o igual a cero. | Abortar la modificación de la cancha y reportar un mensaje de error al usuario, indicándole que el precio por hora debe ser mayor que cero. |
| 9 | El cambio de precio no afecta a las reservas ya creadas: cada reserva conserva el precio con el que se registró. | No aplica | No aplica |


### cambiarEstado

Permite poner una cancha como DISPONIBLE o INACTIVA para las reservas de los jugadores.

**Entradas:**

- `cancha` (Cancha): Contiene el identificador de la cancha y el nuevo estado que se le va a aplicar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Cancha: Cancha con su nuevo estado.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 7 | Debe existir una cancha con el identificador enviado. | No existe una cancha con el identificador enviado. | Abortar el cambio de estado de la cancha y reportar un mensaje de error al usuario, indicándole que no existe una cancha con ese identificador. |
| 2 | RN-05 / RN-15: únicamente el administrador propietario del escenario puede registrar o modificar sus canchas y tarifas; el encargado de sede no puede. | El solicitante no es el administrador propietario del escenario de la cancha. | Abortar el cambio de estado de la cancha y reportar un mensaje de error al usuario, indicándole que no tiene permiso para gestionar las canchas de este escenario. |
| 10 | El nuevo estado debe ser diferente al que tiene actualmente la cancha. | El estado enviado es igual al que la cancha posee actualmente. | Abortar el cambio de estado de la cancha y reportar un mensaje de error al usuario, indicándole que la cancha ya se encuentra en ese estado. |
| 12 | RN-17: no se puede inactivar una cancha que tenga reservas confirmadas futuras. | Se intenta inactivar la cancha y tiene reservas confirmadas futuras. | Abortar el cambio de estado de la cancha y reportar un mensaje de error al usuario, indicándole que hay reservas confirmadas futuras que impiden inactivarla. |


### publicarEnBusqueda

Permite determinar si la cancha es visible en los resultados de búsqueda de los jugadores.

**Entradas:**

- `id` (Alfanumérico): Identificador de la cancha que se evalúa.

**Salida:** Lógico: Verdadero si la cancha debe mostrarse en los resultados de búsqueda.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 7 | Debe existir una cancha con el identificador enviado. | No existe una cancha con el identificador enviado. | Abortar la evaluación de la cancha y reportar un mensaje de error al usuario, indicándole que no existe una cancha con ese identificador. |
| 11 | RN-04: la cancha solo se muestra en la búsqueda si está en estado DISPONIBLE, su escenario está ACTIVO y tiene publicados al menos un horario recurrente y el precio por hora. Si no cumple, simplemente no se muestra. | No aplica | No aplica |


### validarPropietario

Permite verificar que quien intenta gestionar la cancha sea el administrador propietario de su escenario.

**Entradas:**

- `id` (Alfanumérico): Identificador de la cancha que se quiere gestionar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Lógico: Verdadero si el solicitante es el administrador propietario del escenario de la cancha.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 7 | Debe existir una cancha con el identificador enviado. | No existe una cancha con el identificador enviado. | Abortar la operación solicitada sobre la cancha y reportar un mensaje de error al usuario, indicándole que no existe una cancha con ese identificador. |
| 2 | RN-05 / RN-15: únicamente el administrador propietario del escenario puede registrar o modificar sus canchas y tarifas; el encargado de sede no puede. | El solicitante no es el administrador propietario del escenario de la cancha. | Abortar la operación solicitada sobre la cancha y reportar un mensaje de error al usuario, indicándole que no tiene permiso para gestionar las canchas de este escenario. |
