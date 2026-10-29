-- ========================================================
-- Migracion de auditoria (aplicar sobre bases existentes)
-- 1. Crea la tabla bitacora si no existe (A7)
-- 2. Permite el tipo de movimiento CANCELACION (RF15)
-- Es idempotente: puede ejecutarse mas de una vez.
-- ========================================================

-- 1. Bitacora de auditoria
CREATE TABLE IF NOT EXISTS bitacora (
    id SERIAL PRIMARY KEY,
    usuario_id INT REFERENCES usuario(id),
    accion VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_bitacora_usuario_id ON bitacora(usuario_id);
CREATE INDEX IF NOT EXISTS idx_bitacora_fecha_hora ON bitacora(fecha_hora);

-- 2. Tipo CANCELACION para reintegros por cancelacion de venta
ALTER TABLE movimiento_inventario
    DROP CONSTRAINT IF EXISTS movimiento_inventario_tipo_check;

ALTER TABLE movimiento_inventario
    ADD CONSTRAINT movimiento_inventario_tipo_check
    CHECK (tipo IN ('RECEPCION', 'VENTA', 'AJUSTE', 'DEVOLUCION', 'CANCELACION'));
