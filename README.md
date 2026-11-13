# CanchaYa

**CanchaYa** es una aplicación web responsiva para buscar, reservar y gestionar canchas sintéticas de fútbol en Antioquia. Reúne en una sola plataforma lo que necesita el jugador (búsqueda y reserva) y lo que necesita el administrador del escenario (agenda, disponibilidad y tarifas).

Es el trabajo de grado de Ingeniería de Sistemas de la Universidad Católica de Oriente (UCO). El problema, el alcance, los actores y el cronograma están en [docs/proyecto.md](docs/proyecto.md).

## Stack tecnológico

Definido en el diseño arquitectónico: alternativa ALT-01 (D30) y plataforma tecnológica (D31). El detalle está en [docs/arquitectura/](docs/arquitectura/).

| Capa | Tecnología |
|---|---|
| Backend | Java 21 + Spring Boot, un solo backend organizado por módulos (monolito modular) |
| Frontend | Angular, con diseño responsivo; mapas con Leaflet y OpenStreetMap |
| Base de datos | PostgreSQL + PostGIS en Supabase (plan gratuito); migraciones con Flyway |
| Fotos | Supabase Storage |
| Arquitectura | Hexagonal + DDD; límites entre módulos revisados con ArchUnit |
| Seguridad | Spring Security con JWT (rol en el token); contraseñas con bcrypt; sin puerta de enlace (D32) |
| Identificadores | UUID generados en el servidor |
| Correo | SMTP de Brevo por el puerto 2525 |
| Despliegue | Docker en Render (Web Service para el backend, Static Site para el frontend); UptimeRobot revisa la verificación de estado cada 5 minutos |

## Cómo ejecutar en local

**Requisitos:** Docker Desktop encendido y Java 21 (JDK). No hace falta instalar Maven: el backend trae su propio `mvnw`.

1. Levanta la base de datos (PostgreSQL con PostGIS, puerto **5434**) y el buzón de correo de prueba (Mailpit), desde la raíz del repositorio:

   ```bash
   docker compose up -d
   ```

2. Ejecuta el backend. Los valores por defecto ya apuntan a la base local, así que no hay que definir variables de entorno:

   ```bash
   cd projects/api
   ./mvnw spring-boot:run        # en Windows (cmd): mvnw.cmd spring-boot:run
   ```

3. Comprueba que responde: <http://localhost:8080/actuator/health> debe mostrar `{"status":"UP"}`. Los correos que envía la aplicación se ven en <http://localhost:8025>.

**Pruebas:** `./mvnw verify` desde `projects/api` (necesita Docker, porque las pruebas de base de datos usan Testcontainers).

Para apagar los servicios: `docker compose down` (conserva los datos) o `docker compose down -v` (los borra). Las variables de entorno, el usuario administrador de prueba y los demás comandos están en [projects/api/AGENTS.md](projects/api/AGENTS.md).

## Despliegue

Cada cambio que llega a `main` pasa por la integración continua (`.github/workflows/backend.yml`: compila, corre todas las pruebas y construye la imagen Docker). Render publica el backend automáticamente solo cuando esa revisión termina en verde. La base de datos está en Supabase y sus credenciales son variables secretas de Render: nunca se escriben en el repositorio (D35).

## Estructura del repositorio

Es un monorepo:

```
CanchaYa/
├── README.md
├── AGENTS.md                 # Guía para agentes de IA
├── CLAUDE.md                 # Remite a AGENTS.md
├── .claude/                  # Configuración de Claude Code (permisos y skills)
├── .github/                  # Plantilla de pull request e integración continua
├── docker-compose.yml        # Base de datos y buzón de correo para desarrollo local
├── docs/                     # Documentación del trabajo de grado en texto
├── scripts/                  # Utilidades (exportar la documentación)
└── projects/                 # Proyectos de la aplicación, cada uno con su propia arquitectura
    └── api/                  # Backend (Java 21 + Spring Boot)
```

El detalle de cada carpeta está en [AGENTS.md](AGENTS.md).

## Documentación

Los artefactos de análisis y diseño se mantienen en Excel, en la carpeta de documentos del trabajo de grado. La carpeta [docs/](docs/README.md) tiene una copia en texto para consultarlos mientras se programa; su README dice qué archivo consultar en cada caso. Las reglas de negocio mandan desde las decisiones D01–D39 ([docs/decisiones.md](docs/decisiones.md)).

La copia se regenera con `python scripts/exportar_docs.py` cada vez que cambia un Excel.

## Contribuir

1. Crea una rama por historia de usuario: `<tipo>/HU<nn>-<descripcion-corta>`, donde `<tipo>` es un tipo de Conventional Commits (por ejemplo `feat/HU21-reservar-franja`).
2. Escribe los mensajes de commit con [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/), por ejemplo `feat(reservas): validar el límite de 2 horas por día`. Las reglas completas están en [AGENTS.md](AGENTS.md#mensajes-de-commit).
3. Abre el pull request con un título en el mismo formato y con la plantilla de `.github/`. El skill `pull-request` la llena a partir de los cambios.
4. Las instrucciones de prueba deben permitir verificar cada criterio de aceptación de la HU.

## Equipo

- **Andrés Cardona Jaramillo**: desarrollador (análisis, arquitectura, desarrollo, pruebas y documentación).
- **Juan Camilo Acevedo**: asesor del trabajo de grado.
