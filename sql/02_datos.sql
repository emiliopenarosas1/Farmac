-- ========================================================
-- Datos iniciales: Farmac
-- Passwords iniciales con BCrypt (verificados):
-- admin / admin123
-- empleado1 / caja123
-- Los scripts son idempotentes: pueden ejecutarse mas de una vez.
-- ========================================================

-- Usuarios (1 ENCARGADO, 1 EMPLEADO)
INSERT INTO usuario (nombre_usuario, nombre_completo, password_hash, rol, activo) VALUES
('admin', 'Carlos Ramirez (Encargado)', '$2a$10$fm2dGYB.bd80nsqMQ00waePHAiyCiZdOj8RlXKOVT3uzR7N/SX5/O', 'ENCARGADO', TRUE),
('empleado1', 'Laura Gomez (Empleado)', '$2a$10$0UvIkCceCpYYbajeb42PVO8NyJznYD/lw1PpgcMOlJhdmpISMnjym', 'EMPLEADO', TRUE)
ON CONFLICT (nombre_usuario) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    rol = EXCLUDED.rol,
    activo = EXCLUDED.activo;

-- Proveedores (3 proveedores) - se evita duplicar por razon_social
INSERT INTO proveedor (razon_social, contacto, telefono, activo)
SELECT v.razon_social, v.contacto, v.telefono, TRUE
FROM (VALUES
    ('Distribuidora Farmacéutica Nacional S.A.', 'Roberto Mendez', '555-0101'),
    ('Laboratorios Médicos del Centro', 'Patricia Valenzuela', '555-0102'),
    ('Suministros Hospitalarios y Genéricos', 'Esteban Silva', '555-0103')
) AS v(razon_social, contacto, telefono)
WHERE NOT EXISTS (
    SELECT 1 FROM proveedor p WHERE p.razon_social = v.razon_social
);

-- Categorías (5 categorías)
INSERT INTO categoria (nombre) VALUES
('Analgésicos y Antiinflamatorios'),
('Antibióticos y Antivirales'),
('Salud Digestiva'),
('Cardiovascular y Presión'),
('Cuidado Personal y Primeros Auxilios')
ON CONFLICT (nombre) DO NOTHING;

-- Productos (~30 productos)
-- Se resuelven categoría y proveedor por nombre para no depender de IDs fijos.
INSERT INTO producto (codigo_barras, nombre, categoria_id, proveedor_id, precio_venta, requiere_receta, stock_minimo, activo)
SELECT v.codigo_barras, v.nombre, c.id, pr.id, v.precio_venta, v.requiere_receta, v.stock_minimo, TRUE
FROM (VALUES
    ('75010001', 'Paracetamol 500mg (20 tabletas)', 'Analgésicos y Antiinflamatorios', 'Distribuidora Farmacéutica Nacional S.A.', 35.50, FALSE, 10),
    ('75010002', 'Ibuprofeno 400mg (10 cápsulas)', 'Analgésicos y Antiinflamatorios', 'Distribuidora Farmacéutica Nacional S.A.', 48.00, FALSE, 8),
    ('75010003', 'Naproxeno 250mg (20 tabletas)', 'Analgésicos y Antiinflamatorios', 'Laboratorios Médicos del Centro', 62.00, FALSE, 6),
    ('75010004', 'Ketorolaco 10mg (10 tabletas sublinguales)', 'Analgésicos y Antiinflamatorios', 'Laboratorios Médicos del Centro', 75.00, TRUE, 5),
    ('75010005', 'Diclofenaco 100mg Liberación Prolongada', 'Analgésicos y Antiinflamatorios', 'Distribuidora Farmacéutica Nacional S.A.', 85.00, FALSE, 6),
    ('75010006', 'Aspirina Protect 100mg (28 tabletas)', 'Analgésicos y Antiinflamatorios', 'Suministros Hospitalarios y Genéricos', 95.00, FALSE, 12),
    ('75010007', 'Celecoxib 200mg (10 cápsulas)', 'Analgésicos y Antiinflamatorios', 'Laboratorios Médicos del Centro', 140.00, TRUE, 4),

    ('75010008', 'Amoxicilina 500mg (12 cápsulas)', 'Antibióticos y Antivirales', 'Distribuidora Farmacéutica Nacional S.A.', 85.00, TRUE, 8),
    ('75010009', 'Azitromicina 500mg (3 tabletas)', 'Antibióticos y Antivirales', 'Distribuidora Farmacéutica Nacional S.A.', 120.00, TRUE, 5),
    ('75010010', 'Ciprofloxacino 500mg (14 tabletas)', 'Antibióticos y Antivirales', 'Laboratorios Médicos del Centro', 110.00, TRUE, 6),
    ('75010011', 'Cefalexina 500mg (20 cápsulas)', 'Antibióticos y Antivirales', 'Laboratorios Médicos del Centro', 98.00, TRUE, 6),
    ('75010012', 'Aciclovir 400mg (35 tabletas)', 'Antibióticos y Antivirales', 'Suministros Hospitalarios y Genéricos', 160.00, TRUE, 4),
    ('75010013', 'Trimetoprima y Sulfametoxazol 80/400mg', 'Antibióticos y Antivirales', 'Suministros Hospitalarios y Genéricos', 65.00, TRUE, 5),

    ('75010014', 'Omeprazol 20mg (14 cápsulas)', 'Salud Digestiva', 'Distribuidora Farmacéutica Nacional S.A.', 42.00, FALSE, 15),
    ('75010015', 'Pantoprazol 40mg (7 tabletas)', 'Salud Digestiva', 'Laboratorios Médicos del Centro', 115.00, FALSE, 8),
    ('75010016', 'Ranitidina Genérica 150mg', 'Salud Digestiva', 'Distribuidora Farmacéutica Nacional S.A.', 30.00, FALSE, 5),
    ('75010017', 'Subsalicilato de Bismuto 262mg Suspensión', 'Salud Digestiva', 'Suministros Hospitalarios y Genéricos', 58.00, FALSE, 7),
    ('75010018', 'Loperamida 2mg (12 tabletas)', 'Salud Digestiva', 'Distribuidora Farmacéutica Nacional S.A.', 25.00, FALSE, 10),
    ('75010019', 'Metoclopramida 10mg (20 tabletas)', 'Salud Digestiva', 'Laboratorios Médicos del Centro', 38.00, FALSE, 6),

    ('75010020', 'Losartán 50mg (30 grageas)', 'Cardiovascular y Presión', 'Distribuidora Farmacéutica Nacional S.A.', 75.00, TRUE, 12),
    ('75010021', 'Enalapril 10mg (30 tabletas)', 'Cardiovascular y Presión', 'Laboratorios Médicos del Centro', 50.00, TRUE, 10),
    ('75010022', 'Amlodipino 5mg (30 tabletas)', 'Cardiovascular y Presión', 'Distribuidora Farmacéutica Nacional S.A.', 68.00, TRUE, 8),
    ('75010023', 'Metoprolol 100mg (20 tabletas)', 'Cardiovascular y Presión', 'Laboratorios Médicos del Centro', 82.00, TRUE, 6),
    ('75010024', 'Atorvastatina 20mg (30 tabletas)', 'Cardiovascular y Presión', 'Suministros Hospitalarios y Genéricos', 130.00, TRUE, 10),
    ('75010025', 'Hidroclorotiazida 25mg (30 tabletas)', 'Cardiovascular y Presión', 'Distribuidora Farmacéutica Nacional S.A.', 45.00, TRUE, 7),

    ('75010026', 'Alcohol Etílico 70% Desnaturalizado 500ml', 'Cuidado Personal y Primeros Auxilios', 'Suministros Hospitalarios y Genéricos', 32.00, FALSE, 15),
    ('75010027', 'Gasas Esterilizadas 10x10cm (10 piezas)', 'Cuidado Personal y Primeros Auxilios', 'Suministros Hospitalarios y Genéricos', 22.00, FALSE, 20),
    ('75010028', 'Venda Elástica 10cm x 5m', 'Cuidado Personal y Primeros Auxilios', 'Suministros Hospitalarios y Genéricos', 28.00, FALSE, 10),
    ('75010029', 'Solución Antiséptica Iodopovidona 120ml', 'Cuidado Personal y Primeros Auxilios', 'Suministros Hospitalarios y Genéricos', 49.00, FALSE, 8),
    ('75010030', 'Termómetro Clínico Digital Infrarrojo', 'Cuidado Personal y Primeros Auxilios', 'Laboratorios Médicos del Centro', 195.00, FALSE, 4)
) AS v(codigo_barras, nombre, categoria, proveedor, precio_venta, requiere_receta, stock_minimo)
JOIN categoria c ON c.nombre = v.categoria
JOIN proveedor pr ON pr.razon_social = v.proveedor
ON CONFLICT (codigo_barras) DO NOTHING;
