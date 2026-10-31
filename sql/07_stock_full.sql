-- ========================================================
-- Datos demo: stock para todos los productos
-- ========================================================
INSERT INTO lote (producto_id, numero_lote, fecha_caducidad, cantidad_recibida, cantidad_actual, precio_compra, proveedor_id, fecha_recepcion, referencia_factura)
SELECT p.id, 'LOTE-' || p.codigo_barras || '-FULL', CURRENT_DATE + 120, 40, 40, 10.00, 1, CURRENT_DATE, 'FACT-FULL'
FROM producto p
ON CONFLICT (producto_id, numero_lote) DO NOTHING;

INSERT INTO movimiento_inventario (lote_id, tipo, cantidad, motivo, referencia_id, usuario_id, fecha_hora)
SELECT l.id, 'RECEPCION', l.cantidad_recibida, 'Inyeccion full', l.referencia_factura, 1, CURRENT_TIMESTAMP
FROM lote l
WHERE l.referencia_factura='FACT-FULL'
AND NOT EXISTS (SELECT 1 FROM movimiento_inventario mi WHERE mi.lote_id=l.id AND mi.tipo='RECEPCION');
