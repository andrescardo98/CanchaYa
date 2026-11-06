# Catálogo de parámetros

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.9. Especificación funcional/1.1.9.5. Artefactos apoyo/Catálogo de parámetros - CanchaYa.xlsx`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

## Convenciones


**Catálogo de parámetros — CanchaYa**

- Artefacto de apoyo 1.1.9.5. Reúne en un solo lugar los valores de negocio y técnicos que aplican las reglas de CanchaYa, para que no queden repetidos a mano en las HU, en el modelo enriquecido ni en los mensajes. Corte: 26-oct-2026.
- Elemento · Uso
- PAR-<OBJ>-nn · Código del parámetro. <OBJ> es el objeto de dominio, con los mismos prefijos del catálogo de mensajes (GEN, USU, ESC, TCA, HOR, RES, RSE).
- Valor / Unidad · Valor vigente y su unidad. «Por definir» indica que falta la decisión.
- Origen · Decisión de decisiones.md (Dnn), regla de negocio (RN-nn) o modelo de dominio enriquecido que fija el valor.
- HU · Historias del Tallaje que aplican el parámetro.
- Mensajes · Códigos del catálogo de mensajes cuyo texto muestra o depende del valor. Si el valor cambia, esos textos se actualizan con él.
- ¿Configurable? · Sí: el software lo lee de la configuración y se puede cambiar sin modificar el código. No (regla fija): forma parte del diseño; cambiarlo exige desarrollo.
- Estado · Definido: tiene valor aprobado. Por definir: decisión pendiente (Fase 2 o del usuario). Propuesto: valor en uso que aún no está registrado como decisión.

**Notas**

- • Fuente de verdad: si un valor de este catálogo contradice decisiones.md, manda decisiones.md.
- • Las longitudes y formatos de cada atributo (requisitos de información) siguen en el modelo de dominio enriquecido. Aquí solo están los que las HU citan como parámetros de negocio.
- • No se incluyen parámetros de sanciones ni de series de reservas: son trabajo futuro y no tienen parámetros definidos (D24).

## Parámetros

| Código | Nombre | Valor | Unidad | Descripción | Origen | Objeto | HU | Mensajes | ¿Configurable? | Estado | Observación |
|---|---|---|---|---|---|---|---|---|---|---|---|
| PAR-GEN-01 | Zona horaria de la plataforma | America/Bogota (UTC-5) | — | Todas las fechas y horas (franjas, plazos de cancelación y confirmación, tarea programada) se calculan con la hora del servidor en esta zona. | D27 | General | HU20, HU21, HU24, HU26, HU27, HU28, HU33 | — | Sí | Definido |  |
| PAR-GEN-02 | Reintentos de envío de correo | 0 | reintentos | Si un correo falla, no se reintenta: se registra el error y la operación no se revierte. | D15 | General | HU13, HU22, HU25, HU26 | ERR-GEN-08; COR-ESC-01; COR-RES-01 a COR-RES-06 | Sí | Definido | Release 1 sin reintentos. |
| PAR-USU-01 | Longitud mínima de la contraseña | 8 | caracteres | La contraseña debe tener al menos esta cantidad de caracteres. | D12 | Usuario | HU1, HU3 | ERR-USU-12 | Sí | Definido |  |
| PAR-USU-02 | Composición de la contraseña | Al menos una letra y un número | — | Caracteres que debe incluir la contraseña. | D12 | Usuario | HU1, HU3 | ERR-USU-12 | No (regla fija) | Definido |  |
| PAR-USU-03 | Tipos de documento admitidos | CC, CE, TI, PASAPORTE, NIT | — | Tipos de documento válidos en el registro. La TI solo se admite con el rol Jugador y exige declarar la autorización del acudiente (Ley 1581 de 2012). | D12 | Usuario | HU1 | ERR-USU-13; ERR-USU-14; ERR-USU-15 | No (regla fija) | Definido |  |
| PAR-USU-04 | Formato de la cédula de ciudadanía (CC) | 6 a 10 | dígitos | Longitud y composición del número de una CC. | D12 | Usuario | HU1 | ERR-USU-13 | No (regla fija) | Definido |  |
| PAR-USU-05 | Formato de la cédula de extranjería (CE) | 6 a 10 | caracteres alfanuméricos | Longitud y composición del número de una CE. | D12 | Usuario | HU1 | ERR-USU-13 | No (regla fija) | Definido |  |
| PAR-USU-06 | Formato de la tarjeta de identidad (TI) | 10 u 11 | dígitos | Longitud y composición del número de una TI. | D12 | Usuario | HU1 | ERR-USU-13 | No (regla fija) | Definido |  |
| PAR-USU-07 | Formato del pasaporte | 6 a 12 | caracteres alfanuméricos | Longitud y composición del número de un pasaporte. | D12 | Usuario | HU1 | ERR-USU-13 | No (regla fija) | Definido |  |
| PAR-USU-08 | Formato del NIT | 9 dígitos, guion y dígito de verificación | — | Ejemplo: 900123456-7. | D12 | Usuario | HU1 | ERR-USU-13 | No (regla fija) | Definido |  |
| PAR-USU-09 | Vigencia del token de acceso | 60 | minutos | Tiempo durante el cual el token JWT es válido; al vencer, se pide iniciar sesión de nuevo. | D37 | Usuario | HU2, HU4 | ERR-GEN-05 | Sí | Definido | Sin renovación: al vencer, el usuario inicia sesión de nuevo. Al cerrar sesión se elimina el token del dispositivo (HU4); el servidor no guarda una lista de tokens cerrados (D37). |
| PAR-USU-10 | Intentos fallidos de inicio de sesión permitidos | 5 | intentos | Cantidad de intentos fallidos de inicio de sesión que se admiten, por cuenta y por origen, dentro de la ventana de conteo (PAR-USU-11). Al llegar a ese número, se rechazan los intentos durante el tiempo de bloqueo (PAR-USU-12). | D28 | Usuario | HU2 | ERR-USU-16 | Sí | Definido | Escenario de calidad ESC-SEG-03. |
| PAR-USU-11 | Ventana de conteo de intentos fallidos | 5 | minutos | Periodo en el que se cuentan los intentos fallidos de inicio de sesión. | D28 | Usuario | HU2 | ERR-USU-16 | Sí | Definido |  |
| PAR-USU-12 | Tiempo de bloqueo del inicio de sesión | 15 | minutos | Tiempo durante el cual se rechazan los intentos de inicio de sesión de la cuenta o del origen que superó los intentos permitidos. | D28 | Usuario | HU2 | ERR-USU-16 | Sí | Definido |  |
| PAR-ESC-01 | Fotos máximas por escenario | 5 | fotos | Cantidad máxima de fotos que puede tener un escenario. | D13 | Escenario | HU5, HU8 | ERR-ESC-13 | Sí | Definido |  |
| PAR-ESC-02 | Tamaño máximo de cada foto | 2 | MB | Peso máximo de cada foto del escenario. | D13 | Escenario | HU5, HU8 | ERR-ESC-13 | Sí | Definido |  |
| PAR-ESC-03 | Formatos de foto admitidos | JPG, PNG | — | Formatos de imagen aceptados para las fotos del escenario. | D13 | Escenario | HU5, HU8 | ERR-ESC-13 | Sí | Definido | Dónde se guardan las fotos en el hosting es una decisión abierta de la Fase 2. |
| PAR-ESC-04 | Radio de búsqueda por defecto | 5 | km | Radio con el que se ejecuta la búsqueda si el jugador no elige otro. | D11; D26 | Escenario | HU18 | — | Sí | Definido |  |
| PAR-ESC-05 | Radio de búsqueda máximo | 30 | km | Radio más amplio que puede elegir el jugador. | D11; D26 | Escenario | HU18 | ERR-GEN-04 | Sí | Definido |  |
| PAR-ESC-06 | Radio de búsqueda mínimo | 1 | km | Radio más pequeño que puede elegir el jugador. | D26 | Escenario | HU18 | ERR-GEN-04 | Sí | Definido |  |
| PAR-TCA-01 | Capacidad de jugadores de un tipo de cancha | 2 a 22 | jugadores | Rango permitido para la capacidad de jugadores de una modalidad. | Modelo enriquecido: TipoCancha, política 3 | TipoCancha | HU15, HU16 | ERR-TCA-03 | Sí | Definido |  |
| PAR-HOR-01 | Duración de la franja | 1 | hora | Las franjas reservables duran una hora y se calculan a partir de los horarios recurrentes; no se almacenan. | D10 | HorarioRecurrente | HU7, HU11, HU20, HU29 | — | No (regla fija) | Definido | El modelo de disponibilidad depende de este valor; cambiarlo exige rediseñar el cálculo de franjas. |
| PAR-HOR-02 | Granularidad de horarios y bloqueos | Horas en punto (hh:00) | — | La hora de inicio y la de fin de horarios y bloqueos deben ser en punto. | RN-18; D18 | HorarioRecurrente, Bloqueo | HU7, HU11, HU30 | ERR-HOR-13; ERR-BLO-12 | No (regla fija) | Definido |  |
| PAR-RES-01 | Ventana de reserva | 30 | días calendario | Solo se puede consultar disponibilidad y reservar desde hoy hasta esta cantidad de días. | RN-11 | Reserva, HorarioRecurrente | HU18, HU20, HU21 | ERR-RES-02; ERR-HOR-10 | Sí | Definido |  |
| PAR-RES-02 | Franjas máximas por reserva | 2 | franjas seguidas | Una reserva cubre 1 o 2 franjas consecutivas de la misma cancha; el precio es la suma de las franjas. | RN-08; D10 | Reserva | HU20, HU21 | ERR-RES-31 | Sí | Definido |  |
| PAR-RES-03 | Horas máximas por jugador en un día | 2 | horas | Suma de horas que un jugador puede tener reservadas para una misma fecha. | RN-08; D10 | Reserva | HU21 | ERR-RES-09 | Sí | Definido |  |
| PAR-RES-04 | Reservas activas máximas por jugador | 5 | reservas | Reservas confirmadas activas que puede tener un jugador a la vez. Una reserva de 2 horas cuenta como una. | RN-08; D10 | Reserva | HU21 | ERR-RES-08 | Sí | Definido |  |
| PAR-RES-05 | Antelación mínima para cancelar | Más de 3 | horas | El jugador solo puede cancelar si faltan más de estas horas para el inicio. | RN-10; D06 | Reserva | HU23, HU24 | ERR-RES-16; CNF-RES-01 | Sí | Definido | El límite es estricto: con exactamente 3 horas ya no se puede cancelar. |
| PAR-RES-06 | Anticipación del correo de confirmación de asistencia | 5 | horas | El correo para confirmar asistencia se envía esta cantidad de horas antes del inicio. Una reserva hecha con menos anticipación no requiere confirmación. | RN-13 | Reserva | HU21, HU26 | COR-RES-01; COR-RES-06 | Sí | Definido |  |
| PAR-RES-07 | Hora mínima de inicio que exige confirmación | 12:00 m. | hora del día | Solo las reservas que inician a esta hora o después exigen confirmación de asistencia. | RN-13 | Reserva | HU21, HU26 | COR-RES-01 | Sí | Definido |  |
| PAR-RES-08 | Plazo para confirmar la asistencia | Más de 3 | horas | El jugador puede confirmar mientras falten más de estas horas para el inicio. Al llegar exactamente a ese punto, la tarea programada cancela la reserva si sigue PENDIENTE. | RN-13; D06 | Reserva | HU26, HU27, HU28 | ERR-RES-27; COR-RES-05; COR-RES-06 | Sí | Definido | Tiene el mismo valor que PAR-RES-05, pero es otra regla; se pueden cambiar por separado. |
| PAR-RES-09 | Plazo para marcar una inasistencia | 24 | horas | La inasistencia se puede marcar hasta esta cantidad de horas después de la hora de fin de la reserva. No se revierte. | RN-09; D20 | Reserva | HU33 | ERR-RES-32 | Sí | Definido |  |
| PAR-RES-10 | Longitud máxima de la justificación de cancelación | 250 | caracteres | Texto que escribe el propietario al cancelar por fuerza mayor. | Modelo enriquecido: Reserva | Reserva | HU32 | ERR-GEN-03; ERR-RES-18 | Sí | Definido |  |
| PAR-RES-11 | Frecuencia de la tarea programada | Por definir | minutos | Cada cuánto se ejecuta el proceso que envía las solicitudes de confirmación y cancela las reservas no confirmadas. | Decisión técnica abierta de la Fase 2 | Reserva | HU26, HU28 | ERR-RES-28 | Sí | Por definir | Debe ser lo bastante frecuente para cumplir «exactamente a 3 horas» (D06) y «5 horas antes» con un margen aceptable. |
| PAR-RES-12 | Confirmación de asistencia activa | No | Sí / No | Indica si las reservas nuevas exigen confirmación de asistencia. Con «No», toda reserva nace NO_REQUERIDA y el correo de la reserva no trae el aviso de confirmación. | D33 | Reserva | HU21, HU22, HU26, HU28 | COR-RES-01 | Sí | Definido | Pasa a «Sí» cuando se publiquen HU26–HU28 (Release 2). Las reservas creadas antes siguen NO_REQUERIDA. |
| PAR-RSE-01 | Rango de la calificación | 1 a 5 | números enteros | Valores permitidos para la calificación de un escenario. | RN-14 | Reseña | HU37 | ERR-RSE-05 | Sí | Definido |  |
| PAR-RSE-02 | Longitud máxima del comentario de una reseña | 500 | caracteres | El comentario es opcional. | Modelo enriquecido: Reseña | Reseña | HU37 | ERR-GEN-03 | Sí | Definido |  |
