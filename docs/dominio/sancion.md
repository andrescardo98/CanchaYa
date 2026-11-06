# Sancion

> Archivo generado por `scripts/exportar_docs.py` a partir de `1. Artefactos proyecto/1.1. Artefactos estratégicos/1.1.4. Modelo de dominio/1.1.3.2. Modelo de dominio enriquecido/Modelo_de_dominio_enriquecido_CanchaYa.xlsx, hoja Sancion`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.

Objeto de dominio que representa una suspensión temporal impuesta a un jugador para reservar canchas, derivada de inasistencias reiteradas. TRABAJO FUTURO: se modela, pero no se implementa en el alcance actual.

## Atributos

| Atributo | Tipo de Dato | Longitud mínima | Longitud máxima | Formato | Valor por defecto | Regla especial | ¿Auto generado? | ¿Calculado? | ¿Obligatorio? | ¿Sensible? | ¿Identifica al registro? | Descripción |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| id | Alfanumérico | 36 | 36 | Formato de un Identificador Único Universal (UUID) |  | Corresponde a una cadena de 36 caracteres: 32 dígitos del 0 al 9 y letras de la "a" a la "f", en grupos de 8-4-4-4-12 separados por guiones (D37) | SI | NO | SI | NO | SI | Identificador único de la sanción. |
| jugador | Usuario |  |  |  |  | El usuario referenciado debe tener rol JUGADOR | NO | NO | SI | NO | NO | Jugador sancionado. |
| fechaInicio | Fecha |  |  | dd/mm/aaaa hh:mm:ss |  |  | SI | NO | SI | NO | NO | Fecha y hora desde la cual rige la suspensión. |
| fechaFin | Fecha |  |  | dd/mm/aaaa hh:mm:ss |  | fechaFin debe ser posterior a fechaInicio | NO | NO | SI | NO | NO | Fecha y hora en que termina la suspensión. |
| motivo | Alfanumérico | 1 | 200 |  |  |  | NO | NO | SI | NO | NO | Causa de la sanción (por ejemplo, inasistencias reiteradas). |
| estado | Alfanumérico |  |  | Enumeración | ACTIVA | Valores permitidos: ACTIVA \| CUMPLIDA \| REVOCADA | NO | NO | SI | NO | NO | Estado de la sanción. |


### Uso de cada atributo por responsabilidad

| Atributo | aplicar | verificarVigencia |
|---|---|---|
| id | Se asigna automáticamente | No requerido |
| jugador | Requerido | Filtro |
| fechaInicio | Requerido | Filtro |
| fechaFin | Requerido | Filtro |
| motivo | Requerido | No requerido |
| estado | Se asigna automáticamente | Filtro |


## Responsabilidades


### aplicar

TRABAJO FUTURO. Permite registrar una suspensión temporal a un jugador que acumula inasistencias reiteradas.

**Entradas:**

- `sancion` (Sancion): Contiene el jugador, las fechas de inicio y de fin y el motivo de la sanción que se va a registrar.

**Salida:** Sancion: Sanción registrada en estado ACTIVA, con su identificador asignado.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | Debe existir el usuario al que se aplica o se consulta la sanción, y debe tener rol JUGADOR. | No existe el usuario enviado o no tiene rol JUGADOR. | Abortar la aplicación de la sanción y reportar un mensaje de error al usuario, indicándole que no existe un jugador con el identificador enviado. |
| 2 | RN-09: la sanción se basa en las reservas del jugador marcadas NO_ASISTIO y en las canceladas por falta de confirmación (origenCancelacion = SISTEMA_SIN_CONFIRMACION). Trabajo futuro: el umbral y la duración no tienen parámetros definidos. | El jugador no alcanza el umbral de inasistencias definido. | Abortar la aplicación de la sanción y reportar un mensaje de error al usuario, indicándole que el jugador no cumple las condiciones para ser sancionado. |
| 3 | La fecha de fin de la sanción debe ser posterior a la fecha de inicio. | La fecha de fin es igual o anterior a la fecha de inicio. | Abortar la aplicación de la sanción y reportar un mensaje de error al usuario, indicándole que la fecha de fin debe ser posterior a la fecha de inicio. |
| 4 | No debe existir otra sanción con el mismo identificador (UUID). | Existe una sanción con el mismo identificador. | Se debe reintentar con un nuevo identificador generado de forma automática, hasta que no exista, para continuar con el proceso de registro. |
| 5 | La sanción es global: aplica a todos los escenarios de la plataforma. | No aplica | No aplica |


### verificarVigencia

TRABAJO FUTURO. Permite saber si un jugador tiene una sanción activa en una fecha dada.

**Entradas:**

- `jugador` (Usuario): Jugador del que se verifica la sanción.
- `fecha` (Fecha): Fecha para la que se quiere saber si la sanción está vigente.

**Salida:** Lógico: Verdadero si el jugador tiene una sanción ACTIVA cuyo rango de fechas cubre la fecha consultada.

| Política | Descripción | Excepción | ¿Qué hago? |
|---|---|---|---|
| 1 | Debe existir el usuario al que se aplica o se consulta la sanción, y debe tener rol JUGADOR. | No existe el usuario enviado o no tiene rol JUGADOR. | Abortar la verificación de la sanción y reportar un mensaje de error al usuario, indicándole que no existe un jugador con el identificador enviado. |
| 6 | Un jugador con una sanción ACTIVA que cubre la fecha consultada no puede crear nuevas reservas: Reserva.confirmar debe rechazarla. | No aplica | No aplica |
| 5 | La sanción es global: aplica a todos los escenarios de la plataforma. | No aplica | No aplica |
