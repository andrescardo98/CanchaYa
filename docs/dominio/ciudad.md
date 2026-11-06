# Ciudad

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.4. Modelo de dominio/1.1.3.2. Modelo de dominio enriquecido/Modelo_de_dominio_enriquecido_CanchaYa.xlsx, hoja Ciudad`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

Objeto de dominio de referencia que representa un municipio o ciudad de un departamento, donde se ubican los escenarios. Se carga inicialmente con los municipios de Antioquia.

## Atributos

| Atributo | Tipo de Dato | Longitud mínima | Longitud máxima | Formato | Regla especial | ¿Auto generado? | ¿Calculado? | ¿Obligatorio? | ¿Sensible? | ¿Identifica al registro? | Descripción |
|---|---|---|---|---|---|---|---|---|---|---|---|
| id | Alfanumérico | 36 | 36 | Formato de un Identificador Único Universal (UUID) | Corresponde a una cadena de 36 caracteres: 32 dígitos del 0 al 9 y letras de la "a" a la "f", en grupos de 8-4-4-4-12 separados por guiones (D37) | SI | NO | SI | NO | SI | Identificador único de la ciudad. |
| codigo | Alfanumérico | 5 | 5 | Código DANE de 5 dígitos |  | NO | NO | SI | NO | NO | Código DANE del municipio. |
| departamento | Departamento |  |  |  |  | NO | NO | SI | NO | NO | Departamento al que pertenece la ciudad (1 Departamento — N Ciudades). |
| nombre | Alfanumérico | 1 | 100 |  |  | NO | NO | SI | NO | NO | Nombre del municipio o ciudad. |


### Uso de cada atributo por responsabilidad

| Atributo | registrar | modificar | eliminar |
|---|---|---|---|
| id | Se asigna automáticamente | Requerido/No modificable | Requerido |
| codigo | Requerido | Requerido | No requerido |
| departamento | Requerido | Requerido | No requerido |
| nombre | Requerido | Requerido | No requerido |


## Combinaciones únicas

| Nombre combinación | Descripción | Atributos |
|---|---|---|
| CiudadUnicaPorDepartamento | No es posible tener más de una ciudad con el mismo nombre en un mismo departamento. | departamento, nombre |
| CodigoUnico | No es posible tener más de una ciudad con el mismo código DANE. | codigo |


## Responsabilidades


### registrar

TRABAJO FUTURO: se modela, pero en el alcance actual el catálogo se carga una sola vez con los datos del DANE. Permite registrar una ciudad en el catálogo.

**Entradas:**

- `ciudad` (Ciudad): Contiene los datos de la nueva ciudad que se va a registrar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Ciudad: Registro creado, con su identificador asignado.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | RN-16: únicamente el administrador del sistema puede gestionar el catálogo de ciudades. | El solicitante no tiene rol ADMINISTRADOR_SISTEMA. | Abortar el registro de la ciudad y reportar un mensaje de error al usuario, indicándole que solo el administrador del sistema puede gestionar este catálogo. |
| 2 | Debe existir el departamento al que pertenece la ciudad. | No existe un departamento con el identificador enviado. | Abortar el registro de la ciudad y reportar un mensaje de error al usuario, indicándole que el departamento seleccionado no existe. |
| 3 | No debe existir otra ciudad con el mismo nombre en el mismo departamento (CiudadUnicaPorDepartamento). | Existe una ciudad con el mismo nombre en el departamento. | Abortar el registro de la ciudad y reportar un mensaje de error al usuario, indicándole que ya existe una ciudad con ese nombre en el departamento. |
| 4 | No debe existir otra ciudad con el mismo código DANE (CodigoUnico). | Existe una ciudad con el mismo código DANE. | Abortar el registro de la ciudad y reportar un mensaje de error al usuario, indicándole que ya existe una ciudad con ese código DANE. |
| 5 | No debe existir otra ciudad con el mismo identificador (UUID). | Existe una ciudad con el mismo identificador. | Se debe reintentar con un nuevo identificador generado de forma automática, hasta que no exista, para continuar con el proceso de registro. |


### modificar

TRABAJO FUTURO: se modela, pero en el alcance actual el catálogo se carga una sola vez con los datos del DANE. Permite cambiar los datos de una ciudad existente.

**Entradas:**

- `ciudad` (Ciudad): Contiene los datos que serán usados para la modificación.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Ciudad: Registro con sus datos actualizados.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | RN-16: únicamente el administrador del sistema puede gestionar el catálogo de ciudades. | El solicitante no tiene rol ADMINISTRADOR_SISTEMA. | Abortar la modificación de la ciudad y reportar un mensaje de error al usuario, indicándole que solo el administrador del sistema puede gestionar este catálogo. |
| 6 | Debe existir una ciudad con el identificador enviado. | No existe una ciudad con el identificador enviado. | Abortar la modificación de la ciudad y reportar un mensaje de error al usuario, indicándole que no existe una ciudad con ese identificador. |
| 2 | Debe existir el departamento al que pertenece la ciudad. | No existe un departamento con el identificador enviado. | Abortar la modificación de la ciudad y reportar un mensaje de error al usuario, indicándole que el departamento seleccionado no existe. |
| 7 | No debe existir otra ciudad con el mismo nombre en el mismo departamento (CiudadUnicaPorDepartamento), a excepción de que sea la misma ciudad que se está modificando. | Existe otra ciudad con diferente identificador y con el mismo nombre en el departamento. | Abortar la modificación de la ciudad y reportar un mensaje de error al usuario, indicándole que ya existe una ciudad con ese nombre en el departamento. |
| 8 | No debe existir otra ciudad con el mismo código DANE (CodigoUnico), a excepción de que sea la misma ciudad que se está modificando. | Existe otra ciudad con diferente identificador y con el mismo código DANE. | Abortar la modificación de la ciudad y reportar un mensaje de error al usuario, indicándole que ya existe una ciudad con ese código DANE. |


### eliminar

TRABAJO FUTURO: se modela, pero en el alcance actual el catálogo se carga una sola vez con los datos del DANE. Permite dar de baja de forma definitiva una ciudad existente.

**Entradas:**

- `id` (Alfanumérico): Identificador del registro que se va a eliminar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** No aplica: No retorna información.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | RN-16: únicamente el administrador del sistema puede gestionar el catálogo de ciudades. | El solicitante no tiene rol ADMINISTRADOR_SISTEMA. | Abortar la eliminación de la ciudad y reportar un mensaje de error al usuario, indicándole que solo el administrador del sistema puede gestionar este catálogo. |
| 6 | Debe existir una ciudad con el identificador enviado. | No existe una ciudad con el identificador enviado. | Abortar la eliminación de la ciudad y reportar un mensaje de error al usuario, indicándole que no existe una ciudad con ese identificador. |
| 9 | La ciudad no debe tener escenarios asociados. | Existe al menos un escenario ubicado en la ciudad. | Abortar la eliminación de la ciudad y reportar un mensaje de error al usuario, indicándole que la ciudad tiene escenarios asociados y no se puede eliminar. |
