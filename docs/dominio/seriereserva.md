# SerieReserva

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.4. Modelo de dominio/1.1.3.2. Modelo de dominio enriquecido/Modelo_de_dominio_enriquecido_CanchaYa.xlsx, hoja SerieReserva`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

Objeto de dominio que representa un conjunto de reservas recurrentes (por ejemplo, de lunes a viernes, o el mismo día durante varias semanas) creadas por un jugador en una sola operación. TRABAJO FUTURO: se modela, pero no se implementa en el alcance actual.

## Atributos

| Atributo | Tipo de Dato | Longitud mínima | Longitud máxima | Formato | Valor por defecto | Regla especial | ¿Auto generado? | ¿Calculado? | ¿Obligatorio? | ¿Sensible? | ¿Identifica al registro? | Descripción |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| id | Alfanumérico | 36 | 36 | Formato de un Identificador Único Universal (UUID) |  | Corresponde a una cadena de 36 caracteres: 32 dígitos del 0 al 9 y letras de la "a" a la "f", en grupos de 8-4-4-4-12 separados por guiones (D37) | SI | NO | SI | NO | SI | Identificador único de la serie de reservas. |
| jugador | Usuario |  |  |  |  | El usuario referenciado debe tener rol JUGADOR | NO | NO | SI | NO | NO | Jugador que crea la serie. |
| cancha | Cancha |  |  |  |  |  | NO | NO | SI | NO | NO | Cancha reservada en todas las ocurrencias de la serie. |
| diasSemana | Alfanumérico |  | 60 |  |  | Lista de uno o más días (LUNES..DOMINGO) separados por coma | NO | NO | SI | NO | NO | Días de la semana en que se repite la reserva (por ejemplo, LUNES,MARTES,MIERCOLES,JUEVES,VIERNES). |
| horaInicio | Hora |  |  | hh:mm |  |  | NO | NO | SI | NO | NO | Hora de inicio de cada ocurrencia. |
| horaFin | Hora |  |  | hh:mm |  |  | NO | NO | SI | NO | NO | Hora de fin de cada ocurrencia. |
| fechaInicio | Fecha |  |  | dd/mm/aaaa |  |  | NO | NO | SI | NO | NO | Primera fecha de la serie. |
| fechaFin | Fecha |  |  | dd/mm/aaaa |  | No puede superar los 30 días calendario desde la creación de la serie (RN-11) | NO | NO | SI | NO | NO | Última fecha de la serie. |
| estado | Alfanumérico |  |  | Enumeración | ACTIVA | Valores permitidos: ACTIVA \| CANCELADA | NO | NO | SI | NO | NO | Estado de la serie. |


### Uso de cada atributo por responsabilidad

| Atributo | crear | cancelar |
|---|---|---|
| id | Se asigna automáticamente | Requerido |
| jugador | Requerido | No requerido |
| cancha | Requerido | No requerido |
| diasSemana | Requerido | No requerido |
| horaInicio | Requerido | No requerido |
| horaFin | Requerido | No requerido |
| fechaInicio | Requerido | No requerido |
| fechaFin | Requerido | No requerido |
| estado | Se asigna automáticamente | Se asigna automáticamente |


## Responsabilidades


### crear

TRABAJO FUTURO. Permite generar en una sola operación las reservas individuales de la serie dentro del rango de fechas, mostrando antes qué fechas están disponibles.

**Entradas:**

- `serieReserva` (SerieReserva): Contiene el jugador, la cancha, los días de la semana, las horas y el rango de fechas de la serie que se va a crear.

**Salida:** SerieReserva: Serie registrada en estado ACTIVA, con su identificador y sus reservas individuales generadas.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | El usuario que crea la serie debe tener rol JUGADOR. | El usuario que crea la serie no tiene rol JUGADOR. | Abortar la creación de la serie de reservas y reportar un mensaje de error al usuario, indicándole que solo un jugador puede crear series de reservas. |
| 2 | RN-11: la fecha de fin de la serie no puede superar los 30 días calendario desde su creación. | La fecha de fin supera los 30 días calendario siguientes. | Abortar la creación de la serie de reservas y reportar un mensaje de error al usuario, indicándole que la serie solo puede abarcar los próximos 30 días. |
| 3 | Cada ocurrencia de la serie debe cumplir las mismas reglas que una reserva individual (RN-01, RN-03). | Una o más fechas de la serie no están disponibles. | Abortar la creación de la serie de reservas y reportar un mensaje de error al usuario, indicándole que hay fechas no disponibles, indicándole cuáles son. |
| 4 | No debe existir otra serie de reservas con el mismo identificador (UUID). | Existe una serie de reservas con el mismo identificador. | Se debe reintentar con un nuevo identificador generado de forma automática, hasta que no exista, para continuar con el proceso de registro. |
| 5 | Trabajo futuro, sin parámetros definidos: cómo cuenta una serie frente al límite de RN-08 y si se crean solo las fechas libres o es todo o nada se definirá al implementarla. | No aplica | No aplica |


### cancelar

TRABAJO FUTURO. Permite cancelar la serie completa o solo sus ocurrencias futuras.

**Entradas:**

- `id` (Alfanumérico): Identificador de la serie que se va a cancelar.
- `solicitante` (Usuario): Usuario autenticado que ejecuta la acción; se usa para validar sus permisos.

**Salida:** SerieReserva: Serie en estado CANCELADA.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 6 | Debe existir una serie con el identificador enviado, en estado ACTIVA. | No existe una serie con el identificador enviado o no está en estado ACTIVA. | Abortar la cancelación de la serie de reservas y reportar un mensaje de error al usuario, indicándole que la serie no existe o ya no está activa. |
| 7 | La serie debe pertenecer al jugador que realiza la acción. | El solicitante no es el jugador que creó la serie. | Abortar la cancelación de la serie de reservas y reportar un mensaje de error al usuario, indicándole que no tiene permiso para cancelar esta serie. |
| 8 | RN-10: cada ocurrencia solo se puede cancelar si faltan más de 3 horas para su hora de inicio. | Faltan 3 horas o menos para el inicio de una de las ocurrencias. | Cancelar las demás ocurrencias futuras e informar al usuario cuál no se pudo cancelar por faltar 3 horas o menos. |
