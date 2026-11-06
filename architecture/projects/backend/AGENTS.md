# AGENTS.md · Backend

Reglas propias del backend. Las generales (idioma, decisiones, mensajes, commits, arquitectura de módulos) están en el [AGENTS.md de la raíz](../../../AGENTS.md).

## Tecnología

- Java 21 y Spring Boot 4.1 con Maven. Se usa siempre el wrapper `./mvnw` (en Windows `mvnw.cmd`), no un Maven instalado aparte, para que todos compilen con la misma versión.
- PostgreSQL 16 con PostGIS. La estructura de la base la crean las migraciones de Flyway en `src/main/resources/db/migration/`; JPA solo la valida (`ddl-auto=validate`).
- Pruebas con JUnit 5, AssertJ, Testcontainers (base real en Docker) y ArchUnit (límites entre módulos).

## Comandos

Desde `architecture/projects/backend/`:

| Para… | Comando |
|---|---|
| Levantar la base de datos y el buzón de correo local | `docker compose up -d` (desde la raíz del repositorio) |
| Ejecutar el backend en local | `./mvnw spring-boot:run` → <http://localhost:8080/actuator/health> |
| Correr todas las pruebas (necesita Docker encendido) | `./mvnw verify` |
| Correr una sola prueba | `./mvnw test -Dtest=NombreDeLaPrueba` |
| Construir la imagen como en Render | `docker build -t canchaya-backend .` |

En local, la base escucha en el puerto **5434** (no en el 5432) para no chocar con otros PostgreSQL del computador. Los correos de prueba se ven en <http://localhost:8025> (Mailpit).

## Variables de entorno

| Variable | Para qué | Valor local por defecto |
|---|---|---|
| `PORT` | Puerto HTTP (lo pone Render) | `8080` |
| `DB_URL` | Conexión JDBC a PostgreSQL | `jdbc:postgresql://localhost:5434/canchaya` |
| `DB_USUARIO` / `DB_CLAVE` | Credenciales de la base | `canchaya` / `canchaya` |

Los valores reales de Render y Supabase nunca se escriben en el código ni en el repositorio: van como variables secretas de Render (RT-10, D35).

## Reglas

1. **Migraciones**: cada cambio de la base es un archivo nuevo `V<n>__<descripcion>.sql`. Nunca se edita una migración que ya se aplicó en algún ambiente; se crea otra.
2. **Pruebas primero**: cada criterio de aceptación de la HU tiene al menos una prueba, con el código del criterio en su nombre o comentario (por ejemplo `CA3`).
3. **Pruebas de base de datos**: las que necesitan la base importan `TestcontainersConfiguration`. Las del dominio no usan Spring ni la base: son pruebas unitarias normales, rápidas.
4. **ArchUnit**: `ArquitecturaTest` revisa que el dominio no dependa de Spring ni de la base y que cada módulo solo use los módulos permitidos. Si falla, se corrige el código, no la regla. Si la regla está mal, se reporta.
5. **Zona horaria**: todo corre en `America/Bogota` (D27): el `main`, las pruebas (configuradas en el `pom.xml`) y el contenedor (`Dockerfile`).
