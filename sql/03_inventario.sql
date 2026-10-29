-- ========================================================
-- Schema y Datos: Inventario y Caducidades (Farmac Ciclo 2)
-- RF07, RF08, RF09, RF10, RF19
-- ========================================================

-- Tabla: lote
CREATE TABLE IF NOT EXISTS lote (
    id SERIAL PRIMARY KEY,
    producto_id INT NOT NULL REFERENCES producto(id),
    numero_lote VARCHAR(50) NOT NULL,
    fecha_caducidad DATE NOT NULL,
    cantidad_recibida INT NOT NULL CHECK (cantidad_recibida > 0),
    cantidad_actual INT NOT NULL CHECK (cantidad_actual >= 0),
    precio_compra NUMERIC(10,2) NOT NULL CHECK (precio_compra > 0),
    proveedor_id INT NOT NULL REFERENCES proveedor(id),
    fecha_recepcion DATE NOT NULL DEFAULT CURRENT_DATE,
    referencia_factura VARCHAR(100),
    CONSTRAINT uk_producto_lote UNIQUE (producto_id, numero_lote)
);

-- Tabla: movimiento_inventario
CREATE TABLE IF NOT EXISTS movimiento_inventario (
    id SERIAL PRIMARY KEY,
    lote_id INT NOT NULL REFERENCES lote(id),
    tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('RECEPCION', 'VENTA', 'AJUSTE', 'DEVOLUCION', 'CANCELACION')),
    cantidad INT NOT NULL, -- con signo (+ entrada, - salida)
    motivo VARCHAR(255) NOT NULL,
    referencia_id VARCHAR(100),
    usuario_id INT NOT NULL REFERENCES usuario(id),
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices solicitados
CREATE INDEX IF NOT EXISTS idx_lote_fecha_caducidad ON lote(fecha_caducidad);
CREATE INDEX IF NOT EXISTS idx_lote_producto_id ON lote(producto_id);

-- Datos de ejemplo: lotes vigentes, por vencer (<= 60 días) y vencidos.
-- Se resuelven producto y proveedor por nombre/código para no depender de IDs fijos.
INSERT INTO lote (producto_id, numero_lote, fecha_caducidad, cantidad_recibida, cantidad_actual,
                  precio_compra, proveedor_id, fecha_recepcion, referencia_factura)
SELECT p.id, v.numero_lote, CURRENT_DATE + v.dias_caducidad, v.cantidad_recibida, v.cantidad_actual,
       v.precio_compra, pr.id, CURRENT_DATE + v.dias_recepcion, v.referencia_factura
FROM (VALUES
    -- Vigentes (> 60 días de caducidad)
    ('75010001', 'LOTE-PAR-2027A', 300, 100, 85, 20.00, 'Distribuidora Farmacéutica Nacional S.A.', -10, 'FACT-00123'),
    ('75010002', 'LOTE-IBU-2027B', 250, 50, 42, 28.50, 'Distribuidora Farmacéutica Nacional S.A.', -5, 'FACT-00124'),
    -- Por vencer (<= 60 días)
    ('75010001', 'LOTE-PAR-PV01', 25, 40, 15, 18.00, 'Distribuidora Farmacéutica Nacional S.A.', -60, 'FACT-00098'),
    ('75010003', 'LOTE-NAP-PV02', 40, 30, 12, 35.00, 'Laboratorios Médicos del Centro', -70, 'FACT-00085'),
    -- Vencidos (caducidad pasada)
    ('75010001', 'LOTE-PAR-VENC01', -15, 25, 8, 17.50, 'Distribuidora Farmacéutica Nacional S.A.', -180, 'FACT-00050'),
    ('75010002', 'LOTE-IBU-VENC02', -5, 20, 5, 25.00, 'Distribuidora Farmacéutica Nacional S.A.', -160, 'FACT-00055')
) AS v(codigo_barras, numero_lote, dias_caducidad, cantidad_recibida, cantidad_actual,
       precio_compra, proveedor, dias_recepcion, referencia_factura)
JOIN producto p ON p.codigo_barras = v.codigo_barras
JOIN proveedor pr ON pr.razon_social = v.proveedor
ON CONFLICT (producto_id, numero_lote) DO NOTHING;

-- Movimientos de recepción correspondientes (idempotente)
INSERT INTO movimiento_inventario (lote_id, tipo, cantidad, motivo, referencia_id, usuario_id, fecha_hora)
SELECT l.id, 'RECEPCION', l.cantidad_recibida, 'Recepción inicial de mercancía',
       l.referencia_factura, u.id, CURRENT_TIMESTAMP
FROM lote l
JOIN usuario u ON u.nombre_usuario = 'admin'
WHERE NOT EXISTS (
    SELECT 1 FROM movimiento_inventario mi WHERE mi.lote_id = l.id AND mi.tipo = 'RECEPCION'
);
