# HU3-Actualizar mi perfil

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.7. Tallaje producto/Tallaje producto - CanchaYa.xlsx, hoja HU3`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

**Release:** Release 1 · Núcleo · **Prioridad:** 21 · **Talla:** S · **Puntos:** 2

## Tarjeta

- **Como:** Usuario registrado
- **Necesito:** modificar mi nombre, mi número de celular, mi correo electrónico y mi contraseña
- **Para:** mantener al día mis datos de contacto y la seguridad de mi cuenta

> Como Usuario registrado, necesito modificar mi nombre, mi número de celular, mi correo electrónico y mi contraseña, para mantener al día mis datos de contacto y la seguridad de mi cuenta


## Conversación historia

- **Requisito de información:** Modelo de dominio enriquecido, hoja Usuario: atributos y responsabilidad actualizarPerfil (políticas 5, 8, 9, 10, 11 y 12).
- **Prototipo:** Prototipo «Mi perfil»: Prototipos/Celular y Prototipos/Computador, archivo «03 Mi perfil.png» (1.1.9.5 Artefactos apoyo).
- **Catálogo de mensajes:** Catálogo de mensajes - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo), hoja «Mensajes». Códigos usados en esta HU: EXI-USU-02, ERR-GEN-01, ERR-GEN-02, ERR-GEN-03, ERR-GEN-04, ERR-USU-09, ERR-USU-10, ERR-USU-11, ERR-USU-12.
- **Catálogo de parámetros:** Catálogo de parámetros - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo). Parámetros usados en esta HU: PAR-USU-01 (longitud mínima de la contraseña), PAR-USU-02 (composición de la contraseña).
- **Restricciones de diseño:** La nueva contraseña se guarda solo como hash bcrypt; el usuario se toma del token de acceso, no de un dato que envíe el cliente.
- Se pueden modificar nombre, número de celular, correo electrónico y contraseña. El tipo y número de documento y el rol se muestran como solo lectura.
- Si el campo de nueva contraseña se deja vacío, se conserva la contraseña actual.
- Al validar si otro usuario ya tiene el correo electrónico se compara el contenido sin distinguir mayúsculas de minúsculas, excluyendo al propio usuario.

## Conversación aspectos generales y transversales

- **Aspectos generales:** Aspectos generales - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo): AG-01 a AG-23 aplican a todas las HU (validación y formularios, mensajes, seguridad, datos y formatos, interfaz e integraciones).

## Criterios de aceptación

| Código | Escenario |
|---|---|
| CA1 | Dado que un usuario registrado quiere actualizar su perfil, cuando modifique uno o varios de sus datos de forma correcta y ejecute la acción definida para guardar, entonces se persisten los cambios, se muestra el formulario con los datos actualizados y se muestra el mensaje de éxito (EXI-USU-02), indicando que el perfil se actualizó de forma satisfactoria. |
| CA2 | Dado que un usuario registrado quiere actualizar su perfil, cuando ingrese alguno de los datos de forma incorrecta, incumpliendo alguna de las reglas especificadas en el requisito de información, y ejecute la acción definida para guardar, entonces no se persiste la información y se muestran los mensajes de error respectivos al usuario (Problemas de obligatoriedad: ERR-GEN-01, Problemas de formato: ERR-GEN-02, Problemas de longitud: ERR-GEN-03, Problemas de rango: ERR-GEN-04), indicando cuál fue el problema presentado y cómo puede remediarlo. |
| CA3 | Dado que un usuario registrado quiere actualizar su perfil, cuando cambie su correo electrónico por uno que ya pertenece a otro usuario y ejecute la acción definida para guardar, entonces no se persisten los cambios y se muestra el mensaje de error (ERR-USU-10), indicando que ya existe otro usuario con ese correo electrónico. |
| CA4 | Dado que un usuario registrado quiere actualizar su perfil, cuando guarde los cambios sin haber escrito una nueva contraseña, entonces se persisten los demás datos y se conserva su contraseña actual. |
| CA5 | Dado que se recibe una solicitud para actualizar un perfil, cuando incluya un tipo de documento, un número de documento o un rol distinto al registrado, entonces no se persisten los cambios y se muestra el mensaje de error (ERR-USU-11), indicando que el documento y el rol no se pueden modificar. |
| CA6 | Dado que se recibe una solicitud para actualizar un perfil, cuando el usuario autenticado sea distinto al usuario que se quiere modificar, entonces no se persisten los cambios y se muestra el mensaje de error (ERR-USU-09), indicando que no tiene permiso para modificar el perfil de otro usuario. |
| CA7 | Dado que un usuario registrado quiere actualizar su perfil, cuando haya ingresado información en un campo determinado del formulario de forma incorrecta, incumpliendo alguna de las reglas especificadas en el requisito de información, y pierda el foco de este, entonces se aplica la validación respectiva y se muestra junto al campo el mensaje de error correspondiente (Problemas de obligatoriedad: ERR-GEN-01, Problemas de formato: ERR-GEN-02, Problemas de longitud: ERR-GEN-03, Problemas de rango: ERR-GEN-04), indicando cuál fue el problema presentado y cómo puede remediarlo. |
| CA8 | Dado que un usuario registrado quiere cambiar su contraseña, cuando escriba una nueva contraseña de menos de 8 caracteres o que no tenga al menos una letra y un número, y ejecute la acción definida para guardar, entonces no se persisten los cambios y se muestra el mensaje de error (ERR-USU-12), indicando que la contraseña debe tener mínimo 8 caracteres, con al menos una letra y un número. |
