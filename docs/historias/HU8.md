# HU8-Modificar un escenario

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.7. Tallaje producto/Tallaje producto - CanchaYa.xlsx, hoja HU8`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

**Release:** Release 1 · Núcleo · **Prioridad:** 16 · **Talla:** M · **Puntos:** 3

## Tarjeta

- **Como:** Administrador de cancha
- **Necesito:** modificar los datos de mi escenario
- **Para:** que los jugadores vean información actualizada

> Como Administrador de cancha, necesito modificar los datos de mi escenario, para que los jugadores vean información actualizada


## Conversación historia

- **Requisito de información:** Modelo de dominio enriquecido, hoja Escenario: atributos y responsabilidad modificar (políticas 2, 3, 6, 7 y 13).
- **Prototipo:** Prototipo «Edición de escenario»: Prototipos/Celular y Prototipos/Computador, archivo «06 Edición de escenario.png» (1.1.9.5 Artefactos apoyo).
- **Catálogo de mensajes:** Catálogo de mensajes - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo), hoja «Mensajes». Códigos usados en esta HU: EXI-ESC-02, ERR-ESC-02, ERR-ESC-03, ERR-ESC-06, ERR-ESC-07, ERR-GEN-01, ERR-GEN-02, ERR-GEN-03, ERR-GEN-04, ERR-ESC-13.
- **Catálogo de parámetros:** Catálogo de parámetros - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo). Parámetros usados en esta HU: PAR-ESC-01 (fotos máximas por escenario), PAR-ESC-02 (tamaño máximo de cada foto), PAR-ESC-03 (formatos de foto admitidos).
- **Restricciones de diseño:** Validación de propiedad en el servidor a partir del usuario del token de acceso.
- Se pueden modificar nombre, descripción, dirección, ciudad, ubicación, teléfono y correo de contacto y fotos. El estado y el administrador propietario no se modifican desde esta opción.
- El formulario se carga con los datos actuales del escenario.
- El encargado de sede no puede modificar el escenario (RN-15).

## Conversación aspectos generales y transversales

- **Aspectos generales:** Aspectos generales - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo): AG-01 a AG-23 aplican a todas las HU (validación y formularios, mensajes, seguridad, datos y formatos, interfaz e integraciones).

## Criterios de aceptación

| Código | Escenario |
|---|---|
| CA1 | Dado que un administrador de cancha quiere modificar los datos de su escenario, cuando cambie uno o varios datos de forma correcta y ejecute la acción definida para guardar, entonces se persisten los cambios, los jugadores ven la información actualizada en la ficha del escenario y se muestra el mensaje de éxito (EXI-ESC-02), indicando que el escenario se modificó de forma satisfactoria. |
| CA2 | Dado que un administrador de cancha quiere modificar los datos de su escenario, cuando ingrese alguno de los datos de forma incorrecta, incumpliendo alguna de las reglas especificadas en el requisito de información, y ejecute la acción definida para guardar, entonces no se persiste la información y se muestran los mensajes de error respectivos al usuario (Problemas de obligatoriedad: ERR-GEN-01, Problemas de formato: ERR-GEN-02, Problemas de longitud: ERR-GEN-03, Problemas de rango: ERR-GEN-04), indicando cuál fue el problema presentado y cómo puede remediarlo. |
| CA3 | Dado que un administrador de cancha quiere modificar un escenario, cuando el escenario no exista y ejecute la acción definida para guardar, entonces no se persisten los cambios y se muestra el mensaje de error (ERR-ESC-06), indicando que no existe un escenario con ese identificador. |
| CA4 | Dado que se recibe una solicitud para modificar un escenario, cuando el usuario autenticado no sea su administrador propietario (incluido un encargado de sede), entonces no se persisten los cambios y se muestra el mensaje de error (ERR-ESC-07), indicando que no tiene permiso para modificar este escenario. |
| CA5 | Dado que un administrador de cancha quiere modificar su escenario, cuando la ciudad enviada no exista en el catálogo y ejecute la acción definida para guardar, entonces no se persisten los cambios y se muestra el mensaje de error (ERR-ESC-02), indicando que la ciudad seleccionada no existe. |
| CA6 | Dado que un administrador de cancha quiere modificar su escenario, cuando la ubicación quede por fuera de los rangos válidos de latitud y longitud y ejecute la acción definida para guardar, entonces no se persisten los cambios y se muestra el mensaje de error (ERR-ESC-03), indicando que la ubicación del escenario no es válida. |
| CA7 | Dado que un administrador de cancha quiere modificar los datos de su escenario, cuando haya ingresado información en un campo determinado del formulario de forma incorrecta, incumpliendo alguna de las reglas especificadas en el requisito de información, y pierda el foco de este, entonces se aplica la validación respectiva y se muestra junto al campo el mensaje de error correspondiente (Problemas de obligatoriedad: ERR-GEN-01, Problemas de formato: ERR-GEN-02, Problemas de longitud: ERR-GEN-03, Problemas de rango: ERR-GEN-04), indicando cuál fue el problema presentado y cómo puede remediarlo. |
| CA8 | Dado que un administrador de cancha quiere modificar los datos de su escenario, cuando adjunte más de 5 fotos y ejecute la acción definida para guardar, entonces no se persisten los cambios y se muestra el mensaje de error (ERR-ESC-13), indicando que se permiten máximo 5 fotos por escenario. |
| CA9 | Dado que un administrador de cancha quiere modificar los datos de su escenario, cuando adjunte una foto que no esté en formato JPG o PNG y ejecute la acción definida para guardar, entonces no se persisten los cambios y se muestra el mensaje de error (ERR-ESC-13), indicando que las fotos deben estar en formato JPG o PNG. |
| CA10 | Dado que un administrador de cancha quiere modificar los datos de su escenario, cuando adjunte una foto de más de 2 MB y ejecute la acción definida para guardar, entonces no se persisten los cambios y se muestra el mensaje de error (ERR-ESC-13), indicando que cada foto puede pesar máximo 2 MB. |
