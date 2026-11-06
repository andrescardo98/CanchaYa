# HU2-Iniciar sesión

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.7. Tallaje producto/Tallaje producto - CanchaYa.xlsx, hoja HU2`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

**Release:** Release 1 · Núcleo · **Prioridad:** 2 · **Talla:** M · **Puntos:** 3

## Tarjeta

- **Como:** Usuario registrado
- **Necesito:** iniciar sesión con mi correo electrónico y mi contraseña
- **Para:** acceder de forma segura a mi cuenta y a las funciones de mi rol

> Como Usuario registrado, necesito iniciar sesión con mi correo electrónico y mi contraseña, para acceder de forma segura a mi cuenta y a las funciones de mi rol


## Conversación historia

- **Requisito de información:** Modelo de dominio enriquecido, hoja Usuario: atributos y responsabilidad autenticar (políticas 6 y 7).
- **Prototipo:** Prototipo «Inicio de sesión»: Prototipos/Celular y Prototipos/Computador, archivo «02 Inicio de sesión.png» (1.1.9.5 Artefactos apoyo).
- **Catálogo de mensajes:** Catálogo de mensajes - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo), hoja «Mensajes». Códigos usados en esta HU: ERR-GEN-01, ERR-GEN-02, ERR-GEN-05, ERR-USU-06, ERR-USU-07, ERR-USU-16.
- **Catálogo de parámetros:** Catálogo de parámetros - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo). Parámetros usados en esta HU: PAR-USU-09 (vigencia del token de acceso), PAR-USU-10, PAR-USU-11 y PAR-USU-12 (límite de intentos fallidos).
- **Restricciones de diseño:** Autenticación con JWT; el token incluye el identificador y el rol del usuario. La contraseña se compara contra el hash bcrypt.
- Por seguridad, cuando el correo no existe o la contraseña no coincide se muestra el mismo mensaje, sin revelar cuál de los dos datos es el incorrecto.
- Después de iniciar sesión, cada rol llega a su página de inicio: el jugador a la búsqueda de canchas, el administrador de cancha a sus escenarios y el administrador del sistema a la administración de la plataforma.
- La contraseña se ingresa en un campo oculto.

## Conversación aspectos generales y transversales

- **Aspectos generales:** Aspectos generales - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo): AG-01 a AG-23 aplican a todas las HU (validación y formularios, mensajes, seguridad, datos y formatos, interfaz e integraciones).

## Criterios de aceptación

| Código | Escenario |
|---|---|
| CA1 | Dado que un usuario registrado quiere iniciar sesión, cuando ingrese su correo electrónico y su contraseña de forma correcta y ejecute la acción definida para iniciar sesión, entonces se genera su token de acceso, se guarda en el dispositivo y se lleva al usuario a la página de inicio que corresponde a su rol. |
| CA2 | Dado que un usuario quiere iniciar sesión, cuando deje vacío el correo electrónico o la contraseña, o ingrese un correo sin el formato válido, y ejecute la acción definida para iniciar sesión, entonces no se intenta la autenticación y se muestran los mensajes de error respectivos (Problemas de obligatoriedad: ERR-GEN-01, Problemas de formato: ERR-GEN-02). |
| CA3 | Dado que un usuario quiere iniciar sesión, cuando ingrese un correo electrónico que no está registrado y ejecute la acción definida para iniciar sesión, entonces no se genera el token de acceso y se muestra el mensaje de error (ERR-USU-06), indicando que el correo o la contraseña son incorrectos. |
| CA4 | Dado que un usuario registrado quiere iniciar sesión, cuando ingrese su correo electrónico con una contraseña que no coincide y ejecute la acción definida para iniciar sesión, entonces no se genera el token de acceso y se muestra el mensaje de error (ERR-USU-07), que tiene el mismo texto de ERR-USU-06 para no revelar si el correo está registrado, indicando que el correo o la contraseña son incorrectos. |
| CA5 | Dado que una persona sin sesión iniciada intenta entrar a una función que exige autenticación, cuando se cargue la página solicitada, entonces se le lleva a la pantalla de inicio de sesión y se muestra el mensaje (ERR-GEN-05), indicando que debe iniciar sesión para continuar. |
| CA6 | Dado que un usuario tiene una sesión cuyo token de acceso ya venció, cuando ejecute cualquier acción que exija autenticación, entonces la acción no se ejecuta, se elimina el token del dispositivo, se le lleva a la pantalla de inicio de sesión y se muestra el mensaje (ERR-GEN-05). |
| CA7 | Dado que se registraron 5 intentos fallidos de inicio de sesión en 5 minutos para una misma cuenta o desde un mismo origen (PAR-USU-10, PAR-USU-11), cuando se intente iniciar sesión otra vez durante los 15 minutos siguientes (PAR-USU-12), aunque la contraseña sea correcta, entonces no se genera el token de acceso y se muestra el mensaje de error (ERR-USU-16), con el mismo texto exista o no el correo. |
