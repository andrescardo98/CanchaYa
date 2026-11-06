# Imagen del backend de CanchaYa. Render la construye desde este archivo (D31, RT-13).
# Tiene dos etapas: la primera compila con Maven; la segunda solo lleva lo necesario para ejecutar.

# --- Etapa 1: compilar -----------------------------------------------------------
FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /app
# Primero el pom y el wrapper: si no cambian, Docker reutiliza las dependencias descargadas.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
# Las pruebas no corren aquí: las corre la CI (necesitan Docker para la base de pruebas).
RUN ./mvnw -B -q package -DskipTests

# --- Etapa 2: ejecutar ------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app
# Hora de Colombia (D27) y Java limitado al 75 % de la memoria del contenedor
# para que el plan de 512 MB de Render no lo reinicie por falta de memoria.
ENV TZ=America/Bogota \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Duser.timezone=America/Bogota"
# La aplicación no corre como administrador del contenedor.
RUN useradd --system --create-home canchaya
USER canchaya
COPY --from=compilacion /app/target/canchaya-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
