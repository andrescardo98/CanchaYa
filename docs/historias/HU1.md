# HU1-Registrarse en la plataforma

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.7. Tallaje producto/Tallaje producto - CanchaYa.xlsx, hoja HU1`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

**Release:** Release 1 · Núcleo · **Prioridad:** 1 · **Talla:** M · **Puntos:** 3

## Tarjeta

- **Como:** Jugador o administrador de cancha
- **Necesito:** crear mi cuenta eligiendo mi rol (jugador o administrador de cancha)
- **Para:** acceder a las funciones que corresponden a mi rol

> Como Jugador o administrador de cancha, necesito crear mi cuenta eligiendo mi rol (jugador o administrador de cancha), para acceder a las funciones que corresponden a mi rol


## Conversación historia

- **Requisito de información:** Modelo de dominio enriquecido, hoja Usuario: atributos y responsabilidad registrarse (políticas 1, 2, 3, 5 y 12 a 15).
- **Prototipo:** Prototipo «Registro de usuario»: Prototipos/Celular y Prototipos/Computador, archivo «01 Registro de usuario.png» (1.1.9.5 Artefactos apoyo).
- **Catálogo de mensajes:** Catálogo de mensajes - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo), hoja «Mensajes». Códigos usados en esta HU: EXI-USU-01, ERR-GEN-01, ERR-GEN-02, ERR-GEN-03, ERR-GEN-04, ERR-USU-01, ERR-USU-02, ERR-USU-03, ERR-USU-12, ERR-USU-13, ERR-USU-14, ERR-USU-15.
- **Catálogo de parámetros:** Catálogo de parámetros - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo). Parámetros usados en esta HU: PAR-USU-01 (longitud mínima de la contraseña), PAR-USU-02 (composición de la contraseña), PAR-USU-03 (tipos de documento admitidos), PAR-USU-04 (formato de la cédula de ciudadanía (CC)), PAR-USU-05 (formato de la cédula de extranjería (CE)), PAR-USU-06 (formato de la tarjeta de identidad (TI)), PAR-USU-07 (formato del pasaporte), PAR-USU-08 (formato del NIT).
- **Restricciones de diseño:** Contraseña almacenada solo como hash bcrypt; identificador UUID generado en el servidor; validación en cliente y en servidor.
- Deben tenerse en cuenta los campos descritos en el requisito de información que no deben ser visibles por el usuario: id, contrasenaHash y fechaRegistro.
- El rol se elige entre Jugador y Administrador de cancha. El rol Administrador del sistema no aparece como opción: ese usuario se crea con la carga inicial de datos (RN-16). La tarjeta de identidad (TI) solo se admite con el rol Jugador; quien se registra con TI declara que cuenta con la autorización de su padre, madre o acudiente (Ley 1581 de 2012).
- Al validar si ya existe un usuario con el mismo correo electrónico se compara el contenido sin distinguir mayúsculas de minúsculas. La contraseña se ingresa en un campo oculto y nunca se muestra ni se guarda en texto plano. No se verifica el correo electrónico: la cuenta queda activa al registrarse. La recuperación de contraseña está fuera del alcance.

## Conversación aspectos generales y transversales

- **Aspectos generales:** Aspectos generales - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo): AG-01 a AG-23 aplican a todas las HU (validación y formularios, mensajes, seguridad, datos y formatos, interfaz e integraciones).

## Criterios de aceptación

| Código | Escenario |
|---|---|
| CA1 | Dado que un jugador o un administrador de cancha quiere crear su cuenta, cuando ingrese todos los datos de forma correcta (nombre, correo electrónico, contraseña, tipo y número de documento, número de celular y rol), ejecute la acción definida para registrarse y no exista otro usuario con el mismo correo ni con el mismo documento, entonces se persiste el nuevo usuario con el rol seleccionado, su fecha de registro y el hash de su contraseña, se limpia el formulario, se muestra el mensaje de éxito (EXI-USU-01) indicando que la cuenta se creó de forma satisfactoria y se lleva al usuario a la pantalla de inicio de sesión. |
| CA2 | Dado que un jugador o un administrador de cancha quiere crear su cuenta, cuando ingrese alguno de los datos de forma incorrecta, incumpliendo alguna de las reglas especificadas en el requisito de información, y ejecute la acción definida para registrarse, entonces no se persiste la información y se muestran los mensajes de error respectivos al usuario (Problemas de obligatoriedad: ERR-GEN-01, Problemas de formato: ERR-GEN-02, Problemas de longitud: ERR-GEN-03, Problemas de rango: ERR-GEN-04), indicando cuál fue el problema presentado y cómo puede remediarlo. |
| CA3 | Dado que un jugador o un administrador de cancha quiere crear su cuenta, cuando ingrese todos los datos de forma correcta, ejecute la acción definida para registrarse y ya exista un usuario con el mismo correo electrónico, entonces no se persiste la información, se conservan los datos ingresados y se muestra el mensaje de error (ERR-USU-01), indicando que ya existe un usuario registrado con ese correo electrónico. |
| CA4 | Dado que un jugador o un administrador de cancha quiere crear su cuenta, cuando ingrese todos los datos de forma correcta, ejecute la acción definida para registrarse y ya exista un usuario con el mismo tipo y número de documento, entonces no se persiste la información, se conservan los datos ingresados y se muestra el mensaje de error (ERR-USU-02), indicando que ya existe un usuario registrado con ese documento. |
| CA5 | Dado que se recibe una solicitud de registro, cuando el rol enviado no sea Jugador ni Administrador de cancha (por ejemplo, Administrador del sistema), entonces no se persiste la información y se muestra el mensaje de error (ERR-USU-03), indicando que el rol seleccionado no es válido. |
| CA6 | Dado que un jugador o un administrador de cancha quiere crear su cuenta, cuando haya ingresado información en un campo determinado del formulario de forma correcta y pierda el foco de este, entonces se aplican las validaciones necesarias para asegurar la integridad del dato según las reglas especificadas en el requisito de información, y no se muestra ningún mensaje dado que la validación es exitosa. |
| CA7 | Dado que un jugador o un administrador de cancha quiere crear su cuenta, cuando haya ingresado información en un campo determinado del formulario de forma incorrecta, incumpliendo alguna de las reglas especificadas en el requisito de información, y pierda el foco de este, entonces se aplica la validación respectiva y se muestra junto al campo el mensaje de error correspondiente (Problemas de obligatoriedad: ERR-GEN-01, Problemas de formato: ERR-GEN-02, Problemas de longitud: ERR-GEN-03, Problemas de rango: ERR-GEN-04), indicando cuál fue el problema presentado y cómo puede remediarlo. |
| CA8 | Dado que un jugador o un administrador de cancha quiere crear su cuenta, cuando haya ingresado el correo electrónico, pierda el foco del campo y ya exista un usuario con ese correo, entonces se muestra junto al campo el mensaje de error (ERR-USU-01), indicando que ya existe un usuario registrado con ese correo electrónico. |
| CA9 | Dado que un jugador o un administrador de cancha quiere crear su cuenta, cuando ingrese una contraseña de menos de 8 caracteres o que no tenga al menos una letra y un número, y ejecute la acción definida para registrarse, entonces no se persiste la información y se muestra el mensaje de error (ERR-USU-12), indicando que la contraseña debe tener mínimo 8 caracteres, con al menos una letra y un número. |
| CA10 | Dado que un jugador o un administrador de cancha quiere crear su cuenta, cuando el tipo de documento no sea CC, CE, TI, PASAPORTE ni NIT, o el número no cumpla el formato del tipo elegido (CC de 6 a 10 dígitos, CE de 6 a 10 caracteres alfanuméricos, TI de 10 u 11 dígitos, PASAPORTE de 6 a 12 caracteres alfanuméricos, NIT de 9 dígitos con guion y dígito de verificación), y ejecute la acción definida para registrarse, entonces no se persiste la información y se muestra el mensaje de error (ERR-USU-13), indicando que el documento no tiene un formato válido. |
| CA11 | Dado que un usuario acaba de registrarse, cuando el registro quede persistido, entonces puede iniciar sesión de inmediato, sin verificar su correo electrónico. |
| CA12 | Dado que un administrador de cancha quiere crear su cuenta, cuando elija como tipo de documento la tarjeta de identidad (TI) y ejecute la acción definida para registrarse, entonces no se persiste la información y se muestra el mensaje de error (ERR-USU-14), indicando que la tarjeta de identidad solo se admite para cuentas de jugador. |
| CA13 | Dado que un jugador menor de edad quiere crear su cuenta con tarjeta de identidad (TI), cuando ejecute la acción definida para registrarse sin aceptar la declaración de que cuenta con la autorización de su padre, madre o acudiente, entonces no se persiste la información y se muestra el mensaje de error (ERR-USU-15), indicando que debe declarar que cuenta con esa autorización. |
