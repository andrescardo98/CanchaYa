-- HU6: el nombre de la cancha no se repite en el escenario sin importar mayúsculas y
-- minúsculas («Cancha 1» y «cancha 1» son el mismo). La restricción de la V1 sí las
-- distinguía. Se cambia por un índice único sobre lower(nombre) con el mismo nombre, así
-- la base rechaza el repetido aunque dos solicitudes lleguen al mismo tiempo.
ALTER TABLE cancha DROP CONSTRAINT uq_cancha_escenario_nombre;

CREATE UNIQUE INDEX uq_cancha_escenario_nombre ON cancha (escenario_id, lower(nombre));
