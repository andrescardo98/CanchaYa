# HU5-Registrar un escenario

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.7. Tallaje producto/Tallaje producto - CanchaYa.xlsx, hoja HU5`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

**Release:** Release 1 · Núcleo · **Prioridad:** 3 · **Talla:** L · **Puntos:** 5

## Tarjeta

- **Como:** Administrador de cancha
- **Necesito:** registrar mi escenario deportivo con su nombre, descripción, dirección, ciudad, ubicación, datos de contacto y fotos
- **Para:** dar a conocer mi escenario a los jugadores que buscan cancha

> Como Administrador de cancha, necesito registrar mi escenario deportivo con su nombre, descripción, dirección, ciudad, ubicación, datos de contacto y fotos, para dar a conocer mi escenario a los jugadores que buscan cancha


## Conversación historia

- **Requisito de información:** Modelo de dominio enriquecido, hoja Escenario: atributos y responsabilidad registrar (políticas 1 a 5 y 13). También: hojas Departamento y Ciudad (catálogos para seleccionar la ciudad).
- **Prototipo:** Prototipo «Registro de escenario»: Prototipos/Celular y Prototipos/Computador, archivo «05 Registro de escenario.png» (1.1.9.5 Artefactos apoyo).
- **Catálogo de mensajes:** Catálogo de mensajes - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo), hoja «Mensajes». Códigos usados en esta HU: EXI-ESC-01, ERR-ESC-01, ERR-ESC-02, ERR-ESC-03, ERR-GEN-01, ERR-GEN-02, ERR-GEN-03, ERR-GEN-04, ERR-ESC-13.
- **Catálogo de parámetros:** Catálogo de parámetros - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo). Parámetros usados en esta HU: PAR-ESC-01 (fotos máximas por escenario), PAR-ESC-02 (tamaño máximo de cada foto), PAR-ESC-03 (formatos de foto admitidos).
- **Restricciones de diseño:** Latitud y longitud se guardan como punto geográfico (PostGIS) para la búsqueda por cercanía. El almacenamiento de las fotos se define en la arquitectura.
- Deben tenerse en cuenta los campos descritos en el requisito de información que no deben ser visibles por el usuario: id, administrador, estado y calificacionPromedio.
- La ubicación no se digita: el administrador marca el punto en un mapa y de allí se toman la latitud y la longitud. La ciudad se elige primero por departamento.
- Descripción, correo de contacto y fotos son opcionales. El escenario queda INACTIVO y no aparece en la búsqueda hasta que el administrador del sistema lo active (RN-16) y tenga canchas con precio y horario (RN-04).

## Conversación aspectos generales y transversales

- **Aspectos generales:** Aspectos generales - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo): AG-01 a AG-23 aplican a todas las HU (validación y formularios, mensajes, seguridad, datos y formatos, interfaz e integraciones).

## Criterios de aceptación

| Código | Escenario |
|---|---|
| CA1 | Dado que un administrador de cancha quiere registrar su escenario, cuando ingrese todos los datos obligatorios de forma correcta (nombre, dirección, ciudad, ubicación en el mapa y teléfono de contacto), con o sin los opcionales, y ejecute la acción definida para registrar, entonces se persiste el escenario en estado INACTIVO y asociado a él como propietario, se limpia el formulario y se muestra el mensaje de éxito (EXI-ESC-01), indicando que el escenario se registró y que será visible para los jugadores cuando el administrador del sistema lo active. |
| CA2 | Dado que un administrador de cancha quiere registrar su escenario, cuando ingrese alguno de los datos de forma incorrecta, incumpliendo alguna de las reglas especificadas en el requisito de información, y ejecute la acción definida para registrar, entonces no se persiste la información y se muestran los mensajes de error respectivos al usuario (Problemas de obligatoriedad: ERR-GEN-01, Problemas de formato: ERR-GEN-02, Problemas de longitud: ERR-GEN-03, Problemas de rango: ERR-GEN-04), indicando cuál fue el problema presentado y cómo puede remediarlo. |
| CA3 | Dado que se recibe una solicitud para registrar un escenario, cuando el usuario autenticado no tenga el rol Administrador de cancha, entonces no se persiste la información y se muestra el mensaje de error (ERR-ESC-01), indicando que solo un administrador de cancha puede registrar escenarios. |
| CA4 | Dado que un administrador de cancha quiere registrar su escenario, cuando la ciudad enviada no exista en el catálogo de ciudades y ejecute la acción definida para registrar, entonces no se persiste la información y se muestra el mensaje de error (ERR-ESC-02), indicando que la ciudad seleccionada no existe. |
| CA5 | Dado que un administrador de cancha quiere registrar su escenario, cuando la latitud no esté entre -90 y 90 o la longitud no esté entre -180 y 180 y ejecute la acción definida para registrar, entonces no se persiste la información y se muestra el mensaje de error (ERR-ESC-03), indicando que la ubicación del escenario no es válida. |
| CA6 | Dado que un administrador de cancha quiere registrar su escenario, cuando seleccione un departamento, entonces la lista de ciudades se carga solo con las ciudades de ese departamento. |
| CA7 | Dado que un administrador de cancha quiere registrar su escenario, cuando haya ingresado información en un campo determinado del formulario de forma correcta y pierda el foco de este, entonces se aplican las validaciones necesarias para asegurar la integridad del dato según las reglas especificadas en el requisito de información, y no se muestra ningún mensaje dado que la validación es exitosa. |
| CA8 | Dado que un administrador de cancha quiere registrar su escenario, cuando haya ingresado información en un campo determinado del formulario de forma incorrecta, incumpliendo alguna de las reglas especificadas en el requisito de información, y pierda el foco de este, entonces se aplica la validación respectiva y se muestra junto al campo el mensaje de error correspondiente (Problemas de obligatoriedad: ERR-GEN-01, Problemas de formato: ERR-GEN-02, Problemas de longitud: ERR-GEN-03, Problemas de rango: ERR-GEN-04), indicando cuál fue el problema presentado y cómo puede remediarlo. |
| CA9 | Dado que un administrador de cancha quiere registrar su escenario, cuando adjunte más de 5 fotos y ejecute la acción definida para registrar, entonces no se persiste la información y se muestra el mensaje de error (ERR-ESC-13), indicando que se permiten máximo 5 fotos por escenario. |
| CA10 | Dado que un administrador de cancha quiere registrar su escenario, cuando adjunte una foto que no esté en formato JPG o PNG y ejecute la acción definida para registrar, entonces no se persiste la información y se muestra el mensaje de error (ERR-ESC-13), indicando que las fotos deben estar en formato JPG o PNG. |
| CA11 | Dado que un administrador de cancha quiere registrar su escenario, cuando adjunte una foto de más de 2 MB y ejecute la acción definida para registrar, entonces no se persiste la información y se muestra el mensaje de error (ERR-ESC-13), indicando que cada foto puede pesar máximo 2 MB. |
