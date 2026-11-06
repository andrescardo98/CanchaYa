# HU6-Registrar una cancha

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.7. Tallaje producto/Tallaje producto - CanchaYa.xlsx, hoja HU6`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

**Release:** Release 1 · Núcleo · **Prioridad:** 5 · **Talla:** S · **Puntos:** 2

## Tarjeta

- **Como:** Administrador de cancha
- **Necesito:** agregar las canchas de mi escenario con su nombre, tipo y precio por hora
- **Para:** que los jugadores puedan reservar cada una de ellas

> Como Administrador de cancha, necesito agregar las canchas de mi escenario con su nombre, tipo y precio por hora, para que los jugadores puedan reservar cada una de ellas


## Conversación historia

- **Requisito de información:** Modelo de dominio enriquecido, hoja Cancha: atributos y responsabilidad registrar (políticas 1 a 6). También: hoja TipoCancha (catálogo para seleccionar el tipo).
- **Prototipo:** Prototipo «Canchas del escenario»: Prototipos/Celular y Prototipos/Computador, archivo «07 Canchas del escenario.png» (1.1.9.5 Artefactos apoyo).
- **Catálogo de mensajes:** Catálogo de mensajes - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo), hoja «Mensajes». Códigos usados en esta HU: EXI-CAN-01, ERR-CAN-01, ERR-CAN-02, ERR-CAN-03, ERR-CAN-04, ERR-CAN-05, ERR-GEN-01, ERR-GEN-02, ERR-GEN-03, ERR-GEN-04.
- **Catálogo de parámetros:** No aplican parámetros de negocio.
- **Restricciones de diseño:** Validación de permisos en el servidor a partir del usuario del token de acceso.
- Deben tenerse en cuenta los campos descritos en el requisito de información que no deben ser visibles por el usuario: id y estado (la cancha se registra DISPONIBLE).
- Al validar si ya existe una cancha con el mismo nombre en el escenario se compara el contenido sin distinguir mayúsculas de minúsculas.
- El tipo de cancha se elige de una lista de solo lectura del catálogo, que en el Release 1 se carga con datos iniciales (F5, F7 y F11).

## Conversación aspectos generales y transversales

- **Aspectos generales:** Aspectos generales - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo): AG-01 a AG-23 aplican a todas las HU (validación y formularios, mensajes, seguridad, datos y formatos, interfaz e integraciones).

## Criterios de aceptación

| Código | Escenario |
|---|---|
| CA1 | Dado que un administrador de cancha quiere agregar una cancha a su escenario, cuando ingrese el nombre, el tipo y el precio por hora de forma correcta, ejecute la acción definida para registrar y no exista otra cancha con el mismo nombre en el escenario, entonces se persiste la cancha en estado DISPONIBLE, se actualiza la lista de canchas del escenario, se limpia el formulario y se muestra el mensaje de éxito (EXI-CAN-01), indicando que la cancha se registró de forma satisfactoria. |
| CA2 | Dado que un administrador de cancha quiere agregar una cancha a su escenario, cuando ingrese alguno de los datos de forma incorrecta, incumpliendo alguna de las reglas especificadas en el requisito de información, y ejecute la acción definida para registrar, entonces no se persiste la información y se muestran los mensajes de error respectivos al usuario (Problemas de obligatoriedad: ERR-GEN-01, Problemas de formato: ERR-GEN-02, Problemas de longitud: ERR-GEN-03, Problemas de rango: ERR-GEN-04), indicando cuál fue el problema presentado y cómo puede remediarlo. |
| CA3 | Dado que un administrador de cancha quiere agregar una cancha, cuando el escenario indicado no exista y ejecute la acción definida para registrar, entonces no se persiste la información y se muestra el mensaje de error (ERR-CAN-01), indicando que no existe el escenario al que se quiere agregar la cancha. |
| CA4 | Dado que se recibe una solicitud para agregar una cancha, cuando el usuario autenticado no sea el administrador propietario del escenario (incluido un encargado de sede), entonces no se persiste la información y se muestra el mensaje de error (ERR-CAN-02), indicando que no tiene permiso para gestionar las canchas de este escenario. |
| CA5 | Dado que un administrador de cancha quiere agregar una cancha, cuando ingrese todos los datos de forma correcta, ejecute la acción definida para registrar y ya exista una cancha con el mismo nombre en el escenario, entonces no se persiste la información y se muestra el mensaje de error (ERR-CAN-03), indicando que ya existe una cancha con ese nombre en el escenario. |
| CA6 | Dado que un administrador de cancha quiere agregar una cancha, cuando el tipo de cancha enviado no exista en el catálogo y ejecute la acción definida para registrar, entonces no se persiste la información y se muestra el mensaje de error (ERR-CAN-04), indicando que el tipo de cancha seleccionado no existe. |
| CA7 | Dado que un administrador de cancha quiere agregar una cancha, cuando ingrese un precio por hora menor o igual a cero y ejecute la acción definida para registrar, entonces no se persiste la información y se muestra el mensaje de error (ERR-CAN-05), indicando que el precio por hora debe ser mayor que cero. |
| CA8 | Dado que un administrador de cancha quiere agregar una cancha, cuando haya ingresado el nombre, pierda el foco del campo y ya exista una cancha con ese nombre en el escenario, entonces se muestra junto al campo el mensaje de error (ERR-CAN-03), indicando que ya existe una cancha con ese nombre en el escenario. |
| CA9 | Dado que un administrador de cancha quiere agregar una cancha a su escenario, cuando haya ingresado información en un campo determinado del formulario de forma incorrecta, incumpliendo alguna de las reglas especificadas en el requisito de información, y pierda el foco de este, entonces se aplica la validación respectiva y se muestra junto al campo el mensaje de error correspondiente (Problemas de obligatoriedad: ERR-GEN-01, Problemas de formato: ERR-GEN-02, Problemas de longitud: ERR-GEN-03, Problemas de rango: ERR-GEN-04), indicando cuál fue el problema presentado y cómo puede remediarlo. |
| CA10 | Dado que un administrador de cancha abre el formulario para registrar una cancha, cuando despliegue el campo tipo de cancha, entonces se listan los tipos del catálogo cargado con los datos iniciales, sin opción de crearlos ni editarlos. |
