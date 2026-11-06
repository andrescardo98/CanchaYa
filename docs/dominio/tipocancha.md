# TipoCancha

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.4. Modelo de dominio/1.1.3.2. Modelo de dominio enriquecido/Modelo_de_dominio_enriquecido_CanchaYa.xlsx, hoja TipoCancha`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

Objeto de dominio que representa la modalidad de fútbol para la que está configurada una cancha (F5, F7, F11), junto con sus características propias. En el Release 1 el catálogo se carga con datos iniciales (F5, F7 y F11) y se usa como lista de solo lectura; su gestión es del Release 2.

## Atributos

| Atributo | Tipo de Dato | Longitud mínima | Longitud máxima | Rango Inicial | Rango Final | Formato | Regla especial | ¿Auto generado? | ¿Calculado? | ¿Obligatorio? | ¿Sensible? | ¿Identifica al registro? | Descripción |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| id | Alfanumérico | 36 | 36 |  |  | Formato de un Identificador Único Universal (UUID) | Corresponde a una cadena de 36 caracteres: 32 dígitos del 0 al 9 y letras de la "a" a la "f", en grupos de 8-4-4-4-12 separados por guiones (D37) | SI | NO | SI | NO | SI | Identificador único del tipo de cancha. |
| codigo | Alfanumérico | 2 | 5 |  |  | Solo letras mayúsculas [A-Z] y dígitos [0-9] | Valores cargados inicialmente: F5, F7 y F11. El administrador del sistema puede registrar otros (RN-16) | NO | NO | SI | NO | NO | Código corto de la modalidad de fútbol. |
| nombre | Alfanumérico | 1 | 50 |  |  |  |  | NO | NO | SI | NO | NO | Nombre legible de la modalidad de fútbol (ej. 'Fútbol 5'). |
| capacidadJugadores | Numérico |  |  | 2 | 22 |  |  | NO | NO | SI | NO | NO | Número máximo de jugadores que admite este tipo de cancha (ambos equipos). |


### Uso de cada atributo por responsabilidad

| Atributo | registrar | modificar | eliminar |
|---|---|---|---|
| id | Se asigna automáticamente | Requerido/No modificable | Requerido |
| codigo | Requerido | Requerido | No requerido |
| nombre | Requerido | Requerido | No requerido |
| capacidadJugadores | Requerido | Requerido | No requerido |


## Combinaciones únicas

| Nombre combinación | Descripción | Atributos |
|---|---|---|
| CodigoUnico | No es posible tener más de un tipo de cancha con el mismo código. | codigo |


## Responsabilidades


### registrar

Permite registrar la información de un nuevo tipo de cancha (modalidad de fútbol), que podrá ser utilizado para clasificar las canchas.

**Entradas:**

- `tipoCancha` (TipoCancha): Contiene los datos del nuevo tipo de cancha que se va a registrar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** TipoCancha: Tipo de cancha registrado, con su identificador asignado.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | RN-16: únicamente el administrador del sistema puede gestionar el catálogo de tipos de cancha. | El solicitante no tiene rol ADMINISTRADOR_SISTEMA. | Abortar el registro del nuevo tipo de cancha y reportar un mensaje de error al usuario, indicándole que solo el administrador del sistema puede gestionar los tipos de cancha. |
| 2 | No debe existir otro tipo de cancha con el mismo código (CodigoUnico). | Existe un tipo de cancha con el mismo código. | Abortar el registro del nuevo tipo de cancha y reportar un mensaje de error al usuario, indicándole que ya existe un tipo de cancha con el mismo código. |
| 3 | La capacidad de jugadores debe estar entre 2 y 22. | La capacidad de jugadores está por fuera del rango permitido. | Abortar el registro del nuevo tipo de cancha y reportar un mensaje de error al usuario, indicándole que la capacidad de jugadores debe estar entre 2 y 22. |
| 4 | No debe existir otro tipo de cancha con el mismo identificador (UUID). | Existe un tipo de cancha con el mismo identificador. | Se debe reintentar con un nuevo identificador generado de forma automática, hasta que no exista, para continuar con el proceso de registro. |


### modificar

Permite cambiar el código, el nombre o la capacidad de jugadores de un tipo de cancha existente.

**Entradas:**

- `tipoCancha` (TipoCancha): Contiene los datos que serán usados para modificar el tipo de cancha.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** TipoCancha: Tipo de cancha con sus datos actualizados.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | RN-16: únicamente el administrador del sistema puede gestionar el catálogo de tipos de cancha. | El solicitante no tiene rol ADMINISTRADOR_SISTEMA. | Abortar la modificación del tipo de cancha y reportar un mensaje de error al usuario, indicándole que solo el administrador del sistema puede gestionar los tipos de cancha. |
| 5 | Debe existir un tipo de cancha con el identificador enviado. | No existe un tipo de cancha con el identificador enviado. | Abortar la modificación del tipo de cancha y reportar un mensaje de error al usuario, indicándole que no existe un tipo de cancha con ese identificador. |
| 6 | No debe existir otro tipo de cancha con el mismo código, a excepción de que sea el mismo tipo de cancha que se está modificando. | Existe otro tipo de cancha con diferente identificador y con el mismo código que se quiere asignar. | Abortar la modificación del tipo de cancha y reportar un mensaje de error al usuario, indicándole que ya existe otro tipo de cancha con el código que se desea asignar. |
| 3 | La capacidad de jugadores debe estar entre 2 y 22. | La capacidad de jugadores está por fuera del rango permitido. | Abortar la modificación del tipo de cancha y reportar un mensaje de error al usuario, indicándole que la capacidad de jugadores debe estar entre 2 y 22. |


### eliminar

Permite dar de baja de forma definitiva un tipo de cancha existente.

**Entradas:**

- `id` (Alfanumérico): Identificador del tipo de cancha que se va a eliminar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** No aplica: No retorna información.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | RN-16: únicamente el administrador del sistema puede gestionar el catálogo de tipos de cancha. | El solicitante no tiene rol ADMINISTRADOR_SISTEMA. | Abortar la eliminación del tipo de cancha y reportar un mensaje de error al usuario, indicándole que solo el administrador del sistema puede gestionar los tipos de cancha. |
| 5 | Debe existir un tipo de cancha con el identificador enviado. | No existe un tipo de cancha con el identificador enviado. | Abortar la eliminación del tipo de cancha y reportar un mensaje de error al usuario, indicándole que no existe un tipo de cancha con ese identificador. |
| 7 | El tipo de cancha no debe estar siendo utilizado por ninguna cancha. | Existe al menos una cancha asociada al tipo de cancha. | Abortar la eliminación del tipo de cancha y reportar un mensaje de error al usuario, indicándole que el tipo de cancha está asignado a una o más canchas y no se puede eliminar. |
