# Usuario

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.4. Modelo de dominio/1.1.3.2. Modelo de dominio enriquecido/Modelo_de_dominio_enriquecido_CanchaYa.xlsx, hoja Usuario`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

Objeto de dominio que representa a las personas registradas en la plataforma: jugadores que reservan canchas, administradores de cancha que gestionan escenarios deportivos (el propietario y los encargados de sede, que son actores de negocio distintos con el mismo rol técnico ADMINISTRADOR_CANCHA, limitado por EncargadoEscenario) y el administrador del sistema, que activa los escenarios y gestiona los catálogos.

## Atributos

| Atributo | Tipo de Dato | Longitud mínima | Longitud máxima | Formato | Regla especial | ¿Auto generado? | ¿Calculado? | ¿Obligatorio? | ¿Sensible? | ¿Identifica al registro? | Descripción |
|---|---|---|---|---|---|---|---|---|---|---|---|
| id | Alfanumérico | 36 | 36 | Formato de un Identificador Único Universal (UUID) | Corresponde a una cadena de 36 caracteres: 32 dígitos del 0 al 9 y letras de la "a" a la "f", en grupos de 8-4-4-4-12 separados por guiones (D37) | SI | NO | SI | NO | SI | Identificador único del usuario. |
| nombre | Alfanumérico | 1 | 100 | [A-Z] [a-z] espacios |  | NO | NO | SI | NO | NO | Nombre completo del usuario. |
| correoElectronico | Alfanumérico | 5 | 150 | usuario@dominio.com | Debe contener al menos un símbolo "@" y un "." | NO | NO | SI | NO | NO | Correo usado para autenticación y notificaciones. |
| contrasenaHash | Alfanumérico | 60 | 60 | Hash bcrypt |  | NO | NO | SI | SI | NO | Hash de la contraseña; nunca se almacena en texto plano. |
| tipoDocumento | Alfanumérico |  |  | Enumeración | Valores permitidos: CC \| CE \| TI \| PASAPORTE \| NIT | NO | NO | SI | NO | NO | Tipo de documento de identificación del usuario. |
| numeroDocumento | Alfanumérico | 5 | 20 |  | CC: solo dígitos, entre 6 y 10. CE: entre 6 y 10 caracteres alfanuméricos. TI: solo dígitos, 10 u 11. PASAPORTE: entre 6 y 12 caracteres alfanuméricos. NIT: 9 dígitos, guion y dígito de verificación (por ejemplo, 900123456-7). TI solo se admite con rol JUGADOR | NO | NO | SI | SI | NO | Número de documento del usuario. Se usa para políticas de no-show/sanciones y, a futuro, procesamiento de pagos (KYC). |
| numeroCelular | Alfanumérico |  | 15 | indicativo + número de celular |  | NO | NO | SI | SI | NO | Número de celular de contacto del usuario, usado para notificaciones y contacto directo (WhatsApp/llamadas). |
| rol | Alfanumérico |  |  | Enumeración | Valores permitidos: JUGADOR \| ADMINISTRADOR_CANCHA \| ADMINISTRADOR_SISTEMA. El encargado de un escenario también tiene rol ADMINISTRADOR_CANCHA; sus permisos se limitan por EncargadoEscenario (RN-15). ADMINISTRADOR_SISTEMA no se puede elegir en el registro: su usuario se crea con la carga inicial de datos (RN-16) | NO | NO | SI | NO | NO | Rol que determina los permisos del usuario dentro de la plataforma. |
| fechaRegistro | Fecha |  |  | dd/mm/aaaa hh:mm:ss |  | SI | NO | SI | NO | NO | Fecha y hora en que el usuario se registró. |


### Uso de cada atributo por responsabilidad

| Atributo | registrarse | autenticar | actualizarPerfil |
|---|---|---|---|
| id | Se asigna automáticamente | No requerido | Requerido/No modificable |
| nombre | Requerido | No requerido | Requerido |
| correoElectronico | Requerido | Requerido | Requerido |
| contrasenaHash | Se calcula | Se consulta | Opcional |
| tipoDocumento | Requerido | No requerido | No modificable |
| numeroDocumento | Requerido | No requerido | No modificable |
| numeroCelular | Requerido | No requerido | Requerido |
| rol | Requerido | No requerido | No modificable |
| fechaRegistro | Se asigna automáticamente | No requerido | No modificable |


## Combinaciones únicas

| Nombre combinación | Descripción | Atributos |
|---|---|---|
| CorreoElectronicoUnico | No es posible tener más de un usuario con el mismo correo electrónico. | correoElectronico |
| DocumentoUnico | No es posible tener más de un usuario con el mismo tipo y número de documento. | tipoDocumento, numeroDocumento |


## Responsabilidades


### registrarse

Permite crear una nueva cuenta de usuario con el rol seleccionado (jugador o administrador de cancha). El administrador del sistema no se registra por esta vía.

**Entradas:**

- `usuario` (Usuario): Contiene los datos del nuevo usuario que se va a registrar, junto con la contraseña, que solo se usa para calcular su hash.

**Salida:** Usuario: Usuario registrado, con su identificador y su fecha de registro asignados. No incluye la contraseña ni su hash.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | No debe existir otro usuario con el mismo correo electrónico (CorreoElectronicoUnico). | Existe un usuario con el mismo correo electrónico. | Abortar el registro del nuevo usuario y reportar un mensaje de error al usuario, indicándole que ya existe un usuario con el mismo correo electrónico. |
| 2 | No debe existir otro usuario con el mismo tipo y número de documento (DocumentoUnico). | Existe un usuario con el mismo tipo y número de documento. | Abortar el registro del nuevo usuario y reportar un mensaje de error al usuario, indicándole que ya existe un usuario con el mismo tipo y número de documento. |
| 3 | El rol seleccionado debe ser JUGADOR o ADMINISTRADOR_CANCHA. El rol ADMINISTRADOR_SISTEMA no se puede elegir en el registro: su usuario se crea con la carga inicial de datos (RN-16). | El rol enviado no es JUGADOR ni ADMINISTRADOR_CANCHA. | Abortar el registro del nuevo usuario y reportar un mensaje de error al usuario, indicándole que el rol seleccionado no es válido. |
| 4 | No debe existir otro usuario con el mismo identificador (UUID). | Existe un usuario con el mismo identificador. | Se debe reintentar con un nuevo identificador generado de forma automática, hasta que no exista, para continuar con el proceso de registro. |
| 5 | La contraseña nunca se almacena en texto plano: se guarda únicamente su hash (bcrypt). | No aplica | No aplica |
| 12 | La contraseña debe tener mínimo 8 caracteres e incluir al menos una letra y un número. | La contraseña no cumple la longitud o la composición exigidas. | Abortar el registro del nuevo usuario y reportar un mensaje de error al usuario, indicándole que la contraseña debe tener mínimo 8 caracteres, con al menos una letra y un número. |
| 13 | El tipo de documento debe ser CC, CE, TI, PASAPORTE o NIT y el número debe cumplir el formato del tipo: CC de 6 a 10 dígitos; CE de 6 a 10 caracteres alfanuméricos; TI de 10 u 11 dígitos; PASAPORTE de 6 a 12 caracteres alfanuméricos; NIT de 9 dígitos, guion y dígito de verificación. | El tipo de documento no es CC, CE, TI, PASAPORTE ni NIT, o el número no cumple el formato del tipo. | Abortar el registro del nuevo usuario y reportar un mensaje de error al usuario, indicándole que el documento no tiene un formato válido. |
| 14 | La tarjeta de identidad (TI) solo se admite con rol JUGADOR. | Se envía el tipo de documento TI con un rol distinto de JUGADOR. | Abortar el registro del nuevo usuario y reportar un mensaje de error al usuario, indicándole que la tarjeta de identidad solo se admite para cuentas de jugador. |
| 15 | Si el tipo de documento es TI (menor de edad), el usuario debe declarar que cuenta con la autorización de su padre, madre o acudiente para el tratamiento de sus datos (Ley 1581 de 2012). | Se registra un usuario con TI sin aceptar la declaración de autorización del acudiente. | Abortar el registro del nuevo usuario y reportar un mensaje de error al usuario, indicándole que debe declarar que cuenta con la autorización de su acudiente. |


### autenticar

Permite validar las credenciales de un usuario y generar su token de acceso (JWT).

**Entradas:**

- `usuario` (Usuario): Contiene el correo electrónico y la contraseña con los que se inicia sesión.

**Salida:** Alfanumérico: Token de acceso (JWT) que identifica la sesión del usuario y su rol.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 6 | Debe existir un usuario registrado con el correo electrónico ingresado. | No existe un usuario con el correo electrónico ingresado. | Abortar el inicio de sesión y reportar un mensaje de error al usuario, indicándole que el correo o la contraseña son incorrectos (no se revela cuál de los dos, por seguridad). |
| 7 | La contraseña ingresada debe coincidir con el hash almacenado. | La contraseña ingresada no coincide con el hash almacenado. | Abortar el inicio de sesión y reportar un mensaje de error al usuario, indicándole que el correo o la contraseña son incorrectos (no se revela cuál de los dos, por seguridad). |


### actualizarPerfil

Permite a un usuario modificar sus datos personales: nombre, número de celular, correo electrónico y contraseña.

**Entradas:**

- `usuario` (Usuario): Contiene los datos que serán usados para actualizar el perfil; la contraseña solo se envía si se va a cambiar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Usuario: Usuario con sus datos actualizados. No incluye la contraseña ni su hash.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 8 | Debe existir un usuario con el identificador enviado. | No existe un usuario con el identificador enviado. | Abortar la actualización del perfil y reportar un mensaje de error al usuario, indicándole que no existe un usuario con ese identificador. |
| 9 | Un usuario solo puede modificar su propio perfil. | El usuario autenticado es diferente al usuario que se quiere modificar. | Abortar la actualización del perfil y reportar un mensaje de error al usuario, indicándole que no tiene permiso para modificar el perfil de otro usuario. |
| 10 | No debe existir otro usuario con el mismo correo electrónico, a excepción de que sea el mismo usuario que se está modificando. | Existe otro usuario con diferente identificador y con el mismo correo electrónico que se quiere asignar. | Abortar la actualización del perfil y reportar un mensaje de error al usuario, indicándole que ya existe otro usuario con el correo electrónico que se desea asignar. |
| 11 | El tipo de documento, el número de documento y el rol no se pueden modificar desde el perfil. | Se envía un tipo de documento, un número de documento o un rol diferente al registrado. | Abortar la actualización del perfil y reportar un mensaje de error al usuario, indicándole que el documento y el rol no se pueden modificar. |
| 5 | La contraseña nunca se almacena en texto plano: se guarda únicamente su hash (bcrypt). | No aplica | No aplica |
| 12 | La contraseña debe tener mínimo 8 caracteres e incluir al menos una letra y un número. Aplica solo si se envía una nueva contraseña. | La contraseña no cumple la longitud o la composición exigidas. | Abortar la actualización del perfil y reportar un mensaje de error al usuario, indicándole que la contraseña debe tener mínimo 8 caracteres, con al menos una letra y un número. |
