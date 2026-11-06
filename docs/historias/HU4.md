# HU4-Cerrar sesión

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.7. Tallaje producto/Tallaje producto - CanchaYa.xlsx, hoja HU4`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

**Release:** Release 1 · Núcleo · **Prioridad:** 22 · **Talla:** S · **Puntos:** 2

## Tarjeta

- **Como:** Usuario registrado
- **Necesito:** cerrar mi sesión en la plataforma
- **Para:** evitar que otra persona use mi cuenta desde el mismo dispositivo

> Como Usuario registrado, necesito cerrar mi sesión en la plataforma, para evitar que otra persona use mi cuenta desde el mismo dispositivo


## Conversación historia

- **Requisito de información:** Modelo de dominio enriquecido, hoja Usuario: atributos y responsabilidad autenticar (token de acceso).
- **Prototipo:** Prototipo «Menú de usuario»: Prototipos/Celular y Prototipos/Computador, archivo «04 Menú de usuario.png» (1.1.9.5 Artefactos apoyo).
- **Catálogo de mensajes:** Catálogo de mensajes - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo), hoja «Mensajes». Códigos usados en esta HU: INF-USU-01, ERR-GEN-05.
- **Catálogo de parámetros:** Catálogo de parámetros - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo). Parámetros usados en esta HU: PAR-USU-09 (vigencia del token de acceso).
- **Restricciones de diseño:** Con JWT la sesión no se guarda en el servidor: cerrar sesión elimina el token del dispositivo y el token vence según su tiempo de vigencia.
- La opción de cerrar sesión está disponible para todos los roles desde el menú de usuario.
- Cerrar sesión solo afecta al dispositivo desde el que se ejecuta.

## Conversación aspectos generales y transversales

- **Aspectos generales:** Aspectos generales - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo): AG-01 a AG-23 aplican a todas las HU (validación y formularios, mensajes, seguridad, datos y formatos, interfaz e integraciones).

## Criterios de aceptación

| Código | Escenario |
|---|---|
| CA1 | Dado que un usuario con sesión iniciada quiere cerrar su sesión, cuando ejecute la acción definida para cerrar sesión, entonces se elimina el token de acceso del dispositivo, se le lleva a la pantalla de inicio de sesión y se muestra el mensaje (INF-USU-01), indicando que la sesión se cerró correctamente. |
| CA2 | Dado que un usuario cerró su sesión, cuando intente volver a una página que exige autenticación (por ejemplo, con el botón Atrás del navegador), entonces no se muestra la información y se le lleva a la pantalla de inicio de sesión con el mensaje (ERR-GEN-05). |
