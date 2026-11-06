# Reseña

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.4. Modelo de dominio/1.1.3.2. Modelo de dominio enriquecido/Modelo_de_dominio_enriquecido_CanchaYa.xlsx, hoja Reseña`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

Objeto de dominio que representa la calificación y el comentario de un jugador sobre un escenario, a partir de una reserva que ya jugó.

## Atributos

| Atributo | Tipo de Dato | Longitud mínima | Longitud máxima | Precisión | Rango Inicial | Rango Final | Formato | Regla especial | ¿Auto generado? | ¿Calculado? | ¿Obligatorio? | ¿Sensible? | ¿Identifica al registro? | Descripción |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| id | Alfanumérico | 36 | 36 |  |  |  | Formato de un Identificador Único Universal (UUID) | Corresponde a una cadena de 36 caracteres: 32 dígitos del 0 al 9 y letras de la "a" a la "f", en grupos de 8-4-4-4-12 separados por guiones (D37) | SI | NO | SI | NO | SI | Identificador único de la reseña. |
| reserva | Reserva |  |  |  |  |  |  | La reserva debe haber finalizado y no estar CANCELADA ni NO_ASISTIO. El escenario reseñado se obtiene por la cancha de la reserva (RN-14) | NO | NO | SI | NO | NO | Reserva a partir de la cual el jugador opina; garantiza que solo reseñan quienes jugaron. |
| calificacion | Numérico |  |  | 0 | 1 | 5 |  |  | NO | NO | SI | NO | NO | Calificación general del escenario, de 1 a 5. |
| comentario | Alfanumérico | 0 | 500 |  |  |  |  |  | NO | NO | NO | NO | NO | Comentario opcional del jugador (atención, estado de la cancha, etc.). |
| fechaCreacion | Fecha |  |  |  |  |  | dd/mm/aaaa hh:mm:ss |  | SI | NO | SI | NO | NO | Fecha y hora en que se publicó la reseña. |


### Uso de cada atributo por responsabilidad

| Atributo | publicar |
|---|---|
| id | Se asigna automáticamente |
| reserva | Requerido |
| calificacion | Requerido |
| comentario | Opcional |
| fechaCreacion | Se asigna automáticamente |


## Combinaciones únicas

| Nombre combinación | Descripción | Atributos |
|---|---|---|
| ResenaUnicaPorReserva | No es posible tener más de una reseña para la misma reserva. | reserva |


## Responsabilidades


### publicar

Permite registrar la calificación y el comentario de un jugador sobre un escenario en el que ya jugó; el escenario se obtiene de la cancha de la reserva.

**Entradas:**

- `resena` (Reseña): Contiene la reserva, la calificación y el comentario de la reseña que se va a publicar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** Reseña: Reseña registrada, con su identificador y su fecha de creación.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | Debe existir la reserva a partir de la cual se publica la reseña. | No existe una reserva con el identificador enviado. | Abortar la publicación de la reseña y reportar un mensaje de error al usuario, indicándole que no existe la reserva que se quiere reseñar. |
| 2 | La reserva debe pertenecer al jugador que publica la reseña. | El solicitante no es el jugador de la reserva. | Abortar la publicación de la reseña y reportar un mensaje de error al usuario, indicándole que solo puede reseñar sus propias reservas. |
| 3 | RN-14: la reserva debe haber finalizado y no estar CANCELADA ni marcada como NO_ASISTIO. | La reserva aún no ha finalizado, fue cancelada o está marcada como NO_ASISTIO. | Abortar la publicación de la reseña y reportar un mensaje de error al usuario, indicándole que solo puede reseñar reservas que ya jugó. |
| 4 | RN-14: se permite una única reseña por reserva (ResenaUnicaPorReserva). | Ya existe una reseña para la reserva enviada. | Abortar la publicación de la reseña y reportar un mensaje de error al usuario, indicándole que ya publicó una reseña para esta reserva. |
| 5 | RN-14: la calificación es un número entero de 1 a 5; el comentario es opcional. | La calificación no es un número entero entre 1 y 5. | Abortar la publicación de la reseña y reportar un mensaje de error al usuario, indicándole que la calificación debe ser un número de 1 a 5. |
| 6 | No debe existir otra reseña con el mismo identificador (UUID). | Existe una reseña con el mismo identificador. | Se debe reintentar con un nuevo identificador generado de forma automática, hasta que no exista, para continuar con el proceso de registro. |
| 7 | La calificación promedio no se almacena: se calcula al consultar (Escenario.calcularCalificacionPromedio), por lo que la nueva reseña se refleja de inmediato. | No aplica | No aplica |
| 8 | RN-14: una vez publicada, la reseña no se puede modificar ni eliminar. | No aplica | No aplica |
