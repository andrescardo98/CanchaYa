-- =====================================================================
-- CanchaYa - Modelo entidad-relación (estructura de la base de datos)
-- PostgreSQL 16 o superior con PostGIS 3.x (Supabase, D31)
-- Fuente: Modelo de dominio enriquecido + decisiones D01-D31.
-- Alcance: Release 1 a 3. Sancion y SerieReserva son trabajo futuro (D24).
-- Este archivo se usa como primera migración de Flyway (RT-04):
--   src/main/resources/db/migration/V1__estructura_inicial.sql
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS postgis;      -- puntos geográficos y búsqueda por radio (AC-05.1)
CREATE EXTENSION IF NOT EXISTS btree_gist;   -- reglas de "no se cruzan" con igualdad + rango (D29)

-- ---------------------------------------------------------------------
-- Contexto Ubicación
-- ---------------------------------------------------------------------
CREATE TABLE departamento (
    id      UUID         NOT NULL,
    codigo  CHAR(2)      NOT NULL,
    nombre  VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_departamento        PRIMARY KEY (id),
    CONSTRAINT uq_departamento_codigo UNIQUE (codigo),
    CONSTRAINT uq_departamento_nombre UNIQUE (nombre),
    CONSTRAINT ck_departamento_codigo CHECK (codigo ~ '^[0-9]{2}$')
);

CREATE TABLE ciudad (
    id              UUID          NOT NULL,
    codigo          CHAR(5)       NOT NULL,
    departamento_id UUID          NOT NULL,
    nombre          VARCHAR(100)  NOT NULL,
    CONSTRAINT pk_ciudad                     PRIMARY KEY (id),
    CONSTRAINT fk_ciudad_departamento        FOREIGN KEY (departamento_id) REFERENCES departamento (id),
    CONSTRAINT uq_ciudad_codigo              UNIQUE (codigo),
    CONSTRAINT uq_ciudad_departamento_nombre UNIQUE (departamento_id, nombre),
    CONSTRAINT ck_ciudad_codigo              CHECK (codigo ~ '^[0-9]{5}$')
);

-- ---------------------------------------------------------------------
-- Contexto Identidad y acceso
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id                 UUID          NOT NULL,
    nombre             VARCHAR(100)  NOT NULL,
    correo_electronico VARCHAR(150)  NOT NULL,
    contrasena_hash    CHAR(60)      NOT NULL,
    tipo_documento     VARCHAR(10)   NOT NULL,
    numero_documento   VARCHAR(20)   NOT NULL,
    numero_celular     VARCHAR(15)   NOT NULL,
    rol                VARCHAR(25)   NOT NULL,
    fecha_registro     TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_usuario                  PRIMARY KEY (id),
    CONSTRAINT uq_usuario_correo           UNIQUE (correo_electronico),
    CONSTRAINT uq_usuario_documento        UNIQUE (tipo_documento, numero_documento),
    -- se guarda en minúsculas para que la unicidad no dependa de mayúsculas
    CONSTRAINT ck_usuario_correo           CHECK (correo_electronico = lower(correo_electronico)
                                                  AND correo_electronico ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    CONSTRAINT ck_usuario_tipo_documento   CHECK (tipo_documento IN ('CC', 'CE', 'TI', 'PASAPORTE', 'NIT')),
    -- D12: formato del número según el tipo de documento
    CONSTRAINT ck_usuario_numero_documento CHECK (
           (tipo_documento = 'CC'        AND numero_documento ~ '^[0-9]{6,10}$')
        OR (tipo_documento = 'CE'        AND numero_documento ~ '^[A-Za-z0-9]{6,10}$')
        OR (tipo_documento = 'TI'        AND numero_documento ~ '^[0-9]{10,11}$')
        OR (tipo_documento = 'PASAPORTE' AND numero_documento ~ '^[A-Za-z0-9]{6,12}$')
        OR (tipo_documento = 'NIT'       AND numero_documento ~ '^[0-9]{9}-[0-9]$')),
    CONSTRAINT ck_usuario_rol              CHECK (rol IN ('JUGADOR', 'ADMINISTRADOR_CANCHA', 'ADMINISTRADOR_SISTEMA')),
    -- D12 / RNG-06: la TI solo se admite con el rol Jugador
    CONSTRAINT ck_usuario_ti_solo_jugador  CHECK (tipo_documento <> 'TI' OR rol = 'JUGADOR')
);

-- ---------------------------------------------------------------------
-- Contexto Escenarios y canchas
-- ---------------------------------------------------------------------
CREATE TABLE tipo_cancha (
    id                  UUID         NOT NULL,
    codigo              VARCHAR(5)   NOT NULL,
    nombre              VARCHAR(50)  NOT NULL,
    capacidad_jugadores SMALLINT     NOT NULL,
    CONSTRAINT pk_tipo_cancha           PRIMARY KEY (id),
    CONSTRAINT uq_tipo_cancha_codigo    UNIQUE (codigo),
    CONSTRAINT ck_tipo_cancha_codigo    CHECK (codigo ~ '^[A-Z0-9]{2,5}$'),
    CONSTRAINT ck_tipo_cancha_capacidad CHECK (capacidad_jugadores BETWEEN 2 AND 22)
);

CREATE TABLE escenario (
    id                UUID           NOT NULL,
    nombre            VARCHAR(100)   NOT NULL,
    descripcion       VARCHAR(500),
    direccion         VARCHAR(200)   NOT NULL,
    ciudad_id         UUID           NOT NULL,
    latitud           NUMERIC(8, 6)  NOT NULL,
    longitud          NUMERIC(9, 6)  NOT NULL,
    -- se calcula sola a partir de latitud y longitud; es la que usa la búsqueda por radio (D11, D26)
    ubicacion         GEOGRAPHY(POINT, 4326) GENERATED ALWAYS AS (
                          ST_SetSRID(ST_MakePoint(longitud::DOUBLE PRECISION, latitud::DOUBLE PRECISION), 4326)::GEOGRAPHY
                      ) STORED,
    telefono_contacto VARCHAR(15)    NOT NULL,
    correo_contacto   VARCHAR(150),
    administrador_id  UUID           NOT NULL,
    estado            VARCHAR(10)    NOT NULL DEFAULT 'INACTIVO',
    CONSTRAINT pk_escenario               PRIMARY KEY (id),
    CONSTRAINT fk_escenario_ciudad        FOREIGN KEY (ciudad_id)        REFERENCES ciudad (id),
    CONSTRAINT fk_escenario_administrador FOREIGN KEY (administrador_id) REFERENCES usuario (id),
    CONSTRAINT ck_escenario_latitud       CHECK (latitud BETWEEN -90 AND 90),
    CONSTRAINT ck_escenario_longitud      CHECK (longitud BETWEEN -180 AND 180),
    CONSTRAINT ck_escenario_correo        CHECK (correo_contacto IS NULL OR correo_contacto ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    CONSTRAINT ck_escenario_estado        CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);
CREATE INDEX ix_escenario_ubicacion     ON escenario USING GIST (ubicacion);
CREATE INDEX ix_escenario_ciudad        ON escenario (ciudad_id);
CREATE INDEX ix_escenario_administrador ON escenario (administrador_id);

-- D13: hasta 5 fotos por escenario. La posición 1 a 5 y su unicidad garantizan el máximo.
CREATE TABLE foto_escenario (
    id           UUID          NOT NULL,
    escenario_id UUID          NOT NULL,
    url          VARCHAR(500)  NOT NULL,
    orden        SMALLINT      NOT NULL,
    CONSTRAINT pk_foto_escenario           PRIMARY KEY (id),
    CONSTRAINT fk_foto_escenario_escenario FOREIGN KEY (escenario_id) REFERENCES escenario (id),
    CONSTRAINT uq_foto_escenario_orden     UNIQUE (escenario_id, orden),
    CONSTRAINT ck_foto_escenario_orden     CHECK (orden BETWEEN 1 AND 5)
);

CREATE TABLE cancha (
    id             UUID            NOT NULL,
    escenario_id   UUID            NOT NULL,
    tipo_cancha_id UUID            NOT NULL,
    nombre         VARCHAR(50)     NOT NULL,
    precio_hora    NUMERIC(10, 2)  NOT NULL,
    estado         VARCHAR(10)     NOT NULL DEFAULT 'DISPONIBLE',
    CONSTRAINT pk_cancha                  PRIMARY KEY (id),
    CONSTRAINT fk_cancha_escenario        FOREIGN KEY (escenario_id)   REFERENCES escenario (id),
    CONSTRAINT fk_cancha_tipo_cancha      FOREIGN KEY (tipo_cancha_id) REFERENCES tipo_cancha (id),
    CONSTRAINT uq_cancha_escenario_nombre UNIQUE (escenario_id, nombre),
    CONSTRAINT ck_cancha_precio_hora      CHECK (precio_hora > 0),
    CONSTRAINT ck_cancha_estado           CHECK (estado IN ('DISPONIBLE', 'INACTIVA'))
);
CREATE INDEX ix_cancha_tipo_cancha ON cancha (tipo_cancha_id);

CREATE TABLE horario_recurrente (
    id           UUID        NOT NULL,
    escenario_id UUID        NOT NULL,
    dia_semana   VARCHAR(9)  NOT NULL,
    hora_inicio  TIME        NOT NULL,
    hora_fin     TIME        NOT NULL,
    -- rango de horas [inicio, fin) que usa la regla de "no se cruzan"
    rango_horas  INT4RANGE GENERATED ALWAYS AS (
                     int4range(EXTRACT(HOUR FROM hora_inicio)::INTEGER, EXTRACT(HOUR FROM hora_fin)::INTEGER, '[)')
                 ) STORED,
    CONSTRAINT pk_horario_recurrente            PRIMARY KEY (id),
    CONSTRAINT fk_horario_recurrente_escenario  FOREIGN KEY (escenario_id) REFERENCES escenario (id),
    CONSTRAINT ck_horario_recurrente_dia_semana CHECK (dia_semana IN ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO')),
    -- D10: horas en punto
    CONSTRAINT ck_horario_recurrente_en_punto   CHECK (EXTRACT(MINUTE FROM hora_inicio) = 0 AND EXTRACT(SECOND FROM hora_inicio) = 0
                                                   AND EXTRACT(MINUTE FROM hora_fin) = 0 AND EXTRACT(SECOND FROM hora_fin) = 0),
    CONSTRAINT ck_horario_recurrente_orden      CHECK (hora_fin > hora_inicio),
    -- dos horarios del mismo escenario y día no se pueden cruzar
    CONSTRAINT ex_horario_recurrente_sin_cruce  EXCLUDE USING GIST (escenario_id WITH =, dia_semana WITH =, rango_horas WITH &&)
);

CREATE TABLE bloqueo (
    id            UUID          NOT NULL,
    cancha_id     UUID          NOT NULL,
    fecha         DATE          NOT NULL,
    hora_inicio   TIME          NOT NULL,
    hora_fin      TIME          NOT NULL,
    rango         TSRANGE GENERATED ALWAYS AS (tsrange(fecha + hora_inicio, fecha + hora_fin, '[)')) STORED,
    tipo_bloqueo  VARCHAR(20)   NOT NULL,
    motivo        VARCHAR(200),
    creado_por_id UUID          NOT NULL,
    CONSTRAINT pk_bloqueo            PRIMARY KEY (id),
    CONSTRAINT fk_bloqueo_cancha     FOREIGN KEY (cancha_id)     REFERENCES cancha (id),
    CONSTRAINT fk_bloqueo_creado_por FOREIGN KEY (creado_por_id) REFERENCES usuario (id),
    CONSTRAINT ck_bloqueo_tipo       CHECK (tipo_bloqueo IN ('MANTENIMIENTO', 'TORNEO', 'USO_PROPIO', 'RESERVA_PRESENCIAL')),
    -- D18: horas en punto; D23: un solo día
    CONSTRAINT ck_bloqueo_en_punto   CHECK (EXTRACT(MINUTE FROM hora_inicio) = 0 AND EXTRACT(SECOND FROM hora_inicio) = 0
                                        AND EXTRACT(MINUTE FROM hora_fin) = 0 AND EXTRACT(SECOND FROM hora_fin) = 0),
    CONSTRAINT ck_bloqueo_orden      CHECK (hora_fin > hora_inicio),
    -- dos bloqueos de la misma cancha no se pueden cruzar
    CONSTRAINT ex_bloqueo_sin_cruce  EXCLUDE USING GIST (cancha_id WITH =, rango WITH &&)
);

CREATE TABLE encargado_escenario (
    id               UUID         NOT NULL,
    escenario_id     UUID         NOT NULL,
    usuario_id       UUID         NOT NULL,
    fecha_asignacion TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_encargado_escenario           PRIMARY KEY (id),
    CONSTRAINT fk_encargado_escenario_escenario FOREIGN KEY (escenario_id) REFERENCES escenario (id),
    CONSTRAINT fk_encargado_escenario_usuario   FOREIGN KEY (usuario_id)   REFERENCES usuario (id),
    CONSTRAINT uq_encargado_escenario_usuario   UNIQUE (escenario_id, usuario_id)
);
CREATE INDEX ix_encargado_escenario_usuario ON encargado_escenario (usuario_id);

-- ---------------------------------------------------------------------
-- Contexto Reservas
-- ---------------------------------------------------------------------
CREATE TABLE reserva (
    id                      UUID            NOT NULL,
    cancha_id               UUID            NOT NULL,
    jugador_id              UUID            NOT NULL,
    fecha                   DATE            NOT NULL,
    hora_inicio             TIME            NOT NULL,
    hora_fin                TIME            NOT NULL,
    -- D29: una reserva de 1 o 2 franjas se guarda como un solo rango de tiempo
    rango                   TSRANGE GENERATED ALWAYS AS (tsrange(fecha + hora_inicio, fecha + hora_fin, '[)')) STORED,
    precio                  NUMERIC(10, 2)  NOT NULL,
    estado                  VARCHAR(10)     NOT NULL DEFAULT 'CONFIRMADA',
    confirmacion_asistencia VARCHAR(12)     NOT NULL DEFAULT 'PENDIENTE',
    -- D34: fecha en que se envió la solicitud de confirmación; sin ella la tarea no cancela la reserva
    fecha_solicitud_confirmacion TIMESTAMPTZ,
    origen_cancelacion      VARCHAR(25),
    motivo_cancelacion      VARCHAR(250),
    fecha_creacion          TIMESTAMPTZ     NOT NULL,
    fecha_cancelacion       TIMESTAMPTZ,
    CONSTRAINT pk_reserva                    PRIMARY KEY (id),
    CONSTRAINT fk_reserva_cancha             FOREIGN KEY (cancha_id)  REFERENCES cancha (id),
    CONSTRAINT fk_reserva_jugador            FOREIGN KEY (jugador_id) REFERENCES usuario (id),
    -- D10: horas en punto y 1 o 2 franjas de una hora
    CONSTRAINT ck_reserva_en_punto           CHECK (EXTRACT(MINUTE FROM hora_inicio) = 0 AND EXTRACT(SECOND FROM hora_inicio) = 0
                                                AND EXTRACT(MINUTE FROM hora_fin) = 0 AND EXTRACT(SECOND FROM hora_fin) = 0),
    CONSTRAINT ck_reserva_duracion           CHECK (hora_fin - hora_inicio IN (INTERVAL '1 hour', INTERVAL '2 hours')),
    CONSTRAINT ck_reserva_precio             CHECK (precio > 0),
    CONSTRAINT ck_reserva_estado             CHECK (estado IN ('CONFIRMADA', 'CANCELADA', 'NO_ASISTIO')),
    CONSTRAINT ck_reserva_confirmacion       CHECK (confirmacion_asistencia IN ('PENDIENTE', 'CONFIRMADA', 'NO_REQUERIDA')),
    CONSTRAINT ck_reserva_origen             CHECK (origen_cancelacion IN ('JUGADOR', 'ADMINISTRADOR', 'SISTEMA_SIN_CONFIRMACION')),
    -- origen y fecha de cancelación existen si y solo si la reserva está cancelada (ESC-AUD-01)
    CONSTRAINT ck_reserva_cancelacion        CHECK ((estado = 'CANCELADA') = (origen_cancelacion IS NOT NULL AND fecha_cancelacion IS NOT NULL)),
    -- RN-07: la cancelación por fuerza mayor exige justificación
    CONSTRAINT ck_reserva_motivo_admin       CHECK (origen_cancelacion IS DISTINCT FROM 'ADMINISTRADOR' OR motivo_cancelacion IS NOT NULL),
    -- RT-03 / D29: dos reservas confirmadas de la misma cancha no se pueden cruzar
    CONSTRAINT ex_reserva_sin_cruce          EXCLUDE USING GIST (cancha_id WITH =, rango WITH &&) WHERE (estado = 'CONFIRMADA')
);
CREATE INDEX ix_reserva_cancha_fecha  ON reserva (cancha_id, fecha);
CREATE INDEX ix_reserva_jugador_fecha ON reserva (jugador_id, fecha);
-- tarea programada: reservas confirmadas con asistencia pendiente (D06, AC-02.2)
CREATE INDEX ix_reserva_pendientes    ON reserva (fecha, hora_inicio)
    WHERE estado = 'CONFIRMADA' AND confirmacion_asistencia = 'PENDIENTE';

-- ---------------------------------------------------------------------
-- Contexto Reputación
-- ---------------------------------------------------------------------
-- D05: la calificación promedio no se guarda; se calcula al consultar.
CREATE TABLE resena (
    id             UUID          NOT NULL,
    reserva_id     UUID          NOT NULL,
    calificacion   SMALLINT      NOT NULL,
    comentario     VARCHAR(500),
    fecha_creacion TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_resena              PRIMARY KEY (id),
    CONSTRAINT fk_resena_reserva      FOREIGN KEY (reserva_id) REFERENCES reserva (id),
    CONSTRAINT uq_resena_reserva      UNIQUE (reserva_id),
    CONSTRAINT ck_resena_calificacion CHECK (calificacion BETWEEN 1 AND 5)
);
