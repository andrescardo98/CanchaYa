# HU7-Definir un horario de atención

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.7. Tallaje producto/Tallaje producto - CanchaYa.xlsx, hoja HU7`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

**Release:** Release 1 · Núcleo · **Prioridad:** 6 · **Talla:** M · **Puntos:** 3

## Tarjeta

- **Como:** Administrador de cancha
- **Necesito:** definir los días y las horas en que atiende mi escenario
- **Para:** que la plataforma ofrezca a los jugadores solo las franjas en las que puedo atender

> Como Administrador de cancha, necesito definir los días y las horas en que atiende mi escenario, para que la plataforma ofrezca a los jugadores solo las franjas en las que puedo atender


## Conversación historia

- **Requisito de información:** Modelo de dominio enriquecido, hoja HorarioRecurrente: atributos y responsabilidad definir (políticas 1 a 5 y 13).
- **Prototipo:** Prototipo «Horarios de atención»: Prototipos/Celular y Prototipos/Computador, archivo «08 Horarios de atención.png» (1.1.9.5 Artefactos apoyo).
- **Catálogo de mensajes:** Catálogo de mensajes - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo), hoja «Mensajes». Códigos usados en esta HU: EXI-HOR-01, ERR-GEN-01, ERR-GEN-02, ERR-GEN-03, ERR-GEN-04, ERR-HOR-01, ERR-HOR-02, ERR-HOR-03, ERR-HOR-04, ERR-HOR-13.
- **Catálogo de parámetros:** Catálogo de parámetros - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo). Parámetros usados en esta HU: PAR-HOR-01 (duración de la franja), PAR-HOR-02 (granularidad de horarios y bloqueos).
- **Restricciones de diseño:** Las franjas no se guardan: se calculan en cada consulta a partir de los horarios recurrentes (HorarioRecurrente.calcularFranjas).
- El horario se define para el escenario y rige para todas sus canchas.
- Se pueden definir varios horarios para un mismo día siempre que no se crucen (por ejemplo, de 6:00 a 12:00 y de 14:00 a 23:00).
- Las horas de inicio y de fin se eligen en horas en punto, para que el horario se divida exactamente en franjas de una hora (RN-18).

## Conversación aspectos generales y transversales

- **Aspectos generales:** Aspectos generales - CanchaYa.xlsx (1.1.9.5 Artefactos apoyo): AG-01 a AG-23 aplican a todas las HU (validación y formularios, mensajes, seguridad, datos y formatos, interfaz e integraciones).

## Criterios de aceptación

| Código | Escenario |
|---|---|
| CA1 | Dado que un administrador de cancha quiere definir un horario de atención de su escenario, cuando seleccione el día de la semana, ingrese la hora de inicio y la hora de fin de forma correcta, ejecute la acción definida para guardar y el horario no se cruce con otro del mismo día, entonces se persiste el horario, se actualiza la lista de horarios del escenario, se limpia el formulario y se muestra el mensaje de éxito (EXI-HOR-01), indicando que el horario se definió de forma satisfactoria. |
| CA2 | Dado que un administrador de cancha quiere definir un horario de atención, cuando ingrese alguno de los datos de forma incorrecta, incumpliendo alguna de las reglas especificadas en el requisito de información, y ejecute la acción definida para guardar, entonces no se persiste la información y se muestran los mensajes de error respectivos al usuario (Problemas de obligatoriedad: ERR-GEN-01, Problemas de formato: ERR-GEN-02, Problemas de longitud: ERR-GEN-03, Problemas de rango: ERR-GEN-04), indicando cuál fue el problema presentado y cómo puede remediarlo. |
| CA3 | Dado que un administrador de cancha quiere definir un horario, cuando el escenario indicado no exista y ejecute la acción definida para guardar, entonces no se persiste la información y se muestra el mensaje de error (ERR-HOR-01), indicando que no existe el escenario al que se quiere definir el horario. |
| CA4 | Dado que se recibe una solicitud para definir un horario, cuando el usuario autenticado no sea el administrador propietario del escenario (incluido un encargado de sede), entonces no se persiste la información y se muestra el mensaje de error (ERR-HOR-02), indicando que no tiene permiso para gestionar los horarios de este escenario. |
| CA5 | Dado que un administrador de cancha quiere definir un horario, cuando ingrese una hora de fin igual o anterior a la hora de inicio y ejecute la acción definida para guardar, entonces no se persiste la información y se muestra el mensaje de error (ERR-HOR-03), indicando que la hora de fin debe ser mayor que la hora de inicio. |
| CA6 | Dado que un administrador de cancha quiere definir un horario, cuando el horario ingresado se cruce con otro horario ya definido para el mismo día y ejecute la acción definida para guardar, entonces no se persiste la información y se muestra el mensaje de error (ERR-HOR-04), indicando que el horario se cruza con otro ya definido para ese día. |
| CA7 | Dado que un administrador de cancha quiere definir un segundo horario para un mismo día, cuando el nuevo horario no se cruce con el existente y ejecute la acción definida para guardar, entonces se persiste el horario y ambos quedan vigentes para ese día. |
| CA8 | Dado que un administrador de cancha quiere definir un horario, cuando ingrese una hora de inicio o de fin que no sea en punto y ejecute la acción definida para guardar, entonces no se persiste la información y se muestra el mensaje de error (ERR-HOR-13), indicando que los horarios se definen en horas en punto. |
