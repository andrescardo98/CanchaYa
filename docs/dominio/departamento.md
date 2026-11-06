# Departamento

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.4. Modelo de dominio/1.1.3.2. Modelo de dominio enriquecido/Modelo_de_dominio_enriquecido_CanchaYa.xlsx, hoja Departamento`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

Objeto de dominio de referencia que representa un departamento de Colombia. Agrupa ciudades y permite filtrar la búsqueda de escenarios. Se carga inicialmente con los datos de Antioquia.

## Atributos

| Atributo | Tipo de Dato | Longitud mínima | Longitud máxima | Formato | Regla especial | ¿Auto generado? | ¿Calculado? | ¿Obligatorio? | ¿Sensible? | ¿Identifica al registro? | Descripción |
|---|---|---|---|---|---|---|---|---|---|---|---|
| id | Alfanumérico | 36 | 36 | Formato de un Identificador Único Universal (UUID) | Corresponde a una cadena de 36 caracteres: 32 dígitos del 0 al 9 y letras de la "a" a la "f", en grupos de 8-4-4-4-12 separados por guiones (D37) | SI | NO | SI | NO | SI | Identificador único del departamento. |
| codigo | Alfanumérico | 2 | 2 | Código DANE de 2 dígitos |  | NO | NO | SI | NO | NO | Código DANE del departamento. |
| nombre | Alfanumérico | 1 | 50 |  |  | NO | NO | SI | NO | NO | Nombre del departamento. |


### Uso de cada atributo por responsabilidad

| Atributo | registrar | modificar | eliminar |
|---|---|---|---|
| id | Se asigna automáticamente | Requerido/No modificable | Requerido |
| codigo | Requerido | Requerido | No requerido |
| nombre | Requerido | Requerido | No requerido |


## Combinaciones únicas

| Nombre combinación | Descripción | Atributos |
|---|---|---|
| NombreUnico | No es posible tener más de un departamento con el mismo nombre. | nombre |
| CodigoUnico | No es posible tener más de un departamento con el mismo código DANE. | codigo |


## Responsabilidades


### registrar

TRABAJO FUTURO: se modela, pero en el alcance actual el catálogo se carga una sola vez con los datos del DANE. Permite registrar un departamento en el catálogo.

**Entradas:**

- `departamento` (Departamento): Contiene los datos de el nuevo departamento que se va a registrar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Departamento: Registro creado, con su identificador asignado.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | RN-16: únicamente el administrador del sistema puede gestionar el catálogo de departamentos. | El solicitante no tiene rol ADMINISTRADOR_SISTEMA. | Abortar el registro del departamento y reportar un mensaje de error al usuario, indicándole que solo el administrador del sistema puede gestionar este catálogo. |
| 2 | No debe existir otro departamento con el mismo nombre (NombreUnico). | Existe un departamento con el mismo nombre. | Abortar el registro del departamento y reportar un mensaje de error al usuario, indicándole que ya existe un departamento con ese nombre. |
| 3 | No debe existir otro departamento con el mismo código DANE (CodigoUnico). | Existe un departamento con el mismo código DANE. | Abortar el registro del departamento y reportar un mensaje de error al usuario, indicándole que ya existe un departamento con ese código DANE. |
| 4 | No debe existir otro departamento con el mismo identificador (UUID). | Existe un departamento con el mismo identificador. | Se debe reintentar con un nuevo identificador generado de forma automática, hasta que no exista, para continuar con el proceso de registro. |


### modificar

TRABAJO FUTURO: se modela, pero en el alcance actual el catálogo se carga una sola vez con los datos del DANE. Permite cambiar los datos de un departamento existente.

**Entradas:**

- `departamento` (Departamento): Contiene los datos que serán usados para la modificación.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Departamento: Registro con sus datos actualizados.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | RN-16: únicamente el administrador del sistema puede gestionar el catálogo de departamentos. | El solicitante no tiene rol ADMINISTRADOR_SISTEMA. | Abortar la modificación del departamento y reportar un mensaje de error al usuario, indicándole que solo el administrador del sistema puede gestionar este catálogo. |
| 5 | Debe existir un departamento con el identificador enviado. | No existe un departamento con el identificador enviado. | Abortar la modificación del departamento y reportar un mensaje de error al usuario, indicándole que no existe un departamento con ese identificador. |
| 6 | No debe existir otro departamento con el mismo nombre (NombreUnico), a excepción de que sea el mismo departamento que se está modificando. | Existe otro departamento con diferente identificador y con el mismo nombre. | Abortar la modificación del departamento y reportar un mensaje de error al usuario, indicándole que ya existe un departamento con ese nombre. |
| 7 | No debe existir otro departamento con el mismo código DANE (CodigoUnico), a excepción de que sea el mismo departamento que se está modificando. | Existe otro departamento con diferente identificador y con el mismo código DANE. | Abortar la modificación del departamento y reportar un mensaje de error al usuario, indicándole que ya existe un departamento con ese código DANE. |


### eliminar

TRABAJO FUTURO: se modela, pero en el alcance actual el catálogo se carga una sola vez con los datos del DANE. Permite dar de baja de forma definitiva un departamento existente.

**Entradas:**

- `id` (Alfanumérico): Identificador del registro que se va a eliminar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** No aplica: No retorna información.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | RN-16: únicamente el administrador del sistema puede gestionar el catálogo de departamentos. | El solicitante no tiene rol ADMINISTRADOR_SISTEMA. | Abortar la eliminación del departamento y reportar un mensaje de error al usuario, indicándole que solo el administrador del sistema puede gestionar este catálogo. |
| 5 | Debe existir un departamento con el identificador enviado. | No existe un departamento con el identificador enviado. | Abortar la eliminación del departamento y reportar un mensaje de error al usuario, indicándole que no existe un departamento con ese identificador. |
| 8 | El departamento no debe tener ciudades asociadas. | Existe al menos una ciudad asociada al departamento. | Abortar la eliminación del departamento y reportar un mensaje de error al usuario, indicándole que el departamento tiene ciudades asociadas y no se puede eliminar. |
