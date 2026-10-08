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
    tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('RECEPCION', 'VENTA', 'AJUSTE', 'DEVOLUCION')),
    cantidad INT NOT NULL, -- con signo (+ entrada, - salida)
    motivo VARCHAR(255) NOT NULL,
    referencia_id VARCHAR(100),
    usuario_id INT NOT NULL REFERENCES usuario(id),
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices solicitados
CREATE INDEX IF NOT EXISTS idx_lote_fecha_caducidad ON lote(fecha_caducidad);
CREATE INDEX IF NOT EXISTS idx_lote_producto_id ON lote(producto_id);

-- Datos de ejemplo: lotes vigentes, por vencer (<= 60 días) y vencidos
-- Referenciando productos existentes 1 (Paracetamol) y 2 (Ibuprofeno)
INSERT INTO lote (producto_id, numero_lote, fecha_caducidad, cantidad_recibida, cantidad_actual, precio_compra, proveedor_id, fecha_recepcion, referencia_factura) VALUES
-- Vigentes (> 60 días de caducidad)
(1, 'LOTE-PAR-2027A', CURRENT_DATE + INTERVAL '300 days', 100, 85, 20.00, 1, CURRENT_DATE - INTERVAL '10 days', 'FACT-00123'),
(2, 'LOTE-IBU-2027B', CURRENT_DATE + INTERVAL '250 days', 50, 42, 28.50, 1, CURRENT_DATE - INTERVAL '5 days', 'FACT-00124'),

-- Por vencer (<= 60 días)
(1, 'LOTE-PAR-PV01', CURRENT_DATE + INTERVAL '25 days', 40, 15, 18.00, 1, CURRENT_DATE - INTERVAL '60 days', 'FACT-00098'),
(3, 'LOTE-NAP-PV02', CURRENT_DATE + INTERVAL '40 days', 30, 12, 35.00, 2, CURRENT_DATE - INTERVAL '70 days', 'FACT-00085'),

-- Vencidos (caducidad pasada)
(1, 'LOTE-PAR-VENC01', CURRENT_DATE - INTERVAL '15 days', 25, 8, 17.50, 1, CURRENT_DATE - INTERVAL '180 days', 'FACT-00050'),
(2, 'LOTE-IBU-VENC02', CURRENT_DATE - INTERVAL '5 days', 20, 5, 25.00, 1, CURRENT_DATE - INTERVAL '160 days', 'FACT-00055')
ON CONFLICT (producto_id, numero_lote) DO NOTHING;

-- Movimientos de recepción correspondientes
INSERT INTO movimiento_inventario (lote_id, tipo, cantidad, motivo, referencia_id, usuario_id, fecha_hora)
SELECT id, 'RECEPCION', cantidad_recibida, 'Recepción inicial de mercancía', referencia_factura, 1, CURRENT_TIMESTAMP
FROM lote
WHERE NOT EXISTS (
    SELECT 1 FROM movimiento_inventario mi WHERE mi.lote_id = lote.id AND mi.tipo = 'RECEPCION'
);
