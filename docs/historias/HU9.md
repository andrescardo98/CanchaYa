# HU9-Modificar una cancha

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.7. Tallaje producto/Tallaje producto - CanchaYa.xlsx, hoja HU9`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

**Release:** Release 1 · Núcleo · **Prioridad:** 17 · **Talla:** S · **Puntos:** 2

## Tarjeta

- **Como:** Administrador de cancha
- **Necesito:** cambiar el nombre, el tipo o el precio por hora de una cancha
- **Para:** mantener actualizadas la oferta y las tarifas de mi escenario

> Como Administrador de cancha, necesito cambiar el nombre, el tipo o el precio por hora de una cancha, para mantener actualizadas la oferta y las tarifas de mi escenario


## Conversación historia

- **Requisito de información:** Modelo de dominio enriquecido, hoja Cancha: atributos y responsabilidad modificar (políticas 2, 4, 5, 7, 8 y 9).
- **Prototipo:** Prototipo «Canchas del escenario»: Prototipos/Celular y Prototipos/Computador, archivo «07 Canchas del escenario.png» (1.1.9.5 Artefactos apoyo).
- **Catálogo de mensajes:** Catálogo de mensajes - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo), hoja «Mensajes». Códigos usados en esta HU: EXI-CAN-02, ERR-CAN-02, ERR-CAN-04, ERR-CAN-05, ERR-CAN-07, ERR-CAN-08, ERR-GEN-01, ERR-GEN-02, ERR-GEN-03, ERR-GEN-04.
- **Catálogo de parámetros:** No aplican parámetros de negocio.
- **Restricciones de diseño:** Validación de propiedad en el servidor a partir del usuario del token de acceso.
- Se pueden modificar nombre, tipo y precio por hora. El escenario de la cancha no se modifica.
- El cambio de precio solo aplica a las reservas nuevas: cada reserva conserva el precio con el que se creó.
- Al validar si otra cancha del escenario ya tiene el nombre se compara el contenido sin distinguir mayúsculas de minúsculas, excluyendo la cancha que se modifica.

## Conversación aspectos generales y transversales

- **Aspectos generales:** Aspectos generales - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo): AG-01 a AG-23 aplican a todas las HU (validación y formularios, mensajes, seguridad, datos y formatos, interfaz e integraciones).

## Criterios de aceptación

| Código | Escenario |
|---|---|
| CA1 | Dado que un administrador de cancha quiere modificar una cancha de su escenario, cuando cambie el nombre, el tipo o el precio por hora de forma correcta y ejecute la acción definida para guardar, entonces se persisten los cambios, se actualiza la lista de canchas y se muestra el mensaje de éxito (EXI-CAN-02), indicando que la cancha se modificó de forma satisfactoria. |
| CA2 | Dado que un administrador de cancha quiere modificar una cancha, cuando ingrese alguno de los datos de forma incorrecta, incumpliendo alguna de las reglas especificadas en el requisito de información, y ejecute la acción definida para guardar, entonces no se persiste la información y se muestran los mensajes de error respectivos al usuario (Problemas de obligatoriedad: ERR-GEN-01, Problemas de formato: ERR-GEN-02, Problemas de longitud: ERR-GEN-03, Problemas de rango: ERR-GEN-04), indicando cuál fue el problema presentado y cómo puede remediarlo. |
| CA3 | Dado que un administrador de cancha quiere modificar una cancha, cuando la cancha no exista y ejecute la acción definida para guardar, entonces no se persisten los cambios y se muestra el mensaje de error (ERR-CAN-07), indicando que no existe una cancha con ese identificador. |
| CA4 | Dado que se recibe una solicitud para modificar una cancha, cuando el usuario autenticado no sea el administrador propietario del escenario (incluido un encargado de sede), entonces no se persisten los cambios y se muestra el mensaje de error (ERR-CAN-02), indicando que no tiene permiso para gestionar las canchas de este escenario. |
| CA5 | Dado que un administrador de cancha quiere modificar una cancha, cuando le asigne un nombre que ya tiene otra cancha del mismo escenario y ejecute la acción definida para guardar, entonces no se persisten los cambios y se muestra el mensaje de error (ERR-CAN-08), indicando que ya existe otra cancha con ese nombre. |
| CA6 | Dado que un administrador de cancha quiere modificar una cancha, cuando el tipo de cancha enviado no exista y ejecute la acción definida para guardar, entonces no se persisten los cambios y se muestra el mensaje de error (ERR-CAN-04), indicando que el tipo de cancha seleccionado no existe. |
| CA7 | Dado que un administrador de cancha quiere modificar una cancha, cuando ingrese un precio por hora menor o igual a cero y ejecute la acción definida para guardar, entonces no se persisten los cambios y se muestra el mensaje de error (ERR-CAN-05), indicando que el precio por hora debe ser mayor que cero. |
| CA8 | Dado que un administrador de cancha cambió el precio por hora de una cancha que tiene reservas confirmadas, cuando se consulten esas reservas, entonces conservan el precio con el que se crearon y solo las reservas nuevas toman el precio actualizado. |
