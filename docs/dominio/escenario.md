# Escenario

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.4. Modelo de dominio/1.1.3.2. Modelo de dominio enriquecido/Modelo_de_dominio_enriquecido_CanchaYa.xlsx, hoja Escenario`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

Objeto de dominio que representa el complejo deportivo (conjunto de canchas) registrado y administrado por un administrador dentro de la plataforma, con su ubicación y datos públicos de contacto.

## Atributos

| Atributo | Tipo de Dato | Longitud mínima | Longitud máxima | Precisión | Rango Inicial | Rango Final | Formato | Valor por defecto | Regla especial | ¿Auto generado? | ¿Calculado? | ¿Obligatorio? | ¿Sensible? | ¿Identifica al registro? | Descripción |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| id | Alfanumérico | 36 | 36 |  |  |  | Formato de un Identificador Único Universal (UUID) |  | Corresponde a una cadena de 36 caracteres: 32 dígitos del 0 al 9 y letras de la "a" a la "f", en grupos de 8-4-4-4-12 separados por guiones (D37) | SI | NO | SI | NO | SI | Identificador único del escenario. |
| nombre | Alfanumérico | 1 | 100 |  |  |  |  |  |  | NO | NO | SI | NO | NO | Nombre comercial del escenario deportivo. |
| descripcion | Alfanumérico | 0 | 500 |  |  |  |  |  |  | NO | NO | NO | NO | NO | Texto libre con el que el administrador presenta el escenario en su perfil público (opcional). |
| direccion | Alfanumérico | 1 | 200 |  |  |  |  |  |  | NO | NO | SI | NO | NO | Dirección física del escenario. |
| ciudad | Ciudad |  |  |  |  |  |  |  |  | NO | NO | SI | NO | NO | Ciudad donde se ubica el escenario; permite filtrar la búsqueda por ciudad y por departamento. |
| latitud | Numérico |  |  | 6 | -90 | 90 |  |  |  | NO | NO | SI | NO | NO | Coordenada geográfica usada en la búsqueda por proximidad (PostGIS). Se toma del punto que el administrador marca en el mapa. |
| longitud | Numérico |  |  | 6 | -180 | 180 |  |  |  | NO | NO | SI | NO | NO | Coordenada geográfica usada en la búsqueda por proximidad (PostGIS). Se toma del punto que el administrador marca en el mapa. |
| telefonoContacto | Alfanumérico |  | 15 |  |  |  | indicativo + número de teléfono |  |  | NO | NO | SI | NO | NO | Teléfono público de contacto del escenario, visible en su ficha. Es distinto del numeroCelular personal del administrador. |
| correoContacto | Alfanumérico | 5 | 150 |  |  |  | escenario@dominio.com |  | Debe contener al menos un símbolo "@" y un "." | NO | NO | NO | NO | NO | Correo público de contacto del escenario (opcional), visible en su ficha. |
| fotos | Alfanumérico |  |  |  |  |  | Lista de URL de imágenes |  | De 0 a 5 fotos, en formato JPG o PNG y de hasta 2 MB cada una; cada elemento es la URL de una imagen del escenario | NO | NO | NO | NO | NO | Fotos del escenario que se muestran en su ficha pública (opcional). |
| administrador | Usuario |  |  |  |  |  |  |  | El usuario referenciado debe tener rol ADMINISTRADOR_CANCHA (RN-05) | NO | NO | SI | NO | NO | Administrador propietario del escenario. |
| estado | Alfanumérico |  |  |  |  |  | Enumeración | INACTIVO | Valores permitidos: ACTIVO \| INACTIVO. Se registra INACTIVO y lo activa el administrador del sistema (RN-16) | NO | NO | SI | NO | NO | Estado de publicación del escenario. |
| calificacionPromedio | Numérico |  |  | 1 | 1 | 5 |  |  |  | NO | SI | NO | NO | NO | Promedio de las calificaciones de las reseñas del escenario. Se calcula; no se almacena como dato independiente. |


### Uso de cada atributo por responsabilidad

| Atributo | registrar | modificar | cambiarEstado | calcularCalificacionPromedio |
|---|---|---|---|---|
| id | Se asigna automáticamente | Requerido/No modificable | Requerido/No modificable | Requerido |
| nombre | Requerido | Requerido | No requerido | No requerido |
| descripcion | Opcional | Opcional | No requerido | No requerido |
| direccion | Requerido | Requerido | No requerido | No requerido |
| ciudad | Requerido | Requerido | No requerido | No requerido |
| latitud | Requerido | Requerido | No requerido | No requerido |
| longitud | Requerido | Requerido | No requerido | No requerido |
| telefonoContacto | Requerido | Requerido | No requerido | No requerido |
| correoContacto | Opcional | Opcional | No requerido | No requerido |
| fotos | Opcional | Opcional | No requerido | No requerido |
| administrador | Se asigna automáticamente | No modificable | No modificable | No requerido |
| estado | Se asigna automáticamente | No modificable | Requerido/Modificable | No requerido |
| calificacionPromedio | Se calcula | No modificable | No requerido | Se calcula |


## Responsabilidades


### registrar

Permite dar de alta un nuevo escenario deportivo asociado al administrador que lo registra.

**Entradas:**

- `escenario` (Escenario): Contiene los datos del nuevo escenario que se va a registrar; el atributo administrador corresponde al usuario autenticado.

**Salida:** Escenario: Escenario registrado, con su identificador asignado y en estado INACTIVO.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | RN-05: solo un usuario con rol ADMINISTRADOR_CANCHA puede registrar escenarios; queda asociado a él como propietario. | El usuario que registra el escenario no tiene rol ADMINISTRADOR_CANCHA. | Abortar el registro del nuevo escenario y reportar un mensaje de error al usuario, indicándole que solo un administrador de cancha puede registrar escenarios. |
| 2 | La ciudad indicada debe existir en el catálogo de ciudades. | No existe una ciudad con el identificador enviado. | Abortar el registro del nuevo escenario y reportar un mensaje de error al usuario, indicándole que la ciudad seleccionada no existe. |
| 3 | Las coordenadas deben ser válidas: latitud entre -90 y 90 y longitud entre -180 y 180. | La latitud o la longitud están por fuera del rango permitido. | Abortar el registro del nuevo escenario y reportar un mensaje de error al usuario, indicándole que la ubicación del escenario no es válida. |
| 4 | No debe existir otro escenario con el mismo identificador (UUID). | Existe un escenario con el mismo identificador. | Se debe reintentar con un nuevo identificador generado de forma automática, hasta que no exista, para continuar con el proceso de registro. |
| 5 | RN-16: el escenario se registra en estado INACTIVO y no se presenta a los jugadores hasta que el administrador del sistema lo activa. | No aplica | No aplica |
| 13 | Se permiten máximo 5 fotos por escenario, en formato JPG o PNG y de hasta 2 MB cada una. | Se envían más de 5 fotos, o alguna no es JPG ni PNG o supera 2 MB. | Abortar el registro del nuevo escenario y reportar un mensaje de error al usuario, indicándole la regla de las fotos que se incumple. |


### modificar

Permite cambiar los datos de un escenario ya registrado: nombre, descripción, dirección, ciudad, ubicación, contacto y fotos.

**Entradas:**

- `escenario` (Escenario): Contiene los datos que serán usados para modificar el escenario.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Escenario: Escenario con sus datos actualizados.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 6 | Debe existir un escenario con el identificador enviado. | No existe un escenario con el identificador enviado. | Abortar la modificación del escenario y reportar un mensaje de error al usuario, indicándole que no existe un escenario con ese identificador. |
| 7 | RN-05 / RN-15: únicamente el administrador propietario del escenario puede modificar su información; el encargado de sede no puede. | El solicitante no es el administrador propietario del escenario. | Abortar la modificación del escenario y reportar un mensaje de error al usuario, indicándole que no tiene permiso para modificar este escenario. |
| 2 | La ciudad indicada debe existir en el catálogo de ciudades. | No existe una ciudad con el identificador enviado. | Abortar la modificación del escenario y reportar un mensaje de error al usuario, indicándole que la ciudad seleccionada no existe. |
| 3 | Las coordenadas deben ser válidas: latitud entre -90 y 90 y longitud entre -180 y 180. | La latitud o la longitud están por fuera del rango permitido. | Abortar la modificación del escenario y reportar un mensaje de error al usuario, indicándole que la ubicación del escenario no es válida. |
| 13 | Se permiten máximo 5 fotos por escenario, en formato JPG o PNG y de hasta 2 MB cada una. | Se envían más de 5 fotos, o alguna no es JPG ni PNG o supera 2 MB. | Abortar la modificación del escenario y reportar un mensaje de error al usuario, indicándole la regla de las fotos que se incumple. |


### cambiarEstado

Permite al administrador del sistema activar un escenario recién registrado, para publicarlo en los resultados de búsqueda, o inactivarlo para retirarlo de ellos.

**Entradas:**

- `escenario` (Escenario): Contiene el identificador del escenario y el nuevo estado que se le va a aplicar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Escenario: Escenario con su nuevo estado.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 6 | Debe existir un escenario con el identificador enviado. | No existe un escenario con el identificador enviado. | Abortar el cambio de estado del escenario y reportar un mensaje de error al usuario, indicándole que no existe un escenario con ese identificador. |
| 8 | RN-16: únicamente el administrador del sistema puede activar o inactivar un escenario. | El solicitante no tiene rol ADMINISTRADOR_SISTEMA. | Abortar el cambio de estado del escenario y reportar un mensaje de error al usuario, indicándole que solo el administrador del sistema puede activar o inactivar escenarios. |
| 9 | El nuevo estado debe ser diferente al que tiene actualmente el escenario. | El estado enviado es igual al que el escenario posee actualmente. | Abortar el cambio de estado del escenario y reportar un mensaje de error al usuario, indicándole que el escenario ya se encuentra en ese estado. |
| 10 | RN-04: solo los escenarios en estado ACTIVO se presentan a los jugadores en la búsqueda. | No aplica | No aplica |
| 14 | RN-17: no se puede inactivar un escenario que tenga reservas confirmadas futuras en alguna de sus canchas. | Se intenta inactivar el escenario y tiene reservas confirmadas futuras. | Abortar el cambio de estado del escenario y reportar un mensaje de error al usuario, indicándole que hay reservas confirmadas futuras que impiden inactivar el escenario. |
| 15 | RN-06: al activar o inactivar el escenario se notifica por correo al administrador propietario. Si el envío del correo falla, el cambio de estado no se revierte: se registra el error y no se reintenta el envío (Release 1). | No aplica | No aplica |


### calcularCalificacionPromedio

Permite calcular el promedio de las calificaciones de las reseñas de un escenario.

**Entradas:**

- `id` (Alfanumérico): Identificador del escenario cuya calificación promedio se va a calcular.

**Salida:** Numérico: Promedio de las calificaciones del escenario, con un decimal, entre 1 y 5; vacío si aún no tiene reseñas.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 6 | Debe existir un escenario con el identificador enviado. | No existe un escenario con el identificador enviado. | Abortar el cálculo de la calificación promedio y reportar un mensaje de error al usuario, indicándole que no existe un escenario con ese identificador. |
| 11 | RN-14: solo cuentan las reseñas válidas (una por reserva finalizada), con calificación de 1 a 5. | No aplica | No aplica |
| 12 | Si el escenario aún no tiene reseñas, no se presenta calificación promedio. | No aplica | No aplica |
