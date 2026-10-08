-- ========================================================
-- Schema y Datos: Ventas, Caja y Recetas (Farmac Ciclo 3)
-- RF12, RF13, RF14, RF15, RF16
-- ========================================================

-- Tabla: turno_caja
CREATE TABLE IF NOT EXISTS turno_caja (
    id SERIAL PRIMARY KEY,
    usuario_id INT NOT NULL REFERENCES usuario(id),
    fecha_apertura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    monto_inicial NUMERIC(10,2) NOT NULL CHECK (monto_inicial >= 0),
    fecha_cierre TIMESTAMP,
    efectivo_contado NUMERIC(10,2),
    monto_esperado NUMERIC(10,2),
    diferencia NUMERIC(10,2),
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO', 'CERRADO'))
);

-- Índice único parcial: un solo turno ABIERTO a la vez
CREATE UNIQUE INDEX IF NOT EXISTS idx_unico_turno_abierto
ON turno_caja (estado)
WHERE estado = 'ABIERTO';

-- Tabla: venta
CREATE TABLE IF NOT EXISTS venta (
    id SERIAL PRIMARY KEY,
    turno_id INT NOT NULL REFERENCES turno_caja(id),
    usuario_id INT NOT NULL REFERENCES usuario(id),
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total NUMERIC(10,2) NOT NULL CHECK (total > 0),
    forma_pago VARCHAR(20) NOT NULL CHECK (forma_pago IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA')),
    estado VARCHAR(20) NOT NULL DEFAULT 'COMPLETADA' CHECK (estado IN ('COMPLETADA', 'CANCELADA')),
    motivo_cancelacion VARCHAR(255),
    cancelada_en TIMESTAMP
);

-- Tabla: detalle_venta
CREATE TABLE IF NOT EXISTS detalle_venta (
    id SERIAL PRIMARY KEY,
    venta_id INT NOT NULL REFERENCES venta(id) ON DELETE CASCADE,
    producto_id INT NOT NULL REFERENCES producto(id),
    lote_id INT NOT NULL REFERENCES lote(id),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(10,2) NOT NULL CHECK (precio_unitario > 0),
    subtotal NUMERIC(10,2) NOT NULL CHECK (subtotal > 0)
);

-- Tabla: receta_venta (Las recetas no se borran)
CREATE TABLE IF NOT EXISTS receta_venta (
    id SERIAL PRIMARY KEY,
    venta_id INT NOT NULL REFERENCES venta(id),
    producto_id INT NOT NULL REFERENCES producto(id),
    nombre_medico VARCHAR(150) NOT NULL,
    cedula_profesional VARCHAR(50) NOT NULL,
    fecha_expedicion DATE NOT NULL,
    folio_receta VARCHAR(50) NOT NULL
);

-- Índices adicionales para rendimiento
CREATE INDEX IF NOT EXISTS idx_venta_turno_id ON venta(turno_id);
CREATE INDEX IF NOT EXISTS idx_detalle_venta_venta_id ON detalle_venta(venta_id);
CREATE INDEX IF NOT EXISTS idx_receta_venta_venta_id ON receta_venta(venta_id);
