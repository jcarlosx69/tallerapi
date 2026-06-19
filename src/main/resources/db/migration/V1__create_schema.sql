-- =============================================================
-- V1__create_schema.sql
-- TallerAPI - Esquema inicial (Fase 1: Diseño y base)
--
-- Crea las 5 tablas del modelo de datos descrito en el documento de
-- requisitos: clientes, materiales, proyectos, proyecto_materiales y
-- usuarios. Incluye claves primarias, foráneas, restricciones NOT
-- NULL/UNIQUE y CHECK donde aplica (p.ej. stock_disponible >= 0).
-- =============================================================

-- -------------------------------------------------------------
-- Tabla: clientes
-- -------------------------------------------------------------
CREATE TABLE clientes (
                          id        BIGSERIAL PRIMARY KEY,
                          nombre    VARCHAR(100) NOT NULL,
                          email     VARCHAR(150) UNIQUE,
                          telefono  VARCHAR(20)
);

-- -------------------------------------------------------------
-- Tabla: materiales
-- -------------------------------------------------------------
CREATE TABLE materiales (
                            id                BIGSERIAL PRIMARY KEY,
                            nombre            VARCHAR(100)   NOT NULL,
                            tipo              VARCHAR(20)    NOT NULL,
                            unidad            VARCHAR(20)    NOT NULL,
                            stock_disponible  NUMERIC(12,3)  NOT NULL,
                            coste_unitario    NUMERIC(12,2)  NOT NULL,

                            CONSTRAINT chk_materiales_tipo
                                CHECK (tipo IN ('MADERA', 'HERRAJE', 'BARNIZ', 'OTRO')),

                            CONSTRAINT chk_materiales_unidad
                                CHECK (unidad IN ('UNIDAD', 'METRO', 'KG', 'LITRO')),

                            CONSTRAINT chk_materiales_stock_no_negativo
                                CHECK (stock_disponible >= 0),

                            CONSTRAINT chk_materiales_coste_no_negativo
                                CHECK (coste_unitario >= 0)
);

-- -------------------------------------------------------------
-- Tabla: proyectos
-- -------------------------------------------------------------
CREATE TABLE proyectos (
                           id                      BIGSERIAL PRIMARY KEY,
                           nombre                  VARCHAR(150)   NOT NULL,
                           cliente_id              BIGINT         NOT NULL,
                           fecha_inicio            DATE           NOT NULL,
                           fecha_entrega_prevista  DATE,
                           estado                  VARCHAR(20)    NOT NULL,
                           presupuesto             NUMERIC(12,2),

                           CONSTRAINT fk_proyectos_cliente
                               FOREIGN KEY (cliente_id) REFERENCES clientes (id),

                           CONSTRAINT chk_proyectos_estado
                               CHECK (estado IN ('EN_CURSO', 'TERMINADO', 'ENTREGADO', 'CANCELADO')),

                           CONSTRAINT chk_proyectos_presupuesto_no_negativo
                               CHECK (presupuesto IS NULL OR presupuesto >= 0),

    -- RN-06: si se especifica fecha_entrega_prevista, debe ser >= fecha_inicio
                           CONSTRAINT chk_proyectos_fecha_entrega_posterior
                               CHECK (fecha_entrega_prevista IS NULL OR fecha_entrega_prevista >= fecha_inicio)
);

-- Índice para acelerar el filtro por estado (GET /proyectos?estado=...)
CREATE INDEX idx_proyectos_estado ON proyectos (estado);

-- Índice para acelerar el filtro/ordenación por rango de fechas
CREATE INDEX idx_proyectos_fecha_inicio ON proyectos (fecha_inicio);

-- -------------------------------------------------------------
-- Tabla: proyecto_materiales (relación N:M con atributos propios)
-- -------------------------------------------------------------
CREATE TABLE proyecto_materiales (
                                     id                  BIGSERIAL PRIMARY KEY,
                                     proyecto_id         BIGINT          NOT NULL,
                                     material_id         BIGINT          NOT NULL,
                                     cantidad_asignada   NUMERIC(12,3)   NOT NULL,
                                     fecha_asignacion    TIMESTAMP       NOT NULL DEFAULT now(),

                                     CONSTRAINT fk_proyecto_materiales_proyecto
                                         FOREIGN KEY (proyecto_id) REFERENCES proyectos (id),

                                     CONSTRAINT fk_proyecto_materiales_material
                                         FOREIGN KEY (material_id) REFERENCES materiales (id),

                                     CONSTRAINT chk_proyecto_materiales_cantidad_positiva
                                         CHECK (cantidad_asignada > 0)
);

-- Índices para las consultas habituales: materiales de un proyecto,
-- y asignaciones activas de un material (necesario para RN-04)
CREATE INDEX idx_proyecto_materiales_proyecto ON proyecto_materiales (proyecto_id);
CREATE INDEX idx_proyecto_materiales_material ON proyecto_materiales (material_id);

-- -------------------------------------------------------------
-- Tabla: usuarios (autenticación)
-- -------------------------------------------------------------
CREATE TABLE usuarios (
                          id             BIGSERIAL PRIMARY KEY,
                          username       VARCHAR(50)   NOT NULL UNIQUE,
                          password_hash  VARCHAR(255)  NOT NULL,
                          rol            VARCHAR(20)   NOT NULL,

                          CONSTRAINT chk_usuarios_rol
                              CHECK (rol IN ('ADMIN', 'USER'))
);