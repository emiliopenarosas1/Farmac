-- ========================================================
-- Datos de prueba integrales para Farmac
-- Cubre las 11 tablas y todos los modulos de la UI:
-- usuarios, categorias, proveedores, productos (activos e
-- inactivos), lotes (vigentes, por vencer, vencidos y
-- agotados), movimientos, turnos (cuadrados, con sobrante y
-- con faltante), ventas (completadas y canceladas), detalles,
-- recetas (vigentes y vencidas) y bitacora.
--
-- Es idempotente: usa un rango de IDs reservado (>= 100/1000)
-- y borra solo esos registros antes de volver a insertarlos.
-- No toca los datos reales (IDs bajos).
--
-- Uso:
--   psql -U postgres -d farmac_db -f sql/08_datos_prueba.sql
--
-- Usuarios nuevos (password):
--   encargado2 / admin123
--   empleado2  / caja123
--   empleado3  / caja123
--   empleado_baja / caja123 (inactivo)
-- ========================================================
\set ON_ERROR_STOP on

BEGIN;

-- --------------------------------------------------------
-- 0) Limpieza de la corrida anterior (solo rango de prueba)
-- --------------------------------------------------------
DELETE FROM movimiento_inventario WHERE lote_id >= 1000 OR motivo LIKE '%[demo]%';
DELETE FROM receta_venta          WHERE venta_id >= 1000;
DELETE FROM detalle_venta         WHERE venta_id >= 1000;
DELETE FROM venta                 WHERE id >= 1000;
DELETE FROM bitacora              WHERE id >= 1000 OR usuario_id >= 100;
DELETE FROM turno_caja            WHERE id >= 1000;
DELETE FROM lote                  WHERE id >= 1000;
DELETE FROM producto              WHERE id >= 1000;
DELETE FROM proveedor             WHERE id >= 100;
DELETE FROM categoria             WHERE id >= 100;
DELETE FROM usuario               WHERE id >= 100;

-- --------------------------------------------------------
-- 1) Usuarios
-- --------------------------------------------------------
INSERT INTO usuario (id, nombre_usuario, nombre_completo, password_hash, rol, activo) VALUES
(101, 'encargado2',   'María Fernanda Ríos (Encargado)',  '$2a$10$fm2dGYB.bd80nsqMQ00waePHAiyCiZdOj8RlXKOVT3uzR7N/SX5/O', 'ENCARGADO', TRUE),
(102, 'empleado2',    'Jorge Luis Peña (Empleado)',       '$2a$10$0UvIkCceCpYYbajeb42PVO8NyJznYD/lw1PpgcMOlJhdmpISMnjym', 'EMPLEADO',  TRUE),
(103, 'empleado3',    'Ana Sofía Torres (Empleado)',      '$2a$10$0UvIkCceCpYYbajeb42PVO8NyJznYD/lw1PpgcMOlJhdmpISMnjym', 'EMPLEADO',  TRUE),
(104, 'empleado_baja','Ricardo Núñez (Empleado inactivo)','$2a$10$0UvIkCceCpYYbajeb42PVO8NyJznYD/lw1PpgcMOlJhdmpISMnjym', 'EMPLEADO',  FALSE);

-- --------------------------------------------------------
-- 2) Categorias
-- --------------------------------------------------------
INSERT INTO categoria (id, nombre) VALUES
(100, 'Dermatología y Alergias'),
(101, 'Vitaminas y Suplementos'),
(102, 'Pediatría'),
(103, 'Material de Curación'),
(104, 'Endocrinología y Diabetes'),
(105, 'Respiratorio');

-- --------------------------------------------------------
-- 3) Proveedores
-- --------------------------------------------------------
INSERT INTO proveedor (id, razon_social, contacto, telefono, activo) VALUES
(100, 'Farmacéutica del Pacífico S.A. de C.V.', 'Daniela Castro',   '555-0201', TRUE),
(101, 'BioLab Distribuciones',                  'Héctor Villanueva','555-0202', TRUE),
(102, 'Insumos Médicos del Norte',              'Sergio Lozano',    '555-0203', FALSE);

-- --------------------------------------------------------
-- 4) Productos
-- --------------------------------------------------------
INSERT INTO producto (id, codigo_barras, nombre, categoria_id, proveedor_id, precio_venta, requiere_receta, stock_minimo, activo) VALUES
(1000, '75020001', 'Loratadina 10mg (10 tabletas)',             100, 100,  45.00, FALSE, 10, TRUE),
(1001, '75020002', 'Clotrimazol Crema 1% 20g',                  100, 100,  68.00, FALSE,  6, TRUE),
(1002, '75020003', 'Betametasona Crema 0.05% 20g',              100, 101,  89.00, TRUE,   5, TRUE),
(1003, '75020004', 'Vitamina C 500mg (30 tabletas)',            101, 101,  55.00, FALSE, 15, TRUE),
(1004, '75020005', 'Complejo B (30 cápsulas)',                  101, 101,  78.00, FALSE, 10, TRUE),
(1005, '75020006', 'Vitamina D3 400UI Frasco 30ml',             101, 100,  96.00, FALSE,  8, TRUE),
(1006, '75020007', 'Paracetamol Jarabe Infantil 120ml',         102, 100,  52.00, FALSE, 12, TRUE),
(1007, '75020008', 'Amoxicilina Suspensión 250mg/5ml',          102, 100,  98.00, TRUE,   8, TRUE),
(1008, '75020009', 'Suero Oral Pediátrico 500ml',               102, 101,  28.00, FALSE, 20, TRUE),
(1009, '75020010', 'Jeringa Desechable 5ml (10 piezas)',        103, 102,  35.00, FALSE, 25, TRUE),
(1010, '75020011', 'Guantes de Nitrilo Talla M (100 piezas)',   103, 102, 180.00, FALSE,  5, TRUE),
(1011, '75020012', 'Metformina 850mg (30 tabletas)',            104, 100,  92.00, TRUE,  10, TRUE),
(1012, '75020013', 'Salbutamol Inhalador 100mcg',               105, 101, 145.00, TRUE,   6, TRUE),
(1013, '75020014', 'Producto Descontinuado 100mg',              100, 100,  40.00, FALSE,  5, FALSE);

-- --------------------------------------------------------
-- 5) Lotes
--    IDs 1000-1032: un lote vigente por producto vendido.
--    IDs 1033-1039: casos especiales (por vencer, vencido,
--    agotado, producto nuevo e inactivo).
-- --------------------------------------------------------
INSERT INTO lote (id, producto_id, numero_lote, fecha_caducidad, cantidad_recibida, cantidad_actual,
                  precio_compra, proveedor_id, fecha_recepcion, referencia_factura)
SELECT v.id, p.id, v.numero_lote, CURRENT_DATE + v.dias_cad, v.recibida, v.actual,
       v.precio, v.prov, CURRENT_DATE - v.dias_rec, v.factura
FROM (VALUES
    (1000, '75010001', 'LOTE-DEMO-PAR-2027',   420, 120, 120, 19.00, 100,  8, 'FACT-DEMO-1000'),
    (1001, '75010014', 'LOTE-DEMO-OME-2027',   300,  60,  60, 22.00, 101,  9, 'FACT-DEMO-1001'),
    (1002, '75010008', 'LOTE-DEMO-AMOX-2027',  260,  30,  30, 52.00, 100, 10, 'FACT-DEMO-1002'),
    (1003, '75020004', 'LOTE-DEMO-VITC-2028',  500,  80,  80, 28.00, 101,  3, 'FACT-DEMO-1003'),
    (1004, '75020007', 'LOTE-DEMO-PJAR-2027',  270,  40,  40, 30.00, 100, 12, 'FACT-DEMO-1004'),
    (1005, '75010007', 'LOTE-DEMO-CELE-2027',  330,  30,  30, 80.00, 101,  6, 'FACT-DEMO-1005'),
    (1006, '75010002', 'LOTE-DEMO-IBU-2027',   380,  60,  60, 26.00, 100,  7, 'FACT-DEMO-1006'),
    (1007, '75010020', 'LOTE-DEMO-LOS-2027',   350,  40,  40, 42.00, 100, 11, 'FACT-DEMO-1007'),
    (1008, '75010021', 'LOTE-DEMO-ENA-2027',   340,  35,  35, 38.00, 100, 11, 'FACT-DEMO-1008'),
    (1009, '75020012', 'LOTE-DEMO-MET-2027',   320,  30,  30, 55.00, 100, 13, 'FACT-DEMO-1009'),
    (1010, '75010026', 'LOTE-DEMO-ALC-2029',   800,  10,  10,120.00, 101, 30, 'FACT-DEMO-1010'),
    (1011, '75020003', 'LOTE-DEMO-BET-2027',   240,  25,  25, 52.00, 101, 15, 'FACT-DEMO-1011'),
    (1012, '75020005', 'LOTE-DEMO-COMPB-2027', 290,  30,  30, 45.00, 101, 14, 'FACT-DEMO-1012'),
    (1013, '75010016', 'LOTE-DEMO-RANI-2027',  280,  40,  40, 18.00, 100, 16, 'FACT-DEMO-1013'),
    (1014, '75020011', 'LOTE-DEMO-GUA-2028',   450,  15,  15,110.00, 102, 45, 'FACT-DEMO-1014'),
    (1015, '75020010', 'LOTE-DEMO-JER-2028',   480,  80,  80, 20.00, 102, 40, 'FACT-DEMO-1015'),
    (1016, '75010006', 'LOTE-DEMO-ASP-2027',   310,  45,  45, 55.00, 100, 18, 'FACT-DEMO-1016'),
    (1017, '75010015', 'LOTE-DEMO-PANTO-2027', 300,  40,  40, 65.00, 100, 19, 'FACT-DEMO-1017'),
    (1018, '75010009', 'LOTE-DEMO-AZI-2027',   250,  30,  30, 70.00, 100, 20, 'FACT-DEMO-1018'),
    (1019, '75010012', 'LOTE-DEMO-ACI-2027',   260,  25,  25, 95.00, 101, 21, 'FACT-DEMO-1019'),
    (1020, '75010025', 'LOTE-DEMO-HCTZ-2027',  330,  30,  30, 78.00, 100, 22, 'FACT-DEMO-1020'),
    (1021, '75010023', 'LOTE-DEMO-METO-2027',  340,  30,  30, 48.00, 100, 23, 'FACT-DEMO-1021'),
    (1022, '75020013', 'LOTE-DEMO-SALB-2027',  170,   2,   2, 90.00, 101, 50, 'FACT-DEMO-1022'),
    (1023, '75020009', 'LOTE-DEMO-SUERO-2028', 400,  60,  60, 15.00, 101, 24, 'FACT-DEMO-1023'),
    (1024, '75010004', 'LOTE-DEMO-KETO-2027',  300,  40,  40, 45.00, 101, 25, 'FACT-DEMO-1024'),
    (1025, '75020008', 'LOTE-DEMO-AMOXS-2027', 220,  30,  30, 60.00, 100, 26, 'FACT-DEMO-1025'),
    (1026, '75010005', 'LOTE-DEMO-DICLO-2027', 350,  30,  30, 48.00, 100, 27, 'FACT-DEMO-1026'),
    (1027, '75020001', 'LOTE-DEMO-LOR-2028',   430,  40,  40, 26.00, 100, 28, 'FACT-DEMO-1027'),
    (1028, '75020002', 'LOTE-DEMO-CLO-2027',   320,  30,  30, 40.00, 100, 29, 'FACT-DEMO-1028'),
    (1029, '75010019', 'LOTE-DEMO-MCP-2027',   300,  35,  35, 32.00, 101, 30, 'FACT-DEMO-1029'),
    (1030, '75010018', 'LOTE-DEMO-LOPE-2027',  310,  40,  40, 14.00, 100, 31, 'FACT-DEMO-1030'),
    (1031, '75010027', 'LOTE-DEMO-GASA-2028',  520,  40,  40, 18.00, 101, 32, 'FACT-DEMO-1031'),
    (1032, '75010028', 'LOTE-DEMO-VENDA-2028', 540,  30,  30, 12.00, 101, 33, 'FACT-DEMO-1032'),
    (1033, '75020006', 'LOTE-DEMO-VITD-2027',  300,  30,  30, 60.00, 100, 34, 'FACT-DEMO-1033'),
    (1034, '75020014', 'LOTE-DEMO-DISC-2027',  200,  10,  10, 25.00, 100,200, 'FACT-DEMO-1034'),
    -- Casos especiales
    (1035, '75020001', 'LOTE-DEMO-LOR-PV',      20,  18,  18, 25.00, 100, 60, 'FACT-DEMO-1035'),
    (1036, '75020004', 'LOTE-DEMO-VITC-PV',     45,  34,  34, 27.00, 101, 70, 'FACT-DEMO-1036'),
    (1037, '75020003', 'LOTE-DEMO-BET-VENC',   -35,  20,   6, 50.00, 101,120, 'FACT-DEMO-1037'),
    (1038, '75020008', 'LOTE-DEMO-AMOXS-AGOT', 150,  20,   0, 58.00, 100, 90, 'FACT-DEMO-1038'),
    (1039, '75010001', 'LOTE-DEMO-PAR-PV',      15,  10,  10, 18.00, 100, 40, 'FACT-DEMO-1039')
) AS v(id, codigo, numero_lote, dias_cad, recibida, actual, precio, prov, dias_rec, factura)
JOIN producto p ON p.codigo_barras = v.codigo;

-- --------------------------------------------------------
-- 6) Turnos de caja cerrados (el turno abierto #2 se conserva)
-- --------------------------------------------------------
INSERT INTO turno_caja (id, usuario_id, fecha_apertura, monto_inicial, estado) VALUES
(1000, 101, TIMESTAMP '2026-10-20 08:00:00', 500.00, 'CERRADO'),
(1001, 102, TIMESTAMP '2026-10-22 08:00:00', 400.00, 'CERRADO'),
(1002, 103, TIMESTAMP '2026-10-25 08:00:00', 600.00, 'CERRADO'),
(1003, 102, TIMESTAMP '2026-10-28 08:00:00', 500.00, 'CERRADO'),
(1004, 101, TIMESTAMP '2026-11-01 08:00:00', 800.00, 'CERRADO'),
(1005, 103, TIMESTAMP '2026-11-03 08:00:00', 450.00, 'CERRADO');

-- --------------------------------------------------------
-- 7) Ventas (completadas y canceladas) + una venta en el
--    turno abierto #2 para probar el punto de venta en vivo.
-- --------------------------------------------------------
INSERT INTO venta (id, turno_id, usuario_id, fecha_hora, total, forma_pago, estado, motivo_cancelacion, cancelada_en) VALUES
(1000, 1000, 101, TIMESTAMP '2026-10-20 09:15:00', 1.00, 'EFECTIVO',      'COMPLETADA', NULL, NULL),
(1001, 1000, 101, TIMESTAMP '2026-10-20 11:40:00', 1.00, 'TARJETA',       'COMPLETADA', NULL, NULL),
(1002, 1000, 101, TIMESTAMP '2026-10-20 13:05:00', 1.00, 'EFECTIVO',      'COMPLETADA', NULL, NULL),
(1003, 1000, 101, TIMESTAMP '2026-10-20 17:30:00', 1.00, 'TRANSFERENCIA', 'CANCELADA', 'Cliente solicitó cancelación por cambio de medicamento', TIMESTAMP '2026-10-20 18:00:00'),
(1004, 1001, 102, TIMESTAMP '2026-10-22 08:50:00', 1.00, 'EFECTIVO',      'COMPLETADA', NULL, NULL),
(1005, 1001, 102, TIMESTAMP '2026-10-22 10:20:00', 1.00, 'TARJETA',       'COMPLETADA', NULL, NULL),
(1006, 1001, 102, TIMESTAMP '2026-10-22 12:45:00', 1.00, 'EFECTIVO',      'COMPLETADA', NULL, NULL),
(1007, 1001, 102, TIMESTAMP '2026-10-22 16:10:00', 1.00, 'EFECTIVO',      'COMPLETADA', NULL, NULL),
(1008, 1002, 103, TIMESTAMP '2026-10-25 09:05:00', 1.00, 'EFECTIVO',      'COMPLETADA', NULL, NULL),
(1009, 1002, 103, TIMESTAMP '2026-10-25 14:30:00', 1.00, 'TRANSFERENCIA', 'COMPLETADA', NULL, NULL),
(1010, 1002, 103, TIMESTAMP '2026-10-25 18:15:00', 1.00, 'TARJETA',       'COMPLETADA', NULL, NULL),
(1011, 1003, 102, TIMESTAMP '2026-10-28 08:30:00', 1.00, 'EFECTIVO',      'COMPLETADA', NULL, NULL),
(1012, 1003, 102, TIMESTAMP '2026-10-28 12:00:00', 1.00, 'EFECTIVO',      'COMPLETADA', NULL, NULL),
(1013, 1003, 102, TIMESTAMP '2026-10-28 15:20:00', 1.00, 'TARJETA',       'CANCELADA', 'Error de captura en el punto de venta', TIMESTAMP '2026-10-28 15:45:00'),
(1014, 1004, 101, TIMESTAMP '2026-11-01 09:10:00', 1.00, 'TARJETA',       'COMPLETADA', NULL, NULL),
(1015, 1004, 101, TIMESTAMP '2026-11-01 11:25:00', 1.00, 'EFECTIVO',      'COMPLETADA', NULL, NULL),
(1016, 1004, 101, TIMESTAMP '2026-11-01 13:40:00', 1.00, 'EFECTIVO',      'COMPLETADA', NULL, NULL),
(1017, 1004, 101, TIMESTAMP '2026-11-01 17:55:00', 1.00, 'TRANSFERENCIA', 'COMPLETADA', NULL, NULL),
(1018, 1005, 103, TIMESTAMP '2026-11-03 08:45:00', 1.00, 'EFECTIVO',      'COMPLETADA', NULL, NULL),
(1019, 1005, 103, TIMESTAMP '2026-11-03 10:30:00', 1.00, 'TARJETA',       'COMPLETADA', NULL, NULL),
(1020, 1005, 103, TIMESTAMP '2026-11-03 12:15:00', 1.00, 'EFECTIVO',      'COMPLETADA', NULL, NULL),
(1021, 1005, 103, TIMESTAMP '2026-11-03 19:05:00', 1.00, 'EFECTIVO',      'COMPLETADA', NULL, NULL),
(1022,    2,   2, TIMESTAMP '2026-11-04 10:30:00', 1.00, 'EFECTIVO',      'COMPLETADA', NULL, NULL);

-- --------------------------------------------------------
-- 8) Detalles de venta (FEFO: primer lote demo vigente)
-- --------------------------------------------------------
INSERT INTO detalle_venta (venta_id, producto_id, lote_id, cantidad, precio_unitario, subtotal)
SELECT d.venta_id, p.id, l.id, d.cantidad, p.precio_venta,
       ROUND(p.precio_venta * d.cantidad, 2)
FROM (VALUES
    (1000, '75010001', 2), (1000, '75010014', 1),
    (1001, '75010008', 1), (1001, '75010001', 1),
    (1002, '75020004', 2), (1002, '75020007', 1),
    (1003, '75010007', 1), (1003, '75010002', 1),
    (1004, '75010020', 1), (1004, '75010021', 1),
    (1005, '75020012', 1), (1005, '75020004', 1),
    (1006, '75010026', 1),
    (1007, '75020003', 2), (1007, '75020005', 1),
    (1008, '75010001', 3), (1008, '75010016', 1),
    (1009, '75020011', 1), (1009, '75020010', 2),
    (1010, '75010006', 1), (1010, '75010007', 1),
    (1011, '75010014', 2), (1011, '75010015', 1),
    (1012, '75010009', 1), (1012, '75010001', 1),
    (1013, '75010012', 1),
    (1014, '75010025', 1), (1014, '75010023', 1),
    (1015, '75020013', 1), (1015, '75020009', 2),
    (1016, '75010002', 2), (1016, '75010004', 1),
    (1017, '75020008', 1), (1017, '75020007', 1),
    (1018, '75010004', 1), (1018, '75010005', 1),
    (1019, '75020001', 2), (1019, '75020002', 1),
    (1020, '75010019', 1), (1020, '75010018', 1),
    (1021, '75010027', 2), (1021, '75010028', 1),
    (1022, '75020003', 1), (1022, '75020004', 1)
) AS d(venta_id, codigo, cantidad)
JOIN producto p ON p.codigo_barras = d.codigo
JOIN LATERAL (
    SELECT l.id
    FROM lote l
    WHERE l.producto_id = p.id
      AND l.id >= 1000
      AND l.cantidad_actual > 0
      AND l.fecha_caducidad >= CURRENT_DATE
    ORDER BY l.fecha_caducidad ASC, l.id ASC
    LIMIT 1
) l ON TRUE;

-- Totales reales a partir de los detalles
UPDATE venta v
SET total = COALESCE((SELECT SUM(dv.subtotal) FROM detalle_venta dv WHERE dv.venta_id = v.id), 0)
WHERE v.id >= 1000;

-- Descuento de existencias en lotes demo (solo ventas completadas)
UPDATE lote l
SET cantidad_actual = GREATEST(l.cantidad_actual - COALESCE((
        SELECT SUM(dv.cantidad)
        FROM detalle_venta dv
        JOIN venta v ON v.id = dv.venta_id
        WHERE dv.lote_id = l.id AND v.estado = 'COMPLETADA'
    ), 0), 0)
WHERE l.id >= 1000;

-- --------------------------------------------------------
-- 9) Recetas medicas (vigentes y vencidas)
-- --------------------------------------------------------
INSERT INTO receta_venta (venta_id, producto_id, nombre_medico, cedula_profesional, fecha_expedicion, folio_receta)
SELECT r.venta_id, p.id, r.medico, r.cedula, CURRENT_DATE + r.dias, r.folio
FROM (VALUES
    (1001, '75010008', 'Dr. Hugo Martinez Salas',   'CED-8845123',  -5, 'REC-2026-0001'),
    (1003, '75010007', 'Dra. Paula Jimenez Rios',   'CED-1122334',  -2, 'REC-2026-0002'),
    (1005, '75020012', 'Dra. Elena Vargas Ruiz',    'CED-7712345', -10, 'REC-2026-0003'),
    (1007, '75020003', 'Dr. Pablo Ortiz Mena',      'CED-6612789', -40, 'REC-2026-0004'),
    (1012, '75010009', 'Dra. Lucia Fernandez Paz',  'CED-5543210',  -3, 'REC-2026-0005'),
    (1015, '75020013', 'Dr. Andres Molina Cruz',    'CED-9988776', -25, 'REC-2026-0006'),
    (1017, '75020008', 'Dra. Monica Salas Duarte',  'CED-4455667', -45, 'REC-2026-0007'),
    (1018, '75010004', 'Dr. Raul Dominguez Soto',   'CED-3322110',  -1, 'REC-2026-0008'),
    (1022, '75020003', 'Dra. Elena Vargas Ruiz',    'CED-7712345',  -2, 'REC-2026-0009')
) AS r(venta_id, codigo, medico, cedula, dias, folio)
JOIN producto p ON p.codigo_barras = r.codigo;

-- --------------------------------------------------------
-- 10) Movimientos de inventario
-- --------------------------------------------------------
-- Recepciones de todos los lotes demo
INSERT INTO movimiento_inventario (lote_id, tipo, cantidad, motivo, referencia_id, usuario_id, fecha_hora)
SELECT l.id, 'RECEPCION', l.cantidad_recibida,
       'Recepción de partida de prueba [demo]', l.referencia_factura, 101,
       (l.fecha_recepcion + TIME '09:00')::timestamp
FROM lote l
WHERE l.id >= 1000;

-- Salidas por venta (solo ventas completadas)
INSERT INTO movimiento_inventario (lote_id, tipo, cantidad, motivo, referencia_id, usuario_id, fecha_hora)
SELECT dv.lote_id, 'VENTA', -dv.cantidad,
       'Salida por venta #' || dv.venta_id || ' [demo]', 'VENTA-' || dv.venta_id,
       v.usuario_id, v.fecha_hora
FROM detalle_venta dv
JOIN venta v ON v.id = dv.venta_id
WHERE dv.venta_id >= 1000 AND v.estado = 'COMPLETADA';

-- Reintegros por cancelacion
INSERT INTO movimiento_inventario (lote_id, tipo, cantidad, motivo, referencia_id, usuario_id, fecha_hora)
SELECT dv.lote_id, 'CANCELACION', dv.cantidad,
       'Reintegro por cancelación de venta #' || dv.venta_id || ' [demo]', 'CANCEL-' || dv.venta_id,
       v.usuario_id, COALESCE(v.cancelada_en, v.fecha_hora)
FROM detalle_venta dv
JOIN venta v ON v.id = dv.venta_id
WHERE dv.venta_id >= 1000 AND v.estado = 'CANCELADA';

-- Ajustes y devolucion
INSERT INTO movimiento_inventario (lote_id, tipo, cantidad, motivo, referencia_id, usuario_id, fecha_hora) VALUES
(1035, 'AJUSTE',     -2, 'Merma por empaque dañado [demo]',          'AJUSTE-001', 101, TIMESTAMP '2026-10-29 10:15:00'),
(1014, 'AJUSTE',     -1, 'Conteo físico: faltante de guantes [demo]','AJUSTE-002', 101, TIMESTAMP '2026-11-02 17:40:00'),
(1023, 'DEVOLUCION',  3, 'Devolución de cliente: producto sin usar [demo]', 'DEV-001', 103, TIMESTAMP '2026-11-03 16:20:00');

-- --------------------------------------------------------
-- 11) Cierre de turnos demo (monto esperado y diferencias)
-- --------------------------------------------------------
UPDATE turno_caja t
SET monto_esperado = ROUND(t.monto_inicial + COALESCE((
        SELECT SUM(v.total)
        FROM venta v
        WHERE v.turno_id = t.id AND v.estado = 'COMPLETADA' AND v.forma_pago = 'EFECTIVO'
    ), 0), 2)
WHERE t.id BETWEEN 1000 AND 1005;

UPDATE turno_caja t
SET efectivo_contado = ROUND(t.monto_esperado + d.delta, 2),
    diferencia       = d.delta,
    fecha_cierre     = d.cierre
FROM (VALUES
    (1000,  15.00, TIMESTAMP '2026-10-20 20:00:00'),
    (1001,  -8.50, TIMESTAMP '2026-10-22 20:10:00'),
    (1002,   0.00, TIMESTAMP '2026-10-25 19:50:00'),
    (1003,   3.20, TIMESTAMP '2026-10-28 20:05:00'),
    (1004, -12.00, TIMESTAMP '2026-11-01 18:30:00'),
    (1005,   0.00, TIMESTAMP '2026-11-03 20:00:00')
) AS d(id, delta, cierre)
WHERE t.id = d.id;

-- --------------------------------------------------------
-- 12) Bitacora
-- --------------------------------------------------------
INSERT INTO bitacora (id, usuario_id, accion, descripcion, fecha_hora, ip) VALUES
(1000, 101, 'LOGIN',                'Inicio de sesión exitoso como ENCARGADO',                 TIMESTAMP '2026-10-20 07:55:00', '127.0.0.1'),
(1001, 101, 'CAJA_APERTURA',        'Apertura de Turno #1000 con fondo de $500.00',            TIMESTAMP '2026-10-20 08:00:00', '127.0.0.1'),
(1002, 101, 'VENTA',                'Venta #1000 registrada por $113.00 (EFECTIVO)',           TIMESTAMP '2026-10-20 09:15:00', '127.0.0.1'),
(1003, 101, 'CAJA_CORTE',           'Cierre de Turno #1000 - Diferencia: $15.00 (SOBRANTE)',   TIMESTAMP '2026-10-20 20:00:00', '127.0.0.1'),
(1004, 102, 'LOGIN',                'Inicio de sesión exitoso como EMPLEADO',                  TIMESTAMP '2026-10-22 07:50:00', '127.0.0.1'),
(1005, 102, 'VENTA_CANCELACION',    'Cancelación de venta #1013: Error de captura',            TIMESTAMP '2026-10-28 15:45:00', '127.0.0.1'),
(1006, 101, 'PRODUCTO_ALTA',        'Alta de medicamento 75020003 - Betametasona Crema',       TIMESTAMP '2026-10-30 11:20:00', '127.0.0.1'),
(1007, 101, 'PRODUCTO_EDICION',     'Edición de medicamento #1002 [75020003]',                 TIMESTAMP '2026-10-30 11:35:00', '127.0.0.1'),
(1008, 101, 'INVENTARIO_RECEPCION', 'Recepción de 40 partidas de prueba',                      TIMESTAMP '2026-10-31 09:10:00', '127.0.0.1'),
(1009, 101, 'INVENTARIO_AJUSTE',    'Ajuste por merma en lote LOTE-DEMO-LOR-PV',               TIMESTAMP '2026-10-31 10:15:00', '127.0.0.1'),
(1010, 103, 'LOGIN',                'Inicio de sesión exitoso como EMPLEADO',                  TIMESTAMP '2026-11-03 08:30:00', '127.0.0.1'),
(1011, 103, 'RESPALDO_BD',          'Respaldo manual de la base de datos generado',            TIMESTAMP '2026-11-03 20:30:00', '127.0.0.1');

-- --------------------------------------------------------
-- 13) Sincronizar secuencias con los IDs insertados
-- --------------------------------------------------------
SELECT setval('usuario_id_seq',              (SELECT MAX(id) FROM usuario));
SELECT setval('categoria_id_seq',            (SELECT MAX(id) FROM categoria));
SELECT setval('proveedor_id_seq',            (SELECT MAX(id) FROM proveedor));
SELECT setval('producto_id_seq',             (SELECT MAX(id) FROM producto));
SELECT setval('lote_id_seq',                 (SELECT MAX(id) FROM lote));
SELECT setval('turno_caja_id_seq',           (SELECT MAX(id) FROM turno_caja));
SELECT setval('venta_id_seq',                (SELECT MAX(id) FROM venta));
SELECT setval('bitacora_id_seq',             (SELECT MAX(id) FROM bitacora));
SELECT setval('detalle_venta_id_seq',        (SELECT MAX(id) FROM detalle_venta));
SELECT setval('receta_venta_id_seq',         (SELECT MAX(id) FROM receta_venta));
SELECT setval('movimiento_inventario_id_seq',(SELECT MAX(id) FROM movimiento_inventario));

COMMIT;

-- --------------------------------------------------------
-- Resumen
-- --------------------------------------------------------
SELECT 'usuario' AS tabla, COUNT(*) AS filas FROM usuario
UNION ALL SELECT 'categoria', COUNT(*) FROM categoria
UNION ALL SELECT 'proveedor', COUNT(*) FROM proveedor
UNION ALL SELECT 'producto', COUNT(*) FROM producto
UNION ALL SELECT 'lote', COUNT(*) FROM lote
UNION ALL SELECT 'movimiento_inventario', COUNT(*) FROM movimiento_inventario
UNION ALL SELECT 'turno_caja', COUNT(*) FROM turno_caja
UNION ALL SELECT 'venta', COUNT(*) FROM venta
UNION ALL SELECT 'detalle_venta', COUNT(*) FROM detalle_venta
UNION ALL SELECT 'receta_venta', COUNT(*) FROM receta_venta
UNION ALL SELECT 'bitacora', COUNT(*) FROM bitacora
ORDER BY tabla;
