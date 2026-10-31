-- ========================================================
-- Datos demo adicionales: turnos cerrados y venta histórica
-- ========================================================

INSERT INTO turno_caja (usuario_id, fecha_apertura, monto_inicial, fecha_cierre, efectivo_contado, monto_esperado, diferencia, estado)
VALUES (1, CURRENT_TIMESTAMP - INTERVAL '1 day', 500.00, CURRENT_TIMESTAMP - INTERVAL '12 hours', 1450.00, 1450.00, 0.00, 'CERRADO');

INSERT INTO venta (turno_id, usuario_id, fecha_hora, total, forma_pago, estado)
VALUES ((SELECT id FROM turno_caja WHERE estado='CERRADO' ORDER BY id DESC LIMIT 1), 1, CURRENT_TIMESTAMP - INTERVAL '18 hours', 120.00, 'EFECTIVO', 'COMPLETADA');

INSERT INTO detalle_venta (venta_id, producto_id, lote_id, cantidad, precio_unitario, subtotal)
SELECT v.id, p.id, l.id, 2, 60.00, 120.00
FROM venta v
JOIN producto p ON p.codigo_barras='75010001'
JOIN lote l ON l.numero_lote='LOTE-PAR-2027A'
WHERE v.estado='COMPLETADA'
ORDER BY v.id DESC LIMIT 1;
